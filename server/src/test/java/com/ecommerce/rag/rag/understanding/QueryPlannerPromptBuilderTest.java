package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.memory.ConversationState;

class QueryPlannerPromptBuilderTest {

    private final QueryPlannerPromptBuilder builder;
    private final CatalogTaxonomySnapshot taxonomy;

    QueryPlannerPromptBuilderTest() {
        AppProperties props = new AppProperties();
        props.setUnderstanding(new AppProperties.UnderstandingProperties());
        builder = new QueryPlannerPromptBuilder(props);
        taxonomy = buildTaxonomy();
    }

    private CatalogTaxonomySnapshot buildTaxonomy() {
        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(List.of("美妆护肤", "数码电子", "服饰运动", "食品饮料"));
        snap.setSubCategoriesByCategory(Map.of(
                "数码电子", List.of("笔记本电脑", "智能手机", "真无线耳机"),
                "服饰运动", List.of("跑步鞋", "背包")
        ));
        snap.setAllSubCategories(List.of("笔记本电脑", "智能手机", "真无线耳机", "跑步鞋", "背包"));
        snap.setBrands(List.of("Apple", "Nike", "华为"));
        snap.setMinPrice(new BigDecimal("29"));
        snap.setMaxPrice(new BigDecimal("12999"));
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
        return snap;
    }

    @Test
    void shouldIncludeCategories() {
        String prompt = builder.build("推荐电脑", taxonomy, null, null);
        assertTrue(prompt.contains("数码电子"));
        assertTrue(prompt.contains("笔记本电脑"));
    }

    @Test
    void shouldIncludeSubCategoriesByCategory() {
        String prompt = builder.build("推荐跑鞋", taxonomy, null, null);
        assertTrue(prompt.contains("跑步鞋"));
        assertTrue(prompt.contains("服饰运动"));
    }

    @Test
    void shouldIncludeFilterableAndTextFields() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("category"));
        assertTrue(prompt.contains("description"));
    }

    @Test
    void shouldIncludeJsonSchema() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("QueryPlan"));
        assertTrue(prompt.contains("intent"));
        assertTrue(prompt.toLowerCase().contains("product_search"));
    }

    @Test
    void shouldRequireJsonOnly() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("只输出"));
        assertTrue(prompt.contains("JSON"));
    }

    @Test
    void shouldIncludeMappingRules() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("电脑"));
        assertTrue(prompt.contains("笔记本电脑"));
    }

    @Test
    void shouldIncludeConversationStateWhenPresent() {
        ConversationState state = new ConversationState("s1");
        state.setTurnCount(2);
        state.setCategory("数码电子");
        state.setSubCategory("笔记本电脑");
        state.setLastUserQuery("推荐电脑");
        state.setPositiveKeywords(List.of("开发", "编程"));
        state.setMaxPrice(new BigDecimal("15000"));

        String prompt = builder.build("一万元以下的", taxonomy, state, null);
        assertTrue(prompt.contains("上一轮"));
        assertTrue(prompt.contains("数码电子"));
        assertTrue(prompt.contains("笔记本电脑"));
        assertTrue(prompt.contains("对话轮次"));
        assertTrue(prompt.contains("2"));
    }

    @Test
    void shouldIncludePageContextWhenPresent() {
        PageContextResolution pageCtx = new PageContextResolution();
        Product cp = new Product();
        cp.setProductId("p_digital_001");
        cp.setName("MacBook Pro");
        cp.setBrand("Apple");
        cp.setCategory("数码电子");
        cp.setSubCategory("笔记本电脑");
        cp.setPrice(new BigDecimal("9999"));
        pageCtx.setCurrentProduct(cp);
        pageCtx.setPageType(com.ecommerce.rag.models.dto.PageType.PRODUCT_DETAIL);

        String prompt = builder.build("这个适合开发吗", taxonomy, null, pageCtx);
        assertTrue(prompt.contains("当前商品"));
        assertTrue(prompt.contains("MacBook Pro"));
        assertTrue(prompt.contains("p_digital_001"));
    }

    @Test
    void shouldIncludeSoftKeywordsRules() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("softKeywords"));
        assertTrue(prompt.contains("2-6"));
        assertTrue(prompt.contains("泛化"));
    }

    @Test
    void shouldIncludeConcreteSoftKeywordsExample() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("程序员"));
        assertTrue(prompt.contains("高性能"));
        assertTrue(prompt.contains("大内存"));
        assertTrue(prompt.contains("多任务"));
    }

    @Test
    void shouldIncludeQueryVariantsRule() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("queryVariants"));
        assertTrue(prompt.contains("2-3"));
    }

    @Test
    void shouldIncludeContextRefinementExample() {
        String prompt = builder.build("test", taxonomy, null, null);
        assertTrue(prompt.contains("一万元以下的"));
        assertTrue(prompt.contains("REFINE_PREVIOUS_QUERY"));
        assertTrue(prompt.contains("REFINE_PREVIOUS_SEARCH"));
    }
}
