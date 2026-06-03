package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class QueryPlanToAnalysisMapperTest {

    private final QueryPlanToAnalysisMapper mapper = new QueryPlanToAnalysisMapper();

    @Test
    void shouldMapCategoryAndSubCategory() {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test");
        plan.setIntent("PRODUCT_SEARCH");
        QueryPlanTarget target = new QueryPlanTarget();
        target.setCategory("数码电子");
        target.setSubCategory("笔记本电脑");
        plan.setTarget(target);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals("数码电子", result.getCategory());
        assertEquals("笔记本电脑", result.getSubCategory());
    }

    @Test
    void shouldMapSubCategories() {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test");
        QueryPlanTarget target = new QueryPlanTarget();
        target.setSubCategories(List.of("笔记本电脑", "智能手机"));
        plan.setTarget(target);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals(2, result.getSubCategories().size());
        assertTrue(result.getSubCategories().contains("笔记本电脑"));
    }

    @Test
    void shouldMapCurrentProductId() {
        QueryPlan plan = new QueryPlan();
        QueryPlanTarget target = new QueryPlanTarget();
        target.setCurrentProductId("p_digital_001");
        plan.setTarget(target);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals("p_digital_001", result.getCurrentProductId());
    }

    @Test
    void shouldMapScopeAndExcludeProductIds() {
        QueryPlan plan = new QueryPlan();
        QueryPlanTarget target = new QueryPlanTarget();
        target.setScopeProductIds(List.of("p1", "p2"));
        target.setExcludeProductIds(List.of("p3"));
        plan.setTarget(target);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals(2, result.getScopeProductIds().size());
        assertEquals(1, result.getExcludeProductIds().size());
    }

    @Test
    void shouldMapMinMaxPrice() {
        QueryPlan plan = new QueryPlan();
        QueryPlanPrice price = new QueryPlanPrice();
        price.setMin(new BigDecimal("500"));
        price.setMax(new BigDecimal("10000"));
        plan.setPrice(price);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals(new BigDecimal("500"), result.getMinPrice());
        assertEquals(new BigDecimal("10000"), result.getMaxPrice());
    }

    @Test
    void shouldMapBrandsExcludeToNegativeBrands() {
        QueryPlan plan = new QueryPlan();
        QueryPlanBrands brands = new QueryPlanBrands();
        brands.setExclude(List.of("Nike", "Adidas"));
        brands.setInclude(List.of("Apple"));
        plan.setBrands(brands);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals("Apple", result.getBrand());
        assertTrue(result.getNegativeBrands().contains("Nike"));
        assertTrue(result.getNegativeBrands().contains("Adidas"));
    }

    @Test
    void shouldMapAttributesAndSoftKeywords() {
        QueryPlan plan = new QueryPlan();
        QueryPlanAttributes attributes = new QueryPlanAttributes();
        attributes.setInclude(List.of("高性能", "大内存"));
        attributes.setExclude(List.of("低配"));
        plan.setAttributes(attributes);
        plan.setSoftKeywords(List.of("程序员", "开发"));

        QueryAnalysisResult result = mapper.map(plan, null);
        assertTrue(result.getPositiveKeywords().contains("高性能"));
        assertTrue(result.getPositiveKeywords().contains("程序员"));
        assertTrue(result.getSoftKeywords().contains("程序员"));
        assertTrue(result.getNegativeKeywords().contains("低配"));
    }

    @Test
    void shouldMapQueryVariants() {
        QueryPlan plan = new QueryPlan();
        plan.setQueryVariants(List.of("v1", "v2", "v3"));

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals(3, result.getQueryVariants().size());
    }

    @Test
    void shouldMapRequestedProductCount() {
        QueryPlan plan = new QueryPlan();
        plan.setRequestedProductCount(2);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals(2, result.getRequestedProductCount());
    }

    @Test
    void shouldMapAnswerModeToResponseStyle() {
        QueryPlan plan = new QueryPlan();
        plan.setAnswerMode("SINGLE_RECOMMENDATION");

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals("SINGLE_RECOMMENDATION", result.getResponseStyle());
    }

    @Test
    void normalizedQueryShouldFallbackToOriginal() {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("原始查询");

        QueryAnalysisResult result = mapper.map(plan, null);
        assertEquals("原始查询", result.getNormalizedQuery());
    }

    @Test
    void normalizedQueryShouldUseSubCategoryAndSoftKeywords() {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test");
        QueryPlanTarget target = new QueryPlanTarget();
        target.setSubCategory("笔记本电脑");
        plan.setTarget(target);
        plan.setSoftKeywords(List.of("程序员", "开发", "高性能"));

        QueryAnalysisResult result = mapper.map(plan, null);
        assertTrue(result.getNormalizedQuery().contains("笔记本电脑"));
        assertTrue(result.getNormalizedQuery().contains("程序员"));
    }

    @Test
    void nullListShouldNotThrow() {
        QueryPlan plan = new QueryPlan();
        plan.setSoftKeywords(null);
        plan.setQueryVariants(null);

        QueryAnalysisResult result = mapper.map(plan, null);
        assertNotNull(result);
        assertTrue(result.getSoftKeywords().isEmpty());
        assertTrue(result.getQueryVariants().isEmpty());
    }

    @Test
    void nullPlanShouldReturnEmpty() {
        QueryAnalysisResult result = mapper.map(null, null);
        assertNotNull(result);
        assertNotNull(result.getWarnings());
    }
}
