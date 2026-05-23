# 基于 RAG 的多模态电商智能导购 AI Agent

> 本文件是 Trae / AI 编程助手的项目总上下文入口。AI 助手在每次任务开始前必须先读取本文件。

---

## 1. 项目名称

基于 RAG 的多模态电商智能导购 AI Agent

## 2. 项目目标

构建端到端可演示的 Demo，实现以下核心体验闭环：

1. **Android 原生 App 对话** — 用户在 Kotlin + Jetpack Compose 客户端中进行自然语言导购对话
2. **后端 RAG 检索** — Spring Boot 3 后端接收用户查询，从商品知识库中检索候选商品
3. **大模型流式生成** — 基于检索到的商品列表，通过 Doubao-Seed-2.0-lite（兼容 OpenAI-style API）流式生成导购回复
4. **商品卡片展示** — 客户端在对话中渲染可信的商品卡片（名称、价格、图片、推荐理由）

> 本项目不是普通聊天机器人，而是**基于商品库的可信导购系统**。所有推荐必须来自商品库检索结果，不允许模型凭空捏造。

## 3. MVP 最小闭环范围

```
用户输入文字查询
  → Android App 发送请求
    → Spring Boot 接收
      → RAG Pipeline 检索商品库
        → LLM 基于候选商品生成回复（SSE 流式）
          → Android App 展示文本 + 商品卡片
```

MVP 不包含：
- 图片/语音多模态输入（后续阶段）
- 用户登录/会话持久化（后续阶段）
- 个性化推荐引擎（后续阶段）
- 真实支付/购物车对接（后续阶段）

## 4. 当前技术栈

| 层级 | 选型 | 备注 |
|------|------|------|
| 后端框架 | Java 17/21 + Spring Boot 3 | - |
| 构建工具 | Maven | 使用 mvnw 保证环境一致 |
| 流式通信 | SSE (Server-Sent Events) | 优先 SSE，后续评估 WebSocket |
| 向量数据库 | Qdrant（target），keyword search（MVP first） | 第一阶段用 JSON + 关键词检索跑通闭环 |
| 商品数据 | JSON 文件（MVP）→ SQLite（后续） | 先本地 JSON，不做复杂数据库 |
| 大模型 | Doubao-Seed-2.0-lite | 兼容 OpenAI-style API |
| 客户端 | Android Kotlin + Jetpack Compose | 原生 Android |

## 5. 项目目录结构

```
ecommerce-rag-agent/
├── AGENTS.md                  # 项目总上下文（本文件，AI 助手入口）
├── README.md                  # 项目说明
├── .gitignore                 # Git 忽略规则
├── .env.example               # 环境变量模板（仅占位符）
├── .trae/                     # AI 助手配置
│   ├── rules/                 # 规则文件
│   │   ├── global_rules.md    # 全局行为准则
│   │   ├── permission_rules.md # 权限与安全规则
│   │   ├── coding_rules.md    # 编码规范
│   │   ├── rag_rules.md       # RAG 模块设计规则
│   │   └── git_rules.md       # Git 工作流规则
│   ├── commands/              # 命令定义（Prompt 模板）
│   │   ├── init_project.md
│   │   ├── add_feature.md
│   │   ├── fix_bug.md
│   │   ├── review_code.md
│   │   └── update_context.md
│   └── memory/                # 项目记忆
│       ├── decisions.md       # 技术决策记录
│       ├── changelog.md       # 变更日志
│       └── todo.md            # 任务跟踪
├── docs/                      # 设计文档
│   ├── architecture.md        # 系统架构
│   ├── api_spec.md            # API 规范
│   ├── data_schema.md         # 数据模型
│   ├── rag_design.md          # RAG 设计
│   └── demo_plan.md           # 演示计划
├── server/                    # 后端 Spring Boot 代码
└── client/                    # Android 客户端代码
```

## 6. 当前阶段

**Project Context and Rules Initialization**

已完成目录初始化，正在填充上下文、规则、命令和规划文档。不编写后端业务代码。

## 7. 后续开发阶段

| 阶段 | 名称 | 核心产出 |
|------|------|----------|
| 1 | Backend MVP | Spring Boot 项目骨架、商品数据加载、健康检查 API |
| 2 | RAG Pipeline | Embedding 抽象层、向量数据库集成、检索 API |
| 3 | Streaming API | SSE 对话接口、流式 LLM 生成 |
| 4 | Android Client | Kotlin + Jetpack Compose 对话界面、SSE 消费 |
| 5 | Product Card Rendering | 商品卡片 UI 组件、推荐理由展示 |
| 6 | Multi-turn / Enhancement | 多轮对话上下文、排除已推荐商品、购物车增强 |

## 8. 最高优先级规则

> AI 助手必须在所有操作中遵守以下规则，不得违反：

1. **不得泄露 API Key** — 绝不在代码、注释、日志、文档、README 或 Git 中写入真实 API Key / Token / 密码
2. **不得编造商品信息** — 所有商品名称、价格、库存、优惠、功效必须来自商品库数据
3. **不得越权操作** — 删除、重命名、大规模重构必须经用户明确授权
4. **所有 RAG 回答必须基于商品库检索结果** — 不允许 LLM 凭空推荐不在候选列表中的商品
5. **所有代码修改必须小步、可解释、可回滚** — 每次改动聚焦单一目标
6. **每次完成任务后更新 changelog 和 todo** — 保持项目记忆文件同步