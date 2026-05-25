# 待办任务列表

> 项目当前的任务跟踪，按优先级排序。

## 格式

```
- [ ] [P{0-3}] {任务描述}
  - 优先级说明: P0(紧急) / P1(高) / P2(中) / P3(低)
```

---

## 当前阶段：Backend MVP

### 基础搭建

- [x] [P1] 确认后端构建工具为 Maven（已决策 D-009）
- [x] [P1] 初始化 Spring Boot 3 Maven 项目骨架
- [x] [P1] 创建配置类（环境变量读取 LLM Key、Embedding Key、向量库地址）
- [x] [P1] 实现健康检查 API（GET /api/health）

### 商品数据

- [x] [P1] 设计并创建商品数据 JSON schema（12 字段标准 schema）
- [x] [P1] 准备 100 条商品数据（老师数据集转换 → products.json）
- [x] [P1] 编写商品数据加载服务（JsonLoader + ProductService）
- [x] [P1] 实现商品查询与过滤 API（GET /api/products, GET /api/products/{id}, POST /api/products/search）

### RAG Pipeline

- [x] [P1] 实现候选商品召回（CandidateProductRetriever，基于 ProductService.search() 关键词检索）
- [x] [P1] 实现 RAG Prompt 构造（RagPromptBuilder，System + 候选商品 + 用户问题）
- [ ] [P1] 封装 Embedding 接口（EmbeddingProvider）
- [ ] [P1] 预留 VectorStoreService 接口（后续接入 Qdrant）
- [ ] [P1] 实现向量检索（VectorRetriever）

### 对话与流式

- [x] [P1] 实现 LLM 客户端接口 + Mock 实现（LlmClient + MockLlmClient）
- [x] [P1] 实现 SSE 对话接口（ChatController + ChatService，POST /api/chat/stream）
- [x] [P1] 实现 SSE 事件序列化（text / product_card / error / done）
- [x] [P1] 接入真实 LLM（DoubaoLlmClient，兼容 OpenAI-style API，Mock/Doubao 可切换）

### 后续阶段（暂不启动）

- [x] [P2] Android 客户端搭建（Client MVP 第一阶段：最小闭环对话界面 + SSE 消费 + 商品卡片）
- [x] [P2] 商品详情页与前后端联调增强（Client MVP 第二阶段：Gradle Wrapper + ProductDetailScreen + 导航 + ProductApiClient）
- [ ] [P2] 多轮对话上下文管理
- [ ] [P2] 排除已推荐商品逻辑
- [ ] [P3] 购物车增强功能
- [ ] [P3] 多模态输入（图片搜索）
