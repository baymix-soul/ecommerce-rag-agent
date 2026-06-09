# 交付清单

> 本文档列出项目交付的所有必需文件和检查项，供评委和开发者参考。
> 当前项目支持**双方案交付**：公网后端 + APK 快速体验（推荐），以及本地 Docker 后端 + Android Studio Emulator（备用）。

---

## 1. 方案 A：本地部署必需文件

| 文件 | 说明 |
|------|------|
| `README.md` | 项目根目录入口说明 |
| `docs/judge_quickstart.md` | 评委快速开始指南（本地部署） |
| `docs/demo_script.md` | 演示脚本 |
| `docs/api_spec.md` | API 接口规范 |
| `docs/rag_design.md` | RAG 设计文档 |
| `deploy/docker-compose.yml` | Docker Compose 配置 |
| `deploy/.env.demo.example` | 演示环境变量模板 |
| `deploy/scripts/smoke_test.sh` | 本地冒烟测试脚本 |
| `deploy/scripts/rebuild_index.sh` | 重建索引脚本 |
| `client/android/README.md` | Android 项目说明 |

## 2. 方案 B：公网体验必需文件

| 文件 | 说明 |
|------|------|
| `docs/public_backend_quickstart.md` | 公网后端部署指南 |
| `docs/apk_install_guide.md` | APK 安装指南 |
| `deploy/.env.production.example` | 生产环境变量模板 |
| `deploy/scripts/public_smoke_test.sh` | 公网冒烟测试脚本 |
| `APK 文件` | 随提交材料单独提供 |
| `公网后端地址` | `http://<PUBLIC_IP>:8080` |

## 3. 提交前检查清单

### 3.1 后端测试

- [ ] `cd server && mvn clean test` 全部通过

### 3.2 Docker 构建

- [ ] `cd deploy && docker compose config` 无错误
- [ ] `docker compose up -d --build` 成功启动

### 3.3 服务健康（本地）

- [ ] `GET http://localhost:8080/api/health` 返回 `{"status":"ok"}`
- [ ] `POST http://localhost:8080/api/rag/vector-index/rebuild` 成功
- [ ] `GET http://localhost:8080/api/rag/vector-index/stats` 返回 `count > 0`

### 3.4 服务健康（公网）

- [ ] `GET http://<PUBLIC_IP>:8080/api/health` 返回 `{"status":"ok"}`
- [ ] `GET http://<PUBLIC_IP>:8080/api/rag/vector-index/stats` 返回 `count > 0`
- [ ] `audio_url` 为公网可访问地址

### 3.5 冒烟测试

- [ ] `bash deploy/scripts/smoke_test.sh http://localhost:8080` 全部通过
- [ ] `BASE_URL=http://<PUBLIC_IP>:8080 bash deploy/scripts/public_smoke_test.sh` 全部通过

### 3.6 Android 连接

- [ ] Android Emulator 可连接 `http://10.0.2.2:8080`
- [ ] APK 可连接公网后端 `http://<PUBLIC_IP>:8080`

### 3.7 端到端功能

- [ ] "推荐几款跑鞋" 正常返回文本 + 商品卡片
- [ ] 多轮上下文继承正常
- [ ] 对话式加购正常
- [ ] 购物车问答正常
- [ ] TTS 正常或可降级

## 4. 不应提交的文件

以下文件/目录**不应**提交到 Git 仓库：

| 文件/目录 | 原因 |
|-----------|------|
| `.env` | 包含真实 API Key |
| `deploy/.env` | 包含真实 API Key |
| `deploy/.env.production` | 包含生产环境敏感信息 |
| `qdrant_storage/` | 本地生成的向量数据 |
| `redis_data/` | 本地 Redis 数据 |
| `server/data/tts/` | TTS 生成的 mp3 缓存 |
| `data/tts/` | TTS 生成的 mp3 缓存 |
| `*.mp3` | 音频缓存文件 |
| `target/` | Maven 构建产物 |
| `build/` | Gradle 构建产物 |
| `.gradle/` | Gradle 缓存 |
| `*.apk` | APK 构建产物（随提交材料单独提供） |

## 5. 敏感信息检查

提交前请确认：

- [ ] 代码中无硬编码 API Key
- [ ] 文档中无真实 API Key
- [ ] 日志中无密码或 Token
- [ ] `.env` 文件已加入 `.gitignore`
- [ ] `deploy/.env.production` 已加入 `.gitignore`

## 6. 交付方式说明

### 6.1 推荐方式：公网后端 + APK

> 由于公网 IP 已审批，最终提交优先提供公网 Demo 地址和 APK。

流程：

```text
1. 我们在服务器部署 Redis + Qdrant + Backend
2. 重建 RAG 索引
3. 构建指向公网后端的 APK
4. 评委安装 APK 后直接体验
```

### 6.2 备用方式：本地部署 + Emulator

> 本地部署文档作为备用方案一并提供，适合完整复现部署流程。

流程：

```text
1. 评委 clone 项目
2. 配置 deploy/.env
3. docker compose up -d --build
4. POST /api/rag/vector-index/rebuild
5. Android Studio 打开 client/android
6. Emulator 使用 http://10.0.2.2:8080
7. 端到端测试
```

### 6.3 后续补充

如果后续公网服务不可用，评委可随时切换到本地部署方案。

---

> 如有疑问，请参考 [judge_quickstart.md](judge_quickstart.md) 或 [public_backend_quickstart.md](public_backend_quickstart.md)。
