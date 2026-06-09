# 前端接口说明（后端开发依据）

本文档以**前端实际调用为唯一事实来源**整理后端接口约定，供后端开发实现。所有路径、字段、协议均来自 `android/app/src/main/java/com/ecommerce/rag/client` 目录下的真实代码（截至阶段 9）。

---

## 0. 总览

### 0.1 基础约定

| 项 | 约定 |
|---|---|
| Base URL（开发） | `http://10.0.2.2:8080`（Android Emulator 访问宿主机本地后端） |
| Base URL（生产） | `https://api.your-domain.com`（占位，上线前替换） |
| 编码 | UTF-8 |
| 请求体格式 | JSON（`Content-Type: application/json`） |
| 响应体格式 | JSON（SSE 接口除外，见 §4） |
| 字段命名 | **请求体与响应体一律 snake_case**（前端在客户端层做大小写适配） |
| 时区/时间戳 | 当前接口无时间字段；如未来加，请使用 ISO 8601 UTC 字符串 |
| 货币单位 | 价格字段是数值，币种用单独的 `currency` 字段（默认 `"CNY"`） |

### 0.2 认证模型

- 唯一认证方式：HTTP Bearer Token。
- 登录接口（`POST /api/auth/login`）返回 `access_token`，前端持久化到 SharedPreferences（`auth_prefs`），后续请求在 Header 中以 `Authorization: Bearer <access_token>` 传递。
- 没有 Refresh Token、没有 OAuth、没有 Cookie。
- Token 失效（401/403）时，前端会把用户引导回 LoginScreen 重新登录。

| 接口 | 是否需要 Authorization |
|---|---|
| `POST /api/auth/login` | 否（登录本身） |
| `GET /api/products/{productId}` | 否（公开商品信息） |
| `POST /api/chat/stream` | **可选**（带上时启用对话式加购等高级能力） |
| `GET /api/cart` | **必须** |
| `POST /api/cart/items` | **必须** |
| `PATCH /api/cart/items/{productId}` | **必须** |
| `DELETE /api/cart/items/{productId}` | **必须** |
| `DELETE /api/cart` | **必须** |

### 0.3 通用错误约定

- 非 2xx 视为失败。前端会把 HTTP code 与 body 文本拼到错误信息里展示，例如 `"购物车请求失败 (401): ..."`。
- 推荐响应体（失败时）：
  ```json
  { "code": "INVALID_TOKEN", "message": "token 已过期" }
  ```
  - `code` 是可选的、稳定的枚举字符串（仅用于前端做关键分支，目前无强依赖）。
  - `message` 是面向用户可读的中文文案（前端会原样展示）。
- 401 / 403：前端会按"未登录"处理：清掉本地 token 引导用户重新登录；body 中包含 `"401"`、`"403"`、`"未登录"`、`"unauthorized"` 字符串都会触发该兜底分支。
- 404（仅 `GET /api/products/{productId}`）：前端识别为"商品不存在"。

### 0.4 客户端实现入口（便于联调时定位前端调用栈）

| 接口 | 客户端类 |
|---|---|
| 登录 | `data/remote/LoginApiClient.kt` |
| 商品详情 | `data/remote/ProductApiClient.kt` |
| 购物车 | `data/remote/CartApiClient.kt` |
| 对话 SSE | `data/remote/SseClient.kt` |

---

## 1. 认证 - 登录

### 1.1 `POST /api/auth/login`

为已注册用户颁发 access token。

**请求头**
```
Content-Type: application/json
```

**请求体**
```json
{
  "username": "demo",
  "password": "demo123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `username` | string | 是 | 用户名 |
| `password` | string | 是 | 明文密码（演示项目，传输已通过 HTTPS 保证安全；后端落库需自行 hash） |

**成功响应（HTTP 200）**
```json
{
  "access_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "expires_in": 604800,
  "user_id": "demo-user",
  "username": "demo"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `access_token` | string | **是** | 后续 Authorization 头使用 |
| `token_type` | string | 否 | 默认 `"Bearer"`；前端不存在不会崩 |
| `expires_in` | int (秒) | 否 | 默认 3600；前端不会主动刷新，过期靠 401 兜底 |
| `user_id` | string | **是** | 用户唯一 ID，用于购物车归属 |
| `username` | string | 否 | 缺失时前端用请求里的 username 兜底 |

**失败响应**
- HTTP 401：用户名密码错误
- HTTP 4xx/5xx：body 文本会原样拼到错误提示

**演示账号要求**
- 后端必须内置一个 `username=demo / password=demo123` 的账号。前端启动时会自动用这套账号登录一次（`autoLoginDemoIfNeeded`）。

---

## 2. 商品 - 详情

### 2.1 `GET /api/products/{productId}`

按 ID 取单个商品的详情。

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| `productId` | string | 商品 ID（与 `assets/products.json` 一致，如 `p_clothes_001`） |

**请求头**：无要求（公开接口）。

**成功响应（HTTP 200）**
```json
{
  "product_id": "p_clothes_001",
  "name": "条纹宽松休闲衬衫（薄款）",
  "brand": "City Wave",
  "category": "服饰运动",
  "sub_category": "男装上衣",
  "price": 199.0,
  "currency": "CNY",
  "image_url": "/images/p_clothes_001.jpg",
  "description": "宽松版型、垂感面料……",
  "specs": {
    "颜色": "海军蓝",
    "尺码": "M / L / XL"
  },
  "avg_rating": 4.6
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `product_id` | string | 是 | 商品 ID |
| `name` | string | 是 | 商品名 |
| `brand` | string | 否 | 缺失/空时前端兜底为 `"官方旗舰"` |
| `category` | string | 否 | 一级类目 |
| `sub_category` | string | 否 | 二级类目 |
| `price` | number | **是** | 价格 |
| `currency` | string | 否 | 默认 `"CNY"` |
| `image_url` | string | 是 | 商品主图相对路径，前端会拼成 `BASE_URL + image_url` 加载 |
| `description` | string | 否 | 商品描述长文 |
| `specs` | `Map<string, string>` | 否 | 规格参数键值对 |
| `avg_rating` | number | 否 | 平均评分，前端 clamp 到 `[0, 5]` |

**失败响应**
- HTTP 404：`商品不存在: <productId>`（前端会回退到本地 `assets/products.json` 兜底，无后端不可用风险）

**说明**：商品列表页（Browse）**不调用任何后端接口**，完全使用 `assets/products.json` 渲染。后端不需要实现商品列表接口。

---

## 3. 购物车

所有接口必须带 `Authorization: Bearer <access_token>`。所有接口的成功响应统一使用下面定义的 **CartView** 结构。

### 3.1 CartView（响应体公共结构）

```json
{
  "user_id": "demo-user",
  "currency": "CNY",
  "total_quantity": 3,
  "total_amount": 597.0,
  "items": [
    {
      "product_id": "p_clothes_001",
      "name": "条纹宽松休闲衬衫（薄款）",
      "price": 199.0,
      "currency": "CNY",
      "image_url": "/images/p_clothes_001.jpg",
      "quantity": 3,
      "subtotal": 597.0
    }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `user_id` | string | 否 | 购物车归属用户 ID |
| `currency` | string | 否 | 默认 `"CNY"` |
| `total_quantity` | int | 是 | 总件数（前端用作角标） |
| `total_amount` | number | 是 | 总金额 |
| `items` | array of CartItem | 是 | 商品列表（可为空数组） |

**CartItem**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `product_id` | string | 是 | 商品 ID |
| `name` | string | 是 | 商品名（请直接从商品库 join 出来回传，前端不会再请求一次详情） |
| `price` | number | 是 | 单价 |
| `currency` | string | 否 | 默认 `"CNY"` |
| `image_url` | string | 是 | 同 `GET /api/products/{id}` 中的 `image_url`，相对路径 |
| `quantity` | int | 是 | 数量（≥ 1） |
| `subtotal` | number | 是 | 小计 = price × quantity（前端**不**重算，以后端为准） |

**所有写操作（POST/PATCH/DELETE）成功后请回传完整 CartView，前端不会自己合并增量。**

### 3.2 `GET /api/cart`

获取当前用户购物车。

**请求头**
```
Authorization: Bearer <access_token>
```

**成功响应（HTTP 200）**：CartView。空购物车也请返回 `items: []`，不要 204。

**失败响应**
- 401：未登录或 token 过期 → 前端跳登录页

### 3.3 `POST /api/cart/items`

加购。

**请求头**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**请求体**
```json
{
  "product_id": "p_clothes_001",
  "quantity": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `product_id` | string | 是 | 商品 ID |
| `quantity` | int | 否（默认 1） | 增量数量（要求 ≥ 1） |

**成功响应（HTTP 200）**：更新后的 CartView。

**语义建议**：
- 商品已在购物车里 → 数量累加
- 商品不在购物车里 → 新增一项

### 3.4 `PATCH /api/cart/items/{productId}`

修改某商品的数量为指定值（**绝对值，不是增量**）。

**路径参数**：`productId` = 商品 ID。

**请求头**
```
Authorization: Bearer <access_token>
Content-Type: application/json
```

**请求体**
```json
{
  "quantity": 3
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `quantity` | int | 是 | 目标数量。前端只在 ≥ 1 时发请求；不会传 0（删除请用 DELETE） |

**成功响应（HTTP 200）**：更新后的 CartView。

### 3.5 `DELETE /api/cart/items/{productId}`

删除某商品。

**路径参数**：`productId` = 商品 ID。

**请求头**
```
Authorization: Bearer <access_token>
```

**成功响应（HTTP 200）**：更新后的 CartView（即不含该商品的列表）。

### 3.6 `DELETE /api/cart`

清空整个购物车。

**请求头**
```
Authorization: Bearer <access_token>
```

**成功响应（HTTP 200）**：CartView，其中 `items: []`、`total_quantity: 0`、`total_amount: 0`。

---

## 4. 对话 - SSE 流式接口

### 4.1 `POST /api/chat/stream`

唯一一个 SSE 接口。前端有两个调用方：

- 全屏 ChatScreen / ChatViewModel：**不带 token**（兼容老路径）。
- 悬浮助手 AssistantViewModel：**尽量带 token**，让"把这个加入购物车"等对话式加购可用。

**请求头**
```
Content-Type: application/json
Accept: text/event-stream
Authorization: Bearer <access_token>   (可选)
```

> 后端**必须**接受 Authorization 头缺失的请求（视为匿名）。带上时按已登录用户处理。

**请求体（snake_case，不要求字段顺序）**
```json
{
  "message": "把第一款加入购物车",
  "limit": 5,
  "page_context": {
    "page_type": "PRODUCT_DETAIL",
    "current_product_id": "p_clothes_001",
    "visible_product_ids": ["p_clothes_001", "p_beauty_003"],
    "search_query": null,
    "selected_filters": { "category": "服饰运动" },
    "recently_viewed_product_ids": ["p_clothes_001", "p_digital_002"]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `message` | string | 是 | 用户消息原文 |
| `limit` | int | 否（默认 5） | 推荐候选最多返回多少条 ProductCard |
| `page_context` | object | 否 | 当前页面快照，见下表 |

**PageContext**

| 字段 | 类型 | 说明 |
|---|---|---|
| `page_type` | enum string | 取值 `UNKNOWN / PRODUCT_LIST / PRODUCT_DETAIL / SEARCH_RESULT / CHAT / PROFILE / CART / LOGIN`（阶段 10 起新增后 3 种；老后端可统一按 `UNKNOWN` 处理） |
| `current_product_id` | string \| null | 详情页时是当前商品；其他页可能为 null |
| `visible_product_ids` | string[] | 列表/搜索页屏幕上可见的商品 ID（前端最多上报 20 个） |
| `search_query` | string \| null | 搜索关键词；为空时传 null |
| `selected_filters` | `Map<string, string>` | 筛选条件，例如 `{"category": "服饰运动"}` |
| `recently_viewed_product_ids` | string[] | 最近访问过的商品（默认最多 10 个） |

后端可以选择忽略不感兴趣的字段，但**不要因为字段缺失或为 null 报 400**。

### 4.2 SSE 响应

响应使用 `Content-Type: text/event-stream`。每条事件由两行组成：

```
event: <event-name>
data: <single-line-json>

```

事件之间用空行分隔。每条 `data:` 必须是**单行 JSON**（前端按行解析，不支持跨行 data）。

**事件类型**

| `event` | `data` 结构 | 含义 |
|---|---|---|
| `text` | `{"content": "<增量文本>"}` | 流式文本片段，前端追加到当前 AI 气泡 |
| `product_card` | 见下方 | 推荐商品卡，前端追加到当前 AI 气泡下方 |
| `done` | `{}`（可空对象） | 本次流结束，前端切到默认状态并刷新购物车角标 |
| `error` | `{"code": "<CODE>", "message": "<msg>"}` | 出错，前端停流并展示错误 |

**product_card 的 data 结构**
```json
{
  "product_id": "p_clothes_001",
  "name": "条纹宽松休闲衬衫",
  "price": 199.0,
  "currency": "CNY",
  "image_url": "/images/p_clothes_001.jpg",
  "reason": "性价比高、面料垂感好"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `product_id` | string | 是 | 商品 ID（用于点击跳详情） |
| `name` | string | 是 | 名称 |
| `price` | number | 是 | 价格 |
| `currency` | string | 否 | 默认 `"CNY"` |
| `image_url` | string | 是 | 相对路径，前端会拼 BASE_URL |
| `reason` | string | 否 | 推荐理由短句 |

**error 的 data 结构**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `code` | string | 否 | 默认 `"UNKNOWN"` |
| `message` | string | 否 | 默认 `"Unknown error"` |

**典型流示例**

```
event: text
data: {"content":"为你找到了三款合适的衬衫："}

event: product_card
data: {"product_id":"p_clothes_001","name":"条纹宽松休闲衬衫","price":199.0,"currency":"CNY","image_url":"/images/p_clothes_001.jpg","reason":"性价比首选"}

event: product_card
data: {"product_id":"p_clothes_004","name":"亚麻翻领短袖","price":259.0,"currency":"CNY","image_url":"/images/p_clothes_004.jpg","reason":"透气夏季款"}

event: text
data: {"content":"\n推荐理由已经写在卡片上啦~"}

event: done
data: {}

```

**对话式加购**

- 当用户在助手里说"把这个加入购物车"等指令时，后端可以在响应内部直接调内部购物车服务（同 `POST /api/cart/items`），完成后通过 `text` 事件回复用户成功提示，然后 `done`。
- 前端在收到 `done` 后会**自动调用一次 `GET /api/cart`** 刷新角标，所以后端**不需要**通过 SSE 推任何"购物车已更新"的额外事件。
- 如果 token 缺失/失效，后端可以回 `text` 提示"请先登录"并立即 `done`，前端会自然展示该文本；如果想更强语义，也可以走 `error` 事件，message 任意。

### 4.3 限制

- 单次响应**必须以 `done` 或 `error` 结尾**；前端在收到这两个事件其中之一时会清理 streaming 状态。
- 长时间不发数据会被前端按超时处理（默认 readTimeout 120s）。建议至少每 30s 发一个事件维持连接。
- 不要使用 `id:` 字段（前端忽略），不要使用 retry/comment 行。

---

## 5. 静态资源

- 商品图通过 `image_url` 字段以**相对路径**给到前端，前端拼 `BASE_URL + image_url` 加载。例如：
  - 响应中：`"image_url": "/images/p_clothes_001.jpg"`
  - 前端实际请求：`http://10.0.2.2:8080/images/p_clothes_001.jpg`
- 建议直接复用项目根目录 `static/` 下的图片：把 `/images/*` 映射到 `static/images/*`。
- 也允许返回绝对 URL（`http://...` / `https://...`）；前端识别到协议头就不会再拼 BASE_URL。
- 也允许返回 `file:///android_asset/...`；前端识别后会从 APK 内置 assets 读取（这是本地兜底用的特殊形式，后端**不应该**主动返回这种值）。

---

## 7. Profile / Orders / Address 预留接口（planned）

> **Status: planned / not required for current demo**
>
> 阶段 10 已完成个人中心 UI（`ProfileScreen` + `ProfilePlaceholderScreen`），但其中的"我的订单 / 待付款 / 待发货 / 待收货 / 待评价 / 退款 / 收货地址 / 优惠券 / 收藏 / 浏览历史 / 设置"在前端均是占位页，不发任何后端请求。
>
> 当前阶段 App **不强制要求**后端实现这些接口；下表只是为后端后续设计阶段做提示，字段命名规范同前文（snake_case），鉴权同购物车（必须 `Authorization: Bearer <token>`）。

### 7.1 订单

| 接口 | 用途 | 备注 |
|---|---|---|
| `GET /api/orders` | 列出当前用户全部订单 | 对应 ProfileScreen "查看全部" 入口 |
| `GET /api/orders?status=PENDING_PAYMENT` | 按状态过滤 | 状态枚举建议与前端 `OrderFilter` 对齐：`ALL / PENDING_PAYMENT / PENDING_SHIPMENT / PENDING_RECEIPT / PENDING_REVIEW / REFUND` |
| `GET /api/orders/{orderId}` | 订单详情 | 暂未在前端使用 |
| `POST /api/orders` | 创建订单（结算） | 当前购物车页"去结算"仅 toast，不调用接口 |

**建议响应（OrderSummary）**
```json
{
  "order_id": "o_2025_0001",
  "status": "PENDING_PAYMENT",
  "total_amount": 597.0,
  "currency": "CNY",
  "created_at": "2026-06-06T12:00:00Z",
  "items": [
    { "product_id": "p_clothes_001", "name": "条纹宽松休闲衬衫", "image_url": "/images/p_clothes_001.jpg", "quantity": 3, "price": 199.0, "subtotal": 597.0 }
  ]
}
```

### 7.2 收货地址

| 接口 | 用途 |
|---|---|
| `GET /api/addresses` | 列出当前用户的所有收货地址 |
| `POST /api/addresses` | 新增地址 |
| `PATCH /api/addresses/{addressId}` | 修改地址 |
| `DELETE /api/addresses/{addressId}` | 删除地址 |

**建议 Address 结构**
```json
{
  "address_id": "addr_001",
  "receiver_name": "张三",
  "phone": "13800001234",
  "province": "上海市",
  "city": "上海市",
  "district": "浦东新区",
  "detail": "世纪大道 100 号",
  "is_default": true
}
```

### 7.3 优惠券 / 收藏 / 浏览历史

| 接口 | 用途 | 备注 |
|---|---|---|
| `GET /api/coupons` | 当前用户优惠券列表 | 包含未使用/已使用/已过期分类 |
| `GET /api/favorites` | 收藏的商品列表 | 字段对齐 `CartItem`，但无 quantity/subtotal |
| `POST /api/favorites` | 加入收藏 | body: `{ "product_id": "..." }` |
| `DELETE /api/favorites/{productId}` | 取消收藏 | |
| `GET /api/history` | 浏览历史 | 服务端版本；当前前端只在内存里维护 `recently_viewed_product_ids` |

### 7.4 用户资料

| 接口 | 用途 | 备注 |
|---|---|---|
| `GET /api/profile` | 当前用户的扩展资料（积分 / 红包 / 余额 / 会员等级） | ProfileScreen 顶部资产/权益卡占位字段对应 |
| `PATCH /api/profile` | 更新昵称 / 头像 | 暂未启用 |

---

## 8. 当前前端页面与 API 对照表

只关注阶段 10 当下真实存在的页面与其后端依赖：

| 页面 | 类位置 | 已接入 API | 备注 |
|---|---|---|---|
| ProductBrowseScreen | `ui/browse/ProductBrowseScreen.kt` | 无（本地 `assets/products.json`） | 暂不请求后端，分类/搜索/可见 ID 仅作为 `page_context` 上报 |
| ProductDetailScreen | `ui/detail/ProductDetailScreen.kt` | `GET /api/products/{id}` | 后端失败时回退本地 fallback |
| CartScreen | `ui/cart/CartScreen.kt` | `/api/cart` / `/api/cart/items` 全套 5 个 | 需要 token；401 引导登录 |
| LoginScreen | `ui/auth/LoginScreen.kt` | `POST /api/auth/login` | 启动自动 demo 登录 + 手动登录 |
| ProfileScreen | `ui/profile/ProfileScreen.kt` | 无（只读 `AuthViewModel` + `CartViewModel` 状态） | 阶段 10 UI 改造；订单/地址/收藏等入口跳 [ProfilePlaceholderScreen] |
| ProfilePlaceholderScreen | `ui/profile/ProfilePlaceholderScreen.kt` | 无 | 全部为 §7 中的 planned 接口预占位 |
| AssistantOverlay / MiniChatPanel | `ui/assistant/*` | `POST /api/chat/stream` (SSE) | 尽量带 token；done 后会自动 `GET /api/cart` 刷角标 |
| FloatingBot | `ui/assistant/FloatingBot.kt` | 无 | 纯本地 GIF 角色 |

---

## 9. 后端实现优先级建议

按对前端体验影响从大到小排：

1. **`POST /api/auth/login`** —— 没有它整个登录态闭环不能用，购物车 / 对话式加购都退化。
2. **`GET /api/cart` + `POST /api/cart/items`** —— 角标、加购按钮、Agent 推荐流后的角标刷新都依赖这两个。
3. **`PATCH /api/cart/items/{id}` + `DELETE /api/cart/items/{id}` + `DELETE /api/cart`** —— 购物车页内交互。
4. **`POST /api/chat/stream`** —— 助手对话；token 可选所以可以先支持匿名再加登录态。
5. **`GET /api/products/{id}`** —— 已有本地 fallback，可放最后；只在希望详情页价格 / 标题 / specs 由服务端管控时实现。
6. **§7 预留接口（订单 / 地址 / 优惠券 / 收藏 / 浏览历史 / 用户资料）** —— 阶段 10 仅 UI 占位，不在 demo 关键路径上，做完上面 5 项后按业务优先级排即可。

---

## 10. 联调验收清单

### 10.1 开发期 curl 自检
- 先用 `curl` 验证 5 个 cart 接口的请求/响应字段名（snake_case）和 401 行为，再启动 App 实测。
- `POST /api/auth/login` 用 `demo / demo123` 必须返回带 `access_token` 的 200，前端启动会立即调用一次。
- `POST /api/chat/stream` 不带 Authorization 也必须可用（匿名）；带 Authorization 时按已登录处理。
- `GET /api/cart` 401 响应体含字符串之一（`401` / `unauthorized` / `未登录`）即可触发前端"跳登录"分支。

### 10.2 关键场景手动验收
- 启动 App → 后端 `/api/auth/login` 命中 demo 账号 → 浏览页"我的"头像出现已登录态。
- 详情页 → "加入购物车" → 后端返回更新后的 CartView → 顶部购物车角标 +1。
- 助手对话："把这个加入购物车" → SSE done 后前端自动 `GET /api/cart` → 角标刷新。
- 退出登录 → 角标归零，再次进入购物车页应触发 `needsLogin` 并跳登录。

### 10.3 日志 tag
App 侧打开 Logcat，过滤 tag：
- `SseClient` —— 看 SSE 行级日志
- `CartApiClient` / `LoginApiClient` / `ProductApiClient` —— 看请求/响应错误
- `LocalProductRepository` —— 确认本地 fallback 是否触发

### 10.4 环境
- Emulator：`http://10.0.2.2:8080` 直连宿主机后端。
- 真机：把 `AppConfig.BASE_URL` 临时改成局域网 IP（HTTP 在 `AndroidManifest.xml` 已经 `usesCleartextTraffic=true`）。
- 生产：上线前必须切到 HTTPS 并关闭 cleartext。

---

## 12. Voice / Multimodal Interaction（阶段 14 新增）

> 状态：**前端已实现 ASR（Android 端纯本地）+ TTS（前端调后端 `/api/tts/speak`）**；后端 TTS 可后续实现，未实现时前端只显示一次小错误，不影响文字 SSE。

### 12.1 ASR 语音识别

| 项 | 当前方案 |
|---|---|
| 实现方 | Android 原生 `android.speech.SpeechRecognizer` |
| 语言 | `zh-CN`，开启 `EXTRA_PARTIAL_RESULTS` |
| 后端接口 | **不调用**。识别结果作为普通文本走 `POST /api/chat/stream` |
| 麦克风权限 | `android.permission.RECORD_AUDIO`，运行时申请，首次长按"按住说话"时弹 |
| 包可见性 | `AndroidManifest.xml` 已声明 `<queries><intent action="android.speech.RecognitionService"/></queries>` |
| 设备不支持 | `SpeechRecognizer.isRecognitionAvailable() == false` 时，UI 提示 "当前设备不支持语音识别" |
| 失败兜底 | 任何 `RecognitionListener.onError` 都映射为中文提示，不发送空文本 |

未来若后端要做 ASR（**未要求**，仅占位）：

```http
POST /api/asr/transcribe
Content-Type: multipart/form-data
Authorization: Bearer <access_token>   (可选)
```

请求 form 字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `audio` | file | mp3 / wav，单声道 16kHz 推荐 |
| `language` | string | `zh-CN` |

响应：

```json
{
  "text": "把这个加入购物车",
  "confidence": 0.92
}
```

**Status: planned，本轮 Android 实现不依赖**。

### 12.2 TTS 语音合成

前端调用：

```http
POST /api/tts/speak
Content-Type: application/json
Authorization: Bearer <access_token>   (可选；TTS 不强制登录)
```

请求体：

```json
{
  "text": "已为你推荐三款适合敏感肌的商品。",
  "voice": "zh-CN-XiaoxiaoNeural",
  "format": "mp3"
}
```

请求字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `text` | string | ✅ | 前端已 trim + 截断至 **最多 500 字符**（阶段 14 默认上限） |
| `voice` | string | ✅ | 默认 `zh-CN-XiaoxiaoNeural`（与 `edge-tts` Voice 名一致） |
| `format` | string | ✅ | 当前固定 `mp3`，未来如支持 `wav` 可扩展 |

响应方式 A（**推荐**，前端优先适配）：

```json
{
  "audio_url": "http://127.0.0.1:8080/static/tts/abc.mp3",
  "content_type": "audio/mpeg"
}
```

响应方式 B（可选，前端兜底支持）：

```json
{
  "audio_base64": "<base64-encoded mp3 bytes>",
  "content_type": "audio/mpeg"
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `audio_url` | string? | 任意可被 Android `MediaPlayer.setDataSource(url)` 拉取的地址（HTTP / HTTPS）。后端若用本地静态目录，注意 cleartext。 |
| `audio_base64` | string? | mp3 原始字节的 base64。前端会写入 `cacheDir/tts/last_tts.mp3` 后播放，不持久化。 |
| `content_type` | string? | 仅日志/未来扩展，前端当前不依赖 |

后端实现建议：

- 用免费 [`edge-tts`](https://github.com/rany2/edge-tts) 生成 mp3，再托管在后端静态目录或临时签名 URL 下发；
- 长文本可在后端按句号切分后并行合成、拼接 mp3。前端不知道这些细节；
- 失败时 **返回 4xx/5xx 即可**，前端会显示一次小提示，文字回答不受影响；
- 不要直接返回 `audio/mpeg` 字节流（不在本轮支持，文档保持收口）。

前端行为：

| 触发 | 行为 |
|---|---|
| SSE `done` 后 | 取本轮 AI `ChatMessage.text` 的 trim 前 500 字 → `pendingTtsText` → 由 Composable 调一次 `/api/tts/speak` |
| 商品卡片 JSON | **不进 text**，不会被读出来 |
| 用户长按"按住说话" | 调 SpeechRecognizer，松手后 final 文本作为普通文本走 SSE，**不**触发 TTS |
| 标题栏 🔊/🔇 按钮 | 翻转 `isVoicePlaybackEnabled`：从 true 翻到 false 同时打断当前 `MediaPlayer.stop()` |
| TTS 4xx/5xx / 字段缺失 | 一次性 `ttsError` 提示，文字回答不受影响 |
| 离开页面 / Composable dispose | `MediaPlayer.release()`，临时文件清理 |

### 12.3 Chat + TTS 联动流程

```
用户长按"按住说话"
  → Android SpeechRecognizer（zh-CN, partial=true）
  → final 文本
  → POST /api/chat/stream（保持现有协议）
  → SSE text / product_card / done
  → done 后前端 trim+截断 AI 文本
  → POST /api/tts/speak
  → 响应 audio_url（推荐）/ audio_base64（兜底）
  → MediaPlayer 播放
  → 用户可点击 🔇 立即停止
```

### 12.4 与现有协议的关系

- **不修改** `/api/chat/stream` 请求体 / SSE 事件名 / 字段命名。
- **不修改** 任何购物车 / 登录 / 商品接口。
- 新增的 `/api/tts/speak` 为可选实现：后端未上线时 Android 端只显示一次错误，文字对话、商品卡片、加购、立刻下单全部不受影响。

---

## 11. 变更记录

| 版本 | 说明 |
|---|---|
| v1.0 | 阶段 9 完成时的接口快照：登录 + 购物车闭环 + 对话式加购 + 商品详情 + SSE 推荐流 |
| v1.1 | 阶段 10：新增 §7 Profile/Orders/Address 预留接口；新增 §8 页面-API 对照表；`page_type` 枚举追加 `PROFILE / CART / LOGIN`；前端纯 UI 改造，未修改既有接口路径与协议 |
| v1.2 | 阶段 14：新增 §12 Voice / Multimodal Interaction；ASR 用 Android 原生 SpeechRecognizer，**不需要后端**；新增 **可选** `POST /api/tts/speak`（edge-tts 输出 mp3 audio_url / audio_base64）；`/api/chat/stream` 协议、字段、SSE 事件不变 |
