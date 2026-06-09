package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;

@DisplayName("QueryPlanner Prompt Cart Intent Tests")
class QueryPlannerPromptCartIntentTest {

    private final QueryPlannerPromptBuilder builder;
    private final CatalogTaxonomySnapshot taxonomy;

    QueryPlannerPromptCartIntentTest() {
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("assist");
        AppProperties.UnderstandingProperties understandingProps =
                new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        AppProperties appProperties = new AppProperties();
        appProperties.setUnderstanding(understandingProps);

        CartSemanticHintService hintService = new CartSemanticHintService();
        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, hintService);
        builder = new QueryPlannerPromptBuilder(appProperties, catalog, matcher);
        taxonomy = buildTaxonomy();
    }

    private CatalogTaxonomySnapshot buildTaxonomy() {
        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(java.util.List.of("美妆护肤", "数码电子", "服饰运动", "食品饮料"));
        snap.setSubCategoriesByCategory(java.util.Map.of(
                "数码电子", java.util.List.of("笔记本电脑", "智能手机", "真无线耳机"),
                "服饰运动", java.util.List.of("跑步鞋", "背包")
        ));
        snap.setAllSubCategories(java.util.List.of(
                "笔记本电脑", "智能手机", "真无线耳机", "跑步鞋", "背包"));
        snap.setBrands(java.util.List.of("Apple", "Nike", "华为"));
        snap.setMinPrice(java.math.BigDecimal.valueOf(29));
        snap.setMaxPrice(java.math.BigDecimal.valueOf(12999));
        snap.setFilterableFields(new java.util.ArrayList<>(
                CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new java.util.ArrayList<>(
                CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
        return snap;
    }

    @Test
    @DisplayName("prompt contains CART_SUMMARY definition for cart summary query")
    void testPromptContainsCartSummaryDefinition() {
        String prompt = builder.build("当前已经买了多少钱了", taxonomy, null, null);
        assertTrue(prompt.contains("CART_SUMMARY"),
                "Prompt should contain CART_SUMMARY intent definition");
    }

    @Test
    @DisplayName("prompt contains CART_TOP_UP and CART_COMPLETION_RECOMMEND for top-up query")
    void testPromptContainsCartTopUpDefinition() {
        String prompt = builder.build("如果要凑1000块", taxonomy, null, null);
        assertTrue(prompt.contains("CART_TOP_UP"),
                "Prompt should contain CART_TOP_UP intent definition");
        assertTrue(prompt.contains("CART_COMPLETION_RECOMMEND"),
                "Prompt should contain CART_COMPLETION_RECOMMEND intent definition");
    }

    @Test
    @DisplayName("prompt prohibits fabricating cart amount")
    void testPromptProhibitsFabricatingCartAmount() {
        String prompt = builder.build("当前已经买了多少钱了", taxonomy, null, null);
        assertTrue(prompt.contains("不允许编造购物车总价"),
                "Prompt should prohibit fabricating cart total amount");
    }

    @Test
    @DisplayName("prompt prohibits fabricating product_id")
    void testPromptProhibitsFabricatingProductId() {
        String prompt = builder.build("当前已经买了多少钱了", taxonomy, null, null);
        assertTrue(prompt.contains("不允许编造 product_id"),
                "Prompt should prohibit fabricating product_id");
    }

    @Test
    @DisplayName("prompt contains cart summary example")
    void testPromptContainsCartSummaryExample() {
        String prompt = builder.build("推荐电脑", taxonomy, null, null);
        assertTrue(prompt.contains("当前已经买了多少钱了"),
                "Prompt should contain cart summary example query");
    }

    @Test
    @DisplayName("prompt contains cart top-up example")
    void testPromptContainsCartTopUpExample() {
        String prompt = builder.build("推荐电脑", taxonomy, null, null);
        assertTrue(prompt.contains("如果要凑1000块"),
                "Prompt should contain cart top-up example query");
    }

    @Test
    @DisplayName("prompt contains clarification example")
    void testPromptContainsClarificationExample() {
        String prompt = builder.build("推荐电脑", taxonomy, null, null);
        assertTrue(prompt.contains("凑单推荐一下"),
                "Prompt should contain clarification example query");
    }
}
