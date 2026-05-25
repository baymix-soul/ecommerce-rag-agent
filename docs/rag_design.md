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
2. `product_card`：每个候选商品一条卡片事件
3. `done`：流结束

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
| 候选召回 | `CandidateProductRetriever` | ✅ 已实现 | 调用 ProductService.search() 关键词检索，返回 Top-N 候选 |
| Prompt | `RagPromptBuilder` | ✅ 已实现 | 构造 System Prompt + 候选商品列表 + 用户问题 |
| LLM | `LlmClient` (接口) | ✅ 已实现 | 流式生成接口定义 |
| LLM Mock | `MockLlmClient` | ✅ 已实现 | 不调用外部 API，模拟流式输出 |
| LLM 真实 | `DoubaoLlmClient` | ✅ 已实现 | Doubao-Seed-2.0-lite API 调用，Java HttpClient + SSE 流式解析 |
| LLM 配置 | `LlmConfig` | ✅ 已实现 | Mock/Doubao 切换 + 配置校验 |
| 对话服务 | `ChatService` | ✅ 已实现 | 编排召回→Prompt→LLM→SSE 事件流 |
| 对话接口 | `ChatController` | ✅ 已实现 | POST /api/chat/stream |
| Embedding | `EmbeddingProvider` | ❌ 待实现 | 调用外部 API 将文本转为向量 |
| 向量存储 | `VectorStoreService` | ❌ 待实现 | 商品向量索引管理、ANN 搜索 |
| 向量检索 | `VectorRetriever` | ❌ 待实现 | 多路召回 + 结构化过滤 |
| 重排序 | `Reranker` | ❌ 待实现 | 检索结果精排 + 业务排序 |
| 知识库 | `ProductService` + `JsonLoader` | ✅ 已实现 | 商品数据加载、更新 |

## 检索策略

### 当前阶段：关键词检索 + Mock/真实 LLM

1. 用户查询通过 `CandidateProductRetriever` 调用 `ProductService.search()`
2. 关键词按空格分词，在商品名称/类目/品牌/描述/属性中匹配
3. 按匹配度评分排序，取 Top-N（默认 5，最大 10）
4. `RagPromptBuilder` 构造含候选商品的 Prompt
5. `LlmClient`（Mock 或 Doubao）流式生成导购文本
6. `ChatService` 通过 SSE 返回 text + product_card + done 事件

### 后续阶段：Qdrant 向量检索

接入 Qdrant 后通过 `VectorStoreService` 接口替换实现类：
- 文本 Embedding 转为向量 → ANN 搜索
- 向量相似度 + 关键词匹配融合排序
- 支持元数据过滤（类目、价格区间）

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

## 性能指标 (规划)

- 检索延迟 P99 < 500ms
- 端到端首字延迟 < 2s (含 Embedding + 检索 + LLM 首 token)
- 检索召回率 Top-20 > 90%
- 单次对话最长 30s 超时
