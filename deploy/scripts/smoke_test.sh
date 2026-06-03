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

# Summary
echo "========================================"
echo "  Results: $PASSED passed, $FAILED failed"
echo "========================================"

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
