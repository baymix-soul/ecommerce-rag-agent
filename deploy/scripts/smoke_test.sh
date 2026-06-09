#!/bin/bash
# Smoke Test Script - Linux/macOS
# Usage: ./smoke_test.sh https://api.your-domain.com
#    or: ./smoke_test.sh http://localhost:8080

BASE_URL="${1:-http://localhost:8080}"
PASSED=0
FAILED=0

echo "========================================"
echo "  E-Commerce RAG Agent - Smoke Test"
echo "  Target: $BASE_URL"
echo "========================================"
echo ""

test_endpoint() {
    local name="$1"
    local method="$2"
    local path="$3"
    local validator="$4"
    local body="$5"

    echo "[TEST] $name"
    echo "  $method $BASE_URL$path"

    local http_code
    local response

    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$path" \
            -H "Content-Type: application/json" \
            -d "$body" 2>&1)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$path" 2>&1)
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        local validation_result
        validation_result=$(echo "$body" | eval "$validator" 2>&1)
        if [ $? -eq 0 ] && [ -n "$validation_result" ]; then
            echo "  PASS: $validation_result"
            PASSED=$((PASSED + 1))
        else
            echo "  FAIL: validation failed (HTTP $http_code)"
            echo "  Response (first 500 chars): $(echo "$body" | head -c 500)"
            FAILED=$((FAILED + 1))
        fi
    else
        echo "  FAIL: HTTP $http_code"
        echo "  Response (first 500 chars): $(echo "$body" | head -c 500)"
        FAILED=$((FAILED + 1))
    fi
    echo ""
}

# 1. Health Check
test_endpoint "Health Check" "GET" "/api/health" \
    'python3 -c "import sys,json; d=json.load(sys.stdin); print(\"ok\" if d.get(\"status\")==\"ok\" else \"\")"'

# 2. Product List
test_endpoint "Product List" "GET" "/api/products?limit=3" \
    'python3 -c "import sys,json; d=json.load(sys.stdin); print(\"ok\" if isinstance(d,list) and len(d)>0 and \"product_id\" in d[0] else \"\")"'

# 3. Vector Index Stats
test_endpoint "Vector Index Stats" "GET" "/api/rag/vector-index/stats" \
    'python3 -c "import sys,json; d=json.load(sys.stdin); print(\"ok\" if \"count\" in d and \"embedding_model\" in d else \"\")"'

# 4. Retrieval Debug
test_endpoint "Retrieval Debug" "GET" "/api/rag/retrieval/debug?query=%E6%8E%A8%E8%8D%90%E5%87%A0%E6%AC%BE%E8%B7%91%E9%9E%8B&limit=3" \
    'python3 -c "import sys,json; d=json.load(sys.stdin); print(\"ok\" if \"total\" in d and \"query_analysis\" in d else \"\")"'

# 5. SSE Chat Stream
echo "[TEST] SSE Chat Stream"
echo "  POST $BASE_URL/api/chat/stream"

SSE_BODY='{"message":"推荐一款跑鞋","session_id":"deploy-smoke-1","limit":3}'
SSE_RESPONSE=$(curl -s -N -X POST "$BASE_URL/api/chat/stream" \
    -H "Content-Type: application/json" \
    -d "$SSE_BODY" \
    --max-time 30 2>&1)

if echo "$SSE_RESPONSE" | grep -q "event:done"; then
    HAS_TEXT=$(echo "$SSE_RESPONSE" | grep -c "event:text" || true)
    HAS_CARD=$(echo "$SSE_RESPONSE" | grep -c "event:product_card" || true)
    HAS_ERROR=$(echo "$SSE_RESPONSE" | grep -c "event:error" || true)
    if [ "$HAS_ERROR" -eq 0 ]; then
        echo "  PASS: SSE stream completed (text=$HAS_TEXT, product_card=$HAS_CARD, done=yes, error=no)"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: SSE stream returned error event"
        echo "  Response (first 500 chars): $(echo "$SSE_RESPONSE" | head -c 500)"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  FAIL: SSE stream did not complete (no event:done)"
    echo "  Response (first 500 chars): $(echo "$SSE_RESPONSE" | head -c 500)"
    FAILED=$((FAILED + 1))
fi
echo ""

# 6. Auth Login
test_endpoint "Auth Login" "POST" "/api/auth/login" \
    'python3 -c "import sys,json; d=json.load(sys.stdin); print(\"ok\" if \"access_token\" in d and d.get(\"token_type\")==\"Bearer\" else \"\")"' \
    '{"username":"demo","password":"demo123"}'

# 7. Get Cart (unauthenticated)
echo "[TEST] Get Cart (unauthenticated → 401)"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/cart")
if [ "$HTTP_CODE" = "401" ]; then
    echo "  PASS: GET /api/cart returned 401 for unauthenticated request"
    PASSED=$((PASSED + 1))
else
    echo "  FAIL: Expected 401, got HTTP $HTTP_CODE"
    FAILED=$((FAILED + 1))
fi
echo ""

# 8. Get Cart (authenticated)
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo","password":"demo123"}')
TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)

if [ -n "$TOKEN" ]; then
    CART_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/cart")
    HAS_ITEMS=$(echo "$CART_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('ok' if 'items' in d and 'total_quantity' in d else '')" 2>/dev/null)
    if [ "$HAS_ITEMS" = "ok" ]; then
        echo "  PASS: Authenticated cart access successful"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: Cart response invalid"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  FAIL: Could not get auth token"
    FAILED=$((FAILED + 1))
fi
echo ""

# 9. TTS Speak
echo "[TEST] TTS Speak"
echo "  POST $BASE_URL/api/tts/speak"

TTS_BODY='{"text":"你好，我是智能导购助手","voice":"zh-CN-XiaoxiaoNeural","format":"mp3"}'
TTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/tts/speak" \
    -H "Content-Type: application/json" \
    -d "$TTS_BODY" 2>&1)

TTS_HTTP_CODE=$(echo "$TTS_RESPONSE" | tail -n1)
TTS_BODY_RESP=$(echo "$TTS_RESPONSE" | sed '$d')

if [ "$TTS_HTTP_CODE" -ge 200 ] && [ "$TTS_HTTP_CODE" -lt 300 ]; then
    HAS_AUDIO=$(echo "$TTS_BODY_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print('ok' if 'audio_url' in d or 'audio_base64' in d else '')" 2>/dev/null)
    if [ "$HAS_AUDIO" = "ok" ]; then
        echo "  PASS: TTS returned audio"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: TTS response missing audio_url/audio_base64"
        echo "  Response (first 500 chars): $(echo "$TTS_BODY_RESP" | head -c 500)"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  WARN: TTS returned HTTP $TTS_HTTP_CODE (TTS is optional, not counted as failure)"
    # TTS 是可选功能，不计入失败
fi
echo ""

# Summary
echo "========================================"
echo "  Results: $PASSED passed, $FAILED failed"
echo "========================================"

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
