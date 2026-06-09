# RAG 检索增强生成设计

> 当前已实现 RAG Pipeline + 真实 Doubao LLM + SSE 流式返回。支持 Mock/真实 LLM 切换。

## 核心约束

> **本系统不是普通聊天机器人，而是基于商品库的可信导购系统。**

1. 所有推荐商品必须来自商品库检索结果
2. 不允许 LLM 凭空推荐不在候选列表中的商品
3. 不允许编造商品价格、库存、优惠、功效
4. 每个推荐商品必须包含 `product_id`
5. 推荐理由必须来自商品字段（名称、描述、属性、评分等）
6. 无匹配商品时必须明确说明"商品库暂无合适的商品"

## 当前实现状态

### 已实现（第四阶段）

```
用户输入 ("推荐一款适合油皮的洗面奶")
  │
  ├─ 1. CandidateProductRetriever
  │    └─ 调用 ProductService.search() 关键词检索
  │    └─ 返回 Top-N 候选商品（默认 5，最大 10）
  │
  ├─ 2. RagPromptBuilder
  │    └─ 构造 System Prompt（角色设定 + 7 条约束规则）
  │    └─ 注入候选商品列表（product_id/name/brand/category/sub_category/price/description）
  │    └─ 注入用户问题
  │
  ├─ 3. LlmClient（接口）
  │    ├─ MockLlmClient（默认）
  │    │    └─ 不调用外部 API
  │    │    └─ 根据是否有候选商品返回固定模板文本
  │    │    └─ 模拟流式输出（4 字符 chunk）
  │    │
  │    └─ DoubaoLlmClient（MOCK_LLM_ENABLED=false 时启用）
  │         └─ Java 11+ HttpClient 调用 OpenAI-style Chat Completions API
  │         └─ stream=true 流式请求
  │         └─ 解析 SSE 响应，提取 choices[0].delta.content
  │         └─ 转发给 onText 回调
  │         └─ 401/429/5xx 错误处理
  │
  └─ 4. ChatService → SseEmitter
       └─ text 事件：{"content":"导购文本片段"}（JSON 格式）
       └─ product_card 事件：每个候选商品一条卡片
       └─ done 事件：{}
```

### 待实现

- Embedding 接口（EmbeddingProvider）
- 向量存储（VectorStoreService → Qdrant）
- 向量检索（VectorRetriever）
- 多轮对话上下文管理

## LLM 模式切换

### 配置方式

通过环境变量或 application.yml 配置：

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `app.chat.mock-llm-enabled` | `MOCK_LLM_ENABLED` | `true` | 是否使用 Mock LLM |
| `app.llm.base-url` | `LLM_BASE_URL` | `https://ark.cn-beijing.volces.com/api/v3` | LLM API 地址 |
| `app.llm.api-key` | `LLM_API_KEY` | 空 | LLM API Key |
| `app.llm.model` | `LLM_MODEL` | 空 | LLM 模型名称 |
| `app.llm.timeout-seconds` | `LLM_TIMEOUT_SECONDS` | `30` | 请求超时（秒） |

### 切换逻辑

1. `MOCK_LLM_ENABLED=true`（默认）→ 使用 MockLlmClient，不调用外部 API
2. `MOCK_LLM_ENABLED=false` → 使用 DoubaoLlmClient，调用真实 LLM API
3. `MOCK_LLM_ENABLED=false` 但 `LLM_API_KEY` 为空 → 启动时报错
4. `MOCK_LLM_ENABLED=false` 但 `LLM_MODEL` 为空 → 启动时报错

### DoubaoLlmClient 流式解析

1. 构造 OpenAI-style Chat Completions 请求（`stream: true`）
2. 发送 POST 请求到 `{baseUrl}/chat/completions`
3. 读取 SSE 响应流，逐行解析 `data: {...}` 行
4. 从 JSON 中提取 `choices[0].delta.content` 字段
5. 遇到 `data: [DONE]` 时结束流
6. 对 401（认证失败）、429（频率超限）、5xx（服务异常）给出清晰错误信息

## 完整检索流程（规划）

```
用户输入 ("我想买一双适合跑步的鞋")
  │
  ├─ 1. 意图识别
  │    └─ 分类: 商品搜索 → 跑步鞋
  │
  ├─ 2. 条件抽取
  │    └─ 类目: 运动/跑步鞋, 场景: 跑步
  │
  ├─ 3. Embedding
  │    └─ 将用户查询文本转换为向量 (通过 API 调用)
  │
  ├─ 4. 向量检索
  │    └─ ANN 搜索 Top-20 候选商品
  │
  ├─ 5. 结构化过滤
  │    └─ 按类目过滤、价格区间过滤
  │
  ├─ 6. 重排序
  │    └─ Reranker 精排 + 业务因子 (评分、销量)
  │
  ├─ 7. Prompt 构造
  │    └─ 注入候选商品列表 + 约束规则
  │
  └─ 8. LLM 生成
       └─ SSE 流式返回 (text + product_card 事件)
```

## SSE 事件流设计

| 事件类型 | 方向 | 携带数据 | 说明 |
|----------|------|----------|------|
| `text` | 服务端→客户端 | `{"content":"文本片段"}` | LLM 生成的导购说明文本片段 |
| `product_card` | 服务端→客户端 | `{"product_id","name","price","currency","image_url","reason"}` | 推荐的商品卡片 |
| `error` | 服务端→客户端 | `{"code","message"}` | 错误信息 |
| `done` | 服务端→客户端 | `{}` | 流结束标记 |

**事件顺序**: `text → text → ... → product_card → product_card → ... → done`

当前事件顺序：
1. `text`：LLM 流式输出导购说明（多个 text chunk，每个为 JSON 格式）
2. `product_card`：每个候选商品一条卡片事件，含后端确定性生成的 `reason`
3. `done`：流结束

**product_card.reason 生成规则**：
- 由 `RecommendationReasonService` 基于 `QueryAnalysisResult.softKeywords` / `positiveKeywords` / 商品真实字段 / 价格约束确定性生成
- 不调用 LLM 逐卡片生成，不编造商品没有的功能
- 优先匹配商品文本中的 softKeyword，最多展示 2 个关键词
- 示例：`匹配「轻量、缓震」需求，适合跑步训练。` / `符合跑步鞋筛选，同时满足 ¥1000 以下预算。`
- 兜底：`符合当前检索条件，可优先查看。`

## Prompt 设计

### System Prompt 模板（已实现）

```
你是电商导购助手。你只能基于候选商品列表回答用户问题。

规则：
1. 只能基于下方「候选商品列表」中的商品回答问题
2. 不得编造商品、价格、库存、优惠或功效
3. 如果候选商品中没有合适的，明确说明"商品库中暂无合适的商品"
4. 推荐商品时必须引用 product_id
5. 推荐理由必须来自候选商品字段（name、brand、category、sub_category、price、description、specs 等）
6. 不得推荐候选商品列表之外的任何商品
7. 回复语气自然、专业，像一位有经验的导购
```

### 候选商品列表格式（注入 Prompt）

```
1. product_id: p_beauty_010
   name: 芙丽芳丝净润洗面霜
   brand: 芙丽芳丝
   category: 美妆护肤
   sub_category: 洁面
   price: 89 CNY
   description: 温和氨基酸洁面...
```

### 无候选商品 Prompt

```
用户问题：{message}

候选商品列表：（空）

当前商品库中没有找到与用户需求匹配的商品，请如实告知用户。
```

## 模块划分 (Java 实现)

| 模块 | 核心类 | 状态 | 职责 |
|------|--------|------|------|
| 候选召回 | `CandidateProductRetriever` | ✅ 已实现 | 已被 HybridCandidateRetriever 取代 |
| Hybrid 召回 | `HybridCandidateRetriever` | ✅ 已实现 | keyword/vector/hybrid 三种模式，默认 hybrid |
| 关键词检索 | `KeywordRetriever` | ✅ 已实现 | 轻量封装 ProductService.search() |
| 向量检索 | `VectorRetriever` | ✅ 已实现 | 封装 RagVectorIndexService，空 store 返回空列表 |
| 融合排序 | `CandidateFusionService` | ✅ 已实现 | 规则融合，0.65 vector + 0.35 keyword |
| Prompt | `RagPromptBuilder` | ✅ 已实现 | 构造 System Prompt + 候选商品列表 + 用户问题 |
| LLM | `LlmClient` (接口) | ✅ 已实现 | 流式生成接口定义 |
| LLM Mock | `MockLlmClient` | ✅ 已实现 | 不调用外部 API，模拟流式输出 |
| LLM 真实 | `DoubaoLlmClient` | ✅ 已实现 | Doubao-Seed-2.0-lite API 调用，Java HttpClient + SSE 流式解析 |
| LLM 配置 | `LlmConfig` | ✅ 已实现 | Mock/Doubao 切换 + 配置校验 |
| 对话服务 | `ChatService` | ✅ 已实现 | 编排召回→Prompt→LLM→SSE 事件流，含二次校验和空结果处理 |
| 推荐理由 | `RecommendationReasonService` | ✅ 已实现 | 基于 softKeywords / 商品字段 / 价格约束确定性生成 product_card.reason，不调用 LLM |
| 对话接口 | `ChatController` | ✅ 已实现 | POST /api/chat/stream |
| Embedding Mock | `MockEmbeddingProvider` | ✅ 已实现 | 文本 hash → 确定性伪向量，L2 归一化 |
| Embedding 真实 | `OpenAIStyleEmbeddingProvider` | ✅ 已实现 | 兼容 OpenAI-style /v1/embeddings，batch 拆批，维度检查 |
| Embedding Ark | `ArkMultimodalEmbeddingProvider` | ✅ 已实现 | 火山 Ark /embeddings/multimodal，input 对象数组，预留 image_url |
| 向量存储 | `VectorStoreService` | ✅ 已实现 | InMemoryVectorStoreService（默认）+ QdrantVectorStoreService（VECTOR_STORE=qdrant） |
| Qdrant 管理 | `QdrantCollectionManager` | ✅ 已实现 | collection 创建/检查/计数，基于 REST API |
| Payload 过滤 | `QdrantFilterBuilder` | ✅ 已实现 | category/sub_category/brand/product_id/chunk_type/price range |
| 向量索引 | `RagVectorIndexService` | ✅ 已实现 | 编排 documents → embed → upsert → search |
| 向量检索 | `VectorRetriever` | ✅ 已实现 | 封装 RagVectorIndexService |
| 关键词检索 | `KeywordRetriever` | ✅ 已实现 | 轻量封装 ProductService.search() |
| 融合排序 | `CandidateFusionService` | ✅ 已实现 | 规则融合，0.65 vector + 0.35 keyword |
| Hybrid 召回 | `HybridCandidateRetriever` | ✅ 已实现 | keyword/vector/hybrid，接入 ChatService |
| 硬约束过滤 | `StrictProductConstraintFilter` | ✅ 已实现 | 统一最终硬过滤入口：category/subCategory/price/negativeBrands/negativeKeywords/excludeProductIds，HybridCandidateRetriever + KeywordRetriever 共用 |
| 过滤结果 | `ConstraintCheckResult` | ✅ 已实现 | passed/passedRules/failedRules 现场记录 |
| 重排序 | `Reranker` | ❌ 待实现 | 检索结果精排 + 业务排序 |
| 推荐数量 | `RecommendationCountResolver` | ✅ 已实现 | 根据 query 判断推荐数量：一款/一个/一双→1，几款/有哪些→3 |
| 知识库 | `ProductService` + `JsonLoader` | ✅ 已实现 | 商品数据加载、更新 |

## 检索策略

### 当前阶段：Hybrid Retrieval（关键词 + 向量融合）+ Mock/真实 LLM

当前 `ChatService` 使用 `HybridCandidateRetriever` 进行候选召回：

1. 支持三种模式（通过 `app.retrieval.mode` 配置）：
   - `keyword`：仅使用 `KeywordRetriever` → `ProductService.search()`
   - `vector`：仅使用 `VectorRetriever` → `RagVectorIndexService.search()`
   - `hybrid`（默认）：两路召回 → `CandidateFusionService.merge()` 融合排序
2. 融合规则：finalScore = 0.65 × vectorScore + 0.35 × keywordScore
3. 如果 vector index 未构建（count = 0），hybrid 模式自动 fallback keyword
4. 检索到 chunk 后按 productId 聚合 → 回填 Product → 转为 ChatCandidate
5. `RagPromptBuilder` 构造含候选商品的 Prompt
6. `LlmClient`（Mock 或 Doubao）流式生成导购文本
7. `ChatService` 通过 SSE 返回 text + product_card + done 事件

### 后续阶段：Qdrant 向量检索

当前已支持通过 `VECTOR_STORE=qdrant` 切换到 QdrantVectorStoreService：
- 使用 Qdrant REST API（Java HttpClient，无额外 SDK 依赖）
- Qdrant point id = vectorPointId（确定性 UUID）
- Qdrant vector = EmbeddedRagChunk.vector（当前 64 维 Mock embedding）
- Qdrant payload = 完整商品元数据（product_id/name/brand/category/price/text 等）
- 支持 Qdrant filters（category/sub_category/brand/product_id/chunk_type/price range）
- 默认 in-memory 模式，通过环境变量切换

### Docker 启动 Qdrant（本地开发）

```
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

配置：
- QDRANT_URL=http://localhost:6333（默认）
- QDRANT_API_KEY 本地可为空
- VECTOR_STORE=qdrant
- 当前 collection = ecommerce_rag_chunks_mock
- 当前 vector size = 64（Mock Embedding）
- 距离 = Cosine

**注意**：接入真实 Embedding API 后向量维度可能变化，需重建 collection。

### 后续阶段：Qdrant 向量检索

接入 Qdrant 后通过 `VectorStoreService` 接口替换实现类：
- 文本 Embedding 转为向量 → ANN 搜索
- 向量相似度 + 关键词匹配融合排序
- 支持元数据过滤（类目、价格区间）

## Parent-Child RAG 索引文档设计

### 设计原则

本项目采用**商品实体中心的 Parent-Child Hybrid RAG** 设计：

1. **Product** 是 parent（父文档）
2. **RagChunkDocument** 是 child（子 chunk）
3. child chunk 用于后续向量检索
4. 检索命中 child 后，通过 productId 回填完整 Product
5. 商品卡片仍然来自 ProductService，不允许由 LLM 生成
6. Qdrant 后续只存 child chunk 的向量和 payload

### RagChunkDocument 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| chunkId | String | 唯一 chunk ID（格式：chunk_ + UUID 无连字符） |
| parentId | String | 父文档 ID，当前等于 productId |
| productId | String | 关联的商品 ID，用于回填 Product |
| chunkType | String | 切片类型（PRODUCT_PROFILE / DESCRIPTION / SPECS / SEARCH_SUMMARY） |
| text | String | 用于送入 Embedding 的文本内容 |
| sourceField | String | 来源字段（profile / description / specs / search_summary） |
| name | String | 商品名称（冗余，方便检索结果展示） |
| brand | String | 品牌 |
| category | String | 主类目 |
| subCategory | String | 子类目 |
| price | BigDecimal | 价格 |
| currency | String | 货币单位 |
| avgRating | Double | 平均评分 |
| imageUrl | String | 商品图片 URL |
| metadata | Map\<String, String\> | 元数据（product_id / category / sub_category / brand / price / currency / avg_rating / chunk_type / source_field），用于 Qdrant payload 过滤 |

### Chunk 类型

| 类型 | 用途 | 来源字段 | 生成条件 |
|------|------|----------|----------|
| PRODUCT_PROFILE | 匹配商品整体身份、品牌、类目、价格 | profile | 每个商品必生成 |
| DESCRIPTION | 匹配语义需求（如"油皮""保湿"） | description | 每个商品必生成 |
| SPECS | 匹配规格属性（如容量、颜色） | specs | 仅在 specs 非空时生成 |
| SEARCH_SUMMARY | 综合检索，提高召回稳定性 | search_summary | 每个商品必生成 |
| REVIEW_SUMMARY | 匹配用户评价语义（如"好用吗""反馈"） | review_summary | 仅在 reviewSummary 非空时生成 |
| FAQ | 匹配常见问题（如"能装电脑吗""适合敏感肌吗"） | faq_summary | 仅在 faqSummary 非空时生成 |
| MARKETING_COPY | 匹配卖点语义（如"主要卖点""亮点"） | marketing_copy | 仅在 marketingCopy 非空时生成 |

> 当前阶段：REVIEW_SUMMARY / FAQ / MARKETING_COPY 已接入。原始数据中的 user_reviews 和 official_faq 通过 convert_teacher_dataset.py 脱敏聚合后写入 products.json，再由 RagDocumentBuilder 自动生成对应 chunk。如果原始数据没有这些字段，这些 chunk 可能为空。
>
### text 拼接规则

1. null / 空字符串字段跳过
2. specs 为空时不生成规格文本
3. text 超过 800 字时截断到 800 字（后续可改为固定窗口切割）
4. imageUrl 不放入 text
5. price 进入 profile 文本，但后续仍需作为 metadata 过滤字段
6. 不引入复杂 NLP 分词库

### 后续流程

```
RagChunkDocument
  → EmbeddingProvider.embed(text) → 向量
    → Qdrant upsert(chunkId, 向量, payload=metadata)
      → VectorRetriever.search(query_embedding) → chunkId list
        → 通过 productId 回填 Product
          → 现有 RagPromptBuilder → LlmClient → SSE
```

### 当前状态（第二阶段）

- ✅ RagChunkDocument 数据模型（含 vectorPointId）
- ✅ ChunkType 枚举
- ✅ RagChunkIdGenerator（确定性 chunkId + vectorPointId）
- ✅ RagDocumentBuilder（Product → List\<RagChunkDocument\>）
- ✅ RagDocumentService（依赖 ProductService + RagDocumentBuilder）
- ✅ 预览 API（GET /api/rag/chunks/preview, /product/{id}, /stats）
- ✅ EmbeddingProvider 接口
- ✅ MockEmbeddingProvider（文本 hash → 确定性 L2 归一化向量，默认 64 维）
- ✅ VectorStoreService 接口
- ✅ InMemoryVectorStoreService（ConcurrentHashMap + cosine similarity + 过滤）
- ✅ RagVectorIndexService（编排 documents → embed → upsert → search）
- ✅ 向量索引调试 API（POST /api/rag/vector-index/rebuild, GET /api/rag/vector-index/stats, POST /api/rag/vector-search）
- ❌ 不接入真实 Qdrant
- ❌ 不调用真实 Embedding API

### chunkId 生成规则

chunkId 格式：`{productId}::{chunkType}::{index}`

示例：
- `p_beauty_001::PRODUCT_PROFILE::0`
- `p_beauty_001::DESCRIPTION::0`
- `p_beauty_001::SPECS::0`
- `p_beauty_001::SEARCH_SUMMARY::0`

同一 productId + chunkType + index 每次构建得到相同 chunkId。

### vectorPointId 生成规则

```java
UUID.nameUUIDFromBytes(("rag-chunk:" + chunkId).getBytes(StandardCharsets.UTF_8)).toString()
```

同一 chunkId 每次生成相同 vectorPointId（确定性 UUID v3），用于 Qdrant point id。

### Embedding 抽象

当前使用 MockEmbeddingProvider，不调用外部 API：
- 文本 hash → Random seed → 高斯分布随机向量 → L2 归一化
- 同一文本每次返回相同向量（确定性）
- 维度可配置，默认 64
- 后续替换为真实 DoubaoEmbeddingProvider 等实现

### Vector Store 抽象

当前使用 InMemoryVectorStoreService：
- ConcurrentHashMap 存储 vectorPointId → EmbeddedRagChunk
- Cosine similarity 计算相似度
- 支持 filters（category/sub_category/brand/product_id/chunk_type/max_price/min_price）
- 后续 QdrantVectorStoreService 替代该实现

---

## Ark + Qdrant 联调前检查清单

在切换到 `EMBEDDING_PROVIDER=ark-multimodal` + `VECTOR_STORE=qdrant` 进行真实联调前，请逐项确认：

### 配置一致性

1. **`EMBEDDING_DIMENSION` 必须等于 `QDRANT_VECTOR_SIZE`**。两者不一致时 rebuild 会直接失败，并在日志中提示错误信息。
2. **更换 embedding 模型后必须更换 `QDRANT_COLLECTION` 或重建 collection**。不同模型的向量不可混用，否则检索结果无意义。
3. **不要把真实 `EMBEDDING_API_KEY` 写入任何文件**。只能通过环境变量临时设置（PowerShell: `$env:EMBEDDING_API_KEY="..."`）。

### 数据安全

4. **Qdrant 本地数据目录不要提交 Git**。默认存储在 `./qdrant_storage`，确保已加入 `.gitignore`。
5. **vector index rebuild 会调用真实 Ark API**，可能产生费用或消耗额度。建议先用小规模数据验证后再全量构建。

### 故障排查

6. **如果 rebuild 失败，先检查**：
   - Ark API Key 是否正确（`$env:EMBEDDING_API_KEY`）；
   - `EMBEDDING_MODEL` 是否正确（如 `doubao-embedding-vision-251215`）；
   - Ark 接口路径是否为 `/embeddings/multimodal`（`$env:ARK_MULTIMODAL_EMBEDDING_PATH`）；
   - Qdrant 是否已启动（`docker ps` 确认容器运行中）；
   - `QDRANT_VECTOR_SIZE` 是否与 embedding 返回维度一致。

7. **如果 vector-search 返回结果不合理，不一定是代码错误**，可能是：
   - query 太短或信息不足；
   - chunk text 不够丰富，无法表达商品语义；
   - metadata filter 缺失导致范围过大；
   - embedding 模型不适合当前商品的文本检索场景；
   - 需要引入 QueryAnalyzer 或 rerank 模块进一步优化。

### 环境恢复

8. 调试完成后恢复默认配置：
   ```powershell
   $env:EMBEDDING_PROVIDER="mock"
   $env:VECTOR_STORE="in-memory"
   ```
   后续 `mvn test` 将使用 MockEmbeddingProvider + InMemoryVectorStoreService，不依赖外部服务。

---

## RAG Real Integration Checklist

完成上述「Ark + Qdrant 联调前检查清单」后，按以下步骤执行真实联调和检索质量评估。

### 步骤

1. **启动 Qdrant**
   ```powershell
   docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant
   ```

2. **设置 Ark Embedding 环境变量**
   ```powershell
   $env:EMBEDDING_PROVIDER="ark-multimodal"
   $env:EMBEDDING_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
   $env:EMBEDDING_API_KEY="your_ark_api_key_here"
   $env:EMBEDDING_MODEL="doubao-embedding-vision-251215"
   $env:EMBEDDING_DIMENSION="你的 Ark embedding 实际维度"
   $env:ARK_MULTIMODAL_EMBEDDING_PATH="/embeddings/multimodal"
   ```

3. **设置 Qdrant 环境变量**
   ```powershell
   $env:VECTOR_STORE="qdrant"
   $env:QDRANT_URL="http://localhost:6333"
   $env:QDRANT_COLLECTION="ecommerce_rag_chunks_ark"
   $env:QDRANT_VECTOR_SIZE="你的 Ark embedding 实际维度"
   ```

4. **启动后端**
   ```powershell
   cd server
   mvn spring-boot:run
   ```

5. **重建索引**（会调用真实 Ark API，可能产生费用）
   ```powershell
   Invoke-RestMethod -Method Post http://localhost:8080/api/rag/vector-index/rebuild
   ```

6. **检查索引状态**
   ```powershell
   Invoke-RestMethod http://localhost:8080/api/rag/vector-index/stats
   ```

7. **单条向量搜索**
   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/rag/vector-search `
     -ContentType "application/json" `
     -Body '{"query":"油皮洗面奶","limit":5}'
   ```

8. **检索调试**
   ```powershell
   Invoke-RestMethod "http://localhost:8080/api/rag/retrieval/debug?query=油皮洗面奶&limit=5"
   ```
   查看每个候选的 `vector_score`、`keyword_score`、`final_score`、`matched_sources`、`matched_chunk_types`。

9. **执行评估**
   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/rag/eval/run `
     -ContentType "application/json" `
     -Body '{"top_k":5}'
   ```

10. **执行 Chat Stream**
    ```powershell
    Invoke-WebRequest `
      -Uri http://localhost:8080/api/chat/stream `
      -Method Post `
      -ContentType "application/json" `
      -Body '{"message":"推荐一款适合油皮的洗面奶","limit":3}'
    ```

11. **记录失败 query**
    - 查看 eval 返回的 `failed_queries` 列表
    - 对每条失败 query 单独运行 `/api/rag/eval/run-one` 深入分析

12. **决定下一步优化方向**
    - 根据失败原因分类（`failure_category`）判断：
      - `category_mismatch` 频繁 → 需要 metadata filter
      - `subcategory_mismatch` 频繁 → 需要 query rewrite / QueryAnalyzer
      - `price_constraint_fail` 频繁 → 需要显式价格过滤
      - 排序不合理 → 需要 reranker
      - 语义召回失败 → 需要增强 chunk 文本或换 embedding 模型

### 评估 API 参考

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rag/eval/queries` | GET | 查看评估查询列表 |
| `/api/rag/eval/run` | POST | 运行全部评估，返回 `RagEvalSummary` |
| `/api/rag/eval/run-one` | POST | 运行单条评估，传入 `RagEvalQuery` 对象 |

### 评估报告

评估完成后，参考 `docs/rag_eval_report_template.md` 模板填写评估报告。

---

## 降级策略

| 场景 | 降级方案 |
|------|----------|
| Embedding API 超时 | 返回错误，提示用户稍后重试 |
| 向量库不可用 | 回退到关键词匹配 |
| 检索结果为空 | Prompt 告知 LLM "候选列表为空"，LLM 回复"暂无合适商品" |
| LLM API 超时 | SSE 仅返回 `product_card` 事件列表，不生成文本 |
| LLM 返回了不存在的商品 ID | 后端校验，过滤无效 product_id |
| LLM API 认证失败 (401) | SSE 返回 error 事件，提示检查 API Key |
| LLM API 频率超限 (429) | SSE 返回 error 事件，提示稍后重试 |
| LLM API 服务异常 (5xx) | SSE 返回 error 事件，提示服务暂不可用 |

---
## 多轮对话上下文管理

### RetrievalRouter

新增 `RetrievalRouter`（规则引擎），在检索前识别用户查询意图。

支持的 intent：

| Intent | 触发条件 | 是否检索 |
|--------|----------|----------|
| SMALLTALK | "你好"、"在吗"、"你是谁" | 否 |
| HELP | "怎么用"、"你能做什么"、"帮助" | 否 |
| THANKS | "谢谢"、"好的"、"知道了" | 否 |
| PRODUCT_SEARCH | "推荐"、"有没有"、"我想买"、"适合" | 是 |
| REFINE_PREVIOUS_QUERY | "便宜一点"、"轻一点"、"再便宜点"、"预算500" | 是 |
| NEGATIVE_CONSTRAINT | "不要"、"排除"、"除了"、"不含" | 是 |
| CHANGE_OR_MORE | "换一个"、"还有吗"、"还有什么"、"其他的" | 是 |
| COMPARE_PRODUCTS | "A和B哪个好"、"对比"、"比较" | 是（仅标记） |
| UNKNOWN | 其他 | 是 |

**不需要检索的 intent（SMALLTALK / HELP / THANKS）** 由 ChatService 直接返回简单文本回复，不发送 `product_card`。

### ConversationState

`ConversationState` 保存单次会话的上下文状态：

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话标识 |
| lastUserQuery | String | 上一轮用户原始查询 |
| lastResolvedQuery | String | 上一轮解析后查询 |
| category | String | 继承的类目 |
| subCategory | String | 继承的子类目 |
| subCategories | List\<String\> | 多子类目 |
| minPrice | BigDecimal | 最低价格 |
| maxPrice | BigDecimal | 最高价格 |
| positiveKeywords | List\<String\> | 正向关键词 |
| negativeKeywords | List\<String\> | 负向关键词 |
| negativeBrands | List\<String\> | 排除品牌 |
| recommendedProductIds | List\<String\> | 已推荐商品 ID |
| candidateProductIds | List\<String\> | 候选商品 ID |
| turnCount | int | 对话轮次 |
| updatedAt | Instant | 更新时间 |

实现：`InMemoryConversationMemoryService`，基于 `ConcurrentHashMap`，sessionId 为 null 时默认使用 "default"。

更新逻辑：
1. 第一次查询后，保存 category / subCategory / positiveKeywords。
2. 返回候选商品后，保存 recommendedProductIds。
3. 第二轮补充条件时，继承上一轮的 category/subCategory。

### Contextual QueryAnalyzer

新增 `analyze(String query, ConversationState context)` 方法：

**上下文继承规则**（当 turnCount > 0 时生效）：

| 当前 query 类型 | 继承行为 |
|----------------|---------|
| "要轻量的" | 继承 context.category + context.subCategory，normalizedQuery = "跑步鞋 轻量" |
| "预算500以内" | 继承 context.category + context.subCategory + maxPrice=500，normalizedQuery = context.subCategory |
| "除了耐克还有什么" | 继承 context.category + context.subCategory + negativeBrands+=[Nike,耐克] |
| "换一个 / 还有吗" | 继承 context.category + context.subCategory + excludeProductIds+=context.recommendedProductIds |
| "再便宜点" | 继承 context，maxPrice = context.maxPrice × 0.8，或加入性价比关键词 |
| "不要日系" | negativeBrands += 日系品牌（16 个） |
| "不要含酒精" | negativeKeywords += [酒精,乙醇,alcohol] + warnings |
| "学生党" | positiveKeywords += [性价比,低价,实用] |

**价格解析规则**（修复后支持 13 种常见表达）：

| 表达式 | 解析结果 |
|--------|----------|
| "预算1000以内""预算1000""预算 1000" | maxPrice=1000 |
| "1000以内""1000以下""1000元以下" | maxPrice=1000 |
| "不超过1000""不超过 1000 元""低于1000" | maxPrice=1000 |
| "500到1000元""500-1000元""500~1000元" | minPrice=500, maxPrice=1000 |

价格短语从 normalizedQuery 中移除，价格数字不作为关键词参与检索。 |

### Negative Constraints 处理

检索链路中的负约束过滤顺序：

1. **Qdrant metadata filter**：category / subCategory / price / brand 硬条件
2. **StrictProductConstraintFilter**（统一最终硬过滤入口）：
   - category 匹配（含 alias 匹配）
   - subCategory / subCategories 匹配（含 alias 匹配）
   - minPrice / maxPrice 区间过滤
   - negativeBrands 排除（Nike ↔ 耐克 互相识别）
   - excludeProductIds 直接移除
   - negativeKeywords / avoidIngredientsOrTerms 在 name + description + specs 文本中排除
3. **boostedProductIds**：候选商品在 boostedProductIds 中 → finalScore + 0.05

**当前限制**：
- 无商品成分结构化字段时，"不含酒精"只能基于描述文本排除，不能保证真实成分完整性。
- "除了耐克" 不能返回 Nike/耐克品牌商品（硬过滤）。
- "换一个" 不能返回上一轮推荐过的商品（excludeProductIds）。

### StrictProductConstraintFilter（统一硬约束过滤）

> 从 Bugfix 第一阶段开始，`StrictProductConstraintFilter` 已成为唯一最终硬过滤入口。`HybridCandidateRetriever` 和 `KeywordRetriever` 都使用同一个 filter 实例。

职责：
- 根据 `QueryAnalysisResult` 对 `Product` 执行最终硬约束判断
- 不调用 LLM、Embedding 或 Qdrant
- 过滤原因可用于日志和 retrieval debug

检查规则：

| 检查项 | 字段来源 | 规则 |
|--------|----------|------|
| category | QueryAnalysisResult.category | CategoryMatchService alias 匹配 |
| subCategory / subCategories | QueryAnalysisResult.subCategory / subCategories | CategoryMatchService alias 匹配，多候选兼容 |
| minPrice | QueryAnalysisResult.minPrice | product.price >= minPrice |
| maxPrice | QueryAnalysisResult.maxPrice | product.price <= maxPrice |
| negativeBrands | QueryAnalysisResult.negativeBrands | brand 不在列表中，Nike ↔ 耐克 互相识别 |
| excludeProductIds | QueryAnalysisResult.excludeProductIds | productId 不在排除列表中 |
| negativeKeywords | QueryAnalysisResult.negativeKeywords + avoidIngredientsOrTerms | product.name + description + specs 中不含关键词 |

`ConstraintCheckResult` 记录每次检查的 passed/failedRules。如果所有候选都不满足硬约束，返回空列表，不允许 fallback 到其他类目（除非 analysis 没有 category/subCategory/price 等硬约束）。

### 当前未实现

- BGE Reranker
- 图片找货
- LLM query rewrite（已预留接口，默认不开启）
- 多模态输入
- 数据库持久化

### Query Understanding 基础设施（第一阶段：taxonomy + schema + validator）

> **状态：已实现基础设施，不接管实际检索。**

新增 `server/src/main/java/com/ecommerce/rag/rag/understanding/` 包：

| 类 | 职责 |
|----|------|
| `CatalogTaxonomyService` | 从 ProductService 动态统计当前商品库的 categories / subCategories 映射 / brands / 价格范围 |
| `CatalogTaxonomySnapshot` | taxonomy 快照数据类 |
| `QueryPlan` | 结构化 query intent 数据类（20+ 字段） |
| `QueryPlanTarget/Price/Brands/Attributes` | 子结构 |
| `QueryPlanValidator` | 校验 QueryPlan 合法性，轻量修正 |
| `QueryPlanValidationResult` | 校验结果 |

### Query Understanding 第二阶段：LLMQueryPlanner Shadow Mode

> **状态：已实现 LLMQueryPlanner，shadow mode，不接管真实检索。**

| 类 | 职责 |
|----|------|
| `QueryPlannerPromptBuilder` | 构造 planner prompt：注入 taxonomy 目录 + conversation_state + page_context + QueryPlan schema + 12 条严格规则 |
| `QueryPlanJsonParser` | 从 LLM 输出提取 JSON：支持纯 JSON / ```json fence / 前后说明文字 |
| `LLMQueryPlanner` | 编排 prompt→LlmClient→parse→validate 全流程，超时保护，失败不抛 500 |
| `QueryPlanningResult` | 结果 POJO：plannerEnabled/mode/rawPlan/validatedPlan/parseSuccess/valid/latencyMs/source |
| `QueryUnderstandingResult` | 统一结果：query + legacyAnalysis + planningResult + plannerUsedForRetrieval |
| `QueryUnderstandingService` | 编排 legacy QueryAnalyzer + LLMQueryPlanner + gate + mapper，产出 effectiveAnalysis（Phase 4A: shadow → Phase 4B: assist takeover） |

**配置** (`application.yml`):
```yaml
app.understanding.planner:
  enabled: false          # 默认关闭
  mode: shadow            # shadow 模式
  timeout-seconds: 10
  max-taxonomy-items: 80
  include-brands: true
  cache-enabled: true
```

**Debug API**：
- `GET /api/rag/understanding/taxonomy` — 查看当前 taxonomy
- `POST /api/rag/understanding/validate-plan` — 校验 QueryPlan JSON
- `POST /api/rag/understanding/plan` — shadow mode 对比：legacy analysis + LLM planner result

**StrictProductConstraintFilter 仍然保持最终硬过滤，不受此影响。**

### 购物车语义理解

购物车相关语义采用 **Semantic Frame + Rule Match + LLM Assist Slot Filling** 三层结构处理：

1. **Semantic Frame Layer** (`CartSemanticFrameCatalog` + `CartSemanticFrameMatcher`): 
   - 预设 5 个语义框架：`cart.summary`, `cart.amount_gap_query`, `cart.completion_recommend`, `cart.completion_clarify`, `cart.add_item`
   - 规则层匹配 EXACT/PARTIAL/NONE，发现可能的购物车语义
   - 反模式排除：`推荐2000元以内的电脑` ≠ 购物车
   - 规则层不作为最终判断器，只输出匹配信息和槽位提取

2. **LLMQueryPlanner**: 
   - Prompt 注入全部可用语义框架（含义、槽位、正例、反例）
   - Prompt 注入 CartSemanticMatchResult（matchLevel / candidateFrameIds / extractedSlots / missingSlots）
   - LLM 只能在预定义 semantic frames 中选择，不允许发明新 frameId
   - PARTIAL 匹配时 LLM 负责判断语义等价性并补齐槽位

3. **Backend Deterministic Execution**: 
   - `AMOUNT_GAP_QUERY`：仅计算差额并返回 text，不推荐商品
   - `COMPLETION_RECOMMEND`：计算差额 + CartTopUpRecommendationService 推荐商品
   - CartService 读取真实购物车，ProductService 获取真实商品
   - LLM 不编造购物车金额、商品、product_id

CartPlan action 统一为：`CART_SUMMARY | AMOUNT_GAP_QUERY | COMPLETION_RECOMMEND | ADD_TO_CART | REMOVE_FROM_CART`

凑单推荐策略：gap = targetAmount - cart.totalAmount，排除已有商品，按 abs(gap - price) 升序排序，tolerance 默认 100 元，最多 3 个推荐。

### Query Understanding 第四阶段 B：ChatService Assist 模式局部接管（2026-06-03）

> **状态：已实现。assist 模式下 ChatService 根据 gating 条件使用 planner effectiveAnalysis 进行真实检索。**

**接管规则：**

| 条件 | 行为 |
|------|------|
| `planner enabled=true` + `mode=assist` + legacy 不完整 + planner 高置信度合法 | 使用 planner effectiveAnalysis |
| `planner enabled=true` + `mode=assist` + legacy 已完整 | 优先 legacy（不强制接管） |
| `planner enabled=true` + `mode=assist` + planner 低置信度/error/invalid | fallback legacy |
| `planner enabled=false` | 使用 legacy（disabled mode） |
| `mode=shadow` | 使用 legacy（shadow mode） |

**legacy 不完整判断（isLegacyIncomplete）：**
1. legacy.category + legacy.subCategory 均为空 → incomplete
2. legacy 有 category+subCategory 但 positiveKeywords 为空，且 planner 有 softKeywords → incomplete
3. legacy.maxPrice 为空，但 planner 有合法 maxPrice → incomplete
4. legacy intent 与 planner intent 不一致，且 planner 是 REFINE_PREVIOUS_QUERY / NEGATIVE_CONSTRAINT → incomplete

**核心保障：**
- ConversationState 保存 effectiveAnalysis（planner 或 legacy）的 category/subCategory/maxPrice/positiveKeywords
- 多轮 refinement 能读取上一轮 planner 写入的上下文
- recommendedProductIds 只保存实际发送的 product_card
- Debug controller 与 ChatService 使用同一入口，effectiveAnalysis / selectedSource 一致
- StrictProductConstraintFilter 始终是最终硬过滤

### Query Understanding 第四阶段 A：ChatService Shadow 接入（2026-06-02）

> **状态：已实现。ChatService 已接入 QueryUnderstandingService，shadow 语义确保不改变现有推荐行为。**

`ChatService` 的检索路径已从：

```
RetrievalRouter.route()
→ QueryAnalyzer.analyze(message, state, pageContext)
→ HybridCandidateRetriever.retrieveWithAnalysis(message, limit, analysis)
```

改为：

```
RetrievalRouter.route()
→ QueryUnderstandingService.understandForRetrieval(message, sessionId, pageContext)
→ effectiveAnalysis（assist 模式下 gateDecision.allowed=true 时 = planner analysis，否则 = legacyAnalysis）
→ HybridCandidateRetriever.retrieveWithAnalysis(message, limit, effectiveAnalysis)
```

**核心保障**：
- ChatService 新增 QueryUnderstanding 日志行：query/sessionId/plannerEnabled/plannerMode/selectedSource/plannerUsedForRetrieval/fallbackReason/legacy:[cat/sub/maxPrice]/effective:[cat/sub/maxPrice]
- assist mode 下 `selectedSource=PLANNER` 时 `plannerUsedForRetrieval=true`，`effectiveAnalysis=planner analysis`
- shadow mode 下 `selectedSource=SHADOW_ONLY` 时 `plannerUsedForRetrieval=false`，`effectiveAnalysis=legacyAnalysis`
- planner disabled 时 `selectedSource=DISABLED`，`effectiveAnalysis=legacyAnalysis`
- planner error 时 fallback legacy，不抛 500
- RetrievalRouter 仍用于 SMALLTALK/HELP/THANKS 前置判断
- StrictProductConstraintFilter 仍是最终硬过滤
- `QueryUnderstandingService.understandForRetrieval()` 为空 query/user 返回安全结果，不抛 500
- pageContext / sessionId 正常传入 QueryUnderstandingService
- 本阶段是 shadow 接入，assist/takeover 配置保留但不默认开启

### ChatService 输出一致性

> 从 Bugfix 第二阶段开始，ChatService 保证 Prompt candidates 和 product_card candidates 使用同一批 finalCandidates。

**一致性流程**：

1. `HybridCandidateRetriever.retrieveWithAnalysis()` 返回已通过 StrictProductConstraintFilter 过滤的候选
2. ChatService 立即对 candidate 列表执行**二次 StrictProductConstraintFilter 校验**，生成 `finalCandidates`
3. `memoryService.updateAfterRetrieval()` 在检索完成后**立即**更新 ConversationState（不等 LLM 完成）
4. `RagPromptBuilder.build()` 使用 `finalCandidates` 构造 Prompt
5. `sendProductCards()` 使用 `finalCandidates`，发送前再对每个候选执行 `constraintFilter.passes()` 校验
6. 不满足约束的 candidate 不会被包装为 product_card 发送，并记录 warn 日志
7. `recommendedProductIds` 只保存实际发送的 productId（即通过所有校验的 candidate）

**关键原则**：

```
Prompt candidates = product_card candidates = ConversationState.recommendedProductIds 来源
```

**finalCandidates 为空时的处理**：

- `RagPromptBuilder` 构造"候选商品列表为空"的 Prompt，LLM 将明确告知用户没有满足条件的商品
- SSE 只发送 `text` 和 `done` 事件
- 不发送 `product_card` 事件
- 不允许 LLM 编造商品
- 不允许 fallback 到其他类目或超预算商品

**ConversationState 更新时机**（从检索完成 → LLM 完成 → 改进为检索完成后立即更新）：

```
用户请求
  → QueryAnalyzer.analyze()
    → HybridCandidateRetriever.retrieveWithAnalysis()
      → ChatService.postRetrievalFilter()  二次校验生成 finalCandidates
        →记忆服务 ConversationState 更新 ← 在这里更新
          → RagPromptBuilder.build(finalCandidates)
            → LlmClient.streamGenerate()
              → sendProductCards(finalCandidates) 再次校验
                → SSE done
```

**sessionId 说明**：

- `sessionId` 不传时默认使用 `"default"`
- `"default"` 会跨会话共享上下文
- 前端应传入稳定不变的 sessionId 以支持多轮对话（如 UUID 或用户 ID + 当前页面标识）
- 不同前端页面或用户应使用不同的 sessionId，避免上下文污染

### 输出体验优化

> 从输出体验优化阶段开始，LLM 回复被限制为简短格式，商品卡片数量由 RecommendationCountResolver 控制。

**推荐数量判断**（RecommendationCountResolver）：

| 用户表达 | requestedCount | 说明 |
|----------|---------------|------|
| "推荐一款""推荐一个""推荐一双""哪款最好" | 1 | 明确单数 |
| "推荐几款""有哪些""有什么选择" | 3 | 明确复数 |
| 其他（如"好用的跑鞋"） | 3 | 默认 |

**卡片数量裁剪**：

```
displayLimit = min(requestedCount, maxProductCardLimit, request.limit)
```

- 默认 `maxProductCardLimit = 3`
- 前端传 `limit=10` 时仍最多 3 张卡片
- 用户明确"一款"时只返回 1 张卡片
- 不影响 StrictProductConstraintFilter 过滤规则

**LLM 输出风格**（RagPromptBuilder 4 套模板）：

| 模式 | 触发条件 | 格式 |
|------|----------|------|
| SINGLE_RECOMMENDATION | 1 个 candidate | `推荐你优先看这款：{名} 适合：{一句话} 理由：{一句话}`（≤120字） |
| MULTI_RECOMMENDATION | 2-3 个 candidates | `给你筛了 N 款：1.{名}：{理由} 2.{名}：{理由}`（≤180字） |
| NO_MATCH | 0 个 candidate | `没找到满足条件的商品，放宽条件再试试` |
| CURRENT_PRODUCT_QA | PRODUCT_DETAIL | `从当前商品信息看…依据…` 不推荐其他商品 |

**Prompt 新增规则**：
- 不输出 `product_id`
- 不重复商品卡片已经展示的信息
- 不过量营销话术和客服用语
- 不展开完整参数

---
## PageContext-aware RAG

### 概述

后端支持前端传入 `page_context`，根据用户当前浏览页面（商品详情页 / 列表页 / 聊天页）调整 RAG 检索和 LLM 回答策略。

### PageContext DTO

| 字段 | 类型 | 说明 |
|------|------|------|
| pageType | PageType | 页面类型 |
| currentProductId | String | 当前查看的商品 ID |
| visibleProductIds | List\<String\> | 列表页可见商品 ID |
| searchQuery | String | 列表页搜索关键词 |
| selectedFilters | Map\<String, Object\> | 已选筛选条件 |
| recentlyViewedProductIds | List\<String\> | 最近浏览商品 ID |

### PageType 枚举

- `PRODUCT_DETAIL` — 商品详情页
- `PRODUCT_LIST` — 商品列表页
- `CHAT` — 全屏聊天页
- `UNKNOWN` — 未知/未传（向后兼容）

### PageContextResolver

解析 `PageContext` → `PageContextResolution`：
- 根据 `currentProductId` 从 ProductService 查询当前商品
- 根据 `visibleProductIds` 批量查询可见商品列表
- 不存在的 ID 记录 warning 但不中断流程
- page_context 为 null 时返回 UNKNOWN

### PRODUCT_DETAIL 处理策略

| 用户 query | 行为 |
|-----------|------|
| "这个适合敏感肌吗" | 识别 references "当前商品"，设置 currentProductId，prompt 含商品详情，跳过 product_card |
| "有没有更便宜的" | 继承当前商品 category/subCategory，maxPrice=当前价格×0.8，排除当前商品 ID，正常检索+product_card |
| "有没有类似的/换一个" | 继承当前商品 category/subCategory，排除当前商品 ID，正常检索 |
| "除了这个" | 排除当前商品 ID，继承 category/subCategory |

### PRODUCT_LIST 处理策略

| 用户 query | 行为 |
|-----------|------|
| "有没有更便宜的耳机" | searchQuery="耳机" + selectedFilters.category 合并到 analysis |
| "还有吗" | visibleProductIds 进入 boostedProductIds (+0.05 score boost) |
| selected_filters={category:"数码电子"} | category 进入 analysis.category |

visibleProductIds 作为 soft scope（boost 而非硬过滤）。

### PageContext 与 ConversationState 的关系

- **PageContext**: 前端实时传入，反映当前页面状态（无状态）
- **ConversationState**: 后端维护，跨轮次累积上下文（有状态）
- 两者互相补充：PageContext 提供"用户在哪个页面"，ConversationState 提供"用户之前问过什么"
- QueryAnalyzer 优先使用 PageContext 推理当前商品引用，再使用 ConversationState 继承上轮类目

### Negative Constraints 与 PageContext

检索链路中负约束过滤顺序：

1. Qdrant metadata filter：category / subCategory / price / brand 硬条件
2. StrictProductConstraintFilter（统一最终硬过滤入口）：category/subCategory/price/negativeBrands/excludeProductIds/negativeKeywords
3. boostedProductIds：候选商品在 boostedProductIds 中 → finalScore + 0.05
4. page filter fallback：如果 page_context 过滤过严导致无结果，fallback 到普通同类检索

### 当前限制

- 复杂商品对比（A 和 B 哪个好）暂未完整实现
- visible_product_ids 作为 soft boost，不做硬范围限制
- 详情页"当前商品问答"模式下仍会执行检索，但跳过 product_card 发送

---
## 性能指标 (规划)

- 检索延迟 P99 < 500ms
- 端到端首字延迟 < 2s (含 Embedding + 检索 + LLM 首 token)
- 检索召回率 Top-20 > 90%
- 单次对话最长 30s 超时
