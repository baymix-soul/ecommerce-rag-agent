# 数据模型设计

> 已实现阶段。当前 MVP 使用标准 12 字段 Product schema，数据来源于老师提供的电商数据集。

## 0. 原始老师数据集字段

老师数据集每个商品一个 JSON 文件，包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| product_id | string | 商品 ID，如 p_beauty_001 |
| title | string | 商品标题 |
| brand | string | 品牌 |
| category | string | 主类目（美妆护肤/数码电子/服饰运动/食品饮料） |
| sub_category | string | 子类目 |
| base_price | float | 基础价格 |
| image_path | string | 本地图片路径（原始格式：分类目录/images/{id}_live.jpg） |
| skus | array | SKU 列表，每项含 sku_id/properties/price |
| rag_knowledge.marketing_description | string | 营销描述文本 |
| rag_knowledge.official_faq | array | 官方 FAQ（question/answer 对） |
| rag_knowledge.user_reviews | array | 用户评价（nickname/rating/content） |

## 1. 标准 products.json 字段

通过 scripts/convert_teacher_dataset.py 转换后的标准格式：

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| product_id | string | product_id | 商品唯一 ID |
| name | string | title | 商品名称 |
| brand | string | brand（清洗后） | 品牌（去除中英文重复，如 "Apple 苹果" → "Apple"） |
| category | string | category | 主类目 |
| sub_category | string | sub_category | 子类目 |
| price | number | base_price | 基础价格（数字类型） |
| price_range | string | skus[].price | 价格区间，如 "720~1260" 或 "199" |
| image_url | string | 转换生成 | 统一格式 /images/{product_id}.jpg |
| description | string | rag_knowledge.marketing_description | 商品描述 |
| specs | object | skus[].properties | 合并所有 SKU 属性，同属性多值用顿号拼接 |
| avg_rating | number | user_reviews[].rating | 用户评分平均值，保留 1 位小数，无评价时 0.0 |
| currency | string | 固定值 | "CNY" |

**标准 products.json 示例**:

```json
{
  "product_id": "p_beauty_001",
  "name": "雅诗兰黛特润修护肌活精华露淡纹紧致保湿夜间修护抗初老精华30ml",
  "brand": "雅诗兰黛",
  "category": "美妆护肤",
  "sub_category": "精华",
  "price": 720.0,
  "price_range": "720~1260",
  "image_url": "/images/p_beauty_001.jpg",
  "description": "雅诗兰黛特润修护肌活精华露（小棕瓶）是品牌经典抗初老单品...",
  "specs": {
    "容量": "30ml 经典装、50ml 加大装、75ml 家用装"
  },
  "avg_rating": 2.2,
  "currency": "CNY"
}
```

**数据转换脚本使用方式**:

```bash
python scripts/convert_teacher_dataset.py \
    --input "E:\ecommerce_agent_dataset_供参考\ecommerce_agent_dataset" \
    --output "server/src/main/resources/data/products.json" \
    --images-output "server/src/main/resources/static/images"
```

## 2. 商品模型 (Product.java)

```java
public class Product {
    @JsonProperty("product_id")   private String productId;
    @JsonProperty("name")         private String name;
    @JsonProperty("brand")        private String brand;
    @JsonProperty("category")     private String category;
    @JsonProperty("sub_category") private String subCategory;
    @JsonProperty("price")        private BigDecimal price;
    @JsonProperty("price_range")  private String priceRange;
    @JsonProperty("image_url")    private String imageUrl;
    @JsonProperty("description")  private String description;
    @JsonProperty("specs")        private Map<String, String> specs;
    @JsonProperty("avg_rating")   private Double avgRating;
    @JsonProperty("currency")     private String currency;
}
```

必填字段：productId, name, brand, category, subCategory, price, imageUrl, description, currency

## 3. 商品卡片 (ProductCard.java)

```java
public class ProductCard {
    @JsonProperty("product_id") private String productId;
    @JsonProperty("name")       private String name;
    @JsonProperty("price")      private BigDecimal price;
    @JsonProperty("currency")   private String currency;
    @JsonProperty("image_url")  private String imageUrl;
    @JsonProperty("reason")     private String reason;  // MVP 阶段为空字符串，后续由 RAG 推荐理由填充
}
```

## 4. 商品搜索请求 (ProductSearchRequest.java)

```java
public class ProductSearchRequest {
    @JsonProperty("query")        private String query;         // 搜索关键词
    @JsonProperty("category")     private String category;      // 类目过滤
    @JsonProperty("sub_category") private String subCategory;   // 子类目过滤
    @JsonProperty("brand")        private String brand;         // 品牌过滤
    @JsonProperty("min_price")    private BigDecimal minPrice;  // 最低价格
    @JsonProperty("max_price")    private BigDecimal maxPrice;  // 最高价格
    @JsonProperty("limit")        private Integer limit;        // 返回数量，默认 10，最大 20
}
```

## 5. 商品搜索响应 (ProductSearchResponse.java)

```java
public class ProductSearchResponse {
    @JsonProperty("query")    private String query;
    @JsonProperty("total")    private Integer total;
    @JsonProperty("products") private List<ProductCard> products;
}
```

## 6. 对话请求 (ChatRequest) — 待实现

```java
public class ChatRequest {
    private String sessionId;
    private String message;
}
```

## 7. SSE 事件数据 (SseEvent) — 待实现

```java
public class SseEvent {
    private String event;   // text | product_card | error | done
    private String data;    // JSON 序列化的事件数据
}
```

## 8. 向量文档 (VectorDoc) — 待实现

```java
public class VectorDoc {
    private String docId;
    private String productId;
    private String content;
    private List<Float> embedding;
    private Map<String, Object> metadata;
}
```

## 实体关系

```
Product 1 ── 1 VectorDoc（后续）
Product ── ProductCard（视图投影）
ProductSearchRequest ── ProductSearchResponse
ChatRequest ── SSE Events (text | product_card | error | done)（后续）
```

## 关键词检索评分规则（当前 MVP）

| 命中字段 | 得分 |
|----------|------|
| name | +5 |
| subCategory | +4 |
| category | +3 |
| brand | +3 |
| specs (key/value) | +2 |
| description | +1 |

query 按空格分词，每个 token 独立评分后累加。仅返回 score > 0 的商品，按分数降序排列。

后续会接入 Embedding + Qdrant 向量检索，替换当前关键词检索。
