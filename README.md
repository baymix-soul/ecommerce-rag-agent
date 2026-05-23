# 基于 RAG 的多模态电商智能导购 AI Agent

## 项目简介

本项目构建一个基于检索增强生成（RAG）的电商智能导购 AI Agent，构建端到端可演示的 Demo：

- **Android 原生 App**：Kotlin + Jetpack Compose 对话界面
- **后端 RAG 检索**：Spring Boot 3 + 向量数据库
- **大模型流式生成**：Doubao-Seed-2.0-lite（SSE 流式）
- **商品卡片展示**：导购回复中渲染可信商品卡片

> 本项目不是普通聊天机器人，而是**基于商品库的可信导购系统**。所有推荐必须来自商品库检索结果。

## 技术栈

| 层级 | 选型 |
|------|------|
| 后端框架 | Java 17/21 + Spring Boot 3 |
| 构建工具 | Maven |
| 流式通信 | SSE (Server-Sent Events) |
| 向量数据库 | Qdrant（target），keyword search（MVP first） |
| 商品数据 | JSON 文件（MVP）→ SQLite（后续） |
| 大模型 | Doubao-Seed-2.0-lite（兼容 OpenAI-style API） |
| 客户端 | Android Kotlin + Jetpack Compose |

## 快速开始

> 当前为项目初始化和规则填充阶段，尚未包含可运行的代码。

```bash
# 克隆项目
git clone <repo-url>
cd ecommerce-rag-agent

# 复制环境变量模板
cp .env.example .env
# 编辑 .env 填入必要的配置（不要提交 .env 到 Git 仓库）

# 后续启动后端（待实现）
# cd server && ./mvnw spring-boot:run
```

## 项目结构

```
ecommerce-rag-agent/
├── AGENTS.md                  # 项目总上下文（AI 助手入口）
├── README.md                  # 项目说明
├── .gitignore                 # Git 忽略规则
├── .env.example               # 环境变量模板
├── .trae/                     # AI 助手配置
│   ├── rules/                 # 规则文件
│   ├── commands/              # 命令 Prompt 模板
│   └── memory/                # 项目记忆
├── docs/                      # 设计文档
├── server/                    # 后端 Spring Boot 代码
└── client/                    # Android 客户端代码
```

## 开发阶段

1. **Project Context & Rules Init**（当前）— 上下文、规则、文档
2. **Backend MVP** — Spring Boot 骨架、商品数据加载
3. **RAG Pipeline** — Embedding、向量检索
4. **Streaming API** — SSE 对话流接口
5. **Android Client** — Kotlin + Jetpack Compose
6. **Enhancement** — 多轮对话、购物车增强

## 许可

待定