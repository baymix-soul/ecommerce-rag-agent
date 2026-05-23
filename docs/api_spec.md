# API 接口规范

> 规划文档，当前为设计阶段，尚未实现。

## 基础信息

- **Base URL**: `http://localhost:8080/api/v1`
- **Content-Type**: `application/json`
- **流式接口**: `text/event-stream` (SSE)
- **认证方式**: 暂不实现（后续阶段）

---

## 1. 健康检查

### GET /health

返回服务运行状态。

**Response (200)**:
```json
{
  "status": "ok",
  "version": "0.1.0",
  "vector_db_connected": true,
  "product_count": 100
}
```

---

## 2. 对话接口（核心）

### POST /api/v1/chat/stream

发送用户消息，SSE 流式获取 AI 导购回复。

**Content-Type**: `application/json` → **Response**: `text/event-stream`

**Request**:
```json
{
  "session_id": "uuid-string",
  "message": "我想买一双适合跑步的鞋"
}
```

**SSE 事件流**:

```
event: text
data: {"delta": "为您找到几款适合跑步的鞋，为您推荐以下商品："}

event: text
data: {"delta": "\n\n1. **Nike Air Zoom Pegasus** - 轻量透气"}

event: product_card
data: {"product_id": "PROD-001", "name": "Nike Air Zoom Pegasus 40", "price": 899.00, "image_url": "https://...", "reason": "轻量缓震，适合日常跑步训练"}

event: text
data: {"delta": "\n2. **Adidas Ultraboost 23** ..."}

event: product_card
data: {"product_id": "PROD-005", "name": "Adidas Ultraboost 23", "price": 1099.00, "image_url": "https://...", "reason": "BOOST 科技中底，缓震优秀"}

event: done
data: {}
```

---

## 3. 商品检索接口

### POST /api/v1/search

按文本检索商品。

**Request**:
```json
{
  "query": "白色连衣裙",
  "filters": {
    "category": "服装",
    "price_min": 100.0,
    "price_max": 500.0
  },
  "top_k": 10
}
```

**Response (200)**:
```json
{
  "results": [
    {
      "product_id": "PROD-010",
      "name": "纯白棉质连衣裙",
      "price": 299.00,
      "image_url": "https://...",
      "score": 0.93
    }
  ],
  "total": 5
}
```

---

## 4. 商品管理接口（管理后台，后续阶段）

### GET /api/v1/products

获取商品列表，支持分页。

**Query Params**: `page=1&size=20&category=服装`

### GET /api/v1/products/{productId}

获取单个商品详情。

### POST /api/v1/products/rebuild-index

重建向量索引（仅管理员）。

---

## 错误响应格式

```json
{
  "error": {
    "code": "INVALID_QUERY",
    "message": "查询参数无效",
    "timestamp": "2026-05-21T10:00:00Z"
  }
}
```

**错误码**:

| 码 | 含义 |
|----|------|
| INVALID_QUERY | 查询参数无效 |
| SESSION_NOT_FOUND | 会话不存在 |
| EMBEDDING_TIMEOUT | Embedding 服务超时 |
| LLM_TIMEOUT | LLM 调用超时 |
| VECTOR_DB_ERROR | 向量数据库异常 |
| INTERNAL_ERROR | 服务器内部错误 |