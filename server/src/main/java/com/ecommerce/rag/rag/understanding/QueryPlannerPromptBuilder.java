package com.ecommerce.rag.rag.understanding;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.models.entity.Product;

@Component
public class QueryPlannerPromptBuilder {

    private final AppProperties appProperties;
    private final CartSemanticFrameCatalog cartFrameCatalog;
    private final CartSemanticFrameMatcher cartFrameMatcher;

    public QueryPlannerPromptBuilder(AppProperties appProperties,
                                      CartSemanticFrameCatalog cartFrameCatalog,
                                      CartSemanticFrameMatcher cartFrameMatcher) {
        this.appProperties = appProperties;
        this.cartFrameCatalog = cartFrameCatalog;
        this.cartFrameMatcher = cartFrameMatcher;
    }

    public String build(String query, CatalogTaxonomySnapshot taxonomy,
                         ConversationState conversationState,
                         PageContextResolution pageContext) {

        int maxTaxonomyItems = appProperties.getUnderstanding() != null
                && appProperties.getUnderstanding().getPlanner() != null
                ? appProperties.getUnderstanding().getPlanner().getMaxTaxonomyItems() : 80;
        boolean includeBrands = appProperties.getUnderstanding() != null
                && appProperties.getUnderstanding().getPlanner() != null
                && appProperties.getUnderstanding().getPlanner().isIncludeBrands();

        StringBuilder sb = new StringBuilder();
        sb.append("你是电商导购Query Planner。你只负责把用户自然语言转成结构化QueryPlan JSON，不直接推荐商品。\n\n");

        sb.append(buildTaxonomySection(taxonomy, maxTaxonomyItems, includeBrands));
        sb.append("\n");

        sb.append(buildContextSection(conversationState, pageContext));
        sb.append("\n");

        sb.append(buildSchemaSection());
        sb.append("\n");

        sb.append(buildRulesSection());
        sb.append("\n");

        sb.append(buildCartSemanticFramesSection());
        sb.append("\n");

        sb.append(buildCartSemanticMatchSection(query, pageContext, conversationState));
        sb.append("\n");

        sb.append(buildCartRulesSection());
        sb.append("\n");

        sb.append(buildExamplesSection());
        sb.append("\n");

        sb.append("用户查询：").append(query).append("\n\n");

        sb.append("只输出QueryPlan JSON，不要其他解释、文字、markdown标记。直接输出纯JSON。");

        return sb.toString();
    }

    private String buildTaxonomySection(CatalogTaxonomySnapshot taxonomy, int maxItems, boolean includeBrands) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 当前商品目录\n\n");

        if (taxonomy == null || taxonomy.isEmpty()) {
            sb.append("（商品目录为空）\n");
            return sb.toString();
        }

        sb.append("可用类目及子类目：\n");
        int count = 0;
        for (Map.Entry<String, List<String>> entry : taxonomy.getSubCategoriesByCategory().entrySet()) {
            if (count >= maxItems) break;
            sb.append("- ").append(entry.getKey()).append("：");
            sb.append(String.join("、", entry.getValue()));
            sb.append("\n");
            count++;
        }

        if (taxonomy.getMinPrice() != null && taxonomy.getMaxPrice() != null) {
            sb.append("\n价格范围：").append(taxonomy.getMinPrice())
                    .append(" ~ ").append(taxonomy.getMaxPrice()).append(" CNY\n");
        }

        sb.append("\n可过滤字段：");
        sb.append(String.join("、", taxonomy.getFilterableFields()));
        sb.append("\n");

        sb.append("可检索文本字段：");
        sb.append(String.join("、", taxonomy.getTextFields()));
        sb.append("\n");

        if (includeBrands && taxonomy.getBrands() != null && !taxonomy.getBrands().isEmpty()) {
            List<String> brands = taxonomy.getBrands();
            int brandLimit = Math.min(brands.size(), 30);
            sb.append("\n品牌：");
            sb.append(String.join("、", brands.subList(0, brandLimit)));
            if (brands.size() > brandLimit) {
                sb.append("（共").append(brands.size()).append("个品牌）");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildContextSection(ConversationState state, PageContextResolution pageContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 对话上下文\n\n");

        if (state != null && state.getTurnCount() > 0) {
            sb.append("上一轮查询：").append(state.getLastUserQuery() != null ? state.getLastUserQuery() : "(未知)").append("\n");
            if (state.getCategory() != null && !state.getCategory().isBlank()) {
                sb.append("上一轮类目：").append(state.getCategory()).append(" / ").append(state.getSubCategory()).append("\n");
            }
            if (state.getMaxPrice() != null) {
                sb.append("上一轮最高价格：").append(state.getMaxPrice()).append(" CNY\n");
            }
            if (state.getPositiveKeywords() != null && !state.getPositiveKeywords().isEmpty()) {
                sb.append("上一轮正向关键词：").append(String.join("、", state.getPositiveKeywords())).append("\n");
            }
            if (state.getNegativeBrands() != null && !state.getNegativeBrands().isEmpty()) {
                sb.append("上一轮排除品牌：").append(String.join("、", state.getNegativeBrands())).append("\n");
            }
            if (state.getRecommendedProductIds() != null && !state.getRecommendedProductIds().isEmpty()) {
                sb.append("上一轮已推荐商品ID：").append(String.join("、", state.getRecommendedProductIds())).append("\n");
            }
            sb.append("对话轮次：").append(state.getTurnCount()).append("\n");
        } else {
            sb.append("（无上下文，这是第一轮对话）\n");
        }

        if (pageContext != null && pageContext.hasPageContext()) {
            sb.append("\n当前页面上下文：\n");
            sb.append("页面类型：").append(pageContext.getPageType().name()).append("\n");
            if (pageContext.isHasValidCurrentProduct() && pageContext.getCurrentProduct() != null) {
                Product cp = pageContext.getCurrentProduct();
                sb.append("当前商品：").append(cp.getName())
                        .append(" (").append(cp.getProductId()).append(")")
                        .append(", 品牌：").append(cp.getBrand())
                        .append(", 类目：").append(cp.getCategory()).append(" / ").append(cp.getSubCategory())
                        .append(", 价格：").append(cp.getPrice()).append(" CNY\n");
            }
            if (pageContext.getPageSearchQuery() != null && !pageContext.getPageSearchQuery().isBlank()) {
                sb.append("列表页搜索词：").append(pageContext.getPageSearchQuery()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildSchemaSection() {
        return """
                ## QueryPlan JSON Schema
                
                输出格式必须是以下JSON结构，所有字段可选：
                
                {
                  "originalQuery": "用户原始查询文本",
                  "normalizedQuery": "规范化后的查询",
                  "intent": "SMALLTALK|HELP|THANKS|PRODUCT_SEARCH|REFINE_PREVIOUS_QUERY|NEGATIVE_CONSTRAINT|CHANGE_OR_MORE|CURRENT_PRODUCT_QA|COMPARE_PRODUCTS|CART_SUMMARY|CART_TOP_UP|CART_COMPLETION_RECOMMEND|UNKNOWN",
                  "needsRetrieval": true/false,
                  "contextAction": "NONE|NEW_SEARCH|REFINE_PREVIOUS_SEARCH|REPLACE_PREVIOUS_SEARCH|EXCLUDE_FROM_PREVIOUS|CURRENT_PRODUCT_REFERENCE|READ_CART|READ_CART_AND_RECOMMEND|ASK_CLARIFICATION",
                  "target": {
                    "category": "从目录中选择的类目名，不确定填null",
                    "subCategory": "从目录中选择的子类目名，不确定填null",
                    "subCategories": [],
                    "currentProductId": null,
                    "scopeProductIds": [],
                    "excludeProductIds": []
                  },
                  "price": {
                    "min": null,
                    "max": null,
                    "currency": "CNY",
                    "strict": true
                  },
                  "brands": {
                    "include": [],
                    "exclude": []
                  },
                  "attributes": {
                    "include": [],
                    "exclude": []
                  },
                  "softKeywords": [],
                  "queryVariants": [],
                  "requestedProductCount": 3,
                  "answerMode": "MULTI_RECOMMENDATION|SINGLE_RECOMMENDATION|CART_QA|CART_RECOMMENDATION",
                  "needsClarification": false,
                  "confidence": 0.0,
                  "warnings": [],
                  "source": "LLM",
                  "cart": {
                    "action": "CART_SUMMARY|AMOUNT_GAP_QUERY|COMPLETION_RECOMMEND|ADD_TO_CART|REMOVE_FROM_CART",
                    "targetAmount": null,
                    "currency": "CNY",
                    "needsCart": true/false,
                    "needsRecommendation": true/false,
                    "referencedProductIds": [],
                    "productReference": null
                  }
                }
                """;
    }

    private String buildRulesSection() {
        return """
                ## 严格规则
                
                1. 只能使用商品目录中存在的 category / subCategory。
                2. 不确定 category / subCategory 时填 null，不要编造。
                3. 用户说"电脑/笔记本/开发电脑/办公电脑"时，如果目录中有"笔记本电脑"，应映射到"数码电子/笔记本电脑"。
                4. 用户说"一万元以下/1万以内/一万以内/预算一万"时，price.max=10000。
                5. 用户说"几千块/不太贵/便宜点"但没有明确数字时，不要编造 maxPrice，只放入 softKeywords。
                6. 用户说"不要/除了/排除"时，应填 brands.exclude 或 attributes.exclude。
                7. 用户说"这个/这款/它"且 page_context 有 currentProductId 时，应使用 CURRENT_PRODUCT_REFERENCE。
                8. 用户只补充价格/颜色/轻量/便宜点/除了某品牌时，若 conversation_state 有上一轮 category/subCategory，应使用 REFINE_PREVIOUS_QUERY 和 REFINE_PREVIOUS_SEARCH。
                9. 不要输出 product_id，除非来自 page_context.currentProductId 或 conversation_state.recommendedProductIds。
                10. 不要输出候选商品列表。
                11. 不要输出解释文字。
                12. 所有价格单位都是 CNY，currency 始终填 "CNY"。
                13. confidence 根据你对类目/意图判断的确定性设定：确定 0.9+，推测 0.7-0.9，不确定 0.5-0.7。
                
                ## softKeywords 规则
                
                14. softKeywords 必须是短关键词（建议 2-6 个汉字），不要输出"适合xxx使用"这种泛化短语。
                15. 从用户表达的「职业/身份、使用场景、核心需求、关键参数」中提取具体关键词。
                16. 例如"适合程序员的电脑" → softKeywords 应为 ["程序员","编程","开发","高性能","大内存","多任务","SSD","续航","屏幕","键盘"]
                17. 例如"油皮用洗面奶" → softKeywords 应为 ["控油","清爽","深层清洁","泡沫","氨基酸"]
                18. 例如"送礼用的食品" → softKeywords 应为 ["礼盒","送礼","节日","高档","精致包装"]
                19. softKeywords 总数不超过 10 个，优先保留最核心的需求词。
                
                ## queryVariants 规则
                
                20. queryVariants 应尽量输出 2-3 条，包含类目词和核心软需求。
                21. 变体格式建议："[场景/需求] + [类目词]"。
                22. queryVariants 不要重复用户原始查询，应重新组织表达方式。
                """;
    }

    private String buildCartSemanticFramesSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 可用购物车语义框架 (Available Cart Semantic Frames)\n\n");
        sb.append("你只能在以下预定义语义框架中选择。不允许发明新的框架。\n\n");

        List<CartSemanticFrame> frames = cartFrameCatalog.getAllFrames();
        for (CartSemanticFrame frame : frames) {
            sb.append("### frame: ").append(frame.getFrameId()).append("\n");
            sb.append("- 含义: ").append(frame.getDescription()).append("\n");
            sb.append("- intent: ").append(frame.getIntent()).append("\n");
            sb.append("- cart.action: ").append(frame.getCartAction()).append("\n");
            if (frame.getRequiredSlots() != null && !frame.getRequiredSlots().isEmpty()) {
                sb.append("- 必要槽位: ").append(String.join(", ", frame.getRequiredSlots())).append("\n");
            } else {
                sb.append("- 必要槽位: (无)\n");
            }
            if (frame.getPositiveExamples() != null && !frame.getPositiveExamples().isEmpty()) {
                sb.append("- 正例: ").append(String.join(", ", frame.getPositiveExamples())).append("\n");
            }
            if (frame.getNegativeExamples() != null && !frame.getNegativeExamples().isEmpty()) {
                sb.append("- 反例: ").append(String.join(", ", frame.getNegativeExamples())).append("\n");
            }

            if ("cart.amount_gap_query".equals(frame.getFrameId())) {
                sb.append("- 等价表达归一化: \"离2000还差多少\" / \"还差多少到两千\" / \"差多少到2000\" / \"距离2000还差多少\" — 都应归一化为 frameId=cart.amount_gap_query, intent=CART_TOP_UP, cart.action=AMOUNT_GAP_QUERY, targetAmount=2000, needsRecommendation=false\n");
            }

            sb.append("- QueryPlan 模板: ");
            if ("cart.summary".equals(frame.getFrameId())) {
                sb.append("{\"intent\":\"CART_SUMMARY\",\"cart\":{\"action\":\"CART_SUMMARY\",\"needsCart\":true,\"needsRecommendation\":false}}\n");
            } else if ("cart.amount_gap_query".equals(frame.getFrameId())) {
                sb.append("{\"intent\":\"CART_TOP_UP\",\"cart\":{\"action\":\"AMOUNT_GAP_QUERY\",\"targetAmount\":2000,\"needsCart\":true,\"needsRecommendation\":false}}\n");
            } else if ("cart.completion_recommend".equals(frame.getFrameId())) {
                sb.append("{\"intent\":\"CART_TOP_UP\",\"cart\":{\"action\":\"COMPLETION_RECOMMEND\",\"targetAmount\":1000,\"needsCart\":true,\"needsRecommendation\":true}}\n");
            } else if ("cart.completion_clarify".equals(frame.getFrameId())) {
                sb.append("{\"intent\":\"CART_TOP_UP\",\"needsClarification\":true,\"cart\":{\"action\":\"COMPLETION_RECOMMEND\",\"targetAmount\":null,\"needsCart\":true,\"needsRecommendation\":true}}\n");
            } else if ("cart.add_item".equals(frame.getFrameId())) {
                sb.append("{\"intent\":\"根据上下文确定\",\"cart\":{\"action\":\"ADD_TO_CART\",\"needsCart\":true}}\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildCartSemanticMatchSection(String query, PageContextResolution pageContext,
                                                  ConversationState state) {
        CartSemanticMatchResult result = cartFrameMatcher.match(query, null, state);

        StringBuilder sb = new StringBuilder();
        sb.append("## 购物车语义匹配结果 (Cart Semantic Match Result)\n\n");
        sb.append("- 是否购物车相关: ").append(result.isCartRelated()).append("\n");
        sb.append("- 匹配级别: ").append(result.getMatchLevel()).append("\n");

        if (result.getMatchedFrameId() != null) {
            sb.append("- 匹配框架ID: ").append(result.getMatchedFrameId()).append("\n");
        }

        if (result.getCandidateFrameIds() != null && !result.getCandidateFrameIds().isEmpty()) {
            sb.append("- 候选框架ID: ").append(result.getCandidateFrameIds()).append("\n");
        }

        if (result.getRuleParsedTargetAmount() != null) {
            sb.append("- 规则解析的目标金额: ").append(result.getRuleParsedTargetAmount()).append("\n");
        }

        if (result.getMissingSlots() != null && !result.getMissingSlots().isEmpty()) {
            sb.append("- 缺失槽位: ").append(result.getMissingSlots()).append("\n");
        }

        if (result.getMatchedPatterns() != null && !result.getMatchedPatterns().isEmpty()) {
            sb.append("- 匹配模式: ").append(result.getMatchedPatterns()).append("\n");
        }

        sb.append("- 规则置信度: ").append(String.format("%.2f", result.getRuleConfidence())).append("\n");

        sb.append("\n注意：\n");
        sb.append("- 如果 matchLevel=EXACT 且槽位完整，优先使用该框架。\n");
        sb.append("- 如果 matchLevel=PARTIAL，判断 query 是否与候选 frame 语义等价，并补齐缺失槽位。\n");
        sb.append("- 如果 matchLevel=NONE 但 query 明显不是购物车语义，输出 PRODUCT_SEARCH 或其他非购物车 intent。\n");
        sb.append("- 不允许发明未知 frameId。\n");
        sb.append("- 不允许编造购物车金额、购物车商品、product_id。\n");
        sb.append("- targetAmount 必须是数字。\n");
        sb.append("- \"推荐2000元以内的电脑\" 不是购物车意图，应输出 PRODUCT_SEARCH, price.max=2000。\n");

        return sb.toString();
    }

    private String buildCartRulesSection() {
        return """
                ## 购物车意图规则
                
                23. 购物车意图只能从上述"可用购物车语义框架"中选取，不允许发明新的 cart action 或 frameId。
                24. 如果语义匹配结果显示 matchLevel=EXACT 且槽位完整，直接使用匹配框架对应模板。
                25. 如果 matchLevel=PARTIAL，判断用户 query 与候选 frame 的语义是否等价，补齐缺失槽位。
                26. targetAmount 必须是数字。如果用户没有提供目标金额且框架要求 target_amount，设置 needsClarification=true。
                27. 不允许编造购物车总价。购物车真实数据由后端 CartService 提供。
                28. 不允许编造购物车商品。购物车内容由后端 CartService 提供。
                29. 不允许编造 product_id。referencedProductIds 只能来自 page_context 或 conversation_state。
                30. cart.currency 默认填 "CNY"。
                31. "推荐2000元以内的电脑" 这类查询不是购物车意图，应输出 PRODUCT_SEARCH, price.max=2000, cart=null。
                """;
    }

    private String buildExamplesSection() {
        return """
                ## 输出示例
                
                示例 1：
                用户查询："推荐几款适合程序员的电脑"
                
                应输出：
                {
                  "originalQuery": "推荐几款适合程序员的电脑",
                  "normalizedQuery": "适合程序员的笔记本电脑",
                  "intent": "PRODUCT_SEARCH",
                  "needsRetrieval": true,
                  "contextAction": "NEW_SEARCH",
                  "target": {
                    "category": "数码电子",
                    "subCategory": "笔记本电脑",
                    "subCategories": [],
                    "currentProductId": null,
                    "scopeProductIds": [],
                    "excludeProductIds": []
                  },
                  "price": { "min": null, "max": null, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": [], "exclude": [] },
                  "softKeywords": ["程序员","编程","开发","高性能","大内存","多任务","SSD","续航","屏幕","键盘"],
                  "queryVariants": [
                    "适合程序员的笔记本电脑",
                    "编程开发用高性能笔记本",
                    "大内存办公开发电脑"
                  ],
                  "requestedProductCount": 3,
                  "answerMode": "MULTI_RECOMMENDATION",
                  "needsClarification": false,
                  "confidence": 0.95,
                  "warnings": [],
                  "source": "LLM"
                }
                
                示例 2：
                上下文：上一轮 category=数码电子, subCategory=笔记本电脑, softKeywords=["程序员","编程","开发"]
                用户查询："一万元以下的"
                
                应输出：
                {
                  "originalQuery": "一万元以下的",
                  "normalizedQuery": "一万元以下的笔记本电脑",
                  "intent": "REFINE_PREVIOUS_QUERY",
                  "needsRetrieval": true,
                  "contextAction": "REFINE_PREVIOUS_SEARCH",
                  "target": {
                    "category": "数码电子",
                    "subCategory": "笔记本电脑",
                    "subCategories": [],
                    "currentProductId": null,
                    "scopeProductIds": [],
                    "excludeProductIds": []
                  },
                  "price": { "min": null, "max": 10000, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": ["程序员","编程","开发"], "exclude": [] },
                  "softKeywords": ["程序员","编程","开发"],
                  "queryVariants": [
                    "一万元以下适合程序员的笔记本电脑",
                    "预算一万以内开发办公笔记本"
                  ],
                  "requestedProductCount": 3,
                  "answerMode": "MULTI_RECOMMENDATION",
                  "needsClarification": false,
                  "confidence": 0.90,
                  "warnings": [],
                  "source": "LLM"
                }
                
                示例 3：
                用户查询："当前已经买了多少钱了？"
                
                应输出：
                {
                  "originalQuery": "当前已经买了多少钱了？",
                  "normalizedQuery": "购物车金额查询",
                  "intent": "CART_SUMMARY",
                  "needsRetrieval": false,
                  "contextAction": "READ_CART",
                  "target": { "category": null, "subCategory": null, "subCategories": [], "currentProductId": null, "scopeProductIds": [], "excludeProductIds": [] },
                  "price": { "min": null, "max": null, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": [], "exclude": [] },
                  "softKeywords": [],
                  "queryVariants": [],
                  "requestedProductCount": 0,
                  "answerMode": "CART_QA",
                  "needsClarification": false,
                  "confidence": 0.94,
                  "warnings": [],
                  "source": "LLM",
                  "cart": {
                    "action": "CART_SUMMARY",
                    "targetAmount": null,
                    "currency": "CNY",
                    "needsCart": true,
                    "needsRecommendation": false,
                    "referencedProductIds": [],
                    "productReference": null
                  }
                }
                
                示例 4：
                用户查询："离2000还差多少"
                
                应输出：
                {
                  "originalQuery": "离2000还差多少",
                  "normalizedQuery": "购物车差额查询",
                  "intent": "CART_TOP_UP",
                  "needsRetrieval": false,
                  "contextAction": "READ_CART",
                  "target": { "category": null, "subCategory": null, "subCategories": [], "currentProductId": null, "scopeProductIds": [], "excludeProductIds": [] },
                  "price": { "min": null, "max": null, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": [], "exclude": [] },
                  "softKeywords": [],
                  "queryVariants": [],
                  "requestedProductCount": 0,
                  "answerMode": "CART_QA",
                  "needsClarification": false,
                  "confidence": 0.93,
                  "warnings": [],
                  "source": "LLM",
                  "cart": {
                    "action": "AMOUNT_GAP_QUERY",
                    "targetAmount": 2000,
                    "currency": "CNY",
                    "needsCart": true,
                    "needsRecommendation": false,
                    "referencedProductIds": [],
                    "productReference": null
                  }
                }
                
                示例 5：
                用户查询："如果要凑1000块，有没有推荐商品？"
                
                应输出：
                {
                  "originalQuery": "如果要凑1000块，有没有推荐商品？",
                  "normalizedQuery": "凑单到1000元推荐",
                  "intent": "CART_TOP_UP",
                  "needsRetrieval": true,
                  "contextAction": "READ_CART_AND_RECOMMEND",
                  "target": { "category": null, "subCategory": null, "subCategories": [], "currentProductId": null, "scopeProductIds": [], "excludeProductIds": [] },
                  "price": { "min": null, "max": null, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": [], "exclude": [] },
                  "softKeywords": [],
                  "queryVariants": [],
                  "requestedProductCount": 3,
                  "answerMode": "CART_RECOMMENDATION",
                  "needsClarification": false,
                  "confidence": 0.92,
                  "warnings": [],
                  "source": "LLM",
                  "cart": {
                    "action": "COMPLETION_RECOMMEND",
                    "targetAmount": 1000,
                    "currency": "CNY",
                    "needsCart": true,
                    "needsRecommendation": true,
                    "referencedProductIds": [],
                    "productReference": null
                  }
                }
                
                示例 6：
                用户查询："凑单推荐一下"
                
                应输出：
                {
                  "originalQuery": "凑单推荐一下",
                  "normalizedQuery": "凑单推荐",
                  "intent": "CART_TOP_UP",
                  "needsRetrieval": false,
                  "contextAction": "ASK_CLARIFICATION",
                  "target": { "category": null, "subCategory": null, "subCategories": [], "currentProductId": null, "scopeProductIds": [], "excludeProductIds": [] },
                  "price": { "min": null, "max": null, "currency": "CNY", "strict": true },
                  "brands": { "include": [], "exclude": [] },
                  "attributes": { "include": [], "exclude": [] },
                  "softKeywords": [],
                  "queryVariants": [],
                  "requestedProductCount": 3,
                  "answerMode": "CART_RECOMMENDATION",
                  "needsClarification": true,
                  "confidence": 0.85,
                  "warnings": ["targetAmount is null for CART_TOP_UP, needs clarification"],
                  "source": "LLM",
                  "cart": {
                    "action": "COMPLETION_RECOMMEND",
                    "targetAmount": null,
                    "currency": "CNY",
                    "needsCart": true,
                    "needsRecommendation": true,
                    "referencedProductIds": [],
                    "productReference": null
                  }
                }
                """;
    }
}
