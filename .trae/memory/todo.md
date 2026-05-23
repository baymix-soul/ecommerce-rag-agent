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

- [ ] [P1] 封装 Embedding 接口（EmbeddingProvider）
- [ ] [P1] 实现关键词检索（KeywordRetriever，MVP 第一阶段）— ProductService 已实现基础关键词检索，后续需抽取为独立 Retriever
- [ ] [P1] 预留 VectorStoreService 接口（后续接入 Qdrant）
- [ ] [P1] 实现检索 API（VectorRetriever）

### 对话与流式

- [ ] [P1] 设计并实现 RAG Prompt 模板（PromptTemplate）
- [ ] [P1] 实现 LLM 客户端（LLMClient，兼容 OpenAI-style API）
- [ ] [P1] 实现 SSE 对话接口（ChatController + ChatService）
- [ ] [P1] 实现 SSE 事件序列化（SSEHelper）

### 后续阶段（暂不启动）

- [ ] [P2] Android 客户端搭建
- [ ] [P2] 多轮对话上下文管理
- [ ] [P2] 排除已推荐商品逻辑
- [ ] [P3] 购物车增强功能
- [ ] [P3] 多模态输入（图片搜索）
