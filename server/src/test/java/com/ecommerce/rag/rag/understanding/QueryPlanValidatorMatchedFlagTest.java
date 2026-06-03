package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPlanValidatorMatchedFlagTest {

    private QueryPlanValidator validator;
    private CatalogTaxonomySnapshot taxonomy;

    @BeforeEach
    void setUp() {
        validator = new QueryPlanValidator();
        taxonomy = buildTaxonomy();
    }

    private CatalogTaxonomySnapshot buildTaxonomy() {
        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(List.of("美妆护肤", "数码电子", "服饰运动", "食品饮料"));
        snap.setAllSubCategories(List.of("精华", "洁面", "防晒", "真无线耳机", "笔记本电脑", "跑步鞋", "背包"));
        snap.setSubCategoriesByCategory(java.util.Map.of(
                "数码电子", List.of("笔记本电脑", "真无线耳机"),
                "服饰运动", List.of("跑步鞋", "背包")
        ));
        snap.setBrands(List.of("Apple", "Nike", "华为"));
        snap.setMinPrice(new BigDecimal("50"));
        snap.setMaxPrice(new BigDecimal("15000"));
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
        return snap;
    }

    private QueryPlan createPlan(String intent, String category, String subCategory) {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test");
        plan.setIntent(intent);
        plan.setSource("TEST");
        QueryPlanTarget target = new QueryPlanTarget();
        target.setCategory(category);
        target.setSubCategory(subCategory);
        plan.setTarget(target);
        return plan;
    }

    @Test
    void validCategoryShouldSetMatchedTrue() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "数码电子", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getCategoryMatched());
        assertTrue(result.getCategoryMatched());
    }

    @Test
    void validCategoryShouldNotAddWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "数码电子", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        boolean hasCategoryMatchWarning = result.getWarnings().stream()
                .anyMatch(w -> w.startsWith("category_matched"));
        assertFalse(hasCategoryMatchWarning, "valid category should NOT be in warnings");
    }

    @Test
    void validSubCategoryShouldSetMatchedTrue() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "数码电子", "笔记本电脑");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getSubCategoryMatched());
        assertTrue(result.getSubCategoryMatched());
    }

    @Test
    void validSubCategoryShouldNotAddWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "数码电子", "笔记本电脑");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        boolean hasSubMatchWarning = result.getWarnings().stream()
                .anyMatch(w -> w.startsWith("sub_category_matched"));
        assertFalse(hasSubMatchWarning, "valid subCategory should NOT be in warnings");
    }

    @Test
    void unknownCategoryShouldSetMatchedFalseAndAddWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "汽车用品", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getCategoryMatched());
        assertFalse(result.getCategoryMatched());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("unknown_category")));
    }

    @Test
    void unknownSubCategoryShouldSetMatchedFalseAndAddWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, "汽车配件");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getSubCategoryMatched());
        assertFalse(result.getSubCategoryMatched());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("unknown_sub_category")));
    }

    @Test
    void subCategoryNotUnderCategoryShouldStillSetMatchedTrue() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "数码电子", "背包");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertTrue(result.getSubCategoryMatched());
        boolean hasNotUnderWarning = result.getWarnings().stream()
                .anyMatch(w -> w.contains("sub_category_not_under_category"));
        assertTrue(hasNotUnderWarning);
    }

    @Test
    void nullTargetShouldNotSetMatchedFlags() {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test");
        plan.setIntent("PRODUCT_SEARCH");
        plan.setSource("TEST");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertFalse(result.getCategoryMatched());
        assertFalse(result.getSubCategoryMatched());
    }

    @Test
    void emptyCategoryShouldNotSetMatchedFlags() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertFalse(result.getCategoryMatched());
    }
}
