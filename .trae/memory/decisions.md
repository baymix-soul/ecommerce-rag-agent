# 技术决策记录

> 记录项目开发过程中的所有关键技术决策，包括决策背景、选项分析和最终选择。

## 决策格式

```
## D-{序号}: {决策标题}
- **日期**: YYYY-MM-DD
- **状态**: proposed / accepted / deprecated / superseded
- **背景**: 为什么要做这个决策
- **选项**: 考虑过的方案
- **决策**: 最终选择的方案和理由
- **影响**: 对项目的影响
```

---

## D-001: 后端框架选型
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: 需要选择一个后端框架来构建 RAG 电商导购 API 服务
- **选项**: Spring Boot 3 / FastAPI / Flask
- **决策**: 选择 **Java 17/21 + Spring Boot 3**
  - 团队技术栈以 Java 为主
  - Spring Boot 生态成熟，SSE 支持完善
  - 适合构建生产级后端服务
- **影响**: 后端全部使用 Java 17/21 和 Spring Boot 3 构建

## D-002: 使用 AGENTS.md 作为项目总上下文
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: AI 编程助手需要一个统一的项目上下文入口
- **选项**: PROJECT_CONTEXT.md / AGENTS.md / README.md
- **决策**: 使用 **AGENTS.md** 作为 Trae / AI 编程助手的项目总上下文入口
  - 与 Trae 平台的约定一致
  - AI 助手在每次任务开始前必须先读取该文件
- **影响**: 所有上下文信息集中在 AGENTS.md 中维护

## D-003: 流式通信优先 SSE
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: 对话接口需要流式返回 LLM 生成内容
- **选项**: SSE / WebSocket / gRPC stream
- **决策**: **SSE (Server-Sent Events)** 优先
  - 实现简单，Spring Boot 原生支持
  - 单向推送足够满足流式文本生成需求
  - 客户端消费兼容性好（OkHttp 原生支持）
  - 后续如需要双向通信再引入 WebSocket
- **影响**: 对话接口使用 SSE，事件类型定义在 rag_rules.md

## D-004: 向量数据库选型 Qdrant，关键词检索降级
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: 需要选择一个向量数据库存储商品 Embedding
- **选项**: Qdrant / Chroma / Milvus / FAISS
- **决策**: **Qdrant first，keyword search fallback**
  - Qdrant 性能更好，Rust 实现，过滤能力强
  - MVP 第一阶段先预留 VectorStoreService 接口
  - 先用 JSON 商品数据 + 关键词检索跑通最小闭环
  - 后续再接入 Qdrant 做向量检索，切换时仅替换实现类
- **影响**: java/rag/retriever 下定义 VectorRetriever 接口，第一阶段实现 KeywordRetriever

## D-005: 商品数据 MVP 先用 JSON
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: MVP 阶段需要商品数据，但不应过早引入复杂数据库
- **选项**: JSON 文件 / SQLite / MySQL / PostgreSQL
- **决策**: **JSON 文件（MVP）→ SQLite（后续）**
  - MVP 阶段 50-100 条商品用 JSON 文件足够
  - 零部署成本，修改方便
  - 后续数据量增大后迁移到 SQLite
- **影响**: 商品数据存储为 `resources/data/products.json`

## D-006: 客户端优先 Android Kotlin
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: 需要选择一个客户端平台来演示完整端到端体验
- **选项**: Android Kotlin / React Native / Flutter / Web
- **决策**: **Android Kotlin + Jetpack Compose**
  - 原生性能最好
  - Jetpack Compose 声明式 UI，开发效率高
  - SSE 消费成熟（OkHttp）
- **影响**: 客户端为原生 Android App

## D-007: 暂不引入 LangChain / LlamaIndex
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: RAG Pipeline 可以使用框架简化开发
- **选项**: LangChain / LlamaIndex / 手写轻量 Pipeline
- **决策**: **先手写轻量 RAG Pipeline**
  - 框架引入复杂度和学习成本高
  - 当前 RAG 流程简单（单步检索 + LLM 生成）
  - 手写可以完全控制检索和 Prompt 细节
  - 后续如有复杂需求再评估引入框架
- **影响**: RAG 模块全部手写实现

## D-008: API Key 只能从环境变量读取
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: 项目需要调用 LLM 和 Embedding API，需要管理 API Key
- **选项**: 环境变量 / 配置文件 / 数据库 / 代码硬编码
- **决策**: **只能从环境变量读取 API Key**
  - 安全性最高
  - 与 12-Factor App 原则一致
  - 方便在不同环境切换
  - `.env` 不提交到 Git
- **影响**: 所有 `LLM_API_KEY`、`EMBEDDING_API_KEY` 等必须通过环境变量注入

## D-009: 后端构建工具选择 Maven
- **日期**: 2026-05-21
- **状态**: accepted
- **背景**: Spring Boot 项目需要在 Maven 和 Gradle 之间选择构建工具
- **选项**: Maven / Gradle
- **决策**: **Maven**
  - Spring Boot 项目结构清晰，`pom.xml` 易于理解和维护
  - 适合教学、Demo 和后续标准化部署
  - 避免 Maven/Gradle 混用导致的配置碎片化
  - Maven Wrapper (`mvnw`) 可保证构建环境一致
- **影响**: 后端项目统一使用 Maven 构建，`server/pom.xml` 为唯一构建描述文件