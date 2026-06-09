# 最小上线部署文档 (MVP Deployment)

> 本文档适用于 E-Commerce RAG Agent 的最小可访问闭环部署。不引入 Kubernetes、Redis、数据库等复杂架构。

---

## 1. 最小上线架构图

```
┌──────────────────────────────────────────────────────────────┐
│                      Cloud Server (云服务器)                   │
│                                                              │
│  ┌─────────────┐     ┌─────────────┐    ┌───────────┐    ┌───────────┐  │
│  │   Nginx     │────▶│   Backend   │───▶│  Qdrant   │    │   Redis   │  │
│  │  (HTTPS)    │     │  (8080)     │    │  (6333)   │    │  (6379)   │  │
│  │  :443       │     │  app.jar    │    │  storage/  │    │ /data     │  │
│  └──────┬──────┘     └──────┬──────┘    └─────┬─────┘    └─────┬─────┘  │
│         │                   │                  │                │        │
│         │          Docker Compose Network       │                │        │
│         │            (volumes: qdrant_storage, redis_data)      │        │
└─────────┼───────────────────────────────────────────────────────┼────────┘
          │                                                       │
     ┌────▼────┐                                             ┌────▼────┐
     │  HTTPS  │                                             │  Ark    │
     │  Client │                                             │  API    │
     │ (Android│                                             │(LLM/Emb)│
     │  App)   │                                             └─────────┘
     └─────────┘

数据流:
  Android App → HTTPS → Nginx → Backend:8080 → Qdrant:6333
                                              → Redis:6379
                                              → Ark LLM API
                                              → Ark Embedding API
```

---

## 2. 服务器准备

### 最低配置要求

| 资源 | 最低 | 推荐 |
|------|------|------|
| CPU | 2 核 | 4 核 |
| 内存 | 4 GB | 8 GB |
| 磁盘 | 20 GB | 40 GB (含 Qdrant 持久化) |
| 系统 | Ubuntu 20.04+ / Debian 11+ | Ubuntu 22.04 |

### 安装依赖

```bash
# Docker + Docker Compose
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER

# Nginx (如果不由 Docker 管理)
sudo apt-get update
sudo apt-get install -y nginx certbot python3-certbot-nginx

# curl (smoke test)
sudo apt-get install -y curl
```

---

## 3. Docker / Docker Compose 部署步骤

### 3.1 构建并启动

```bash
# 1. 进入项目根目录
cd ecommerce-rag-agent

# 2. 创建生产环境变量文件
cp deploy/.env.production.example deploy/.env.production

# 3. 编辑 deploy/.env.production，填入真实 API Key
#    （绝对不要提交此文件到 Git）
vim deploy/.env.production

# 4. 构建并启动所有服务
docker compose -f deploy/docker-compose.yml up -d --build

# 5. 查看日志
docker compose -f deploy/docker-compose.yml logs -f backend

# 6. 检查服务状态
docker compose -f deploy/docker-compose.yml ps
```

### 3.2 docker-compose 服务说明

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| `backend` | 本地构建 (multi-stage) | 内网 8080 | Spring Boot 应用 |
| `qdrant` | qdrant/qdrant | 内网 6333 | 向量数据库，volume 持久化 |
| `redis` | redis:7-alpine | 内网 6379 | Token 存储 + 购物车数据，volume 持久化 |

### 3.3 环境变量说明

参见 `deploy/.env.production.example`。关键变量：

- **LLM_API_KEY** / **LLM_MODEL**: 真实 LLM 调用必备
- **EMBEDDING_API_KEY** / **EMBEDDING_MODEL** / **EMBEDDING_DIMENSION**: 真实 Embedding 必备
- **QDRANT_VECTOR_SIZE**: 必须等于 EMBEDDING_DIMENSION
- **QDRANT_COLLECTION**: 生产建议用 `ecommerce_rag_chunks`
- **QUERY_PLANNER_ENABLED**: demo 稳定性优先时可设为 false
- **REDIS_ENABLED** / **REDIS_HOST** / **REDIS_PORT**: Redis 连接配置
- **DEMO_USERNAME** / **DEMO_PASSWORD**: Demo 登录凭据
- **AUTH_TOKEN_TTL_SECONDS**: Token 有效期（默认 7 天）

---

## 4. Nginx + HTTPS 配置说明

### 4.1 安装证书（Let's Encrypt）

```bash
# 1. 先配置 HTTP（不加 SSL）
sudo cp deploy/nginx/ecommerce-rag-agent.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/ecommerce-rag-agent.conf /etc/nginx/sites-enabled/

# 2. 修改域名占位符: your-domain.com → 你的真实域名
sudo vim /etc/nginx/sites-available/ecommerce-rag-agent.conf

# 3. 测试配置
sudo nginx -t

# 4. 重载 Nginx
sudo systemctl reload nginx

# 5. 获取 SSL 证书
sudo certbot --nginx -d api.your-domain.com

# 6. 证书自动续期 (certbot 会自动配置 cron)
sudo certbot renew --dry-run
```

### 4.2 SSE 关键配置

Nginx 配置中 `/api/chat/stream` 必须包含以下 SSE 专用配置：

```nginx
proxy_buffering off;        # 禁用缓冲，立即转发每个 SSE 事件
proxy_cache off;            # 禁用缓存
proxy_read_timeout 3600s;   # 长时间 SSE 连接不断开
proxy_send_timeout 3600s;
chunked_transfer_encoding on;
```

如果缺少以上配置，SSE 可能出现：流式输出延迟、连接过早断开、事件合并等问题。

---

## 5. Qdrant 持久化说明

### 5.1 数据存储

- Qdrant 数据存储在 Docker volume `qdrant_storage` 中
- 映射到 Qdrant 容器的 `/qdrant/storage` 路径
- 服务重启后索引数据不会丢失
- docker-compose down 不会删除 volume（除非加 `-v` 参数）

### 5.2 检查 Qdrant 状态

```bash
# 检查 Qdrant collection
curl http://localhost:6333/collections/ecommerce_rag_chunks

# 检查 vector index stats
curl http://localhost:8080/api/rag/vector-index/stats
```

### 5.3 数据清理

```bash
# 如果需要彻底重建索引（删除 collection）
curl -X DELETE http://localhost:6333/collections/ecommerce_rag_chunks

# 然后重新 rebuild
curl -X POST https://api.your-domain.com/api/rag/vector-index/rebuild
```

---

## 6. 首次索引重建步骤

### 6.1 执行 rebuild

第一次部署后，需要手动触发索引重建。rebuild 会：
1. 读取 100 条商品数据
2. 生成 500-700 个 RagChunkDocument
3. 调用真实 Embedding API 逐条向量化
4. 批量写入 Qdrant

```bash
# Linux/macOS
curl -X POST https://api.your-domain.com/api/rag/vector-index/rebuild

# Windows PowerShell
Invoke-RestMethod -Method Post "https://api.your-domain.com/api/rag/vector-index/rebuild"
```

### 6.2 验证 rebuild 结果

```bash
# 检查索引状态（应返回 indexed_chunks > 0）
curl https://api.your-domain.com/api/rag/vector-index/stats

# 预期响应示例:
# {"count":650,"embedding_model":"doubao-embedding-vision-251215","dimension":1024}

# 测试检索
curl "https://api.your-domain.com/api/rag/retrieval/debug?query=推荐几款跑鞋&limit=3"
```

### 6.3 注意事项

1. **rebuild 会调用真实 Embedding API**，可能耗时 1-3 分钟，可能产生少量费用
2. **rebuild 需要 Qdrant 可用**，如果 Qdrant 未启动会报错
3. **rebuild 成功后 Qdrant volume 会保存向量**，下次重启无需再次 rebuild
4. **不建议每次启动自动 rebuild**（`QDRANT_RECREATE_ON_START=false`）
5. **如果修改了商品数据（products.json）或切换到不同的 Embedding 模型**，需要重新 rebuild
6. **维度一致性**：重建时会检查 `EMBEDDING_DIMENSION == QDRANT_VECTOR_SIZE`，不一致则失败

---

## 7. Android API 地址切换

### 7.1 切换 Base URL

编辑 `client/android/app/src/main/java/com/ecommerce/rag/client/config/AppConfig.kt`：

```kotlin
object AppConfig {
    // 开发调试（注释掉）
    // const val BASE_URL = "http://10.0.2.2:8080"

    // 生产环境
    const val BASE_URL = "https://api.your-domain.com"

    // ...其他配置不变
}
```

### 7.2 HTTPS 要求

- 生产环境必须使用 HTTPS
- 如果使用 HTTP（仅限临时测试），需要确保 `AndroidManifest.xml` 中 `usesCleartextTraffic=true`
- 正式上线前应将 `usesCleartextTraffic` 设为 `false` 或移除

### 7.3 验证

- 修改 BASE_URL 后重新编译 APK
- 安装到测试设备，发送一条消息
- 确认能收到 SSE 流式文本和商品卡片

---

## 8. Smoke Test

```bash
# 本地 Docker (Linux)
bash deploy/scripts/smoke_test.sh http://localhost:8080

# 远程 HTTPS
bash deploy/scripts/smoke_test.sh https://api.your-domain.com

# Windows PowerShell
.\deploy\scripts\smoke_test.ps1 -BaseUrl "http://localhost:8080"
.\deploy\scripts\smoke_test.ps1 -BaseUrl "https://api.your-domain.com"
```

### Smoke Test 验证项

1. **健康检查** - `GET /api/health`
2. **商品列表** - `GET /api/products?limit=3`
3. **向量索引状态** - `GET /api/rag/vector-index/stats`
4. **检索 debug** - `GET /api/rag/retrieval/debug?query=推荐几款跑鞋&limit=3`
5. **SSE 聊天** - `POST /api/chat/stream` → 验证 text/product_card/done 事件
6. **Auth Login** - `POST /api/auth/login`
7. **Cart Access (unauthenticated)** - `GET /api/cart` → 401
8. **Cart Access (authenticated)** - `GET /api/cart` with Bearer token

全部 8 项通过即表示最小上线闭环就绪。

---

## 9. 常见问题

### 9.1 SSE 没有流式输出（文本延迟出现/一次全部输出）

- **原因**：Nginx 代理 buffering 未关闭
- **解决**：确认 `/api/chat/stream` 配置了 `proxy_buffering off; proxy_cache off;`

### 9.2 Qdrant 连接失败

- **现象**：`Connection refused: qdrant:6333`
- **原因**：Qdrant 容器未启动或 backend 的 `QDRANT_URL` 错误
- **解决**：
  ```bash
  docker compose -f deploy/docker-compose.yml ps qdrant
  docker compose -f deploy/docker-compose.yml logs qdrant
  ```
  确认 `QDRANT_URL=http://qdrant:6333`（Docker 网络内使用服务名）

### 9.3 向量维度不一致

- **现象**：rebuild 时报 "Dimension mismatch"
- **原因**：`EMBEDDING_DIMENSION` ≠ `QDRANT_VECTOR_SIZE`
- **解决**：两者必须相等。修改后需删除旧 collection 再重建。

### 9.4 LLM API Key 未配置

- **现象**：LLM 调用返回 401 / `LLM API key not configured`
- **解决**：编辑 `deploy/.env.production`，设置 `LLM_API_KEY` 和 `LLM_MODEL`

### 9.5 Android 不能访问 HTTP

- **现象**：Cleartext HTTP traffic not permitted
- **解决**：
  - 生产环境必须使用 HTTPS API
  - 临时测试可在 `AndroidManifest.xml` 设置 `android:usesCleartextTraffic="true"`
  - 正式上线前务必移除此设置

### 9.6 图片加载失败

- **现象**：商品图片 URL 为相对路径 `/images/xxx.jpg`，Android 拼接后无法访问
- **解决**：确认后端 static/images 有图片文件，Nginx location /images/ 已配置反代

### 9.7 Planner 延迟较高

- **现象**：assist 模式下 planner 增加了额外延迟
- **解决**：
  - 设置 `QUERY_PLANNER_ENABLED=false` 关闭 planner
  - 或设置 `QUERY_PLANNER_MODE=shadow` 仅记录不接管

### 9.8 服务器重启后索引是否还在

- **回答**：是。Qdrant 数据存储在 Docker volume `qdrant_storage` 中，重启不丢失。
- **注意**：只有执行 `docker compose down -v` 才会删除 volume。

### 9.9 连接超时

- **现象**：SSE 连接 30s 后断开
- **原因**：后端没有设置连接超时，但 Nginx 或某些网络设备可能超时
- **解决**：
  - Nginx: `proxy_read_timeout 3600s;`
  - 后端: Spring Boot 默认不休眠 SSE 连接
  - 如有反向代理层（CDN/负载均衡），也需要调整超时

---

## 10. 回滚方案

如果遇到不稳定的情况，可以快速回滚到基础模式：

```bash
# 编辑 deploy/.env.production，修改以下配置：
MOCK_LLM_ENABLED=false       # 保持真实 LLM（推荐）
QUERY_PLANNER_ENABLED=false  # 关闭 planner
QUERY_REWRITE_ENABLED=false  # 关闭 rewrite
```

```bash
# 重启 backend
docker compose -f deploy/docker-compose.yml restart backend
```

回滚后：
- 用户仍可使用真实 LLM 进行导购对话
- 关闭 planner/rewrite 后不引入额外 LLM 调用，延迟更低
- 检索仍使用 Hybrid Retrieval（vector + keyword）
- SSE 协议不变，Android App 无需改动

---

## 11. 安全说明

1. **不提交 `.env.production`** — 已加入 `.gitignore`（`deploy/.env.production`）
2. **API Key 只放服务器环境变量** — 通过 docker-compose `env_file` 注入
3. **Nginx 不打印 Authorization header** — 配置文件未包含 `proxy_set_header Authorization` 透传
4. **HTTPS 强制** — 80 端口自动 301 重定向到 443
5. **不暴露 Qdrant 端口到公网** — docker-compose 未暴露 6333 端口
6. **后端不暴露到公网** — docker-compose 使用 `expose` 而非 `ports`，仅 Nginx 可以访问

> 当前阶段不实现用户认证，所有 API 无需 Token 即可访问。如需限制访问，建议在 Nginx 层配置 IP 白名单或 Basic Auth。

---

## 12. Docker build / compose config 验证（如未执行）

如果在当前 Windows 开发环境中无法执行 Docker：

```bash
# 安装 Docker Desktop for Windows 后，在 PowerShell 中验证：

# 验证 Dockerfile 语法
cd server
docker build --no-cache -t ecommerce-backend:test .
# 预期: 构建成功，无错误

# 验证 docker-compose 语法
cd ..\deploy
docker compose config
# 预期: 输出完整 compose 配置，无错误
```

在开发环境验证通过后，将项目推送到 Git，在服务器端拉取构建。

---

## 13. 当前上线能力边界

| 能力 | 状态 |
|------|------|
| Spring Boot Docker 化部署 | ✅ 已实现 |
| Qdrant 持久化运行 | ✅ 已实现 |
| Nginx HTTPS 反代 | ✅ 配置模板就绪 |
| `/api/chat/stream` SSE 在线可用 | ✅ 需配好 LLM Key |
| Android 公网 API 调用 | ✅ 切换 BASE_URL 即可 |
| 手动重建 Qdrant 索引 | ✅ `POST /rebuild` |
| 用户认证 / 登录 | ✅ Demo 登录 + Bearer Token |
| ConversationState 持久化 | ❌ 仅 in-memory，重启丢失 |
| 多副本 / 负载均衡 | ❌ 未实现 |
| CI/CD 自动化 | ❌ 未实现 |
| 监控 / 告警 | ❌ 未实现 |
| Redis 购物车 | ✅ Token 存储 + 购物车数据 |
| TTS 语音合成 | ✅ POST /api/tts/speak + GET /api/tts/audio/{fileName} |
| ASR 语音识别 | ❌ Android 原生实现，不需要后端接口 |

---

## 14. TTS 部署说明

### 14.1 Docker 部署

当前 Dockerfile 已在 runtime stage 安装 `python3` + `pip` + `edge-tts`：

```dockerfile
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && pip3 install --no-cache-dir edge-tts \
    && rm -rf /var/lib/apt/lists/*
```

构建镜像时自动包含 TTS 依赖，无需额外操作。

### 14.2 环境变量

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `TTS_ENABLED` | `true` | 是否启用 TTS |
| `TTS_PROVIDER` | `edge-tts-cli` | TTS 提供者 |
| `TTS_AUDIO_DIR` | `./data/tts` | 音频文件存储目录 |
| `TTS_PUBLIC_BASE_URL` | 空 | 生产环境建议设置为 `https://api.your-domain.com` |
| `TTS_MAX_TEXT_LENGTH` | `500` | 最大文本长度 |
| `TTS_DEFAULT_VOICE` | `zh-CN-XiaoxiaoNeural` | 默认语音角色 |
| `TTS_DEFAULT_FORMAT` | `mp3` | 默认音频格式 |
| `TTS_COMMAND_TIMEOUT_SECONDS` | `30` | edge-tts 命令超时 |

### 14.3 本地开发（无 Docker）

如需在本地测试 TTS，需先安装 edge-tts：

```bash
pip install edge-tts
```

然后启动后端，调用：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/tts/speak" `
  -ContentType "application/json" `
  -Body '{"text":"你好，我是你的智能导购助手。","voice":"zh-CN-XiaoxiaoNeural","format":"mp3"}'
```

### 14.4 音频文件清理

TTS 音频文件保存 24 小时后自动清理，无需手动管理。

---

## 15. 下一步建议

1. **云服务器实际部署**：在阿里云/腾讯云等平台申请服务器，按本文档执行部署
2. **域名 + SSL**：配置真实域名和 Let's Encrypt 证书
3. **CI/CD**：使用 GitHub Actions 实现自动构建 Docker 镜像并推送
4. **后端日志持久化**：配置 Docker volume 挂载日志目录
5. **Planner vs Legacy 系统评估**：评估 planner 质量和缓存优化
6. **图片找货**：接入 Ark 多模态 image 输入
7. **TTS 优化**：支持更多语音角色、流式音频、音频缓存复用

---

> 本阶段只完成最小上线闭环，不继续开发 RAG 功能，不改 SSE，不改核心检索逻辑。
