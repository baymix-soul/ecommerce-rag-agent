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

    public QueryPlannerPromptBuilder(AppProperties appProperties) {
        this.appProperties = appProperties;
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
                  "intent": "SMALLTALK|HELP|THANKS|PRODUCT_SEARCH|REFINE_PREVIOUS_QUERY|NEGATIVE_CONSTRAINT|CHANGE_OR_MORE|CURRENT_PRODUCT_QA|COMPARE_PRODUCTS|UNKNOWN",
                  "needsRetrieval": true/false,
                  "contextAction": "NONE|NEW_SEARCH|REFINE_PREVIOUS_SEARCH|REPLACE_PREVIOUS_SEARCH|EXCLUDE_FROM_PREVIOUS|CURRENT_PRODUCT_REFERENCE|ASK_CLARIFICATION",
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
                  "answerMode": "MULTI_RECOMMENDATION|SINGLE_RECOMMENDATION",
                  "needsClarification": false,
                  "confidence": 0.0,
                  "warnings": [],
                  "source": "LLM"
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
                """;
    }
}
