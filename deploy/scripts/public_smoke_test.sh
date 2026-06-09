#!/usr/bin/env bash
set -e

BASE_URL=${BASE_URL:-http://localhost:8080}

echo "========================================"
echo "  E-commerce RAG Agent Smoke Test"
echo "  Target: $BASE_URL"
echo "========================================"
echo ""

PASSED=0
FAILED=0

# 1. Health Check
echo "[TEST] Health Check"
echo "  GET $BASE_URL/api/health"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/health" 2>&1)
HEALTH_HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
HEALTH_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HEALTH_HTTP_CODE" = "200" ]; then
    echo "  PASS: Health check OK"
    echo "  Response: $HEALTH_BODY"
    PASSED=$((PASSED + 1))
else
    echo "  FAIL: Health check failed with HTTP $HEALTH_HTTP_CODE"
    echo "  Response: $HEALTH_BODY"
    FAILED=$((FAILED + 1))
fi
echo ""

# 2. Auth Login
echo "[TEST] Auth Login"
echo "  POST $BASE_URL/api/auth/login"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo","password":"demo123"}' 2>&1)
LOGIN_HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$LOGIN_HTTP_CODE" = "200" ]; then
    ACCESS_TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
    if [ -n "$ACCESS_TOKEN" ]; then
        echo "  PASS: Login successful, token received"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: Login response missing access_token"
        echo "  Response: $LOGIN_BODY"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  FAIL: Login failed with HTTP $LOGIN_HTTP_CODE"
    echo "  Response: $LOGIN_BODY"
    FAILED=$((FAILED + 1))
fi
echo ""

# 3. Vector Index Stats
echo "[TEST] Vector Index Stats"
echo "  GET $BASE_URL/api/rag/vector-index/stats"
STATS_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/rag/vector-index/stats" 2>&1)
STATS_HTTP_CODE=$(echo "$STATS_RESPONSE" | tail -n1)
STATS_BODY=$(echo "$STATS_RESPONSE" | sed '$d')

if [ "$STATS_HTTP_CODE" = "200" ]; then
    COUNT=$(echo "$STATS_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])" 2>/dev/null)
    if [ -n "$COUNT" ] && [ "$COUNT" -gt 0 ]; then
        echo "  PASS: Vector index has $COUNT documents"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: Vector index count is 0 or missing"
        echo "  Response: $STATS_BODY"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  FAIL: Vector stats failed with HTTP $STATS_HTTP_CODE"
    echo "  Response: $STATS_BODY"
    FAILED=$((FAILED + 1))
fi
echo ""

# 4. Chat Stream
echo "[TEST] Chat Stream"
echo "  POST $BASE_URL/api/chat/stream"
CHAT_BODY='{"message":"推荐几款跑鞋","session_id":"smoke-test-1","limit":3}'
CHAT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/chat/stream" \
    -H "Content-Type: application/json" \
    -H "Accept: text/event-stream" \
    -d "$CHAT_BODY" 2>&1)

CHAT_HTTP_CODE=$(echo "$CHAT_RESPONSE" | tail -n1)
CHAT_BODY_RESP=$(echo "$CHAT_RESPONSE" | sed '$d')

if [ "$CHAT_HTTP_CODE" = "200" ]; then
    HAS_DONE=$(echo "$CHAT_BODY_RESP" | grep -c "event:done" || true)
    if [ "$HAS_DONE" -ge 1 ]; then
        echo "  PASS: Chat stream returned done event"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: Chat stream missing done event"
        echo "  Response (first 500 chars): $(echo "$CHAT_BODY_RESP" | head -c 500)"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  FAIL: Chat stream failed with HTTP $CHAT_HTTP_CODE"
    echo "  Response (first 500 chars): $(echo "$CHAT_BODY_RESP" | head -c 500)"
    FAILED=$((FAILED + 1))
fi
echo ""

# 5. TTS Speak
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
fi
echo ""

# 6. Cart Get with Token
echo "[TEST] Cart Get (Authenticated)"
echo "  GET $BASE_URL/api/cart"
if [ -n "$ACCESS_TOKEN" ]; then
    CART_RESPONSE=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$BASE_URL/api/cart" 2>&1)
    CART_HTTP_CODE=$(echo "$CART_RESPONSE" | tail -n1)
    CART_BODY=$(echo "$CART_RESPONSE" | sed '$d')

    if [ "$CART_HTTP_CODE" = "200" ]; then
        echo "  PASS: Cart retrieved successfully"
        echo "  Response: $CART_BODY"
        PASSED=$((PASSED + 1))
    else
        echo "  FAIL: Cart failed with HTTP $CART_HTTP_CODE"
        echo "  Response: $CART_BODY"
        FAILED=$((FAILED + 1))
    fi
else
    echo "  SKIP: No access token available"
fi
echo ""

# Summary
echo "========================================"
echo "  Results: $PASSED passed, $FAILED failed"
echo "========================================"

if [ "$FAILED" -gt 0 ]; then
    exit 1
fi
