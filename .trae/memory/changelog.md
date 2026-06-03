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

## [2026-06-03] 最小上线闭环：Docker 化 + Nginx + 部署文档 + Smoke Test

- **类型**: chore + docs
- **描述**: 完成最小上线闭环部署能力，不修改核心业务逻辑、SSE 协议、RAG 检索链路
  - 创建 server/Dockerfile（multi-stage build，Maven 构建 + JRE 运行，全部配置通过环境变量注入）
  - 创建 deploy/docker-compose.yml（backend + qdrant，qdrant_storage volume 持久化，healthcheck 依赖）
  - 创建 deploy/.env.production.example（全部生产必填变量占位符，含注释说明维度一致性/首次 rebuild/demo 调低配置）
  - 创建 deploy/nginx/ecommerce-rag-agent.conf（HTTPS 反代 + SSE 专用配置：buffering off/cache off/timeout 3600s/chunked_encoding）
  - 创建 deploy/scripts/smoke_test.ps1 + smoke_test.sh（5 项测试：health/products/vector-index-stats/retrieval-debug/SSE chat stream）
  - 更新 client/android/AppConfig.kt（新增生产 BASE_URL 注释模板 + HTTPS 说明，默认保持 10.0.2.2 开发地址）
  - 创建 docs/deployment_mvp.md（完整部署文档：架构图、服务器准备、Docker Compose 部署、Nginx+HTTPS、Qdrant 持久化、首次索引重建、Android 切换、Smoke Test、常见问题 9 项、回滚方案、安全说明、上线能力边界）
  - 更新 .gitignore（新增 deploy/.env.production + qdrant_storage/ 忽略规则）
  - 更新 .trae/memory/changelog.md + todo.md
  - 不修改核心 RAG 业务逻辑、不修改 SSE 协议、不修改 product_card 格式、不修改 LLM Prompt、不引入新功能/Redis/MySQL/Kubernetes
  - 不写真实 API Key、不写真实域名、不破坏本地开发配置
- **影响范围**: server/Dockerfile, deploy/*, client/android/.../AppConfig.kt, docs/deployment_mvp.md, .gitignore, .trae/memory/*
- **关联决策**: D-001, D-009

---

## [2026-06-03] Backend Query Understanding Refactor 第四阶段 B：ChatService Assist 模式局部接管

- **类型**: feat(server)
- **描述**: ChatService 在 QUERY_PLANNER_MODE=assist 时根据 gating 条件使用 planner effectiveAnalysis 进行真实检索
  - 增强 QueryPlanGatingService.isLegacyIncomplete()（新增 maxPrice 缺失检测、intent 差异检测、planner softKeywords 补充检测）
  - ChatService 已从 Phase 4A 使用 understanding.getEffectiveAnalysis()，assist 模式下 gateDecision.allowed=true 时自动使用 planner analysis
  - ConversationState 保存流程已通过 effectiveAnalysis 正确保存 planner 的 category/subCategory/maxPrice/positiveKeywords
  - Debug controller 与 ChatService 使用同一 QueryUnderstandingService.understand() 入口，effectiveAnalysis 一致
  - 编写 ChatServiceAssistPlannerIntegrationTest（8 个 case：planner takeover/category+subCategory/price refinement/low confidence fallback/planner error fallback/legacy complete prefer legacy/plannerUsedForRetrieval flag/selectedSource）
  - 编写 ConversationStatePlannerUpdateTest（5 个 case：category+subCategory save/maxPrice save/multi-round context inheritance/recommendedProductIds/session isolation）
  - 编写 ChatServiceAssistNoRegressionTest（6 个 SpringBootTest：一款跑鞋/几款跑鞋/轻量不给背包/预算 1000/无匹配不发 card/PRODUCT_DETAIL）
  - 编写 RetrievalDebugAndChatConsistencyTest（4 个 SpringBootTest：effectiveAnalysis 输出/planner result/disabled mode selectedSource/effectiveAnalysis non-null）
  - 全部 619 个测试通过（+22）
  - assist 模式仅在 legacy 不完整 + planner 高置信度合法时接管，legacy 完整时优先 legacy
  - StrictProductConstraintFilter 仍是最终硬过滤，product_card 仍由后端 Product 生成
  - 不修改 SSE 协议、不修改 Android 客户端、不绕过硬约束
  - 不调用真实 LLM、不调用真实 Ark API、不连接真实 Qdrant
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/understanding/QueryPlanGatingService.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend Query Understanding Refactor 第四阶段 A：QueryUnderstandingService 生产化 + ChatService Shadow 接入

- **类型**: feat(server)
- **描述**: 将 QueryUnderstandingService 接入 ChatService 主链路，shadow 语义确保不改变现有推荐行为
  - 修改 QueryUnderstandingService.java（新增 understandForRetrieval() 生产入口：空 query/user 安全处理、effectiveAnalysis 永远非 null）
  - 修改 ChatService.java（注入 QueryUnderstandingService，检索路径改为调用 understandForRetrieval → effectiveAnalysis，替代原来 QueryAnalyzer.analyze() 直接调用）
  - ChatService 新增 QueryUnderstanding 日志（query/sessionId/plannerEnabled/plannerMode/selectedSource/plannerUsedForRetrieval/fallbackReason/legacy:[cat/sub/maxPrice]/effective:[cat/sub/maxPrice]）
  - 保留 RetrievalRouter 用于 SMALLTALK/HELP/THANKS 前置判断；保留 StrictProductConstraintFilter 最终硬过滤；保留 product_card 二次校验；不修改 SSE 协议
  - 编写 QueryUnderstandingServiceProductionTest（12 个 case）、ChatServiceQueryUnderstandingShadowTest（7 个）、ChatServiceNoRegressionTest（6 个）、HybridCandidateRetrieverExplicitAnalysisTest（5 个）
  - 全部 597 个测试通过（+30）
  - 默认 planner disabled，shadow mode 下 effectiveAnalysis=legacyAnalysis，plannerUsedForRetrieval=false
  - ChatService 不再直接调用 QueryAnalyzer（保留注入但不调用）
  - 不修改 Android 客户端、不修改 SSE 协议、不绕过 StrictProductConstraintFilter
  - 不调用真实 LLM、不调用真实 Ark API、不连接真实 Qdrant
- **影响范围**: server/src/main/java/com/ecommerce/rag/services/ChatService.java, server/src/main/java/com/ecommerce/rag/rag/understanding/QueryUnderstandingService.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend Query Understanding Refactor 第三阶段：QueryPlan assist/takeover 局部接管真实检索

- **类型**: feat(server)
- **描述**: 让 LLMQueryPlanner 在满足 gating 条件时局部接管真实检索，低置信度/错误/unknown 则 fallback legacy
  - 创建 QueryPlanToAnalysisMapper.java（15 个映射规则：category/subCategory/price/brands/attributes/softKeywords/queryVariants/responseStyle 等，附 filters 构造和 warnings 注入）
  - 创建 QueryPlanGateDecision.java（allowed/selectedSource/reason/fallbackReasons/confidence/valid/hasErrors/mode）
  - 创建 QueryPlanGatingService.java（gating 规则：disabled→不允许, shadow→SHADOW_ONLY, error/parseFail/lowConf/invalid/unknownCategory/unknownSubCategory/UNKNOWN/needsClarification→fallback, assist→legacyComplete→fallback, takeover→通过用planner）
  - 修改 AppProperties.java（PlannerProperties +5 字段：minConfidence/allowTakeoverIntents/fallbackOnWarnings/fallbackOnUnknownCategory/fallbackOnUnknownSubCategory）
  - 修改 application.yml（+5 配置项，全环境变量驱动）
  - 修改 QueryUnderstandingService.java（注入 gatingService+mapper+appProperties，新流程：legacy + planner → gate → effectiveAnalysis，planner takeover 时附加 inheritedFromContext/PageContext/currentProductId/boostedProductIds）
  - 修改 QueryUnderstandingResult.java（+5 字段：gateDecision/effectiveAnalysis/selectedSource/fallbackReason，legacyAnalysis 和 effectiveAnalysis 类型改为 Object 和 QueryAnalysisResult）
  - 修改 RagRetrievalDebugController.java（注入 QueryUnderstandingService，输出 legacy_analysis/planner_result/validated_plan/gate_decision/effective_analysis/selected_source/planner_used_for_retrieval/fallback_reason）
  - 修复 3 个旧 debug 测试（RetrievalDebugConstraintTest/ControllerTest/RewriteTest：query_analysis→effective_analysis）
  - 修复 QueryUnderstandingServiceTest（新构造函数参数）
  - 编写 3 个测试类（QueryPlanToAnalysisMapperTest 14 + QueryPlanGatingServiceTest 15 + QueryUnderstandingServiceTakeoverTest 7 = 36 个新测试）
  - 全部 567 个测试通过（+36）
  - 默认 planner disabled，shadow/takeover/assist 三模式可用，fallback-on-warnings/unknown-category/unknown-sub-category 可配置
  - 不修改 ChatService 主流程、不绕过 StrictProductConstraintFilter、不修改 SSE/Android
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/understanding/**, server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend Query Understanding Refactor 第二阶段补强：planner 输出质量优化 + validator matched flags + 多轮追问测试

- **类型**: feat(server) + fix(server)
- **描述**: 优化 LLMQueryPlanner 输出质量和 QueryPlanValidator 语义
  - 优化 QueryPlannerPromptBuilder：新增 softKeywords 规则（2-6 汉字、不输出泛化短语、含程序员/油皮/送礼具体示例）、queryVariants 规则（应输出 2-3 条、含类目词+软需求）、新增 2 个完整输出示例（程序员电脑 + 一万元以下追问继承）
  - 修复 QueryPlanValidator：新增内部 ValidationState，categoryMatched/subCategoryMatched/priceValid/brandValid 标志正确设置；合法匹配不再进入 warnings，只有真正的未知类目/子类目/品牌/价格异常才进入 warnings
  - 新增 LLMQueryPlannerContextRefinementTest（3 个 case：追问继承 category + maxPrice、matched flags 传递、第一轮不泄漏上下文）
  - 新增 QueryPlanValidatorMatchedFlagTest（9 个 case：合法 matched=true 且不在 warnings、未知 matched=false 且 warnings 存在、null target 不置标志等）
  - 更新 QueryPlannerPromptBuilderTest（+4 个 case：softKeywords 规则、具体示例、queryVariants 规则、上下文追问示例）
  - 全部 531 个测试通过（+16）
  - 不修改 ChatService / HybridCandidateRetriever / QueryAnalyzer / SSE / Android
  - Planner 仍然默认 disabled，shadow mode 不影响真实检索
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/understanding/QueryPlannerPromptBuilder.java, QueryPlanValidator.java; server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend Query Understanding Refactor 第二阶段：LLMQueryPlanner Shadow Mode

- **类型**: feat(server)
- **描述**: 实现 LLMQueryPlanner（shadow mode），将自然语言 → 结构化 QueryPlan；不接管真实检索
  - 创建 QueryPlannerPromptBuilder.java（构造 planner prompt：taxonomy 目录 + conversation_state 上下文 + page_context + QueryPlan schema + 12 条严格规则）
  - 创建 QueryPlanJsonParser.java（从 LLM 输出提取 JSON：支持纯 JSON / ```json fence / 前后说明文字 / 嵌套结构解析）
  - 创建 LLMQueryPlanner.java（编排 prompt→LlmClient→parse→validate 全流程，超时保护，失败不抛500）
  - 创建 QueryPlanningResult.java（结果 POJO：plannerEnabled/mode/rawPlan/validatedPlan/parseSuccess/valid/latencyMs/source，5 种 source：DISABLED/LLM/FALLBACK/ERROR）
  - 创建 QueryUnderstandingResult.java（query + legacyAnalysis + planningResult + plannerUsedForRetrieval）
  - 创建 QueryUnderstandingService.java（编排 legacy QueryAnalyzer + LLMQueryPlanner，shadow compare，plannerUsedForRetrieval=false）
  - 修改 AppProperties.java（新增 UnderstandingProperties + PlannerProperties：enabled/mode/timeoutSeconds/maxTaxonomyItems/includeBrands/cacheEnabled）
  - 修改 application.yml（新增 app.understanding.planner 配置节，6 个环境变量驱动）
  - 扩展 QueryUnderstandingDebugController.java（新增 POST /api/rag/understanding/plan 接口，注入 QueryUnderstandingService）
  - 编写 5 个测试类（QueryPlannerPromptBuilderTest 8 + QueryPlanJsonParserTest 6 + LLMQueryPlannerTest 7 + QueryUnderstandingServiceTest 5 + QueryUnderstandingDebugControllerPlannerTest 5 = 31 个新测试）
  - 全部 515 个测试通过（+31）
  - 默认 planner enabled=false，不调用真实 LLM
  - shadow mode 下不影响 /api/chat/stream 行为
  - 不修改 ChatService / HybridCandidateRetriever / QueryAnalyzer / SSE 协议
  - Planner prompt 明确注入目录 + 规则：只能使用已知 category/subCategory、电脑→笔记本电脑、一万元→10000、严格规则
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/understanding/**, server/src/main/java/com/ecommerce/rag/api/QueryUnderstandingDebugController.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend Query Understanding Refactor 第一阶段：CatalogTaxonomyService + QueryPlan + QueryPlanValidator

- **类型**: feat(server)
- **描述**: 搭建 Query Understanding 基础设施，为后续 LLMQueryPlanner 接入做准备，不接管现有检索链路
  - 创建 CatalogTaxonomySnapshot.java（categories/subCategoriesByCategory/allSubCategories/brands/minPrice/maxPrice/filterableFields/textFields/generatedAt）
  - 创建 CatalogTaxonomyService.java（从 ProductService 动态统计类目/子类目/品牌/价格范围，线程安全读写锁，空商品库返回 empty snapshot）
  - 创建 QueryPlan.java（完整结构化 intent：originalQuery/intent/needsRetrieval/target/price/brands/attributes/softKeywords/queryVariants/answerMode 等 20+ 字段，含 deepCopy 方法）
  - 创建 QueryPlanTarget.java（category/subCategory/subCategories/currentProductId/scopeProductIds/excludeProductIds）
  - 创建 QueryPlanPrice.java（min/max/currency/strict）
  - 创建 QueryPlanBrands.java（include/exclude）
  - 创建 QueryPlanAttributes.java（include/exclude）
  - 创建 QueryPlanValidationResult.java（originalPlan/validatedPlan/valid/warnings/errors/fixedFields/categoryMatched 等）
  - 创建 QueryPlanValidator.java（category/subCategory/price/brands/variants/keywords/needsRetrieval/requestedProductCount 校验与轻量修正）
  - 创建 QueryUnderstandingDebugController.java（GET /api/rag/understanding/taxonomy + POST /api/rag/understanding/validate-plan）
  - 编写 4 个测试类（CatalogTaxonomyServiceTest 7 + QueryPlanSerializationTest 4 + QueryPlanValidatorTest 17 + QueryUnderstandingDebugControllerTest 4 = 32 个新测试）
  - 全部 484 个测试通过（+32）
  - 不修改 Android 客户端、不修改 SSE 协议、不修改 ChatService、不修改 HybridCandidateRetriever
  - 不修改 QueryAnalyzer、不修改 RetrievalRouter、不修改 QueryRewriteService
  - 不调用真实 LLM、不调用真实 Ark API、不连接真实 Qdrant
  - QueryPlan 不接管实际检索链路，不影响现有 retrieval behavior
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/understanding/**, server/src/main/java/com/ecommerce/rag/api/QueryUnderstandingDebugController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend RAG 软语义理解：LLM Query Rewrite / Query Expansion

- **类型**: feat(server)
- **描述**: 实现软语义理解层（LLM Query Rewrite + 词表扩展），增强泛化召回能力，不替代 QueryAnalyzer 的硬约束解析
  - 创建 soft_semantic_lexicon.json（22 条目，覆盖学生党/通勤/敏感肌/送礼/夏天/熬夜等常见软语义映射）
  - 创建 SoftSemanticLexicon.java（Spring @Component，加载 JSON 并支持 lookup(query, maxKeywords)）
  - 创建 QueryRewriteResult.java（POJO + 5 种 factory：none/fromLexicon/fromLlm/hybrid/fallback，字段含 source/queryVariants/softKeywords/inferredScenarios/confidence）
  - 创建 LLMQueryRewriter.java（通过 LlmClient 调用 LLM，结构化 Prompt 输出 JSON，支持超时和错误处理，含 JSON 解析和 fallback）
  - 创建 QueryRewriteValidator.java（限制 variant/keyword 数量、去重、移除空串、移除价格表达、移除 forbidden 前缀、移除 product_id）
  - 创建 QueryRewriteService.java（Spring @Service，支持 4 种 provider：disabled/lexicon/llm/hybrid，含 LRU cache 最大 500 条）
  - 修改 QueryAnalysisResult.java（新增 queryVariants/softKeywords/rewriteResult 字段）
  - 修改 HybridCandidateRetriever.java（集成 QueryRewriteService，vectorOnly/hybrid 使用 retrieveMultiQuery，keywordOnly/hybrid 使用 softKeywords 增强查询）
  - 修改 VectorRetriever.java（新增 retrieveMultiQuery 方法，支持多 query 并行召回并 chunkId 去重）
  - 修改 KeywordRetriever.java（新增 retrieveWithSoftKeywords 方法，拼接扩展词到查询文本）
  - 修改 AppProperties.java（新增 RewriteProperties：enabled/provider/maxVariants/maxSoftKeywords/timeoutSeconds/cacheEnabled）
  - 修改 application.yml（新增 app.rewrite 配置节，6 个环境变量驱动）
  - 修改 RagRetrievalDebugController.java（debug 输出新增 rewrite_result 字段）
  - 修改 rag_eval_queries.json（新增 10 条软语义 eval query，总计 39 条）
  - 编写 6 个测试类（SoftSemanticLexiconTest 8 + LLMQueryRewriterTest 4 + QueryRewriteValidatorTest 6 + QueryRewriteServiceTest 5 + HybridCandidateRetrieverRewriteTest 5 + RetrievalDebugRewriteTest 3 = 31 个新测试）
  - 全部 452 个测试通过（+31）
  - 不修改 Android 客户端、不修改 SSE 协议、不修改 StrictProductConstraintFilter
  - 单元测试不调用真实 LLM（使用 fake/anonymous LlmClient）
  - 单元测试不调用真实 Ark API、不连接真实 Qdrant
  - 默认 enabled=false，默认 provider=lexicon
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/rewrite/**, server/src/main/java/com/ecommerce/rag/rag/query/QueryAnalysisResult.java, server/src/main/java/com/ecommerce/rag/rag/retriever/**, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/main/resources/rag/soft_semantic_lexicon.json, server/src/main/resources/eval/rag_eval_queries.json, server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend RAG 数据增强：接入 REVIEW_SUMMARY / FAQ / MARKETING_COPY Chunk

- **类型**: feat(server)
- **描述**: 将原始商品数据中的 user_reviews、official_faq、marketing_description 接入 RAG 向量检索，增强非结构化商品信息的检索能力
  - 修改 ChunkType.java（新增 REVIEW_SUMMARY / FAQ / MARKETING_COPY 三个枚举值）
  - 修改 Product.java（新增 reviewSummary / faqSummary / marketingCopy 三个可选字段，JSON key 使用 snake_case）
  - 修改 convert_teacher_dataset.py（新增 build_review_summary() 从 user_reviews 脱敏聚合生成 review_summary，剔除 nickname；新增 build_faq_summary() 从 official_faq 拼接生成 faq_summary；marketing_copy 复用 marketing_description；新增转换统计输出）
  - 修改 RagDocumentBuilder.java（新增 buildReviewSummaryChunk / buildFaqChunk / buildMarketingCopyChunk 三个方法，只在对应字段非空时生成 chunk；chunkId 格式：productId::REVIEW_SUMMARY::0 等）
  - 修改 rag_eval_queries.json（新增 9 条评价/FAQ/卖点类 eval query）
  - 更新 RagEvaluationControllerTest.java（total_queries 从固定 20 改为 >=20，向后兼容未来的新增 query）
  - 编写 4 个测试类（ProductModelExtendedFieldsTest 5 + RagDocumentBuilderEnhancedChunkTest 9 + RagDocumentStatsEnhancedChunkTest 5 + RagVectorIndexEnhancedChunkTest 8 = 27 个新测试）
  - 更新 docs/data_schema.md（新增 review_summary/faq_summary/marketing_copy 字段说明）
  - 更新 docs/rag_design.md（新增 REVIEW_SUMMARY/FAQ/MARKETING_COPY chunk 说明，移除"第一版不做"的过时说明）
  - 更新 docs/api_spec.md（stats 示例新增三种 chunk type）
  - 更新 docs/rag_midterm_report.md（Product 字段表 12→15，ChunkType 表 4→7，标记"用户评价/FAQ向量化"为已实现）
  - 全部 421 个测试通过（+27）
  - 不修改 Android 客户端、不修改 SSE 协议、不修改 LLM/Embedding/Qdrant 配置
  - 不调用真实 LLM 生成摘要（仅规则聚合）
  - 不调用真实 Ark API 作为测试
  - 不连接真实 Qdrant 作为测试
  - user_reviews 脱敏：不保留 nickname，只提取 content 文本
- **原始数据字段分析**：
  - user_reviews：存在（array of {nickname, rating, content}），需脱敏 → review_summary
  - official_faq：存在（array of {question, answer}），无敏感信息 → faq_summary
  - marketing_description：存在（string），已用作 description，复用截取版 → marketing_copy
  - marketing_copy / selling_points / highlights / comments / qa：均不存在
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/document/ChunkType.java, RagDocumentBuilder.java; server/src/main/java/com/ecommerce/rag/models/entity/Product.java; scripts/convert_teacher_dataset.py; server/src/main/resources/eval/rag_eval_queries.json; server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend RAG 输出体验优化：LLM 回复风格 + 商品卡片数量控制

- **类型**: feat(server)
- **描述**: 优化 LLM 回复风格和商品卡片数量控制，让回答更适合 Android 悬浮导购面板
  - 创建 RecommendationCountResolver.java（根据 query 判断推荐数量：一款/一个/一双→1，几款/有哪些→3，默认3）
  - 扩展 QueryAnalysisResult.java（新增 requestedProductCount / responseStyle / SINGLE_RECOMMENDATION/MULTI_RECOMMENDATION/CURRENT_PRODUCT_QA/NO_MATCH 常量）
  - 重写 RagPromptBuilder.java（4 套输出模板：SINGLE/MULTI/NO_MATCH/CURRENT_PRODUCT_QA；规则：简短/不输出 product_id/不重复卡片信息/不营销话术）
  - 修改 ChatService.java（集成 RecommendationCountResolver，displayLimit = min(requestedCount, maxCardLimit, request.limit)；Prompt 使用 displayCandidates 构造，product_card 只发送 displayCandidates）
  - 修改 AppProperties.ChatProperties（新增 defaultProductCardLimit=3 / maxProductCardLimit=3）
  - 修改 MockLlmClient.java（精简 Mock 输出为短文本，去除冗长介绍）
  - 编写 4 个测试类（RecommendationCountResolverTest 12 + ChatServiceCandidateLimitTest 6 + RagPromptBuilderOutputStyleTest 8 + MockLlmClientConciseOutputTest 3）
  - 更新 docs/rag_design.md（新增输出体验优化章节）
  - 更新 docs/api_spec.md（说明后端默认最多返回 3 张卡片，明确"一款"时只返回 1 张）
  - 更新 docs/rag_midterm_report.md（第 13 章新增输出体验说明）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/response/**, server/src/main/java/com/ecommerce/rag/rag/prompt/RagPromptBuilder.java, server/src/main/java/com/ecommerce/rag/services/ChatService.java, server/src/main/java/com/ecommerce/rag/rag/llm/MockLlmClient.java, server/src/main/java/com/ecommerce/rag/rag/query/QueryAnalysisResult.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend RAG Bugfix 第二阶段：ChatService 输出一致性 + ConversationState 更新时机 + retrieval debug 增强

- **类型**: fix(server)
- **描述**: 修复 ChatService 输出一致性问题，确保 Prompt candidates 与 product_card candidates 使用同一批 finalCandidates，product_card 发送前执行二次校验，ConversationState 检索完成后立即更新
  - 修改 ChatService.java：注入 StrictProductConstraintFilter，retrieveWithAnalysis 之后立即执行二次约束校验生成 finalCandidates，检索完成后立即更新 ConversationState（不等 LLM 完成），sendProductCards 前再次调用 constraintFilter.passes() 校验每一个 product_card，finalCandidates 为空时不发送 product_card
  - 修改 InMemoryConversationMemoryService.java：新增 negativeKeywords 保存，recommendedProductIds 改为 set（不再 addAll 累积），只保存实际发送的商品 ID
  - 修改 RagRetrievalDebugController.java：注入 StrictProductConstraintFilter，新增 raw_candidate_count / final_candidate_count / final_candidates / filtered_out_candidates 四个输出字段，filtered_out_candidates 含 productId/name/category/subCategory/price/failedRules
  - 编写 3 个测试类（ChatServiceProductCardConstraintTest 8 个 + ConversationStateUpdateTest 7 个 + RetrievalDebugConstraintTest 5 个）
  - 更新 docs/rag_design.md（新增 ChatService 输出一致性 + 二次校验 + 空结果处理说明）
  - 更新 docs/api_spec.md（debug 接口新增 filtered_out_candidates 字段说明）
  - 更新 docs/rag_midterm_report.md（第 13 章新增一致性保证、空结果处理、二次校验说明）
- **影响范围**: server/src/main/java/com/ecommerce/rag/services/ChatService.java, server/src/main/java/com/ecommerce/rag/rag/memory/InMemoryConversationMemoryService.java, server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-02] Backend RAG Bugfix 第一阶段：修复检索约束核心

- **类型**: fix(server)
- **描述**: 修复 QueryAnalyzer 价格解析、多轮 refinement normalizedQuery，新增统一 StrictProductConstraintFilter，消除 HybridCandidateRetriever 与 KeywordRetriever 间过滤行为不一致
  - 修复 QueryAnalyzer 价格解析：支持"1000以内""预算1000以内""不超过1000""低于1000""500~1000元"等 13 种表达，价格短语从 normalizedQuery 中移除
  - 修复多轮 refinement normalizedQuery：继承上下文后 normalizedQuery 包含上下文 subCategory（如"要轻量的"→"跑步鞋 轻量"）
  - 新增 ConstraintCheckResult.java（passed/passedRules/failedRules 现场记录）
  - 新增 StrictProductConstraintFilter.java（统一硬约束过滤：category/subCategory/price/negativeBrands/negativeKeywords/excludeProductIds），引用 CategoryMatchService 做 alias 匹配
  - 修改 HybridCandidateRetriever.java：移除内联 applyHardFilter/applyTextBasedFilter/applyExcludeProductIds，委托 StrictProductConstraintFilter.filterCandidates()
  - 修改 KeywordRetriever.java：移除内联 applyHardFilter/passesHardFilter，委托 StrictProductConstraintFilter.passes()
  - CandidateFusionService 保持不变（仅负责融合排序，无硬过滤逻辑）
  - 编写 4 个测试类（QueryAnalyzerPriceParsingTest 15 个 + ContextualQueryAnalyzerRefinementTest 4 个 + StrictProductConstraintFilterTest 11 个 + HybridCandidateRetrieverConstraintTest 4 个）
  - 更新 docs/rag_design.md（新增 StrictProductConstraintFilter 章节，更新价格解析说明）
  - 更新 docs/api_spec.md（无 API 变更，仅说明约束过滤统一化）
  - 更新 docs/rag_midterm_report.md（报告第 10 章更新，StrictProductConstraintFilter 现为独立类）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/retriever/StrictProductConstraintFilter.java, ConstraintCheckResult.java; server/src/main/java/com/ecommerce/rag/rag/query/QueryAnalyzer.java; server/src/main/java/com/ecommerce/rag/rag/retriever/HybridCandidateRetriever.java, KeywordRetriever.java; server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-06-01] RAG 中期开发报告

- **类型**: docs
- **描述**: 生成 RAG 中期开发报告 docs/rag_midterm_report.md，系统梳理当前项目中检索全链路操作、关键数据结构、接口、配置、评估流程、能力边界和已知问题。报告共 19 章，覆盖商品数据、Chunk 切分、Embedding/Qdrant、QueryAnalyzer、Hybrid Retrieval、硬约束过滤、PageContext、多轮记忆、LLM Prompt/SSE、API、配置项、评估、已知问题及后续优化建议。
  - 发现文档与代码不一致项：docs/rag_design.md 中"当前状态（第二阶段）"仍标注 Qdrant/真实Embedding 为"❌ 不接入"，实际已实现；docs/data_schema.md 中 ChatRequest 字段不完整（缺少 limit/pageContext）
- **影响范围**: docs/rag_midterm_report.md, .trae/memory/changelog.md
- **关联决策**: D-004, D-007

---

## [2026-06-01] Backend RAG 第七阶段：PageContext-aware Agent

- **类型**: feat(server)
- **描述**: 让 /api/chat/stream 支持前端传入的 page_context，在 RAG 检索和 LLM 回答中使用当前页面上下文
  - 创建 PageType.java 枚举（PRODUCT_LIST / PRODUCT_DETAIL / CHAT / UNKNOWN，@JsonCreator 兜底未知值不报 500）
  - 创建 PageContext.java DTO（6 字段 snake_case JSON 映射，可选字段，默认空值）
  - 修改 ChatRequest.java（新增 page_context 字段，旧请求兼容）
  - 创建 PageContextResolution.java（解析结果：currentProduct/visibleProducts/recentlyViewedProducts/warnings）
  - 创建 PageContextResolver.java（根据 ProductService 解析 currentProductId/visibleProductIds/recentlyViewedProductIds，查不到记录 warning 不中断）
  - 修改 QueryAnalysisResult.java（新增 currentProductId/scopeProductIds/boostedProductIds/inheritedFromPageContext/pageSearchQuery/pageFilters/pageWarnings）
  - 修改 QueryAnalyzer.java（新增 analyze(String, ConversationState, PageContextResolution) 方法，PRODUCT_DETAIL 识别"这个/这款/它"引用当前商品、替代/更便宜逻辑，PRODUCT_LIST 合并 searchQuery/filters/boostedProductIds）
  - 修改 HybridCandidateRetriever.java（applyBoostedProductIds +0.05 分 boost，page filter fallback 逻辑）
  - 修改 RagPromptBuilder.java（build() 新增 page_context 参数，PRODUCT_DETAIL 注入当前商品信息，PRODUCT_LIST 注入列表上下文）
  - 修改 ChatService.java（集成 PageContextResolver，当前商品问答模式跳过 product_card 发送）
  - 修改 RagRetrievalDebugController.java（输出 page_context_resolution/inherited_from_page_context/scope_product_ids/boosted_product_ids/page_warnings）
  - 编写 5 个测试类（ChatRequestPageContextTest 6 个 + PageContextResolverTest 8 个 + QueryAnalyzerPageContextTest 10 个 + HybridCandidateRetrieverPageContextTest 4 个 + ChatServicePageContextTest 6 个）
  - 更新 docs/api_spec.md（PageContext 字段说明、PRODUCT_DETAIL/PRODUCT_LIST 示例、旧请求兼容说明、retrieval debug 新字段）
  - 更新 docs/rag_design.md（新增 PageContext-aware RAG 章节）
- **影响范围**: server/src/main/java/com/ecommerce/rag/models/dto/PageType.java, PageContext.java, ChatRequest.java; server/src/main/java/com/ecommerce/rag/rag/context/PageContextResolver.java, PageContextResolution.java; server/src/main/java/com/ecommerce/rag/rag/query/QueryAnalysisResult.java, QueryAnalyzer.java; server/src/main/java/com/ecommerce/rag/rag/retriever/HybridCandidateRetriever.java; server/src/main/java/com/ecommerce/rag/rag/prompt/RagPromptBuilder.java; server/src/main/java/com/ecommerce/rag/services/ChatService.java; server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java; server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-05-27] Backend RAG 第六阶段：RetrievalRouter + ConversationState + Contextual QueryAnalyzer + Negative Constraints

- **类型**: feat(server)
- **描述**: 增强 Agent 的对话理解能力，实现意图路由、多轮上下文记忆、追问继承、负约束过滤
  - 创建 RetrievalIntent.java / RetrievalRouteResult.java / RetrievalRouter.java（规则引擎，9 种 intent，SMALLTALK/HELP/THANKS 不检索）
  - 创建 ConversationState.java（15 字段会话状态模型，含 category/subCategory/price/recommendedProductIds）
  - 创建 ConversationMemoryService.java 接口 + InMemoryConversationMemoryService.java（ConcurrentHashMap 实现，支持 getOrCreate/updateAfterRetrieval/clearSession）
  - 修改 QueryAnalysisResult.java（新增 sessionId/resolvedQuery/inheritedFromContext/excludeProductIds/excludeBrands/avoidIngredientsOrTerms/intent 字段）
  - 修改 QueryAnalyzer.java（新增 analyze(String, ConversationState) 方法，支持上下文继承：类目/子类目/价格/正负关键词/排除推荐商品/日系品牌/酒精过滤/学生党）
  - 修改 HybridCandidateRetriever.java（新增 retrieveWithAnalysis/retrieveRawWithAnalysis/applyTextBasedFilter/applyExcludeProductIds 方法，支持负关键词文本过滤和排除商品 ID 过滤）
  - 修改 ChatService.java（集成 RetrievalRouter + ConversationMemoryService + Contextual QueryAnalyzer；SMALLTALK/HELP/THANKS 不检索直接返回文本；检索完成后自动更新 ConversationState）
  - 修改 RagRetrievalDebugController.java（新增 intent/session_id/inherited_from_context/resolved_query/exclude_product_ids/negative_keywords/memory_before/memory_after 输出字段）
  - 编写 5 个测试类（RetrievalRouterTest 17 个 + ConversationMemoryServiceTest 6 个 + ContextualQueryAnalyzerTest 11 个 + HybridCandidateRetrieverContextTest 4 个 + ChatServiceContextTest 7 个）
  - 更新 docs/rag_design.md（新增多轮对话上下文管理章节，含 RetrievalRouter/ConversationState/Contextual QueryAnalyzer/Negative Constraints 说明）
  - 更新 docs/api_spec.md（session_id 多轮用途说明、多轮对话示例、retrieval debug 新字段）
  - 更新 docs/rag_eval_report_template.md（备注增加当前能力说明）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/router/**, server/src/main/java/com/ecommerce/rag/rag/memory/**, server/src/main/java/com/ecommerce/rag/rag/query/**, server/src/main/java/com/ecommerce/rag/rag/retriever/**, server/src/main/java/com/ecommerce/rag/services/ChatService.java, server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-05-26] Backend RAG 第五阶段修正版：ArkMultimodalEmbeddingProvider 适配火山 Ark 多模态向量接口
- **类型**: feat(server)
- **描述**: 新增 ArkMultimodalEmbeddingProvider，适配火山 Ark /embeddings/multimodal 接口
  - 创建 ArkMultimodalEmbeddingProvider.java（java.net.http.HttpClient + /embeddings/multimodal，input 为对象数组 [{type:"text",text:"..."}] 而非字符串数组）
  - 修改 RagConfig.java（EmbeddingProvider Bean 新增 ark-multimodal case，需要 baseUrl/apiKey/model，路径可配置）
  - 更新 AppProperties.EmbeddingProperties（新增 arkMultimodalPath 字段）
  - 更新 application.yml（新增 ark-multimodal-path 配置，环境变量 ARK_MULTIMODAL_EMBEDDING_PATH）
  - 编写 13 个新测试（ArkMultimodalEmbeddingProviderTest，mock HttpServer 验证请求路径、body 格式、维度检查、错误码等）
  - 全部 201 个测试通过（含之前 188 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/embedding/ArkMultimodalEmbeddingProvider.java, server/src/main/java/com/ecommerce/rag/core/config/RagConfig.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007, D-008

---

## [2026-05-26] Backend RAG 第五阶段：接入真实 EmbeddingProvider
- **类型**: feat(server)
- **描述**: 新增 OpenAIStyleEmbeddingProvider，支持通过环境变量切换 mock/openai-style embedding，保留 MockEmbeddingProvider
  - 创建 OpenAIStyleEmbeddingProvider.java（java.net.http.HttpClient 调用 /v1/embeddings，batch 拆批，维度/401/429/5xx 错误处理，不泄露 API Key）
  - 修改 RagConfig.java（EmbeddingProvider Bean：mock → MockEmbeddingProvider，openai-style → OpenAIStyleEmbeddingProvider，非法值报错）
  - 修改 RagVectorIndexService.java（rebuildIndex 前检查 embedding dimension 与 QDRANT_VECTOR_SIZE 是否一致）
  - 更新 AppProperties.EmbeddingProperties（新增 baseUrl/apiKey/model/dimension/timeoutSeconds/batchSize）
  - 更新 application.yml（新增 embedding 配置节，所有敏感值通过环境变量读取）
  - 编写 10 个新测试（OpenAIStyleEmbeddingProviderTest，使用 com.sun.net.httpserver.HttpServer mock API）
  - 修复 docs/api_spec.md 中错误的 /api/rag/vector-search → /api/rag/vector-index/search 路径
  - 更新 docs/rag_design.md、docs/api_spec.md
  - 全部 188 个测试通过（含之前 178 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/embedding/OpenAIStyleEmbeddingProvider.java, server/src/main/java/com/ecommerce/rag/core/config/RagConfig.java, server/src/main/java/com/ecommerce/rag/rag/vector/RagVectorIndexService.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007, D-008

---

## [2026-05-26] Backend RAG 第四阶段：接入真实 QdrantVectorStoreService
- **类型**: feat(server)
- **描述**: 新增 QdrantVectorStoreService 实现 VectorStoreService，通过 Qdrant REST API 进行向量存储和检索
  - 创建 QdrantVectorStoreService.java（Java HttpClient + Qdrant REST API，upsert/search/count/clear，批量 upsert 每批 500）
  - 创建 QdrantCollectionManager.java（collection 存在检查、自动创建、recreate、count points）
  - 创建 QdrantFilterBuilder.java（category/sub_category/brand/product_id/chunk_type/min_price/max_price → Qdrant filter JSON）
  - 修改 RagConfig.java（Bean switch：VECTOR_STORE=in-memory → InMemoryVectorStoreService，VECTOR_STORE=qdrant → QdrantVectorStoreService）
  - 更新 AppProperties.java（新增 QdrantProperties：url/apiKey/collectionName/vectorSize/distance/recreateOnStart/timeoutSeconds）
  - 更新 application.yml（新增 app.vector.qdrant 配置节，新增 VECTOR_STORE 环境变量，默认 in-memory）
  - 编写 16 个新测试（QdrantFilterBuilderTest 8 + QdrantPointMappingTest 4 + VectorStoreConfigTest 3 + 1），全部通过
  - 更新 docs/rag_design.md、docs/api_spec.md
  - 全部 178 个测试通过（含之前 162 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/vector/QdrantVectorStoreService.java, server/src/main/java/com/ecommerce/rag/rag/vector/QdrantCollectionManager.java, server/src/main/java/com/ecommerce/rag/rag/vector/QdrantFilterBuilder.java, server/src/main/java/com/ecommerce/rag/core/config/RagConfig.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-05-26] Backend RAG 第三阶段：VectorRetriever + Hybrid Retrieval 接入 ChatService
- **类型**: feat(server)
- **描述**: 将 in-memory 向量检索接入 RAG 候选召回链路，形成 hybrid retrieval（向量 + 关键词融合）
  - 创建 RetrievedProductCandidate.java（候选商品级对象：productId/product/vectorScore/keywordScore/finalScore/matchedChunks/matchedSources）
  - 创建 VectorRetriever.java（封装 RagVectorIndexService，vector store 为空时返回空列表不抛异常）
  - 创建 KeywordRetriever.java（轻量封装 ProductService.search()，返回 List\<Product\>）
  - 创建 CandidateFusionService.java（规则融合：vectorScore 0.65 + keywordScore 0.35，多 chunk bonus +0.1，按 finalScore 降序）
  - 创建 HybridCandidateRetriever.java（支持 keyword/vector/hybrid 三种模式，hybrid 模式下 vector store 为空自动 fallback keyword）
  - 修改 ChatService.java（CandidateProductRetriever → HybridCandidateRetriever，SSE 格式不变）
  - 创建 RagRetrievalDebugController.java（GET /api/rag/retrieval/debug?query=&limit=）
  - 更新 AppProperties.java（新增 RetrievalProperties：mode/vectorEnabled/keywordEnabled/autoFallbackToKeyword/defaultCandidateLimit）
  - 更新 application.yml（新增 app.retrieval 配置节）
  - 编写 21 个新测试（CandidateFusionServiceTest 7 + HybridCandidateRetrieverTest 5 + KeywordRetrieverTest 5 + VectorRetrieverTest 4），全部通过
  - 更新 docs/rag_design.md、docs/api_spec.md
  - 全部 162 个测试通过（含之前 141 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/retriever/**, server/src/main/java/com/ecommerce/rag/services/ChatService.java, server/src/main/java/com/ecommerce/rag/core/config/AppProperties.java, server/src/main/resources/application.yml, server/src/main/java/com/ecommerce/rag/api/RagRetrievalDebugController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-05-25] Backend RAG 第二阶段：确定性 Chunk ID + EmbeddingProvider + VectorStoreService 抽象
- **类型**: feat(server)
- **描述**: 在 Parent-Child Chunk 基础上完成 Embedding 和向量存储抽象层，不接入真实 Qdrant
  - 创建 RagChunkIdGenerator.java（确定性 chunkId：`{productId}::{chunkType}::{index}`，确定性 vectorPointId：UUID.nameUUIDFromBytes）
  - 修改 RagChunkDocument.java（新增 vectorPointId 字段，移除随机 UUID chunkId 生成）
  - 修改 RagDocumentBuilder.java（使用 RagChunkIdGenerator 生成 chunkId 和 vectorPointId）
  - 创建 EmbeddedRagChunk.java（chunk + embedding vector + payload 模型）
  - 创建 EmbeddingProvider.java 接口（embed / embedBatch / dimension / modelName）
  - 创建 MockEmbeddingProvider.java（文本 hash → 确定性伪向量，L2 归一化，维度可配置默认 64）
  - 创建 VectorSearchRequest.java / VectorSearchHit.java 检索模型
  - 创建 VectorStoreService.java 接口（upsert / search / count / clear）
  - 创建 InMemoryVectorStoreService.java（ConcurrentHashMap + cosine similarity + 结构化过滤）
  - 创建 RagVectorIndexService.java（编排：documents → embed → upsert → search）
  - 创建 RagVectorController.java（POST /rebuild, GET /stats, POST /search）
  - 创建 RagConfig.java（EmbeddingProvider + VectorStoreService Bean 配置）
  - 更新 AppProperties.java（新增 EmbeddingProperties）
  - 更新 application.yml（新增 app.embedding 配置节）
  - 编写 53 个新测试（RagChunkIdGeneratorTest 15 + MockEmbeddingProviderTest 13 + InMemoryVectorStoreServiceTest 12 + RagVectorIndexServiceTest 5 + RagVectorControllerTest 5 + 更新 RagDocumentBuilderTest +3），全部通过
  - 更新 docs/rag_design.md、docs/api_spec.md
  - 全部 141 个测试通过（含之前 88 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/document/**, server/src/main/java/com/ecommerce/rag/rag/embedding/**, server/src/main/java/com/ecommerce/rag/rag/vector/**, server/src/main/java/com/ecommerce/rag/api/RagVectorController.java, server/src/main/java/com/ecommerce/rag/core/config/**, server/src/main/resources/application.yml, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

## [2026-05-25] Backend RAG 第一阶段：RAG 索引文档设计与 Parent-Child Chunk 生成器
- **类型**: feat(server)
- **描述**: 基于已有 Product 数据，设计并实现 RAG 索引文档层（Parent-Child Chunk），不接入 Qdrant，不调用 Embedding API
  - 创建 RagChunkDocument.java（17 字段数据模型，chunkId/parentId/productId/chunkType/text/sourceField/name/brand/category/subCategory/price/currency/avgRating/imageUrl/metadata）
  - 创建 ChunkType.java 枚举（PRODUCT_PROFILE / DESCRIPTION / SPECS / SEARCH_SUMMARY）
  - 创建 RagDocumentBuilder.java（Product → List\<RagChunkDocument\>，4 种 chunk 生成逻辑，800 字截断，specs 为空跳过）
  - 创建 RagDocumentService.java（依赖 ProductService + RagDocumentBuilder，buildAllChunks / buildChunksByProductId / countByChunkType）
  - 创建 RagDocumentController.java（GET /api/rag/chunks/preview, /api/rag/chunks/product/{productId}, /api/rag/chunks/stats）
  - 编写 31 个新测试（RagDocumentBuilderTest 18 + RagDocumentServiceTest 5 + RagDocumentControllerTest 8），全部通过
  - 更新 docs/rag_design.md（新增 Parent-Child RAG 设计章节）
  - 更新 docs/api_spec.md（新增 3 个 RAG Chunk 预览 API）
  - 全部 88 个测试通过（含之前 57 个）
- **影响范围**: server/src/main/java/com/ecommerce/rag/rag/document/**, server/src/main/java/com/ecommerce/rag/api/RagDocumentController.java, server/src/test/**, docs/**
- **关联决策**: D-004, D-007

---

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
