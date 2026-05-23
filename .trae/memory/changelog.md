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

---

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
