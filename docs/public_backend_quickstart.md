# 公网后端快速开始指南

> 本文档说明如何在公网服务器部署后端服务，供评委通过 APK 直接体验。

---

## 1. 服务器要求

| 资源 | 最低 | 推荐 |
|------|------|------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 40 GB (含 Qdrant 持久化) |
| 系统 | Ubuntu 20.04+ / Debian 11+ | Ubuntu 22.04 |
| 网络 | 公网 IP | 公网 IP + 域名 |

需要开放端口：

- `8080/tcp` — 后端服务（或 `80/443` 通过 Nginx 反代）

不建议公网开放：

- `6379` — Redis
- `6333` — Qdrant REST
- `6334` — Qdrant gRPC

---

## 2. 上传项目

```bash
git clone <REPO_URL>
cd ecommerce-rag-agent
```

> `<REPO_URL>` 为项目仓库地址，请替换为实际地址。

---

## 3. 配置环境变量

```bash
cd deploy
cp .env.production.example .env
nano .env
```

### 关键字段说明

```env
SERVER_PORT=8080

# Vector store
VECTOR_STORE=qdrant
QDRANT_URL=http://qdrant:6333
QDRANT_COLLECTION=ecommerce_rag_chunks_prod
QDRANT_VECTOR_SIZE=2048

# Redis
REDIS_ENABLED=true
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# Auth
DEMO_USERNAME=demo
DEMO_PASSWORD=demo123
AUTH_TOKEN_TTL_SECONDS=604800

# LLM / Embedding
MOCK_LLM_ENABLED=false
LLM_PROVIDER=doubao
DOUBAO_API_KEY=your_api_key_here
DOUBAO_MODEL=doubao-seed-2-0-lite-260428
EMBEDDING_PROVIDER=doubao
DOUBAO_EMBEDDING_MODEL=doubao-embedding-vision-251215

# TTS
TTS_ENABLED=true
TTS_PROVIDER=edge-tts-cli
TTS_AUDIO_DIR=/app/data/tts
# 必须替换为真实公网 IP 或域名
TTS_PUBLIC_BASE_URL=http://<PUBLIC_IP>:8080
TTS_DEFAULT_VOICE=zh-CN-XiaoxiaoNeural
```

### 重要说明

1. `<PUBLIC_IP>` 必须替换为真实公网 IP。
2. 如果有域名和 HTTPS：

   ```env
   TTS_PUBLIC_BASE_URL=https://api.example.com
   ```
3. **不要把真实 `.env` 提交到 Git**。
4. `EMBEDDING_DIMENSION` 必须等于 `QDRANT_VECTOR_SIZE`。

---

## 4. 启动服务

```bash
docker compose up -d --build
```

查看日志：

```bash
docker compose logs -f backend
```

---

## 5. 检查服务

```bash
curl http://<PUBLIC_IP>:8080/api/health
```

预期响应：

```json
{"status":"ok","service":"ecommerce-rag-agent","version":"0.1.0"}
```

---

## 6. 重建 RAG 索引

```bash
curl -X POST http://<PUBLIC_IP>:8080/api/rag/vector-index/rebuild
```

检查重建结果：

```bash
curl http://<PUBLIC_IP>:8080/api/rag/vector-index/stats
```

预期：

```json
{"count":650,"embedding_model":"doubao-embedding-vision-251215","dimension":2048}
```

> `count > 0` 表示索引已建立。维度取决于 embedding 配置。

---

## 7. 公网后端冒烟测试

### 7.1 使用脚本

```bash
BASE_URL=http://<PUBLIC_IP>:8080 bash deploy/scripts/public_smoke_test.sh
```

### 7.2 手动测试

#### Health

```bash
curl http://<PUBLIC_IP>:8080/api/health
```

#### 登录

```bash
curl -X POST http://<PUBLIC_IP>:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'
```

#### 普通推荐

```bash
curl -N -X POST http://<PUBLIC_IP>:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"推荐几款跑鞋","session_id":"public-demo-1","limit":3}'
```

#### TTS

```bash
curl -X POST http://<PUBLIC_IP>:8080/api/tts/speak \
  -H "Content-Type: application/json" \
  -d '{"text":"你好，我是你的智能导购助手。","voice":"zh-CN-XiaoxiaoNeural","format":"mp3"}'
```

响应中的 `audio_url` 必须是公网可访问地址：

```text
http://<PUBLIC_IP>:8080/api/tts/audio/tts_xxx.mp3
```

#### 购物车（需先登录获取 token）

```bash
TOKEN=$(curl -s -X POST http://<PUBLIC_IP>:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

curl -H "Authorization: Bearer $TOKEN" http://<PUBLIC_IP>:8080/api/cart
```

---

## 8. 防火墙建议

### 使用 ufw（Ubuntu）

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 8080/tcp  # 后端服务
sudo ufw enable
```

### 使用云服务器安全组

| 方向 | 端口 | 来源 | 说明 |
|------|------|------|------|
| 入站 | 22 | 本机 IP | SSH |
| 入站 | 8080 | 0.0.0.0/0 | 后端服务 |
| 入站 | 80 | 0.0.0.0/0 | HTTP（如有 Nginx） |
| 入站 | 443 | 0.0.0.0/0 | HTTPS（如有 Nginx） |

**不要**在安全组中开放：

- `6379` — Redis
- `6333` — Qdrant

---

## 9. 停止服务

```bash
cd deploy
docker compose down
```

如需彻底清理数据：

```bash
docker compose down -v
```

---

> 如有其他问题，请参考 [docs/deployment_mvp.md](deployment_mvp.md) 和 [docs/api_spec.md](api_spec.md)。
