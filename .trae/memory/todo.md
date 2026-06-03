# 待办任务列表

> 项目当前的任务跟踪，按优先级排序。

## 格式

```
- [ ] [P{0-3}] {任务描述}
  - 优先级说明: P0(紧急) / P1(高) / P2(中) / P3(低)
```

---

## 当前阶段：Backend MVP

### 基础搭建

- [x] [P1] 确认后端构建工具为 Maven（已决策 D-009）
- [x] [P1] 初始化 Spring Boot 3 Maven 项目骨架
- [x] [P1] 创建配置类（环境变量读取 LLM Key、Embedding Key、向量库地址）
- [x] [P1] 实现健康检查 API（GET /api/health）

### 商品数据

- [x] [P1] 设计并创建商品数据 JSON schema（12 字段标准 schema）
- [x] [P1] 准备 100 条商品数据（老师数据集转换 → products.json）
- [x] [P1] 编写商品数据加载服务（JsonLoader + ProductService）
- [x] [P1] 实现商品查询与过滤 API（GET /api/products, GET /api/products/{id}, POST /api/products/search）

### RAG Pipeline

- [x] [P1] 实现候选商品召回（CandidateProductRetriever，基于 ProductService.search() 关键词检索）
- [x] [P1] 实现 RAG Prompt 构造（RagPromptBuilder，System + 候选商品 + 用户问题）
- [x] [P1] 实现 RAG 索引文档层（RagChunkDocument + ChunkType + RagDocumentBuilder + RagDocumentService）—— Parent-Child Chunk 生成器
- [x] [P1] 实现 RAG Chunk 预览 API（GET /api/rag/chunks/preview, /product/{id}, /stats）
- [x] [P1] 实现确定性 Chunk ID + VectorPointId（RagChunkIdGenerator）
- [x] [P1] 封装 Embedding 接口（EmbeddingProvider + MockEmbeddingProvider）
- [x] [P1] 预留 VectorStoreService 接口（VectorStoreService + InMemoryVectorStoreService）
- [x] [P1] 实现 RAG 向量索引编排（RagVectorIndexService）
- [x] [P1] 实现向量索引调试 API（POST /rebuild, GET /stats, POST /search）
- [x] [P1] 实现向量检索接入 ChatService（VectorRetriever + KeywordRetriever + HybridCandidateRetriever + CandidateFusionService，hybrid/keyword/vector 三种模式）
- [x] [P1] 接入真实 Embedding API（OpenAIStyleEmbeddingProvider + ArkMultimodalEmbeddingProvider，EMBEDDING_PROVIDER 切换 mock/openai-style/ark-multimodal，维度一致性检查）
- [x] [P1] 接入真实 Qdrant（QdrantVectorStoreService + QdrantCollectionManager + QdrantFilterBuilder，VECTOR_STORE 切换 in-memory/qdrant）

### 对话与流式

- [x] [P1] 实现 LLM 客户端接口 + Mock 实现（LlmClient + MockLlmClient）
- [x] [P1] 实现 SSE 对话接口（ChatController + ChatService，POST /api/chat/stream）
- [x] [P1] 实现 SSE 事件序列化（text / product_card / error / done）
- [x] [P1] 接入真实 LLM（DoubaoLlmClient，兼容 OpenAI-style API，Mock/Doubao 可切换）

### 后续阶段

- [x] [P2] 多轮对话上下文管理
- [x] [P2] 排除已推荐商品逻辑
- [ ] [P3] 购物车增强功能
- [ ] [P3] 多模态输入（图片搜索）
- [x] [P2] 意图路由（RetrievalRouter：SMALLTALK/HELP/THANKS 不检索）
- [x] [P2] 上下文继承（Contextual QueryAnalyzer：类目/价格/品牌多轮继承）
- [x] [P2] 负约束过滤（不要日系/除了耐克/不含酒精/换一个）
- [x] [P2] PageContext-aware Agent（前端传入 page_context，PRODUCT_DETAIL 当前商品问答/替代推荐，PRODUCT_LIST 列表约束）
- [x] [P2] RAG 中期开发报告（docs/rag_midterm_report.md，19 章，覆盖检索全链路 + 能力边界 + 文档-代码不一致分析）
- [ ] [P2] BGE Reranker
- [x] [P0] Backend RAG Bugfix 第一阶段：修复 QueryAnalyzer 价格解析 + 多轮 refinement + 统一 StrictProductConstraintFilter（2026-06-02）
- [x] [P0] Backend RAG Bugfix 第二阶段：ChatService 输出一致性 + ConversationState 更新时机 + retrieval debug（2026-06-02）
- [x] [P1] Backend RAG 输出体验优化：LLM 回复风格 + 商品卡片数量控制（2026-06-02）
- [x] [P1] Backend RAG 数据增强：接入 REVIEW_SUMMARY / FAQ / MARKETING_COPY Chunk（2026-06-02）
- [x] [P2] Backend RAG 软语义理解：LLM Query Rewrite / Query Expansion（2026-06-02）
- [x] [P2] Backend Query Understanding Refactor 第一阶段：CatalogTaxonomyService + QueryPlan + QueryPlanValidator（2026-06-02）
- [x] [P2] Backend Query Understanding Refactor 第二阶段：LLMQueryPlanner Shadow Mode（2026-06-02）
- [x] [P2] Backend Query Understanding Refactor 第三阶段：QueryPlan assist/takeover 局部接管（2026-06-02）
- [x] [P2] Backend Query Understanding Refactor 第四阶段 A：QueryUnderstandingService 生产化 + ChatService Shadow 接入（2026-06-02）
- [x] [P2] Backend Query Understanding Refactor 第四阶段 B：ChatService Assist 模式局部接管（2026-06-03）
- [ ] [P2] Backend Query Understanding Refactor 第四阶段 C：Planner vs Legacy 系统评估 + 缓存优化
- [ ] [P3] 图片找货
- [x] [P1] 最小上线闭环：Docker 化 + Nginx + 部署文档 + Smoke Test（2026-06-03）
