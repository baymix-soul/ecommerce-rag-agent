# 变更日志

> 按时间倒序记录项目的所有重要变更。

## 格式

```
## [YYYY-MM-DD] 变更标题
- **类型**: init / feat / fix / docs / refactor / chore
- **描述**: 变更内容简述
- **影响范围**: 涉及的文件/模块
- **关联决策**: D-{序号}（如有）
```

## [2026-05-25] Client MVP 第二阶段：前后端联调增强、商品详情页与 Demo 稳定性
- **类型**: feat(client)
- **描述**: 在 Android Chat MVP 基础上补齐 Gradle Wrapper，新增商品详情页，实现前后端联调增强
  - 生成 gradlew / gradlew.bat 脚本（Gradle Wrapper for Windows）
  - 创建 ProductDetailUiModel.kt（11 字段：productId/name/brand/category/subCategory/price/currency/imageUrl/description/specs/avgRating）
  - 创建 ProductApiClient.kt（OkHttp GET /api/products/{productId}，解析 JSON 含 specs Map 字段）
  - 创建 ProductDetailViewModel.kt（ViewModel + StateFlow，管理 loading/product/error 三态，图片 URL 自动拼接 baseUrl）
  - 创建 ProductDetailScreen.kt（Scaffold + TopAppBar 返回箭头 + 商品大图 + 价格/品牌/类目/评分卡片 + 描述 + 规格参数表）
  - 重构 MainActivity.kt（单 Activity 实现 ChatScreen ↔ ProductDetailScreen 导航，selectedProductId 状态切换）
  - 重构 ChatScreen.kt / MessageBubble.kt（新增 onProductClick 回调链路，点击商品卡片跳转详情页）
  - 图片加载失败时 Coil 自动显示 placeholder（不需要额外处理）
  - AppConfig 保留 BASE_URL = http://10.0.2.2:8080
- **影响范围**: client/android/**
- **关联决策**: D-003, D-006

---

## [2026-05-24] Client MVP 第一阶段：Android 原生客户端最小闭环
- **类型**: feat(client)
- **描述**: 创建 Android Kotlin + Jetpack Compose 客户端，实现最小导购对话界面，消费 SSE 事件，展示 AI 文本回复和商品卡片
  - 创建 Android 工程骨架（Gradle 8.5 + AGP 8.2.2 + Kotlin 1.9.22 + Compose BOM 2024.02.00）
  - 创建 AppConfig.kt（集中配置 baseUrl=http://10.0.2.2:8080, 超时参数）
  - 创建数据模型：ChatMessage, ProductCardUiModel, SseEvent（sealed class）, ChatUiState
  - 创建 SseClient.kt（OkHttp POST + BufferedReader 逐行解析 SSE event/data，支持 text/product_card/done/error 四种事件）
  - 创建 ChatViewModel.kt（ViewModel + StateFlow，管理消息列表/loading/错误，SSE 事件驱动 UI 更新）
  - 创建 ChatScreen.kt（Scaffold + LazyColumn 消息列表 + 底部输入框 + 发送按钮 + LinearProgressIndicator）
  - 创建 MessageBubble.kt（用户/AI 气泡区分，AI 气泡含"正在思考..."loading + 流式文本 + 商品卡片）
  - 创建 ProductCardComposable.kt（商品名 + 价格 + 推荐理由 + Coil 图片加载，黄色边框卡片样式）
  - 创建 Theme（Material3 + 自定义颜色：用户气泡绿/AI 气泡灰/商品卡片黄/价格红）
  - 创建 MainActivity.kt + ChatApplication.kt + AndroidManifest.xml（INTERNET 权限 + usesCleartextTraffic=true）
  - 更新 .gitignore（添加 Android 构建产物和 local.properties）
  - 删除 client/placeholder.txt
- **影响范围**: client/android/**
- **关联决策**: D-003, D-006

---

## [2026-05-24] Backend MVP 第四阶段：接入真实 Doubao LLM + SSE 事件格式优化
- **类型**: feat(server)
- **描述**: 接入真实 Doubao LLM 客户端，支持 Mock/真实 LLM 切换，优化 SSE 事件格式为统一 JSON
  - 创建 DoubaoLlmClient.java（Java 11+ HttpClient + OpenAI-style Chat Completions API + SSE 流式解析）
  - 更新 LlmConfig.java（Mock/Doubao 切换 + 配置校验，api-key/model 为空时给出清晰错误）
  - 更新 ChatService.java（SSE text 事件改为 JSON 格式 `{"content":"..."}`, done 事件改为 `{}`）
  - 更新 RagPromptBuilder.java（增强 Prompt 安全规则：7 条规则，增加"不得推荐候选列表之外商品"）
  - 更新 AppProperties.java（LlmProperties 默认值调整：baseUrl 默认 Doubao API，timeout 默认 30s）
  - 更新 application.yml（LLM 配置环境变量支持，MOCK_LLM_ENABLED 环境变量切换）
  - 更新 .env.example（添加 MOCK_LLM_ENABLED 配置项）
  - 编写 12 个新测试（DoubaoLlmClientTest 7 + LlmConfigTest 5），更新 RagPromptBuilderTest（+1），全部通过
  - 全部 57 个测试通过（含第三阶段 44 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/**, server/src/test/**
- **关联决策**: D-003, D-007, D-008

## [2026-05-23] Backend MVP 第三阶段：RAG Pipeline 雏形 + Mock LLM + SSE Chat API
- **类型**: feat(server)
- **描述**: 搭建可运行的 RAG 对话链路雏形，用户问题→候选召回→Prompt构造→Mock LLM→SSE流式返回
  - 创建 ChatRequest.java DTO（message/sessionId/limit，limit 默认5最大10）
  - 创建 ChatCandidate.java DTO（10 字段，Product→候选商品转换结构）
  - 创建 CandidateProductRetriever.java（基于 ProductService.search() 关键词检索，返回 Top-N 候选商品）
  - 创建 RagPromptBuilder.java（System Prompt + 候选商品列表 + 用户问题，含禁止编造约束）
  - 创建 LlmClient.java 接口（streamGenerate 方法，Consumer 回调模式）
  - 创建 MockLlmClient.java（不调用外部 API，模拟流式输出文本 chunk，区分有/无候选场景）
  - 创建 LlmConfig.java（根据 app.chat.mock-llm-enabled 配置注入 Mock 或真实 LLM）
  - 创建 ChatService.java（异步执行 LLM 生成，SSE 事件序列：text→product_card→done）
  - 创建 ChatController.java（POST /api/chat/stream，text/event-stream 响应）
  - 更新 AppProperties.java 增加 ChatProperties（mockLlmEnabled/defaultCandidateLimit）
  - 更新 application.yml 增加 app.chat 配置节
  - 编写 23 个新测试（CandidateProductRetrieverTest 5 + RagPromptBuilderTest 6 + MockLlmClientTest 3 + ChatServiceTest 7 + ChatControllerTest 2），全部通过
  - 全部 44 个测试通过（含第二阶段 21 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/**, server/src/test/**
- **关联决策**: D-003, D-004, D-007

## [2026-05-23] Backend MVP 第二阶段：商品数据转换 + Product Schema + JSON 加载 + 商品检索 API
- **类型**: feat(server)
- **描述**: 完成商品数据转换、模型定义、加载服务和基础检索 API
  - 创建 Python 数据转换脚本 scripts/convert_teacher_dataset.py
  - 转换老师数据集 100 个商品 JSON → 标准 products.json（12 字段）
  - 复制 100 张商品图片到 server/src/main/resources/static/images/，统一命名 {product_id}.jpg
  - 创建 Product.java 实体类（12 字段，BigDecimal price，Jackson @JsonProperty snake_case）
  - 创建 ProductCard.java VO（6 字段，reason 当前为空字符串）
  - 创建 ProductSearchRequest.java DTO（query/category/subCategory/brand/minPrice/maxPrice/limit）
  - 创建 ProductSearchResponse.java DTO（query/total/products）
  - 创建 ProductNotFoundException.java（继承 AppException，返回 HTTP 404）
  - 创建 JsonLoader.java 工具类（classpath 加载 JSON，Jackson 反序列化）
  - 创建 ProductService.java 服务类（@PostConstruct 加载，校验，关键词检索 + 评分排序）
  - 创建 ProductController.java（GET /api/products, GET /api/products/{id}, POST /api/products/search）
  - 更新 GlobalExceptionHandler 支持 ProductNotFoundException → HTTP 404
  - 删除 4 个 .gitkeep 文件（被实际 Java 类替代）
  - 编写 21 个测试（ProductServiceTest 13 个 + ProductControllerTest 7 个 + RagApplicationTests 1 个），全部通过
- **影响范围**: server/*, scripts/*
- **关联决策**: D-004, D-005, D-007

## [2026-05-23] Backend MVP Spring Boot 工程骨架搭建
- **类型**: feat(server)
- **描述**: 初始化 Spring Boot 3 + Maven 后端工程骨架
  - 创建 pom.xml（Spring Boot 3.3.6, Java 17, 4 个必要依赖）
  - 创建 RagApplication.java 启动类
  - 创建 HealthController.java（GET /api/health 返回 status/service/version）
  - 创建 AppProperties.java 配置类（llm/vector/product 三组属性，全部从环境变量读取）
  - 创建 AppException.java + GlobalExceptionHandler.java 统一异常处理
  - 创建 application.yml（server.port 8080, app.* 配置映射）
  - 创建 products.json 空数组占位
  - 创建 RagApplicationTests.java 上下文加载测试
  - 创建 8 个空包占位（models/dto, models/vo, services, rag/embedding, rag/retriever, rag/prompt, rag/llm, utils）
  - 删除 server/__init__.py（Python 残余文件）
  - 更新 .env.example（补充 LLM_BASE_URL, LLM_MODEL, VECTOR_ENABLED, QDRANT_URL）
- **影响范围**: server/*, .env.example
- **关联决策**: D-001, D-004, D-005, D-008, D-009

## [2026-05-21] 项目上下文与规则文件填充
- **类型**: init
- **描述**: 基于最新技术栈（Java 17/21 + Spring Boot 3 + Android Kotlin）完成项目上下文和规则文件的完整填充
  - 写入 AGENTS.md 作为项目总上下文入口
  - 重写 5 个规则文件：global_rules, permission_rules, coding_rules, rag_rules, git_rules
  - 重写 5 个命令文件为 Prompt 模板：init_project, add_feature, fix_bug, review_code, update_context
  - 重写 3 个记忆文件：decisions（9 条决策）, changelog, todo
  - 补充 5 个文档文件待完成
- **影响范围**: AGENTS.md, .trae/rules/*, .trae/commands/*, .trae/memory/*
- **关联决策**: D-001 ~ D-009

## [2026-05-21] 项目目录初始化（第一轮）
- **类型**: init
- **描述**: 创建项目基础目录结构，搭建 .trae/ 规则体系、命令定义、记忆文件和 docs/ 设计文档框架。技术栈为 Python/FastAPI（已被后续轮次覆盖更新）
- **影响范围**: 全项目
- **关联决策**: 已被 D-001 ~ D-009 覆盖更新
