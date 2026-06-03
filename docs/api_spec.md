# API 接口规范

> 已实现商品相关 API 和 SSE 对话接口。支持 Mock LLM 与 Doubao LLM 两种模式。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **流式接口**: `text/event-stream` (SSE)
- **认证方式**: 暂不实现（后续阶段）

---

## 1. 健康检查

### GET /api/health

返回服务运行状态。

**Response (200)**:
```json
{
  "status": "ok",
  "service": "ecommerce-rag-agent",
  "version": "0.1.0"
}
```

---

## 2. 商品列表

### GET /api/products

返回商品卡片列表。

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | int | 否 | 20 | 返回数量，最大 100 |

**Response (200)**:
```json
[
  {
    "product_id": "p_beauty_001",
    "name": "雅诗兰黛特润修护肌活精华露...",
    "price": 720.0,
    "currency": "CNY",
    "image_url": "/images/p_beauty_001.jpg",
    "reason": ""
  }
]
```

**curl 示例**:
```bash
curl http://localhost:8080/api/products?limit=5
```

**PowerShell 示例**:
```powershell
Invoke-RestMethod http://localhost:8080/api/products?limit=5
```

---

## 3. 商品详情

### GET /api/products/{productId}

根据 productId 返回商品完整信息。

**Path 参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| productId | string | 商品 ID，如 p_beauty_001 |

**Response (200)**:
```json
{
  "product_id": "p_beauty_001",
  "name": "雅诗兰黛特润修护肌活精华露...",
  "brand": "雅诗兰黛",
  "category": "美妆护肤",
  "sub_category": "精华",
  "price": 720.0,
  "price_range": "720~1260",
  "image_url": "/images/p_beauty_001.jpg",
  "description": "雅诗兰黛特润修护肌活精华露（小棕瓶）...",
  "specs": {
    "容量": "30ml 经典装、50ml 加大装、75ml 家用装"
  },
  "avg_rating": 2.2,
  "currency": "CNY"
}
```

**Response (404)**:
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found: non_existent_id"
}
```

**curl 示例**:
```bash
curl http://localhost:8080/api/products/p_beauty_001
```

**PowerShell 示例**:
```powershell
Invoke-RestMethod http://localhost:8080/api/products/p_beauty_001
```

---

## 4. 商品搜索

### POST /api/products/search

按关键词和条件搜索商品。当前为关键词检索，后续会接入 Embedding + Qdrant 向量检索。

**Request Body**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| query | string | 否 | null | 搜索关键词，按空格分词匹配 |
| category | string | 否 | null | 类目过滤（包含匹配） |
| sub_category | string | 否 | null | 子类目过滤（包含匹配） |
| brand | string | 否 | null | 品牌过滤（包含匹配） |
| min_price | number | 否 | null | 最低价格 |
| max_price | number | 否 | null | 最高价格 |
| limit | int | 否 | 10 | 返回数量，最大 20 |

**Request 示例**:
```json
{
  "query": "油皮 洗面奶",
  "category": "美妆护肤",
  "max_price": 200,
  "limit": 10
}
```

**Response (200)**:
```json
{
  "query": "油皮 洗面奶",
  "total": 3,
  "products": [
    {
      "product_id": "p_beauty_010",
      "name": "...",
      "price": 89.0,
      "currency": "CNY",
      "image_url": "/images/p_beauty_010.jpg",
      "reason": ""
    }
  ]
}
```

**curl 示例**:
```bash
curl -X POST http://localhost:8080/api/products/search \
  -H "Content-Type: application/json" \
  -d '{"query":"油皮 洗面奶","max_price":200}'
```

**PowerShell 示例**:
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/products/search -ContentType "application/json" -Body '{"query":"油皮 洗面奶","max_price":200}'
```

---

## 5. 对话接口（核心）

### POST /api/chat/stream

发送用户消息，SSE 流式获取 AI 导购回复。

**Request Body**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| message | string | 是 | — | 用户消息，不能为空 |
| session_id | string | 否 | null | 会话 ID，用于多轮对话上下文。为空时使用 "default"，会共享上下文。建议前端传入稳定唯一 ID（如 UUID），不同页面/用户应使用不同 sessionId。 |
| limit | int | 否 | 5 | 候选商品数量，最大 10。实际返回的 product_card 数量由后端根据 query 语义控制：明确"一款/一个"时返回 1 张，否则最多 3 张。 |
| page_context | object | 否 | null | 页面上下文，用于 RAG 检索时结合当前页面信息。详见下方 PageContext 字段说明。 |

**Request 示例**:
```json
{
  "message": "推荐一款适合油皮的洗面奶",
  "session_id": "user-001",
  "limit": 3
}
```

**多轮对话示例**:

第 1 轮：
```json
{"message": "推荐跑鞋", "session_id": "user-001"}
```

第 2 轮（继承跑鞋上下文）：
```json
{"message": "要轻量的", "session_id": "user-001"}
```

第 3 轮（排除品牌）：
```json
{"message": "除了耐克还有什么", "session_id": "user-001"}
```

第 4 轮（换一个）：
```json
{"message": "换一个", "session_id": "user-001"}
```

**PageContext 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| page_type | string | 页面类型：PRODUCT_LIST / PRODUCT_DETAIL / CHAT / UNKNOWN |
| current_product_id | string | 当前查看的商品 ID（PRODUCT_DETAIL 时有效） |
| visible_product_ids | string[] | 当前列表页可见商品 ID 列表（PRODUCT_LIST 时有效） |
| search_query | string | 列表页搜索关键词 |
| selected_filters | object | 已选择的筛选条件（如 category、sub_category、brand） |
| recently_viewed_product_ids | string[] | 最近浏览商品 ID 列表 |

**PageContext 示例**：

PRODUCT_DETAIL（商品详情页）：
```json
{
  "message": "这个适合敏感肌吗？",
  "limit": 10,
  "page_context": {
    "page_type": "PRODUCT_DETAIL",
    "current_product_id": "p_beauty_001",
    "visible_product_ids": [],
    "search_query": null,
    "selected_filters": {},
    "recently_viewed_product_ids": ["p_beauty_001", "p_digital_003"]
  }
}
```

PRODUCT_LIST（商品列表页）：
```json
{
  "message": "有没有更便宜的耳机？",
  "limit": 10,
  "page_context": {
    "page_type": "PRODUCT_LIST",
    "current_product_id": null,
    "visible_product_ids": ["p_digital_001", "p_digital_002", "p_digital_003"],
    "search_query": "耳机",
    "selected_filters": {"category": "数码电子"},
    "recently_viewed_product_ids": []
  }
}
```

> 旧请求兼容说明：`page_context` 是可选字段，不传时向后兼容，与之前的 RAG 行为一致。

**非检索 intent 的 SSE 事件示例**（如 "你好"、"谢谢"、"怎么用"）：

```
event:text
data:{"content":"你好！我是电商导购助手，可以帮你推荐护肤品、数码产品、运动装备和食品饮料等商品。有什么需要帮忙的吗？"}

event:done
data:{}
```

> SMALLTALK / HELP / THANKS 不调用检索，不发送 product_card。

**Response**: `text/event-stream`

SSE 事件流，事件类型包括：

| 事件类型 | 携带数据 | 说明 |
|----------|----------|------|
| `text` | `{"content":"文本片段"}` | LLM 生成的导购说明文本片段 |
| `product_card` | `{"product_id":"...","name":"...","price":...,"currency":"CNY","image_url":"...","reason":"..."}` | 推荐的商品卡片 |
| `error` | `{"code":"...","message":"..."}` | 错误信息 |
| `done` | `{}` | 流结束标记 |

**SSE 事件示例**:

```
event:text
data:{"content":"我根据你的需求，从商品库中找到了几款相关商品。"}

event:product_card
data:{"product_id":"p_beauty_010","name":"芙丽芳丝净润洗面霜","price":89.0,"currency":"CNY","image_url":"/images/p_beauty_010.jpg","reason":"该商品来自当前商品库候选结果，具体推荐理由将在后续 LLM 阶段生成。"}

event:done
data:{}
```

**无候选商品时的 SSE 事件示例**:

```
event:text
data:{"content":"当前商品库中暂未找到完全匹配的商品，你可以换个关键词或放宽条件再试试。"}

event:done
data:{}
```

**message 为空时的 SSE 事件示例**:

```
event:error
data:{"code":"INVALID_REQUEST","message":"message 不能为空"}
```

### LLM 模式切换

当前支持两种 LLM 模式：

| 模式 | 配置 | 说明 |
|------|------|------|
| Mock LLM（默认） | `MOCK_LLM_ENABLED=true` | 不调用外部 API，返回固定模板文本 |
| Doubao LLM | `MOCK_LLM_ENABLED=false` | 调用 Doubao-Seed-2.0-lite API，流式生成导购文本 |

**Mock 模式测试**:
```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"推荐一款适合油皮的洗面奶","limit":3}'
```

**真实 LLM 模式测试**（需设置环境变量）:
```bash
export LLM_BASE_URL=https://ark.cn-beijing.volces.com/api/v3
export LLM_API_KEY=your_api_key_here
export LLM_MODEL=your_model_here
export MOCK_LLM_ENABLED=false

mvn spring-boot:run

curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"推荐一款适合油皮的洗面奶","limit":3}'
```

**PowerShell 测试**:
```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/chat/stream" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"message":"推荐一款适合油皮的洗面奶","limit":3}'
```

> **重要说明**:
> - product_card 由后端基于候选商品检索结果生成，不由 LLM 模型生成
> - 硬约束过滤（category/subCategory/price/negativeBrands/negativeKeywords/excludeProductIds）现在统一由 StrictProductConstraintFilter 处理
> - 后端默认最多返回 3 张商品卡片，用户明确"一款/一个/一双"时只返回 1 张卡片
> - LLM 文本不输出 product_id，不重复卡片已展示的信息
- 真实 LLM 模式下，导购文本由 Doubao 基于候选商品动态生成
> - Mock 模式下，导购文本为固定模板
> - 不要把真实 API Key 写入任何文件

---

## 6. RAG Chunk 预览（调试用）

以下接口仅用于开发调试，不影响现有 /api/chat/stream 逻辑。

### 6.1 GET /api/rag/chunks/preview

返回前 N 个 RagChunkDocument，用于检查 chunk 是否合理。

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | int | 否 | 20 | 返回数量，最小 1，最大 100 |

**Response (200)**:
```json
[
  {
    "chunk_id": "chunk_a1b2c3d4e5f6...",
    "parent_id": "p_beauty_001",
    "product_id": "p_beauty_001",
    "chunk_type": "PRODUCT_PROFILE",
    "text": "商品名称：雅诗兰黛特润修护肌活精华露...\n品牌：雅诗兰黛\n类目：美妆护肤\n子类目：精华\n价格：720.0 CNY\n评分：2.2\n商品定位：美妆护肤 精华 雅诗兰黛",
    "source_field": "profile",
    "name": "雅诗兰黛特润修护肌活精华露...",
    "brand": "雅诗兰黛",
    "category": "美妆护肤",
    "sub_category": "精华",
    "price": 720.0,
    "currency": "CNY",
    "avg_rating": 2.2,
    "image_url": "/images/p_beauty_001.jpg",
    "metadata": {
      "product_id": "p_beauty_001",
      "category": "美妆护肤",
      "sub_category": "精华",
      "brand": "雅诗兰黛",
      "price": "720.0",
      "currency": "CNY",
      "avg_rating": "2.2"
    }
  }
]
```

### 6.2 GET /api/rag/chunks/product/{productId}

返回某个商品生成的所有 chunk。

**Path 参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| productId | string | 商品 ID |

**Response (200)**: `RagChunkDocument[]`（结构同上）

**Response (404)**:
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "Product not found: non_existent_id"
}
```

### 6.3 GET /api/rag/chunks/stats

返回 chunk 统计信息。

**Response (200)**:
```json
{
  "total_products": 100,
  "total_chunks": 650,
  "by_chunk_type": {
    "PRODUCT_PROFILE": 100,
    "DESCRIPTION": 100,
    "SPECS": 92,
    "SEARCH_SUMMARY": 100,
    "REVIEW_SUMMARY": 80,
    "FAQ": 60,
    "MARKETING_COPY": 90
  }
}
```

> 新增字段说明：
> - `REVIEW_SUMMARY`：从 `review_summary` 字段生成，仅在原始数据有 `user_reviews` 时非空
> - `FAQ`：从 `faq_summary` 字段生成，仅在原始数据有 `official_faq` 时非空
> - `MARKETING_COPY`：从 `marketing_copy` 字段生成，仅在原始数据有 `marketing_description` 时非空
> - 数据转换脚本 `convert_teacher_dataset.py` 会自动脱敏和聚合这些字段
> - 重新运行数据转换脚本后需 `POST /api/rag/vector-index/rebuild` 重建索引

---

## 7. 向量搜索及向量索引调试 API（调试用）

以下接口仅用于开发调试，当前使用 MockEmbeddingProvider + InMemoryVectorStoreService，不调用真实 API。

> **主路径**: 向量搜索推荐使用 `POST /api/rag/vector-search`。`POST /api/rag/vector-index/search` 为兼容别名 (deprecated)，功能完全相同。

### 7.1 POST /api/rag/vector-index/rebuild

构建 in-memory 向量索引。

**Request Body**: 无

**Response (200)**:
```json
{
  "indexed_chunks": 400,
  "vector_store_count": 400,
  "embedding_model": "mock-hash-embedding",
  "dimension": 64
}
```

### 7.2 GET /api/rag/vector-index/stats

返回当前向量索引状态。

**Response (200)**:
```json
{
  "count": 400,
  "embedding_model": "mock-hash-embedding",
  "dimension": 64
}
```

> 未 rebuild 时 count 为 0。

### 7.3 POST /api/rag/vector-search

向量检索查询。兼容别名: `POST /api/rag/vector-index/search`（功能相同，deprecated）。

**Request Body**:

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| query | string | 是 | — | 搜索查询文本 |
| limit | int | 否 | 10 | 返回数量，最大 50 |

**Request 示例**:
```json
{
  "query": "油皮 洗面奶",
  "limit": 5
}
```

**Response (200)**:
```json
{
  "query": "油皮 洗面奶",
  "total": 5,
  "hits": [
    {
      "chunk_id": "p_beauty_010::DESCRIPTION::0",
      "vector_point_id": "a1b2c3d4-...",
      "product_id": "p_beauty_010",
      "chunk_type": "DESCRIPTION",
      "source_field": "description",
      "score": 0.83,
      "text": "商品名称：芙丽芳丝净润洗面霜\n...",
      "payload": {
        "product_id": "p_beauty_010",
        "category": "美妆护肤",
        "chunk_type": "DESCRIPTION",
        "price": 89.0
      }
    }
  ]
}
```

> 未 rebuild 时 search 可返回空结果，不会报 500。

---

## 8. 检索调试 API（调试用）

### 8.1 GET /api/rag/retrieval/debug

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| query | string | 是 | — | 搜索查询文本 |
| limit | int | 否 | 5 | 候选数量 |
| session_id | string | 否 | default | 会话 ID，用于多轮调试 |

**Response (200)**:
```json
{
  "query": "油皮洗面奶",
  "session_id": "default",
  "intent": "PRODUCT_SEARCH",
  "intent_reason": "product search intent detected",
  "mode": "hybrid",
  "vector_index_count": 392,
  "total": 5,
  "query_analysis": {
    "original_query": "油皮洗面奶",
    "normalized_query": "油皮洗面奶",
    "resolved_query": null,
    "inherited_from_context": null,
    "category": "美妆护肤",
    "sub_category": "洁面",
    "sub_categories": [],
    "min_price": null,
    "max_price": null,
    "negative_brands": [],
    "exclude_brands": [],
    "exclude_product_ids": [],
    "positive_keywords": [],
    "negative_keywords": [],
    "avoid_ingredients_or_terms": [],
    "filters": {"category": "美妆护肤", "sub_category": "洁面"},
    "warnings": [],
    "intent": "PRODUCT_SEARCH"
  },
  "memory_before": null,
  "memory_after": {
    "session_id": "default",
    "turn_count": 0,
    "last_user_query": null,
    "category": null,
    "sub_category": null,
    "min_price": null,
    "max_price": null,
    "recommended_product_ids": []
  },
  "page_context_resolution": {
    "page_type": "UNKNOWN",
    "visible_product_count": 0,
    "recently_viewed_count": 0,
    "warnings": ["page_context is null"]
  },
  "inherited_from_page_context": null,
  "scope_product_ids": [],
  "boosted_product_ids": [],
  "page_warnings": [],
  "raw_candidate_count": 12,
  "final_candidate_count": 5,
  "final_candidates": [
    {
      "product_id": "p_beauty_010",
      "name": "芙丽芳丝净润洗面霜",
      "category": "美妆护肤",
      "sub_category": "洁面",
      "brand": "芙丽芳丝",
      "price": 89.0,
      "vector_score": 0.72,
      "keyword_score": 0.9,
      "final_score": 0.783,
      "matched_sources": ["vector", "keyword"],
      "matched_chunk_types": ["DESCRIPTION", "SEARCH_SUMMARY"],
      "matched_text_snippets": ["商品名称：芙丽芳丝净润洗面霜\n商品描述：温和氨基酸洁面..."],
      "constraint_passed": true,
      "passed_rules": ["category_match(美妆护肤)", "sub_category_match(洁面)", "price_ok", "negativeBrands_ok"]
    }
  ],
  "filtered_out_candidates": [
    {
      "product_id": "p_beauty_099",
      "name": "某含酒精精华",
      "category": "美妆护肤",
      "sub_category": "精华",
      "price": 299.0,
      "brand": "某品牌",
      "vector_score": 0.65,
      "keyword_score": 0.8,
      "final_score": 0.71,
      "failed_rules": ["sub_category_mismatch(expected=洁面, actual=精华)"]
    }
  ]
}
```

> 新增字段说明：
> - `intent` / `intent_reason`：RetrievalRouter 识别的用户意图
> - `session_id`：当前会话 ID
> - `query_analysis.resolved_query`：上下文解析后的查询
> - `query_analysis.inherited_from_context`：是否从上下文继承了信息
> - `query_analysis.exclude_brands`：需排除的品牌
> - `query_analysis.exclude_product_ids`：需排除的商品 ID
> - `query_analysis.negative_keywords`：负向关键词
> - `query_analysis.avoid_ingredients_or_terms`：需避免的成分/术语
> - `memory_before`：当前轮次前的会话状态（首次请求为 null）
> - `memory_after`：当前轮次后的会话状态
> - `raw_candidate_count`：原始候选数量（过滤前）
> - `final_candidate_count`：最终候选数量（过滤后）
> - `final_candidates`：通过所有约束的最终候选列表
> - `filtered_out_candidates`：被约束过滤掉的候选，包含 `productId/name/category/subCategory/price/failedRules` 用于排查过滤原因

> 未 rebuild 时 vector_index_count 为 0，此时 hybrid 模式自动 fallback keyword。

---

## 9. 评估 API（调试用）

以下接口用于 RAG 检索质量评估，不调用 LLM，不修改 ChatService。

### 9.1 GET /api/rag/eval/queries

返回评估查询列表。

**Response (200)**: `RagEvalQuery[]`

```json
[
  {
    "query": "推荐一款适合油皮的洗面奶",
    "expected_category": "美妆护肤",
    "expected_sub_category": "洁面",
    "expected_keywords": ["油皮", "控油", "清洁", "洗面"],
    "min_relevant_count": 1,
    "notes": "希望召回洁面/洗面奶相关商品"
  }
]
```

### 9.2 POST /api/rag/eval/run

运行全部评估查询。

**Request Body**:
```json
{"top_k": 5}
```

**Response (200)**: `RagEvalSummary`

```json
{
  "embedding_model": "mock-hash-embedding",
  "vector_store": "in-memory",
  "retrieval_mode": "hybrid",
  "total_queries": 20,
  "passed_queries": 15,
  "pass_rate": 0.75,
  "avg_hit_count": 3.2,
  "failed_queries": ["query1", "query2"],
  "failure_category": ["category_mismatch:...", "subcategory_mismatch:..."],
  "results": [...]
}
```

### 9.3 POST /api/rag/eval/run-one

运行单条评估查询。

**Query 参数**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| top_k | int | 否 | 5 | 候选数量，最大 50 |

**Request Body**: `RagEvalQuery`

```json
{
  "query": "推荐一款适合油皮的洗面奶",
  "expected_category": "美妆护肤",
  "expected_sub_category": "洁面",
  "min_relevant_count": 1
}
```

**Response (200)**: `RagEvalResult`

```json
{
  "query": "推荐一款适合油皮的洗面奶",
  "expected_category": "美妆护肤",
  "expected_sub_category": "洁面",
  "mode": "hybrid",
  "top_k": 5,
  "hits": [...],
  "pass": true,
  "reasons": ["pass", "category_match: 美妆护肤", "subcategory_match: 洁面"],
  "total_hits": 5
}
```

> **注意**: 评估 API 复用现有 retrieval 链路，是否调用真实 Ark embedding 取决于当前环境配置。空 query 返回 400。

---
## 9. Query Understanding Debug API（调试用）

以下接口用于 QueryPlan 基础设施调试，不调用 LLM，不连接 Qdrant，不影响现有 /api/chat/stream。

### 9.1 GET /api/rag/understanding/taxonomy

返回当前商品库的类目、子类目、品牌和价格范围快照。

**Response (200)**:
```json
{
  "categories": ["美妆护肤", "数码电子", "服饰运动", "食品饮料"],
  "sub_categories_by_category": {
    "美妆护肤": ["洁面", "精华", "防晒", "面霜"],
    "数码电子": ["笔记本电脑", "平板电脑", "智能手机", "真无线耳机", "头戴式耳机"],
    "服饰运动": ["跑步鞋", "短袖T恤", "背包", "速干T恤", "卫衣"],
    "食品饮料": ["坚果/零食", "碳酸饮料", "功能饮料", "茶饮", "咖啡", "牛奶", "酸奶"]
  },
  "all_sub_categories": ["洁面", "精华", "防晒", "面霜", "笔记本电脑", "..."],
  "brands": ["Apple", "Nike", "华为", "兰蔻", "雅诗兰黛", "资生堂", "..."],
  "min_price": 29.0,
  "max_price": 12999.0,
  "filterable_fields": ["category", "sub_category", "brand", "price", "product_id", "chunk_type"],
  "text_fields": ["name", "description", "specs", "review_summary", "faq_summary", "marketing_copy", "search_summary"],
  "generated_at": "2026-06-02T12:00:00Z",
  "empty": false
}
```

### 9.2 POST /api/rag/understanding/validate-plan

校验 QueryPlan JSON 的合法性。

**Request Body** (QueryPlan JSON):
```json
{
  "originalQuery": "推荐几款适合程序员的电脑",
  "intent": "PRODUCT_SEARCH",
  "needsRetrieval": true,
  "target": {
    "category": "数码电子",
    "subCategory": "笔记本电脑"
  },
  "price": {
    "max": 10000,
    "currency": "CNY",
    "strict": true
  },
  "softKeywords": ["程序员", "编程", "开发", "大内存"],
  "queryVariants": ["适合程序员的笔记本电脑"]
}
```

**Response (200)**:
```json
{
  "originalPlan": { "..." },
  "validatedPlan": {
    "originalQuery": "推荐几款适合程序员的电脑",
    "intent": "PRODUCT_SEARCH",
    "needsRetrieval": true,
    "target": { "category": "数码电子", "subCategory": "笔记本电脑", "..." },
    "price": { "max": 10000, "currency": "CNY", "strict": true },
    "softKeywords": ["程序员", "编程", "开发", "大内存"],
    "queryVariants": ["适合程序员的笔记本电脑"],
    "requestedProductCount": 3,
    "source": null
  },
  "valid": true,
  "warnings": ["category_matched: 数码电子", "sub_category_matched: 笔记本电脑"],
  "errors": [],
  "fixedFields": ["price.currency=CNY", "price.strict=true", "requestedProductCount=3"],
  "categoryMatched": null,
  "subCategoryMatched": null,
  "priceValid": null,
  "brandValid": null
}
```

> **说明**：
> - 此接口不调用 LLM、不连接 Qdrant、不影响现有检索行为。
> - category/brand 校验基于 CatalogTaxonomyService 从商品库动态提取的 taxonomy。
> - 未知 category/subCategory 会添加 warning 并置空，不会抛 500。

### 9.3 POST /api/rag/understanding/plan

Shadow mode 查询理解接口：同时运行 legacy QueryAnalyzer 和 LLMQueryPlanner，对比结果。

**Request Body**:
```json
{
  "query": "推荐几款适合程序员的电脑",
  "session_id": "test-sc1"
}
```

**Response (200)**:
```json
{
  "query": "推荐几款适合程序员的电脑",
  "sessionId": "test-sc1",
  "legacyAnalysis": {
    "original_query": "推荐几款适合程序员的电脑",
    "normalized_query": "推荐几款适合程序员的电脑",
    "category": null,
    "sub_category": null,
    "max_price": null,
    "intent": "PRODUCT_SEARCH",
    "positive_keywords": [],
    "negative_brands": []
  },
  "planningResult": {
    "originalQuery": "推荐几款适合程序员的电脑",
    "plannerEnabled": false,
    "source": "DISABLED",
    "parseSuccess": false,
    "valid": false
  },
  "plannerUsedForRetrieval": false,
  "warnings": []
}
```

> **说明**：
> - 默认 `QUERY_PLANNER_ENABLED=false`，planningResult.source=DISABLED
> - 设置 `QUERY_PLANNER_ENABLED=true` 后 LLMQueryPlanner 会调用 LLM 生成 QueryPlan
> - `plannerUsedForRetrieval` 始终为 false（shadow mode，不影响真实检索）
> - 通过对比 `legacyAnalysis` 和 `planningResult.validatedPlan` 可评估 LLM planner 质量
> - 此接口不调用真实 LLM 作为单元测试

---
## 10. Qdrant 模式说明

当 `VECTOR_STORE=qdrant` 时，向量存储使用真实 Qdrant。以下接口行为不变，但底层使用 Qdrant 而非 InMemoryVectorStoreService。

### 9.1 Qdrant 配置

通过环境变量配置：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `VECTOR_STORE` | `in-memory` | 向量存储类型：`in-memory` 或 `qdrant` |
| `QDRANT_URL` | `http://localhost:6333` | Qdrant REST API 地址 |
| `QDRANT_API_KEY` | 空 | Qdrant API Key（本地可为空） |
| `QDRANT_COLLECTION` | `ecommerce_rag_chunks_mock` | Collection 名称 |
| `QDRANT_VECTOR_SIZE` | `64` | 向量维度（与 Mock Embedding 一致） |
| `QDRANT_DISTANCE` | `Cosine` | 距离度量 |
| `QDRANT_TIMEOUT_SECONDS` | `10` | HTTP 超时 |

### 9.2 真实 Ark + Qdrant 联调步骤

```powershell
# 1. 启动 Qdrant
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant

# 2. 设置环境变量 (Qdrant)
$env:VECTOR_STORE="qdrant"
$env:QDRANT_URL="http://localhost:6333"
$env:QDRANT_COLLECTION="ecommerce_rag_chunks_ark"
$env:QDRANT_VECTOR_SIZE="你的 Ark embedding 实际维度"

# 3. 设置环境变量 (Ark Embedding)
$env:EMBEDDING_PROVIDER="ark-multimodal"
$env:EMBEDDING_BASE_URL="https://ark.cn-beijing.volces.com/api/v3"
$env:EMBEDDING_API_KEY="your_ark_api_key_here"
$env:EMBEDDING_MODEL="doubao-embedding-vision-251215"
$env:EMBEDDING_DIMENSION="你的 Ark embedding 实际维度"
$env:ARK_MULTIMODAL_EMBEDDING_PATH="/embeddings/multimodal"

# 4. 启动后端
cd server
mvn spring-boot:run

# 5. 重建索引（会调用真实 Ark API，可能产生费用）
Invoke-RestMethod -Method Post http://localhost:8080/api/rag/vector-index/rebuild

# 6. 查看索引状态
Invoke-RestMethod http://localhost:8080/api/rag/vector-index/stats

# 7. 向量搜索（主路径）
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/rag/vector-search `
  -ContentType "application/json" `
  -Body '{"query":"油皮洗面奶","limit":5}'

# 8. 聊天接口（SSE 流式）
Invoke-WebRequest `
  -Uri http://localhost:8080/api/chat/stream `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"message":"推荐一款适合油皮的洗面奶","limit":3}'
```

> **安全提醒**: 不要把真实 `EMBEDDING_API_KEY` 写入任何文件。通过 PowerShell 环境变量临时设置，关闭终端后自动失效。

---

## 错误响应格式

```json
{
  "code": "ERROR_CODE",
  "message": "错误描述"
}
```

**错误码**:

| 码 | HTTP 状态 | 含义 |
|----|-----------|------|
| PRODUCT_NOT_FOUND | 404 | 商品不存在 |
| INVALID_REQUEST | 400 | 请求参数无效（如 message 为空） |
| INVALID_QUERY | 400 | 查询参数无效 |
| LLM_ERROR | 500 | LLM 生成失败 |
| LLM_TIMEOUT | 500 | LLM 调用超时 |
| SESSION_NOT_FOUND | 400 | 会话不存在（待实现） |
| EMBEDDING_TIMEOUT | 500 | Embedding 服务超时（待实现） |
| VECTOR_DB_ERROR | 500 | 向量数据库异常（待实现） |
| INTERNAL_ERROR | 500 | 服务器内部错误 |
