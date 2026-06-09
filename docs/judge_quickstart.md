# 评委快速开始指南（本地部署）

> 本文档面向比赛评委，说明如何在**本地**完成项目部署并运行端到端 Demo。
>
> 如果只想快速体验，请优先使用**公网后端 + APK 方案**（见 [apk_install_guide.md](apk_install_guide.md)）。
> 如果需要完整复现部署流程，请使用本文档的本地部署方案。

---

## 1. 环境要求

### 后端

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 后端运行需要 Java 17 |
| Maven | 3.9+ | 构建工具 |
| Docker / Docker Compose | 最新版 | 启动 Redis、Qdrant、Backend |
| Git | 任意 | 克隆项目 |
| 网络 | — | 可访问 Doubao/Ark API 的网络（如使用真实 LLM） |

> 如果不使用 Docker，需要本地安装 Python 和 edge-tts（TTS 功能需要）。

### Android

| 依赖 | 说明 |
|------|------|
| Android Studio | 最新稳定版 |
| Android Emulator | API 30+ 推荐 |
| Android SDK | 随 Android Studio 安装 |

---

## 2. 克隆项目

```bash
git clone <REPO_URL>
cd ecommerce-rag-agent
```

> `<REPO_URL>` 为项目仓库地址，请替换为实际地址。

---

## 3. 配置环境变量

### 3.1 复制模板

```bash
cp deploy/.env.demo.example deploy/.env
```

### 3.2 编辑 `deploy/.env`

必须填写的配置：

```env
# LLM / Embedding — 必须填写真实 API Key
DOUBAO_API_KEY=your_api_key_here
```

> 如果没有真实 LLM Key，可以设置 `MOCK_LLM_ENABLED=true` 体验基础流程，但推荐质量和真实语义理解会下降。

其他配置保持默认值即可，完整模板说明见 [deploy/.env.demo.example](../deploy/.env.demo.example)。

### 3.3 安全提醒

- **不要**将填写了真实 Key 的 `.env` 提交到 Git 仓库。
- `.env` 已加入 `.gitignore`，正常情况下不会被提交。

---

## 4. 启动后端

```bash
cd deploy
docker compose up -d --build
```

服务启动顺序：

1. `qdrant` — 向量数据库
2. `redis` — 缓存与购物车
3. `backend` — Spring Boot 应用（依赖 qdrant 和 redis 健康检查通过后才启动）

查看日志：

```bash
docker compose logs -f backend
```

---

## 5. 检查服务

### 5.1 Health Check

```bash
curl http://localhost:8080/api/health
```

预期响应：

```json
{"status":"ok","service":"ecommerce-rag-agent","version":"0.1.0"}
```

### 5.2 向量索引状态

```bash
curl http://localhost:8080/api/rag/vector-index/stats
```

首次启动时 `count` 为 0，需要执行重建索引。

---

## 6. 重建 RAG 索引

```bash
curl -X POST http://localhost:8080/api/rag/vector-index/rebuild
```

检查重建结果：

```bash
curl http://localhost:8080/api/rag/vector-index/stats
```

预期：

```json
{"count":650,"embedding_model":"mock-hash-embedding","dimension":64}
```

> `count > 0` 表示索引已建立。维度取决于当前 embedding 配置（Mock 为 64，真实 Ark 为 1024/2048 等）。

也可以使用脚本：

```bash
bash deploy/scripts/rebuild_index.sh
```

---

## 7. 后端冒烟测试

### 7.1 运行冒烟测试脚本

```bash
bash deploy/scripts/smoke_test.sh http://localhost:8080
```

预期输出：`8 tests passed, 0 failed`（或类似全部通过）。

### 7.2 手动测试关键接口

#### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'
```

#### 普通推荐

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"推荐几款跑鞋","session_id":"judge-demo-1","limit":3}'
```

#### 多轮上下文

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"message":"要轻量的","session_id":"judge-demo-1","limit":3}'
```

#### TTS

```bash
curl -X POST http://localhost:8080/api/tts/speak \
  -H "Content-Type: application/json" \
  -d '{"text":"你好，我是你的智能导购助手。","voice":"zh-CN-XiaoxiaoNeural","format":"mp3"}'
```

#### 购物车（需先登录获取 token）

```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# 2. 获取购物车
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cart
```

---

## 8. Android Studio 启动 App

### 8.1 打开项目

1. 启动 Android Studio
2. `File` → `Open` → 选择 `client/android` 目录
3. 等待 Gradle Sync 完成

### 8.2 确认后端地址

打开 `client/android/app/src/main/java/com/ecommerce/rag/client/config/AppConfig.kt`，确认：

```kotlin
const val BASE_URL = "http://10.0.2.2:8080"
```

> **重要**：Android Emulator 访问宿主机后端必须使用 `10.0.2.2`，**不能**用 `localhost`。

### 8.3 启动 Emulator 并运行

1. 打开 Device Manager，启动 Android Emulator（推荐 API 30+）
2. 点击 Android Studio 的 `Run` 按钮（绿色三角形）
3. App 启动后会自动执行 demo 登录
4. 进入商品浏览页
5. 长按/点击悬浮机器人打开聊天框

---

## 9. Android 端注意事项

### 9.1 Emulator 访问后端

- Android Emulator 访问宿主机后端必须使用 `http://10.0.2.2:8080`
- `localhost` 在 Emulator 内指向的是 Emulator 自身，不是宿主机
- **本地部署时，不要把 `AppConfig.BASE_URL` 写成 `localhost`**

### 9.2 真机测试

如需真机测试，需要将 `AppConfig.BASE_URL` 改为宿主机局域网 IP：

```kotlin
const val BASE_URL = "http://192.168.x.x:8080"
```

并确保手机和电脑在同一局域网。

### 9.3 TTS 音频地址

TTS 返回的 `audio_url` 如果包含 `localhost`，会导致 Emulator 无法播放。建议设置：

```env
TTS_PUBLIC_BASE_URL=http://10.0.2.2:8080
```

或在 `deploy/.env` 中配置为宿主机的局域网 IP。

---

## 10. 常见问题

### 10.1 Docker build 慢

- 首次构建需要下载 Maven 依赖和 Docker 镜像，可能需要 5-10 分钟
- 建议开启 Docker 的镜像加速

### 10.2 Qdrant index count=0

- 首次启动后需要执行 `POST /api/rag/vector-index/rebuild`
- 检查 backend 日志是否有 rebuild 失败的错误信息

### 10.3 `/api/rag/vector-index/rebuild` 失败

- 检查 Qdrant 是否已启动：`docker compose ps qdrant`
- 检查 embedding 维度与 Qdrant collection 维度是否一致
- 如果使用真实 Ark Embedding，检查 API Key 是否正确

### 10.4 LLM API Key 未配置

- 现象：SSE 返回 `LLM API key not configured` 或 401
- 解决：在 `deploy/.env` 中设置 `DOUBAO_API_KEY`
- 或设置 `MOCK_LLM_ENABLED=true` 使用 Mock 模式

### 10.5 Android 连不上后端

- 确认后端已启动：`curl http://localhost:8080/api/health`
- 确认 `AppConfig.BASE_URL` 为 `http://10.0.2.2:8080`
- 确认 AndroidManifest.xml 中 `usesCleartextTraffic="true"`
- 检查宿主机防火墙是否放行 8080 端口

### 10.6 App TTS 没声音

- 确认 TTS 后端接口可用：`curl -X POST http://localhost:8080/api/tts/speak ...`
- 确认 `TTS_PUBLIC_BASE_URL` 配置正确（不要用 localhost）
- 检查 Emulator 音量是否开启

### 10.7 购物车 401

- 确认已登录：App 启动后应自动 demo 登录
- 确认 token 未过期
- 检查后端 `DEMO_USERNAME` 和 `DEMO_PASSWORD` 配置

### 10.8 Redis 连接失败

- 检查 redis 容器是否启动：`docker compose ps redis`
- 检查 `REDIS_HOST` 是否为 `redis`（Docker 网络内服务名）

### 10.9 edge-tts 不可用

- 确认 Dockerfile 中已安装 python3 + pip + edge-tts
- 检查 `TTS_ENABLED=true`
- 检查 `TTS_PROVIDER=edge-tts-cli`

### 10.10 端口 8080 被占用

- 修改 `deploy/.env` 中的 `SERVER_PORT`
- 同步修改 docker-compose.yml 的 ports 映射
- 同步修改 Android `AppConfig.BASE_URL`

---

## 11. 回滚到稳定模式

如果遇到不稳定的情况，可以关闭 planner 和 rewrite：

```env
MOCK_LLM_ENABLED=false
QUERY_PLANNER_ENABLED=false
QUERY_REWRITE_ENABLED=false
```

然后重启 backend：

```bash
cd deploy
docker compose restart backend
```

---

## 12. 停止服务

```bash
cd deploy
docker compose down
```

如需彻底清理数据（包括 Qdrant 索引和 Redis 数据）：

```bash
docker compose down -v
```

---

> 如有其他问题，请参考 [docs/api_spec.md](api_spec.md) 和 [docs/rag_design.md](rag_design.md)。
