# 基于 RAG 的多模态电商智能导购 AI Agent

## 项目简介

本项目是一个电商 RAG Agent Demo，支持商品浏览、悬浮智能导购、RAG 商品推荐、多轮上下文、购物车、语音输入和 TTS 语音播报。

- **Android 原生 App**：Kotlin + Jetpack Compose 对话界面
- **后端 RAG 检索**：Spring Boot 3 + 向量数据库 Qdrant + Redis
- **大模型流式生成**：Doubao-Seed-2.0-lite（SSE 流式）
- **商品卡片展示**：导购回复中渲染可信商品卡片，推荐理由由后端确定性生成

> 本项目不是普通聊天机器人，而是**基于商品库的可信导购系统**。所有推荐必须来自商品库检索结果。

## 推荐体验方式

当前项目支持两种演示方式：

1. **公网后端 + APK 快速体验**（推荐）：适合评委直接安装 App 后演示，优先使用。
2. **本地 Docker 后端 + Android Studio Emulator**：适合完整复现部署流程，作为备用方案。

由于公网 IP 已审批，最终提交将优先提供公网 Demo 地址和 APK；本地部署文档作为备用方案一并提供。

---

## 方式一：快速体验公网 Demo（推荐）

```text
1. 安装提供的 APK
2. 打开 App
3. 自动连接公网后端
4. 体验商品浏览、RAG 推荐、购物车、语音输入和 TTS
```

- **后端公网地址**：`http://<PUBLIC_IP>:8080`
- **APK 下载**：APK 文件随提交材料单独提供

详细说明请参考 [docs/apk_install_guide.md](docs/apk_install_guide.md)。

---

## 方式二：本地完整部署

```bash
# 1. 克隆项目
git clone https://github.com/baymix-soul/ecommerce-rag-agent.git
cd ecommerce-rag-agent

# 2. 配置环境变量
cp deploy/.env.demo.example deploy/.env
# 编辑 deploy/.env，填入 LLM_API_KEY（必须),EMBEDDINGKEY(维度需要与Qdrant维度相同)。

# 3. 启动后端
cd deploy
docker compose up -d --build

# 4. 重建 RAG 索引
curl -X POST http://localhost:8080/api/rag/vector-index/rebuild

# 5. 冒烟测试
bash deploy/scripts/smoke_test.sh

# 6. 用 Android Studio 打开 client/android，启动 Emulator，点击 Run
```

详细步骤请参考 [docs/judge_quickstart.md](docs/judge_quickstart.md)。

---

## 核心能力

1. 商品浏览与详情页
2. RAG 商品推荐
3. Hybrid Retrieval：向量检索 + 关键词检索
4. LLM Query Planner
5. 多轮上下文筛选
6. PageContext 页面上下文
7. 对话式加购
8. 购物车金额问答
9. 凑单推荐
10. ASR 语音输入
11. TTS 语音朗读
12. Redis 登录/购物车缓存
13. Qdrant 向量索引

## 快速启动入口

- [APK 安装指南](docs/apk_install_guide.md)
- [公网后端快速开始](docs/public_backend_quickstart.md)
- [评委快速开始指南（本地部署）](docs/judge_quickstart.md)
- [演示脚本](docs/demo_script.md)
- [API 接口规范](docs/api_spec.md)

## 技术栈

**后端：**

- Spring Boot 3, Java 17
- Maven
- Redis
- Qdrant
- Docker Compose
- Doubao/Ark LLM + Embedding
- edge-tts

**前端：**

- Android Kotlin
- Jetpack Compose
- OkHttp
- SpeechRecognizer
- MediaPlayer

## 项目结构

```
ecommerce-rag-agent/
├── README.md                          # 项目说明（本文件）
├── docs/                              # 设计文档与交付文档
│   ├── api_spec.md                    # API 接口规范
│   ├── rag_design.md                  # RAG 设计文档
│   ├── deployment_mvp.md              # 部署文档
│   ├── judge_quickstart.md            # 评委快速开始指南（本地部署）
│   ├── public_backend_quickstart.md   # 公网后端部署指南
│   ├── apk_install_guide.md           # APK 安装指南
│   ├── demo_script.md                 # 演示脚本
│   └── delivery_checklist.md          # 交付清单
├── deploy/                            # 部署配置
│   ├── docker-compose.yml             # Docker Compose 配置
│   ├── .env.demo.example              # 演示环境变量模板
│   ├── .env.production.example        # 生产环境变量模板
│   └── scripts/                       # 部署脚本
│       ├── smoke_test.sh              # 本地冒烟测试
│       ├── public_smoke_test.sh       # 公网冒烟测试
│       └── rebuild_index.sh           # 重建索引
├── server/                            # 后端 Spring Boot 代码
└── client/                            # Android 客户端代码
    └── android/
        └── README.md                  # Android 项目说明
```

## 开发阶段

1. **Project Context & Rules Init** — 上下文、规则、文档
2. **Backend MVP** — Spring Boot 骨架、商品数据加载
3. **RAG Pipeline** — Embedding、向量检索、Hybrid Retrieval
4. **Streaming API** — SSE 对话流接口
5. **Android Client** — Kotlin + Jetpack Compose
6. **Enhancement** — 多轮对话、购物车、语音、TTS

## 许可

待定
