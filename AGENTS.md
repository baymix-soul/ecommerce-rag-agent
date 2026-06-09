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

**最小上线闭环 + 待评估优化**

已完成核心功能开发闭环（后端 RAG Pipeline + Query Understanding + Android 客户端 + MVP 部署配置），代码已推送到 GitHub (`dev` 分支)。当前阶段为上线前最后的文档对齐与待评估优化。

### 已完成的核心能力

| 模块 | 状态 | 核心产出 |
|------|------|----------|
| Backend MVP | ✅ | Spring Boot 3 + Maven 骨架、100 条商品 JSON、搜索/详情/健康检查 API |
| RAG Pipeline | ✅ | Parent-Child Chunk (7 种)、Embedding 抽象 (Mock/OpenAI-style/Ark-multimodal)、Qdrant 向量库、Hybrid Retrieval (0.65 vector + 0.35 keyword) |
| 真实 LLM 接入 | ✅ | DoubaoLlmClient (OpenAI-style SSE 流式)、Mock/真实 LLM 可切换 |
| Streaming API | ✅ | POST /api/chat/stream SSE (text + product_card + done/error) |
| 多轮对话 | ✅ | RetrievalRouter (9 种 intent)、ConversationState 上下文继承、Contextual QueryAnalyzer、StrictProductConstraintFilter |
| PageContext-aware | ✅ | PRODUCT_DETAIL/PRODUCT_LIST 页面上下文、当前商品问答 |
| Query Rewrite | ✅ | SoftSemanticLexicon (22 条) + LLMQueryRewriter、LRU cache 500 条 |
| Query Understanding | ✅ | Phase 1-4B 完成: CatalogTaxonomyService → QueryPlan → LLMQueryPlanner → Gate → assist mode 局部接管 |
| Android Client | ✅ | Kotlin + Jetpack Compose、SSE 消费、商品浏览/详情/悬浮 Agent、Coil 图片加载 |
| 输出优化 | ✅ | RecommendationCountResolver (1/3 张卡片)、4 套输出模板 |
| MVP 部署 | ✅ | Multi-stage Dockerfile、Docker Compose (backend + Qdrant)、Nginx HTTPS + SSE 配置、Smoke Test、部署文档 |
| 用户认证 + 购物车 | ✅ | Demo 登录、Bearer Token、Redis 购物车、5 个 REST API、对话式加购 |

### 测试覆盖

- **619 个测试全部通过** (200+ 测试类)
- 覆盖 RAG 全链路、Query Understanding、ChatService、Android model

## 7. 后续开发阶段

| 优先级 | 任务 | 说明 |
|--------|------|------|
| P1 | 云服务器实际部署 | 按 docs/deployment_mvp.md 在云服务器部署上线 |
| P2 | Planner vs Legacy 系统评估 + 缓存优化 | 对比 planner 与 legacy 在 category/subCategory/price 上的准确率，planner 结果缓存 |
| P2 | 图片找货 | 接入 Ark 多模态 image_url 输入 |
| P3 | ConversationState 持久化 | 当前仅 in-memory，服务重启丢失 |
| P3 | BGE Reranker | 检索结果重排序 |
| P3 | 购物车增强功能 | 基础购物车已完成（2026-06-04），后续可对接真实支付 |

## 8. 最高优先级规则

> AI 助手必须在所有操作中遵守以下规则，不得违反：

1. **不得泄露 API Key** — 绝不在代码、注释、日志、文档、README 或 Git 中写入真实 API Key / Token / 密码
2. **不得编造商品信息** — 所有商品名称、价格、库存、优惠、功效必须来自商品库数据
3. **不得越权操作** — 删除、重命名、大规模重构必须经用户明确授权
4. **所有 RAG 回答必须基于商品库检索结果** — 不允许 LLM 凭空推荐不在候选列表中的商品
5. **所有代码修改必须小步、可解释、可回滚** — 每次改动聚焦单一目标
6. **每次完成任务后更新 changelog 和 todo** — 保持项目记忆文件同步