#!/usr/bin/env bash
set -e

BASE_URL=${BASE_URL:-http://localhost:8080}

echo "========================================"
echo "  RAG Vector Index Rebuild"
echo "  Target: $BASE_URL"
echo "========================================"
echo ""

echo "[1/2] Rebuilding RAG vector index..."
curl -f -X POST "$BASE_URL/api/rag/vector-index/rebuild"

echo ""
echo "[2/2] Vector index stats:"
curl -f "$BASE_URL/api/rag/vector-index/stats"

echo ""
echo "Done."
