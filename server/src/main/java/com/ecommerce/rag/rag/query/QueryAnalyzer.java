package com.ecommerce.rag.rag.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.models.entity.Product;

@Component
public class QueryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalyzer.class);

    private static final Pattern PRICE_RANGE = Pattern.compile("(\\d+)\\s*(?:-|到|至|~)\\s*(\\d+)\\s*元?", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PRICE_BUDGET = Pattern.compile("(?:预算|价格)\\s*(\\d+)\\s*元?\\s*(?:以内|以下)?", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PRICE_LESS = Pattern.compile("(\\d+)\\s*元?\\s*(?:以下|以内)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PRICE_BEFORE = Pattern.compile("(?:不超过|低于|不大于)\\s*(\\d+)\\s*元?", Pattern.UNICODE_CHARACTER_CLASS);

    private static final Map<String, CategoryRule> IDIOM_MAP = new LinkedHashMap<>();

    static {
        IDIOM_MAP.put("洁面", cat("美妆护肤", "洁面"));
        IDIOM_MAP.put("洗面奶", cat("美妆护肤", "洁面"));
        IDIOM_MAP.put("面霜", cat("美妆护肤", "面霜"));
        IDIOM_MAP.put("保湿面霜", cat("美妆护肤", "面霜"));
        IDIOM_MAP.put("防晒", cat("美妆护肤", "防晒"));
        IDIOM_MAP.put("防晒霜", cat("美妆护肤", "防晒"));
        IDIOM_MAP.put("精华", cat("美妆护肤", "精华"));
        IDIOM_MAP.put("护肤", cat("美妆护肤", (String) null));
        IDIOM_MAP.put("护肤品", cat("美妆护肤", (String) null));
        IDIOM_MAP.put("敏感肌", cat("美妆护肤", (String) null));

        IDIOM_MAP.put("蓝牙耳机", cat("数码电子", "真无线耳机"));
        IDIOM_MAP.put("无线耳机", cat("数码电子", "真无线耳机"));
        IDIOM_MAP.put("耳麦", cat("数码电子", "真无线耳机"));
        IDIOM_MAP.put("头戴式耳机", cat("数码电子", "头戴式耳机"));
        IDIOM_MAP.put("耳机", cat("数码电子", "真无线耳机"));
        IDIOM_MAP.put("手机", cat("数码电子", "智能手机"));
        IDIOM_MAP.put("智能手机", cat("数码电子", "智能手机"));
        IDIOM_MAP.put("笔记本", cat("数码电子", "笔记本电脑"));
        IDIOM_MAP.put("笔记本电脑", cat("数码电子", "笔记本电脑"));
        IDIOM_MAP.put("平板", cat("数码电子", "平板电脑"));
        IDIOM_MAP.put("平板电脑", cat("数码电子", "平板电脑"));

        IDIOM_MAP.put("跑鞋", cat("服饰运动", "跑步鞋"));
        IDIOM_MAP.put("跑步鞋", cat("服饰运动", "跑步鞋"));
        IDIOM_MAP.put("运动鞋", cat("服饰运动", "跑步鞋"));
        IDIOM_MAP.put("运动上衣", cat("服饰运动", "短袖T恤",
                List.of("短袖T恤", "速干T恤", "卫衣")));
        IDIOM_MAP.put("双肩包", cat("服饰运动", "背包"));
        IDIOM_MAP.put("背包", cat("服饰运动", "背包"));
        IDIOM_MAP.put("通勤包", cat("服饰运动", "背包"));

        IDIOM_MAP.put("零食", cat("食品饮料", "坚果/零食"));
        IDIOM_MAP.put("饮料", cat("食品饮料", "茶饮",
                List.of("碳酸饮料", "功能饮料", "茶饮", "咖啡", "牛奶", "酸奶")));
        IDIOM_MAP.put("食品礼盒", cat("食品饮料", "坚果/零食"));
        IDIOM_MAP.put("送礼食品", cat("食品饮料", "坚果/零食"));
    }

    private static final Set<String> JAPANESE_BRANDS = Set.of(
            "SK-II", "sk-ii", "sk2", "SK2",
            "资生堂", "安热沙", "珊珂", "芳珂",
            "雪肌精", "珂润", "芙丽芳丝", "植村秀",
            "ALBION", "CPB", "Cle de Peau", "Pola", "POLA"
    );

    private static final Map<String, String> BRAND_KEYWORDS = new LinkedHashMap<>();
    static {
        BRAND_KEYWORDS.put("apple", "Apple");
        BRAND_KEYWORDS.put("苹果", "Apple");
        BRAND_KEYWORDS.put("nike", "Nike");
        BRAND_KEYWORDS.put("耐克", "Nike");
        BRAND_KEYWORDS.put("华为", "华为");
        BRAND_KEYWORDS.put("小米", "小米");
        BRAND_KEYWORDS.put("兰蔻", "兰蔻");
        BRAND_KEYWORDS.put("雅诗兰黛", "雅诗兰黛");
        BRAND_KEYWORDS.put("香奈儿", "香奈儿");
        BRAND_KEYWORDS.put("迪奥", "迪奥");
        BRAND_KEYWORDS.put("资生堂", "资生堂");
    }

    private static final Map<String, List<String>> PERSONA_KEYWORDS = new LinkedHashMap<>();
    static {
        PERSONA_KEYWORDS.put("学生", List.of("性价比", "低价", "实惠", "学生"));
        PERSONA_KEYWORDS.put("学生党", List.of("性价比", "低价", "实惠", "学生"));
        PERSONA_KEYWORDS.put("油皮", List.of("控油", "清爽", "油皮"));
        PERSONA_KEYWORDS.put("敏感肌", List.of("敏感", "舒缓", "修护", "温和"));
        PERSONA_KEYWORDS.put("通勤", List.of("通勤", "轻量", "便携"));
        PERSONA_KEYWORDS.put("送礼", List.of("礼盒", "礼品", "送礼"));
    }

    private static final Pattern NOT_PATTERN = Pattern.compile(
            "(?:不要|排除|不要推荐|不想要|不想买|别推荐)\\s*([\\u4e00-\\u9fa5A-Za-z\\s]+?)(?:[的之]|品牌|$)",
            Pattern.UNICODE_CHARACTER_CLASS);

    public QueryAnalysisResult analyze(String query) {
        if (query == null || query.isBlank()) {
            QueryAnalysisResult empty = new QueryAnalysisResult();
            empty.setOriginalQuery(query);
            return empty;
        }

        QueryAnalysisResult result = new QueryAnalysisResult();
        result.setOriginalQuery(query);

        String cleaned = query.trim();

        cleaned = extractPrice(cleaned, result);

        cleaned = extractNegative(cleaned, result);

        cleaned = extractCategoryAndSub(cleaned, result);

        extractPersonaKeywords(query, result);

        result.setNormalizedQuery(cleaned.trim());

        buildFilters(result);

        log.debug("QueryAnalyzer: '{}' -> category={}, subCategory={}, subCategories={}, "
                        + "minPrice={}, maxPrice={}, negBrands={}, normalized='{}'",
                query, result.getCategory(), result.getSubCategory(),
                result.getSubCategories(), result.getMinPrice(), result.getMaxPrice(),
                result.getNegativeBrands(), result.getNormalizedQuery());

        return result;
    }

    public QueryAnalysisResult analyze(String query, ConversationState context) {
        QueryAnalysisResult result = analyze(query);

        if (context == null || context.getTurnCount() == 0) {
            return result;
        }

        boolean isRefinement = isRefinementQuery(query);
        boolean isSupplement = isSupplementQuery(query);

        if (isRefinement || isSupplement) {
            applyContextInheritance(result, context, query);
        }

        applyContextualRefinements(result, context, query);

        result.setResolvedQuery(result.getNormalizedQuery());
        return result;
    }

    private boolean isRefinementQuery(String query) {
        return query.contains("便宜点") || query.contains("便宜一点")
                || query.contains("再便宜") || query.contains("贵一点")
                || query.contains("轻一点") || query.contains("轻量的")
                || query.contains("要轻量") || query.contains("大一点");
    }

    private boolean isSupplementQuery(String query) {
        return query.startsWith("预算") || query.startsWith("不要")
                || query.startsWith("除了") || query.startsWith("换一个")
                || query.contains("还有吗") || query.contains("还有什么")
                || query.contains("其他的") || query.contains("别的")
                || query.contains("不含") || query.contains("不要含");
    }

    private static final Set<String> CURRENT_PRODUCT_REF_KEYWORDS = Set.of(
            "这个", "这款", "它", "当前这个", "这个商品");

    private static final Set<String> CURRENT_PRODUCT_QA_KEYWORDS = Set.of(
            "适合", "怎么样", "优点", "缺点", "参数", "规格", "材质", "适合谁",
            "敏感肌", "成分", "功效", "好不好", "行不行");

    private static final Set<String> ALTERNATIVE_KEYWORDS = Set.of(
            "更便宜", "便宜点", "便宜一点", "类似", "差不多", "替代",
            "换一个", "还有吗", "其他的", "别的", "除了这个");

    public QueryAnalysisResult analyze(String query, ConversationState context,
                                        PageContextResolution pageContext) {
        QueryAnalysisResult result = analyze(query, context);

        if (pageContext == null || !pageContext.hasPageContext()) {
            return result;
        }

        result.getPageWarnings().addAll(pageContext.getWarnings());

        switch (pageContext.getPageType()) {
            case PRODUCT_DETAIL -> applyProductDetailContext(result, pageContext, query);
            case PRODUCT_LIST -> applyProductListContext(result, pageContext, query);
            case CHAT, UNKNOWN -> {}
        }

        return result;
    }

    private void applyProductDetailContext(QueryAnalysisResult result,
                                            PageContextResolution pageContext, String query) {
        Product currentProduct = pageContext.getCurrentProduct();
        if (currentProduct == null) {
            result.getPageWarnings().add("PRODUCT_DETAIL: current product not resolved");
            return;
        }

        result.setCurrentProductId(currentProduct.getProductId());

        boolean refersToCurrent = containsAnyKeyword(query, CURRENT_PRODUCT_REF_KEYWORDS);

        if (refersToCurrent) {
            result.setInheritedFromPageContext(true);
            result.getPageWarnings().add("PRODUCT_DETAIL: user refers to current product");
        }

        boolean isAlternative = containsAnyKeyword(query, ALTERNATIVE_KEYWORDS);

        if (isAlternative) {
            applyProductDetailAlternative(result, pageContext, query);
            return;
        }

        if (refersToCurrent && containsAnyKeyword(query, CURRENT_PRODUCT_QA_KEYWORDS)) {
            result.getPageWarnings().add("PRODUCT_DETAIL: current product QA, no full retrieval needed");
            result.getBoostedProductIds().add(currentProduct.getProductId());
            return;
        }

        if (query.contains("更便宜") || query.contains("便宜点") || query.contains("便宜一点")) {
            applyProductDetailCheaper(result, pageContext);
        }
    }

    private void applyProductDetailAlternative(QueryAnalysisResult result,
                                                PageContextResolution pageContext, String query) {
        Product currentProduct = pageContext.getCurrentProduct();
        if (currentProduct == null) return;

        if (result.getCategory() == null || result.getCategory().isBlank()) {
            result.setCategory(currentProduct.getCategory());
            result.setInheritedFromPageContext(true);
        }

        if (result.getSubCategory() == null || result.getSubCategory().isBlank()) {
            result.setSubCategory(currentProduct.getSubCategory());
            result.setInheritedFromPageContext(true);
        }

        if (!result.getExcludeProductIds().contains(currentProduct.getProductId())) {
            result.getExcludeProductIds().add(currentProduct.getProductId());
            result.getPageWarnings().add("PRODUCT_DETAIL: excluding current product " + currentProduct.getProductId());
        }

        if (query.contains("更便宜") || query.contains("便宜点") || query.contains("便宜一点")) {
            if (result.getMaxPrice() == null && currentProduct.getPrice() != null) {
                result.setMaxPrice(currentProduct.getPrice().multiply(new BigDecimal("0.8"))
                        .setScale(0, RoundingMode.FLOOR));
                result.getPageWarnings().add("PRODUCT_DETAIL: cheaper constraint, maxPrice=" + result.getMaxPrice());
            }
        }
    }

    private void applyProductDetailCheaper(QueryAnalysisResult result,
                                            PageContextResolution pageContext) {
        Product currentProduct = pageContext.getCurrentProduct();
        if (currentProduct == null) return;

        if (result.getCategory() == null || result.getCategory().isBlank()) {
            result.setCategory(currentProduct.getCategory());
            result.setInheritedFromPageContext(true);
        }

        if (result.getSubCategory() == null || result.getSubCategory().isBlank()) {
            result.setSubCategory(currentProduct.getSubCategory());
            result.setInheritedFromPageContext(true);
        }

        if (result.getMaxPrice() == null && currentProduct.getPrice() != null) {
            result.setMaxPrice(currentProduct.getPrice().multiply(new BigDecimal("0.8"))
                    .setScale(0, RoundingMode.FLOOR));
            result.getPageWarnings().add("PRODUCT_DETAIL: cheaper, maxPrice from current product");
        }

        if (!result.getExcludeProductIds().contains(currentProduct.getProductId())) {
            result.getExcludeProductIds().add(currentProduct.getProductId());
        }
        result.setInheritedFromPageContext(true);
    }

    private void applyProductListContext(QueryAnalysisResult result,
                                          PageContextResolution pageContext, String query) {
        String searchQuery = pageContext.getPageSearchQuery();
        if (searchQuery != null && !searchQuery.isBlank()) {
            result.setPageSearchQuery(searchQuery);

            if (result.getNormalizedQuery() == null || result.getNormalizedQuery().isBlank()) {
                result.setNormalizedQuery(searchQuery);
            } else if (!result.getNormalizedQuery().contains(searchQuery)) {
                result.setNormalizedQuery(result.getNormalizedQuery() + " " + searchQuery);
            }

            if (!result.getPositiveKeywords().contains(searchQuery)) {
                result.getPositiveKeywords().add(searchQuery);
            }
        }

        Map<String, Object> filters = pageContext.getSelectedFilters();
        if (filters != null && !filters.isEmpty()) {
            result.setPageFilters(new LinkedHashMap<>(filters));

            if (result.getCategory() == null || result.getCategory().isBlank()) {
                Object category = filters.get("category");
                if (category instanceof String c && !c.isBlank()) {
                    result.setCategory(c);
                    result.setInheritedFromPageContext(true);
                }
            }

            if (result.getSubCategory() == null || result.getSubCategory().isBlank()) {
                Object subCategory = filters.get("sub_category");
                if (subCategory instanceof String s && !s.isBlank()) {
                    result.setSubCategory(s);
                    result.setInheritedFromPageContext(true);
                }
            }

            if (result.getBrand() == null || result.getBrand().isBlank()) {
                Object brand = filters.get("brand");
                if (brand instanceof String b && !b.isBlank()) {
                    result.setBrand(b);
                    result.setInheritedFromPageContext(true);
                }
            }
        }

        List<Product> visibleProducts = pageContext.getVisibleProducts();
        if (visibleProducts != null && !visibleProducts.isEmpty()) {
            for (Product p : visibleProducts) {
                if (!result.getBoostedProductIds().contains(p.getProductId())) {
                    result.getBoostedProductIds().add(p.getProductId());
                }
                result.getScopeProductIds().add(p.getProductId());
            }
            result.getPageWarnings().add("PRODUCT_LIST: boostedVisibleIds=" + visibleProducts.size());
        }

        if (containsAnyKeyword(query, ALTERNATIVE_KEYWORDS)) {
            result.getPageWarnings().add("PRODUCT_LIST: alternative keywords detected");
            result.setInheritedFromPageContext(true);

            if (!result.getExcludeProductIds().isEmpty()) {
                result.getBoostedProductIds().removeAll(result.getExcludeProductIds());
            }
        }
    }

    private boolean containsAnyKeyword(String query, Set<String> keywords) {
        if (query == null || query.isEmpty()) return false;
        for (String kw : keywords) {
            if (query.contains(kw)) return true;
        }
        return false;
    }

    private void applyContextInheritance(QueryAnalysisResult result, ConversationState context, String query) {
        boolean categoryMissing = result.getCategory() == null || result.getCategory().isBlank();
        boolean subCategoryMissing = result.getSubCategory() == null || result.getSubCategory().isBlank();

        if (categoryMissing && context.getCategory() != null && !context.getCategory().isBlank()) {
            result.setCategory(context.getCategory());
            result.setInheritedFromContext(true);
        }

        if (subCategoryMissing && context.getSubCategory() != null && !context.getSubCategory().isBlank()) {
            result.setSubCategory(context.getSubCategory());
            result.setInheritedFromContext(true);
        }

        if (result.getSubCategories().isEmpty() && context.getSubCategories() != null
                && !context.getSubCategories().isEmpty()) {
            result.setSubCategories(new ArrayList<>(context.getSubCategories()));
            result.setInheritedFromContext(true);
        }

        if (context.getPositiveKeywords() != null && !context.getPositiveKeywords().isEmpty()) {
            for (String kw : context.getPositiveKeywords()) {
                if (!result.getPositiveKeywords().contains(kw)) {
                    result.getPositiveKeywords().add(kw);
                }
            }
        }

        if (result.getInheritedFromContext() != null && result.getInheritedFromContext()) {
            String contextTerm = (context.getSubCategory() != null && !context.getSubCategory().isBlank())
                    ? context.getSubCategory() : context.getCategory();
            if (contextTerm != null && !contextTerm.isBlank()) {
                String currentNorm = result.getNormalizedQuery();
                String cleaned = (currentNorm != null && !currentNorm.isBlank())
                        ? currentNorm
                            .replaceAll("除了", "")
                            .replaceAll("[要给换还再点的了一下去]", "")
                            .trim()
                        : "";
                if (!cleaned.isEmpty() && !cleaned.equals(contextTerm)) {
                    result.setNormalizedQuery(contextTerm + " " + cleaned);
                } else {
                    result.setNormalizedQuery(contextTerm);
                }
            }
        }
    }

    private void applyContextualRefinements(QueryAnalysisResult result, ConversationState context, String query) {
        if (query.contains("再便宜") || query.contains("便宜点") || query.contains("便宜一点")) {
            applyCheaperConstraint(result, context);
        }

        if (query.contains("换一个") || query.contains("还有吗") || query.contains("还有什么")
                || query.contains("其他的") || query.contains("别的")) {
            applyExcludeRecommended(result, context);
        }

        if (query.contains("除了")) {
            applyExcludeBrand(result, context, query);
        }

        if (query.contains("不要")) {
            applyNegativeConstraints(result, query);
        }

        if (query.contains("不含") || query.contains("不要含")) {
            applyAvoidIngredients(result, query);
        }

        if (query.contains("学生党")) {
            applyStudentConstraint(result);
        }
    }

    private void applyCheaperConstraint(QueryAnalysisResult result, ConversationState context) {
        if (result.getMaxPrice() != null) {
            return;
        }

        if (context.getMaxPrice() != null) {
            result.setMaxPrice(context.getMaxPrice().multiply(new BigDecimal("0.8"))
                    .setScale(0, RoundingMode.FLOOR));
            result.getWarnings().add("context: 降价20%约束, maxPrice=" + result.getMaxPrice());
        } else {
            result.getPositiveKeywords().add("性价比");
            result.getPositiveKeywords().add("低价");
            result.getWarnings().add("context: 无价格上下文，加入性价比关键词");
        }

        if (result.getMinPrice() == null && context.getMinPrice() != null) {
            result.setMinPrice(context.getMinPrice());
        }
    }

    private void applyExcludeRecommended(QueryAnalysisResult result, ConversationState context) {
        if (context.getRecommendedProductIds() != null && !context.getRecommendedProductIds().isEmpty()) {
            result.getExcludeProductIds().addAll(context.getRecommendedProductIds());
            result.getWarnings().add("context: 排除上一轮推荐商品, count="
                    + context.getRecommendedProductIds().size());
        }
    }

    private void applyExcludeBrand(QueryAnalysisResult result, ConversationState context, String query) {
        String afterExcept = query.replaceAll(".*除了", "").replaceAll("还有.*", "").trim();
        if (afterExcept.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> e : BRAND_KEYWORDS.entrySet()) {
            if (afterExcept.contains(e.getKey())) {
                result.getNegativeBrands().add(e.getValue());
                result.getExcludeBrands().add(e.getValue());
                result.getWarnings().add("context: 排除品牌 " + e.getValue());
            }
        }

        if (afterExcept.contains("耐克") || afterExcept.contains("nike")) {
            if (!result.getNegativeBrands().contains("Nike")) {
                result.getNegativeBrands().add("Nike");
                result.getExcludeBrands().add("Nike");
            }
            if (!result.getNegativeBrands().contains("耐克")) {
                result.getNegativeBrands().add("耐克");
                result.getExcludeBrands().add("耐克");
            }
        }
    }

    private void applyNegativeConstraints(QueryAnalysisResult result, String query) {
        if (query.contains("不要日系") || query.contains("不要日本")) {
            result.getNegativeBrands().addAll(JAPANESE_BRANDS);
            for (String brand : JAPANESE_BRANDS) {
                if (!result.getExcludeBrands().contains(brand)) {
                    result.getExcludeBrands().add(brand);
                }
            }
            result.getWarnings().add("negative: 排除日系品牌, count=" + JAPANESE_BRANDS.size());
        }
    }

    private void applyAvoidIngredients(QueryAnalysisResult result, String query) {
        if (query.contains("不含酒精") || query.contains("不要含酒精")
                || query.contains("不含乙醇") || query.contains("不要含乙醇")) {
            result.getNegativeKeywords().add("酒精");
            result.getNegativeKeywords().add("乙醇");
            result.getNegativeKeywords().add("alcohol");
            result.getAvoidIngredientsOrTerms().add("酒精");
            result.getAvoidIngredientsOrTerms().add("乙醇");
            result.getAvoidIngredientsOrTerms().add("alcohol");
            result.getWarnings().add("negative: 排除含酒精商品，当前商品库不一定包含完整成分表，仅基于描述文本排除");
        }
    }

    private void applyStudentConstraint(QueryAnalysisResult result) {
        result.getPositiveKeywords().add("性价比");
        result.getPositiveKeywords().add("低价");
        result.getPositiveKeywords().add("实用");
    }

    private String extractPrice(String query, QueryAnalysisResult result) {
        Matcher rangeMatcher = PRICE_RANGE.matcher(query);
        if (rangeMatcher.find()) {
            try {
                result.setMinPrice(new BigDecimal(rangeMatcher.group(1)));
                result.setMaxPrice(new BigDecimal(rangeMatcher.group(2)));
            } catch (NumberFormatException ignored) {}
            return query.replace(rangeMatcher.group(), "").trim();
        }

        Matcher budgetMatcher = PRICE_BUDGET.matcher(query);
        if (budgetMatcher.find()) {
            try {
                result.setMaxPrice(new BigDecimal(budgetMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
            return query.replace(budgetMatcher.group(), "").trim();
        }

        Matcher lessMatcher = PRICE_LESS.matcher(query);
        if (lessMatcher.find()) {
            try {
                result.setMaxPrice(new BigDecimal(lessMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
            return query.replace(lessMatcher.group(), "").trim();
        }

        Matcher beforeMatcher = PRICE_BEFORE.matcher(query);
        if (beforeMatcher.find()) {
            try {
                result.setMaxPrice(new BigDecimal(beforeMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
            return query.replace(beforeMatcher.group(), "").trim();
        }

        return query;
    }

    private String extractNegative(String query, QueryAnalysisResult result) {
        Matcher notMatcher = NOT_PATTERN.matcher(query);
        if (notMatcher.find()) {
            String phrase = notMatcher.group(1).trim();
            result.getNegativeKeywords().add(phrase);

            if (phrase.contains("日本") || phrase.contains("日系") || phrase.contains("日货")) {
                result.getNegativeBrands().addAll(JAPANESE_BRANDS);
                result.getWarnings().add("negative_brand: 排除日系品牌");
            }

            for (Map.Entry<String, String> e : BRAND_KEYWORDS.entrySet()) {
                if (phrase.contains(e.getKey())) {
                    result.getNegativeBrands().add(e.getValue());
                    result.getWarnings().add("negative_brand: 排除品牌 " + e.getValue());
                }
            }

            query = query.replace(notMatcher.group(), "").trim();
        }
        return query;
    }

    private String extractCategoryAndSub(String query, QueryAnalysisResult result) {
        String bestMatch = null;
        for (String idiom : IDIOM_MAP.keySet()) {
            if (query.contains(idiom)) {
                if (bestMatch == null || idiom.length() > bestMatch.length()) {
                    bestMatch = idiom;
                }
            }
        }

        if (bestMatch != null) {
            CategoryRule rule = IDIOM_MAP.get(bestMatch);
            result.setCategory(rule.category);
            result.setSubCategory(rule.subCategory);
            if (rule.subCategories != null) {
                result.setSubCategories(new ArrayList<>(rule.subCategories));
            }
        }
        return query;
    }

    private void extractPersonaKeywords(String query, QueryAnalysisResult result) {
        for (Map.Entry<String, List<String>> e : PERSONA_KEYWORDS.entrySet()) {
            if (query.contains(e.getKey())) {
                for (String kw : e.getValue()) {
                    if (!result.getPositiveKeywords().contains(kw)) {
                        result.getPositiveKeywords().add(kw);
                    }
                }
            }
        }
    }

    private void buildFilters(QueryAnalysisResult result) {
        Map<String, Object> filters = new LinkedHashMap<>();

        if (result.getCategory() != null && !result.getCategory().isBlank()) {
            filters.put("category", result.getCategory());
        }

        String sub = result.getSubCategory();
        if (sub != null && !sub.isBlank()) {
            filters.put("sub_category", sub);
        }

        if (result.getMinPrice() != null) {
            filters.put("min_price", result.getMinPrice());
        }
        if (result.getMaxPrice() != null) {
            filters.put("max_price", result.getMaxPrice());
        }

        result.setFilters(filters);
    }

    private static CategoryRule cat(String category, String subCategory) {
        return new CategoryRule(category, subCategory, null);
    }

    private static CategoryRule cat(String category, String subCategory, List<String> subCategories) {
        return new CategoryRule(category, subCategory, subCategories);
    }

    private static class CategoryRule {
        final String category;
        final String subCategory;
        final List<String> subCategories;

        CategoryRule(String category, String subCategory, List<String> subCategories) {
            this.category = category;
            this.subCategory = subCategory;
            this.subCategories = subCategories;
        }
    }
}
