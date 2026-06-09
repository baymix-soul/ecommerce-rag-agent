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

class QueryPlanValidatorTest {

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
        snap.setAllSubCategories(List.of("精华", "洁面", "防晒", "真无线耳机", "笔记本电脑", "跑步鞋", "背包", "坚果/零食"));
        snap.setBrands(List.of("兰蔻", "雅诗兰黛", "安热沙", "华为", "Apple", "Nike", "耐克", "资生堂", "芙丽芳丝"));
        snap.setMinPrice(new BigDecimal("50"));
        snap.setMaxPrice(new BigDecimal("15000"));
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
        return snap;
    }

    private QueryPlan createPlan(String intent, String category, String subCategory) {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("test query");
        plan.setIntent(intent);
        plan.setSource("TEST");
        QueryPlanTarget target = new QueryPlanTarget();
        target.setCategory(category);
        target.setSubCategory(subCategory);
        plan.setTarget(target);
        return plan;
    }

    @Test
    void validCategoryShouldPass() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result);
        assertTrue(result.getValid());
    }

    @Test
    void validSubCategoryShouldPass() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, "洁面");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result);
        assertTrue(result.getValid());
    }

    @Test
    void unknownCategoryShouldAddWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "汽车用品", null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("unknown_category")));
        assertTrue(result.getValid());
    }

    @Test
    void unknownSubCategoryShouldAddWarningAndClear() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, "汽车配件");
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("unknown_sub_category")));
    }

    @Test
    void minPriceGreaterThanMaxShouldSwap() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        QueryPlanPrice price = new QueryPlanPrice();
        price.setMin(new BigDecimal("1000"));
        price.setMax(new BigDecimal("500"));
        plan.setPrice(price);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertTrue(result.getFixedFields().contains("price.min_max_swapped"));
        QueryPlan validated = result.getValidatedPlan();
        assertNotNull(validated.getPrice());
        assertEquals(new BigDecimal("500"), validated.getPrice().getMin());
        assertEquals(new BigDecimal("1000"), validated.getPrice().getMax());
    }

    @Test
    void requestedProductCountTooHighShouldBeCapped() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setRequestedProductCount(10);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertTrue(result.getFixedFields().stream().anyMatch(f -> f.contains("requestedProductCount=3")));
        assertEquals(3, result.getValidatedPlan().getRequestedProductCount());
    }

    @Test
    void softKeywordsShouldBeDedupedAndLimited() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            keywords.add("kw" + (i % 5));
        }
        plan.setSoftKeywords(keywords);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        List<String> validated = result.getValidatedPlan().getSoftKeywords();
        assertTrue(validated.size() <= 8);
        assertFalse(result.getFixedFields().isEmpty());
    }

    @Test
    void queryVariantsShouldBeDedupedAndLimited() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setQueryVariants(List.of("v1", "v1", "v2", "v3", "v4", "v5"));

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        List<String> validated = result.getValidatedPlan().getQueryVariants();
        assertTrue(validated.size() <= 3);
    }

    @Test
    void productIdPatternShouldBeRemovedFromKeywords() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setSoftKeywords(List.of("温和", "p_beauty_001", "保湿"));

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        List<String> validated = result.getValidatedPlan().getSoftKeywords();
        assertFalse(validated.contains("p_beauty_001"));
        assertTrue(validated.contains("温和"));
        assertTrue(validated.contains("保湿"));
    }

    @Test
    void pricePatternShouldBeRemovedFromKeywords() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setSoftKeywords(List.of("温和", "1000元以下", "保湿"));

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        List<String> validated = result.getValidatedPlan().getSoftKeywords();
        assertFalse(validated.contains("1000元以下"));
        assertTrue(validated.contains("温和"));
        assertTrue(validated.contains("保湿"));
    }

    @Test
    void smalltalkShouldHaveNeedsRetrievalFalse() {
        QueryPlan plan = createPlan("SMALLTALK", null, null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getValidatedPlan().getNeedsRetrieval());
        assertFalse(result.getValidatedPlan().getNeedsRetrieval());
    }

    @Test
    void productSearchShouldHaveNeedsRetrievalTrue() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, null);
        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertNotNull(result.getValidatedPlan().getNeedsRetrieval());
        assertTrue(result.getValidatedPlan().getNeedsRetrieval());
    }

    @Test
    void requestedProductCountLessThan1ShouldBeCorrected() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setRequestedProductCount(0);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertEquals(1, result.getValidatedPlan().getRequestedProductCount());
    }

    @Test
    void answerModeSingleRecommendationShouldSetCountTo1() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", null);
        plan.setRequestedProductCount(3);
        plan.setAnswerMode("SINGLE_RECOMMENDATION");

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertEquals(1, result.getValidatedPlan().getRequestedProductCount());
    }

    @Test
    void nullTaxonomyShouldReturnValidWithWarning() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", "美妆护肤", "洁面");
        QueryPlanValidationResult result = validator.validate(plan, null);
        assertTrue(result.getValid());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("taxonomy is null")));
    }

    @Test
    void currencyShouldDefaultToCNY() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, null);
        QueryPlanPrice price = new QueryPlanPrice();
        price.setMax(new BigDecimal("1000"));
        plan.setPrice(price);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertEquals("CNY", result.getValidatedPlan().getPrice().getCurrency());
        assertTrue(result.getFixedFields().contains("price.currency=CNY"));
    }

    @Test
    void strictShouldDefaultToTrue() {
        QueryPlan plan = createPlan("PRODUCT_SEARCH", null, null);
        QueryPlanPrice price = new QueryPlanPrice();
        price.setMax(new BigDecimal("1000"));
        plan.setPrice(price);

        QueryPlanValidationResult result = validator.validate(plan, taxonomy);
        assertTrue(result.getValidatedPlan().getPrice().getStrict());
        assertTrue(result.getFixedFields().contains("price.strict=true"));
    }
}
