# API 接口规范

> 已实现商品相关 API，对话/流式接口待后续阶段实现。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **流式接口**: `text/event-stream` (SSE) — 待实现
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

## 5. 对话接口（核心）— 待实现

### POST /api/chat/stream

发送用户消息，SSE 流式获取 AI 导购回复。

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
| INVALID_QUERY | 400 | 查询参数无效（待实现） |
| SESSION_NOT_FOUND | 400 | 会话不存在（待实现） |
| EMBEDDING_TIMEOUT | 500 | Embedding 服务超时（待实现） |
| LLM_TIMEOUT | 500 | LLM 调用超时（待实现） |
| VECTOR_DB_ERROR | 500 | 向量数据库异常（待实现） |
| INTERNAL_ERROR | 500 | 服务器内部错误 |
