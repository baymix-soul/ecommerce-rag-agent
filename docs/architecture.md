# 系统架构设计

> 已实现。以下为当前系统实际运行架构。

## 总体架构

```
                          ┌──────────────────┐
                          │  Android 客户端    │
                          │  Kotlin + Compose  │
                          └────────┬─────────┘
                                   │ HTTPS (SSE 流式)
                                   ▼
                          ┌──────────────────┐
                          │     Nginx         │
                          │  (HTTPS 反代)     │
                          └────────┬─────────┘
                                   │
                          ┌──────────────────┐
                          │  Spring Boot 3    │
                          │  API 网关层       │
                          │  (REST + SSE)     │
                          └────────┬─────────┘
                                   │
              ┌────────────────────┼──────────────────┐
              │                    │                  │
              ▼                    ▼                  ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌───────────────────┐
    │ ChatService      │ │ RetrievalRouter  │ │ QueryUnderstand-  │
    │ (SSE 流式编排)    │ │ (9 种 intent)   │ │ ingService        │
    └────────┬─────────┘ └────────┬─────────┘ │ (planner gate)    │
             │                    │            └────────┬──────────┘
             │           ┌────────┴────────┐           │
             │           ▼                 ▼           ▼
             │  ┌──────────────────┐  ┌─────────────────────┐
             │  │ QueryAnalyzer    │  │ LLMQueryPlanner     │
             │  │ (规则引擎)       │  │ → QueryPlan + Gate   │
             │  └────────┬─────────┘  └──────────┬──────────┘
             │           │                        │
             │           └──────────┬─────────────┘
             │                      ▼
             │           ┌──────────────────────┐
             │           │ HybridCandidate-     │
             │           │ Retriever (融合检索)  │
             │           └──────────┬───────────┘
             │                      │
             │         ┌────────────┼────────────┐
             │         ▼            ▼            ▼
             │  ┌───────────┐ ┌───────────┐ ┌──────────────┐
             │  │ Vector    │ │ Keyword   │ │ Candidate    │
             │  │ Retriever │ │ Retriever │ │ Fusion (0.65 │
             │  │ + Qdrant  │ │ + Search  │ │ + 0.35)     │
             │  └─────┬─────┘ └─────┬─────┘ └──────┬───────┘
             │        │             │               │
             │        │             ▼               │
             │        │    ┌──────────────────┐     │
             │        │    │ ProductService   │     │
             │        │    │ (100 商品 JSON)  │     │
             │        │    └──────────────────┘     │
             │        │                              │
             │        └──────────┬──────────────────┘
             │                   ▼
             │        ┌──────────────────────┐
             │        │ StrictProduct        │
             │        │ ConstraintFilter     │
             │        └──────────┬───────────┘
             │                   ▼
             │        ┌──────────────────────┐
             │        │ RagPromptBuilder     │
             │        │ (System Prompt)      │
             │        └──────────┬───────────┘
             │                   ▼
             │        ┌──────────────────────┐
             │        │ DoubaoLlmClient      │
             │        │ (OpenAI-style SSE)   │
             │        └──────────┬───────────┘
             │                   ▼
             │        ┌──────────────────────┐
             │        │ SSE 事件流           │
             │        │ text → product_card  │
             │        │ → done               │
             │        └──────────────────────┘
             │
             ▼
   ┌─────────────────────────┐
   │  ConversationMemory     │
   │  Service (In-Memory)    │
   │  ConversationState      │
   └─────────────────────────┘
```

## 模块说明

### 1. Android 客户端 (Client) ✅
- 对话窗口：文本输入，SSE 流式消费
- 商品卡片：展示 product_id、名称、价格、图片、推荐理由
- 状态管理：ViewModel + StateFlow
- 网络层：OkHttp（SSE 消费）+ Gson JSON 解析
- 商品浏览页 (ProductBrowseScreen)：瀑布流网格，分类浏览
- 商品详情页 (ProductDetailScreen)：大图 + 规格参数表 + 描述
- 悬浮 Agent (FloatingBot + MiniChatPanel)：页面上下文感知导购
- 图片加载：Coil 库，加载失败自动 placeholder
- 支持页面上下文 (page_context) 传入

### 2. Spring Boot API 层 ✅
- RESTful API：健康检查、商品查询、商品搜索、RAG 调试、评估
- SSE：对话接口流式返回（text + product_card + done/error）
- 全局异常处理、CORS 支持
- 暂不实现用户认证（后续阶段）

### 3. 查询理解模块 (Query Understanding) ✅
- **RetrievalRouter**：9 种 intent（SMALLTALK/HELP/THANKS 不检索，其余 6 种检索）
- **QueryAnalyzer**：规则型引擎，IDIOM_MAP 覆盖主流电商搜索口语，13 种价格表达解析，品牌/成分排除
- **ConversationState**：15 字段会话状态，category/subCategory/price/brand/positiveKeywords/negativeKeywords/recommendedProductIds 上下文继承
- **LLMQueryPlanner**：LLM 结构化意图理解，填充 QueryPlan（intent/target/price/brands/softKeywords/queryVariants/answerMode）
- **QueryPlanValidator**：校验 category/subCategory/price/brands/attributes 合法性
- **QueryPlanGatingService**：assist mode 下决定 planner 是否接管检索（confidence 阈值、intent 白名单、legacy completeness 检查）
- **QueryRewriteService**：SoftSemanticLexicon (22 条目) + LLMQueryRewriter，disabled/lexicon/llm/hybrid 四模式

### 4. RAG 检索模块 ✅
- Parent-Child Chunk 设计：7 种 ChunkType (PRODUCT_PROFILE/DESCRIPTION/SPECS/SEARCH_SUMMARY/REVIEW_SUMMARY/FAQ/MARKETING_COPY)
- Embedding 抽象：MockEmbeddingProvider / OpenAIStyleEmbeddingProvider / ArkMultimodalEmbeddingProvider
- QdrantVectorStoreService：Qdrant REST API (Java HttpClient)，确定性 UUID v3 point ID，filter builder
- HybridCandidateRetriever：vector/keyword/hybrid 三种模式，auto fallback keyword
- CandidateFusionService：0.65 × vectorScore + 0.35 × keywordScore 融合
- StrictProductConstraintFilter：统一硬过滤 (category/subCategory/price/negativeBrands/negativeKeywords/excludeProductIds)
- 评估框架：20+ eval queries，category/subCategory/price 约束校验

### 5. 对话管理模块 (ConversationMemory) ✅
- InMemoryConversationMemoryService：ConcurrentHashMap 存储
- ConversationState：跨轮次累积 context (category/subCategory/price/positiveKeywords 等)
- 不持久化（服务重启丢失）

### 6. LLM 服务 ✅
- DoubaoLlmClient：兼容 OpenAI-style Chat Completions API + SSE 流式解析
- MockLlmClient：不调用外部 API，模拟流式输
- 超时保护 + 错误处理 (401/429/5xx)

### 7. 部署 ✅
- Dockerfile：multi-stage build (Maven → JRE)
- Docker Compose：backend + Qdrant，volume 持久化
- Nginx：HTTPS 反代 + SSE 专用配置
- Smoke test 脚本 (PS1 + Bash)
- 完整部署文档 (docs/deployment_mvp.md)