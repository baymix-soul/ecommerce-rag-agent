# 数据模型设计

> 规划文档，当前为设计阶段。模型使用 Java 类定义（后续用 Lombok 简化）。

## 1. 商品模型 (Product)

```java
public class Product {
    private String productId;        // 商品唯一 ID，如 "PROD-001"
    private String name;             // 商品名称
    private String description;      // 商品文字描述（用于 Embedding）
    private String category;         // 类目路径，如 "运动/跑步鞋"
    private String brand;            // 品牌
    private double price;            // 当前售价（元）
    private Double originalPrice;    // 原价（可为 null）
    private String imageUrl;         // 商品主图 URL
    private List<String> images;     // 商品图片 URL 列表
    private Map<String, String> attributes; // 其他属性（材质、尺码等）
    private int stock;               // 库存数量
    private double rating;           // 用户评分 (0-5)
    private int salesCount;          // 销量
}
```

**商品 JSON 示例** (`resources/data/products.json`):

```json
{
  "product_id": "PROD-001",
  "name": "Nike Air Zoom Pegasus 40",
  "description": "轻量透气跑鞋，适合日常跑步训练，网面材质，Zoom Air 缓震",
  "category": "运动/跑步鞋",
  "brand": "Nike",
  "price": 899.00,
  "original_price": 1099.00,
  "image_url": "https://example.com/images/prod-001.jpg",
  "attributes": {
    "材质": "网面+合成革",
    "适用场景": "日常跑步",
    "适用季节": "春夏"
  },
  "stock": 200,
  "rating": 4.8,
  "sales_count": 1520
}
```

## 2. 对话请求 (ChatRequest)

```java
public class ChatRequest {
    private String sessionId;         // 会话 ID
    private String message;           // 用户消息文本
}
```

## 3. SSE 事件数据 (SseEvent)

```java
public class SseEvent {
    private String event;             // 事件类型: text | product_card | error | done
    private String data;              // JSON 序列化的事件数据
}
```

**各事件类型的 data 结构**:

`text`:
```json
{"delta": "为您推荐..."}
```

`product_card`:
```json
{
  "product_id": "PROD-001",
  "name": "Nike Air Zoom Pegasus 40",
  "price": 899.00,
  "image_url": "https://...",
  "reason": "轻量缓震，适合日常跑步训练"
}
```

`error`:
```json
{"code": "LLM_TIMEOUT", "message": "LLM 服务响应超时"}
```

`done`:
```json
{}
```

## 4. 商品卡片 (ProductCard)

```java
public class ProductCard {
    private String productId;         // 商品 ID
    private String name;              // 商品名称
    private double price;             // 价格
    private String imageUrl;          // 图片 URL
    private String reason;            // 推荐理由（由 LLM 生成，基于商品字段）
}
```

## 5. 检索结果 (SearchResult)

```java
public class SearchResult {
    private String productId;         // 商品 ID
    private String name;              // 商品名称
    private double price;             // 价格
    private String imageUrl;          // 图片 URL
    private double score;             // 检索相关性得分
}
```

## 6. 向量文档 (VectorDoc)

```java
public class VectorDoc {
    private String docId;             // 文档 ID
    private String productId;         // 关联的商品 ID
    private String content;           // 用于 Embedding 的文本内容
    private List<Float> embedding;    // 向量
    private Map<String, Object> metadata; // 元数据（用于过滤）
}
```

## 实体关系

```
Product 1 ── 1 VectorDoc
ChatRequest ── SSE Events (text | product_card | error | done)
ProductCard 是 Product 的视图投影（LLM 推荐视角）
```