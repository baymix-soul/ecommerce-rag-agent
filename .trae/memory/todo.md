# 待办任务列表

> 项目当前的任务跟踪，按优先级排序。

## 格式

```
- [ ] [P{0-3}] {任务描述}
  - 优先级说明: P0(紧急) / P1(高) / P2(中) / P3(低)
```

---

## 当前阶段：Project Context and Rules Initialization

### 上下文与规则填充

- [x] [P0] 写入 AGENTS.md 项目总上下文
- [x] [P0] 重写 .trae/rules/ 下 5 个规则文件
- [x] [P0] 重写 .trae/commands/ 下 5 个命令 Prompt 模板
- [x] [P0] 重写 .trae/memory/decisions.md（9 条技术决策）
- [x] [P0] 重写 .trae/memory/changelog.md
- [x] [P0] 补充 docs/ 下 5 个设计文档

---

## 下一阶段：Backend MVP

### 基础搭建

- [x] [P1] 确认后端构建工具为 Maven（已决策 D-009）
- [ ] [P1] 初始化 Spring Boot 3 Maven 项目骨架
- [ ] [P1] 创建配置类（环境变量读取 LLM Key、Embedding Key、向量库地址）
- [ ] [P1] 实现健康检查 API（GET /health）

### 商品数据

- [ ] [P1] 设计并创建商品数据 JSON schema
- [ ] [P1] 准备 50-100 条商品 mock 数据（resources/data/products.json）
- [ ] [P1] 编写商品数据加载脚本（JsonLoader）
- [ ] [P1] 实现 ProductService（商品查询、过滤）

### RAG Pipeline

- [ ] [P1] 封装 Embedding 接口（EmbeddingProvider）
- [ ] [P1] 实现关键词检索（KeywordRetriever，MVP 第一阶段）
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