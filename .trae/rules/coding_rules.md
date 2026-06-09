# 编码规则

## 语言与框架

- **后端**: Java 17/21 + Spring Boot 3
- **构建工具**: Maven
- **客户端**: Android Kotlin + Jetpack Compose
- **向量数据库**: Qdrant 或 Chroma
- **商品数据**: JSON（MVP）→ SQLite（后续）

## Java 编码规范

### 代码风格

1. 遵循 Java 标准编码规范（Oracle Java Style Guide）。
2. 使用 4 空格缩进，不使用 Tab。
3. 行宽不超过 120 字符。
4. 所有 public 方法必须标注参数和返回值的完整类型。

### 命名规范

| 类型       | 规范          | 示例                    |
|------------|---------------|------------------------|
| Package    | lowercase     | `com.ecommerce.rag`    |
| 类         | PascalCase    | `ProductService`       |
| 接口       | PascalCase    | `EmbeddingProvider`    |
| 方法       | camelCase     | `searchProducts()`     |
| 变量       | camelCase     | `productList`          |
| 常量       | UPPER_SNAKE   | `MAX_RETRY_COUNT`      |
| 私有成员   | camelCase     | `embeddingClient`      |

### 关键约定

1. **核心逻辑必须注释** — RAG Pipeline、Prompt 构造、检索算法必须写清晰注释
2. **函数保持单一职责** — 一个方法只做一件事，避免过长方法（> 80 行应考虑拆分）
3. **API 返回结构稳定** — 一旦发布的 API 格式，不应随意增删字段
4. **错误处理必须清晰** — 不吞异常，异常信息对调试有帮助
5. **不在业务代码中硬编码 Key** — 所有 API Key 从环境变量或配置类读取
6. **日志不得输出敏感信息** — 不打印 API Key、Token、用户隐私数据
7. 所有外部 API 调用必须有超时和重试机制
8. 使用 SLF4J + Logback 作为日志框架
9. 不做不必要的注释，代码应自解释；复杂逻辑用简洁注释说明意图

## 后端项目结构（建议）

```
server/
├── pom.xml                      # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/rag/
│   │   │   ├── RagApplication.java          # Spring Boot 入口
│   │   │   ├── core/                        # 核心配置
│   │   │   │   ├── config/
│   │   │   │   │   ├── AppConfig.java       # 应用配置（环境变量）
│   │   │   │   │   ├── LLMConfig.java       # LLM 客户端配置
│   │   │   │   │   └── VectorDBConfig.java  # 向量数据库配置
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java
│   │   │   │       └── AppException.java
│   │   │   ├── api/                         # API 路由层
│   │   │   │   ├── ChatController.java      # 对话接口
│   │   │   │   ├── SearchController.java    # 检索接口
│   │   │   │   └── HealthController.java    # 健康检查
│   │   │   ├── models/                      # 数据模型（DTO / VO）
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ChatRequest.java
│   │   │   │   │   └── ChatResponse.java
│   │   │   │   ├── vo/
│   │   │   │   │   └── ProductCard.java
│   │   │   │   └── entity/
│   │   │   │       └── Product.java
│   │   │   ├── services/                    # 业务逻辑
│   │   │   │   ├── ChatService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   └── SearchService.java
│   │   │   ├── rag/                         # RAG 模块
│   │   │   │   ├── embedding/
│   │   │   │   │   └── EmbeddingProvider.java
│   │   │   │   ├── retriever/
│   │   │   │   │   └── VectorRetriever.java
│   │   │   │   ├── prompt/
│   │   │   │   │   └── PromptTemplate.java
│   │   │   │   └── llm/
│   │   │   │       └── LLMClient.java
│   │   │   └── utils/                       # 工具函数
│   │   │       ├── JsonLoader.java
│   │   │       └── SSEHelper.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── data/
│   │           └── products.json            # 商品 mock 数据
│   └── test/
│       └── java/com/ecommerce/rag/
│           ├── api/
│           ├── services/
│           └── rag/
```

## Kotlin / Android 编码规范

1. 遵循 Kotlin 官方编码规范
2. 使用 Jetpack Compose 声明式 UI
3. UI 组件拆分为独立的 Composable 函数
4. 网络请求使用 Retrofit + OkHttp
5. SSE 消费使用 OkHttp 的流式响应处理
6. 状态管理使用 ViewModel + StateFlow