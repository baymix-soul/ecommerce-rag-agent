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
| session_id | string | 否 | null | 会话 ID，当前阶段不做持久化 |
| limit | int | 否 | 5 | 候选商品数量，最大 10 |

**Request 示例**:
```json
{
  "message": "推荐一款适合油皮的洗面奶",
  "limit": 3
}
```

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
> - 真实 LLM 模式下，导购文本由 Doubao 基于候选商品动态生成
> - Mock 模式下，导购文本为固定模板
> - 不要把真实 API Key 写入任何文件

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
