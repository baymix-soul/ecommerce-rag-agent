package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class QueryPlanGatingServiceTest {

    private QueryPlanGatingService gatingService;
    private AppProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        plannerProps.setMinConfidence(0.85);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);
        gatingService = new QueryPlanGatingService(properties);
    }

    private QueryPlanningResult createValidResult(String category, String subCategory,
                                                   double confidence, String intent) {
        FakeLlmClient fakeLlm = new FakeLlmClient();
        String json = String.format(
                "{\"originalQuery\":\"test\",\"intent\":\"%s\",\"confidence\":%s,\"target\":{\"category\":\"%s\",\"subCategory\":\"%s\"}}",
                intent, confidence, category, subCategory);
        fakeLlm.setResponse(json);

        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(List.of("数码电子", "美妆护肤", "服饰运动"));
        snap.setAllSubCategories(List.of("笔记本电脑", "洁面", "跑步鞋"));
        snap.setSubCategoriesByCategory(Map.of(
                "数码电子", List.of("笔记本电脑"),
                "美妆护肤", List.of("洁面"),
                "服饰运动", List.of("跑步鞋")
        ));
        snap.setBrands(List.of("Apple", "华为"));
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));

        CatalogTaxonomyService taxonomyService = new CatalogTaxonomyService(null) {
            @Override
            public CatalogTaxonomySnapshot getSnapshot() { return snap; }
        };

        AppProperties props = new AppProperties();
        AppProperties.PlannerProperties pp = new AppProperties.PlannerProperties();
        pp.setEnabled(true);
        AppProperties.UnderstandingProperties up = new AppProperties.UnderstandingProperties();
        up.setPlanner(pp);
        props.setUnderstanding(up);

        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, new CartSemanticHintService());
        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(props, catalog, matcher);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();
        LLMQueryPlanner planner = new LLMQueryPlanner(fakeLlm, props, promptBuilder, jsonParser, validator, taxonomyService);
        return planner.plan("test", null, null);
    }

    @Test
    void disabledShouldNotAllow() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryPlanningResult pr = new QueryPlanningResult();
        pr.setSource(QueryPlanningResult.SOURCE_DISABLED);

        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_DISABLED, decision.getSelectedSource());
    }

    @Test
    void shadowShouldNotAllow() {
        properties.getUnderstanding().getPlanner().setMode("shadow");
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");

        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_SHADOW_ONLY, decision.getSelectedSource());
    }

    @Test
    void sourceErrorShouldFallback() {
        QueryPlanningResult pr = QueryPlanningResult.error("test", "fake error");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_FALLBACK, decision.getSelectedSource());
    }

    @Test
    void parseFailureShouldFallback() {
        QueryPlanningResult pr = new QueryPlanningResult();
        pr.setSource(QueryPlanningResult.SOURCE_LLM);
        pr.setParseSuccess(false);
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void lowConfidenceShouldFallback() {
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.5, "PRODUCT_SEARCH");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
        assertTrue(decision.getReason().contains("confidence"));
    }

    @Test
    void invalidShouldFallback() {
        QueryPlanningResult pr = new QueryPlanningResult();
        pr.setSource(QueryPlanningResult.SOURCE_LLM);
        pr.setParseSuccess(true);
        pr.setValid(false);
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void validationErrorsShouldFallback() {
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");
        pr.getErrors().add("test error");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void unknownCategoryShouldFallback() {
        QueryPlanningResult pr = createValidResult("汽车用品", null, 0.95, "PRODUCT_SEARCH");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
        assertNotNull(decision.getReason());
    }

    @Test
    void unknownSubCategoryShouldFallback() {
        QueryPlanningResult pr = createValidResult("数码电子", "汽车配件", 0.95, "PRODUCT_SEARCH");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void unknownIntentShouldFallback() {
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "UNKNOWN");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void needsClarificationShouldFallback() {
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");
        pr.getValidatedPlan().setNeedsClarification(true);
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertFalse(decision.isAllowed());
    }

    @Test
    void legalHighConfidenceShouldAllow() {
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertTrue(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_PLANNER, decision.getSelectedSource());
    }

    @Test
    void assistModeShouldFallbackWhenLegacyComplete() {
        properties.getUnderstanding().getPlanner().setMode("assist");
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");

        QueryAnalysisResult legacy = new QueryAnalysisResult();
        legacy.setCategory("数码电子");
        legacy.setSubCategory("笔记本电脑");
        legacy.getPositiveKeywords().add("性能");

        QueryPlanGateDecision decision = gatingService.decide(pr, legacy, null);
        assertFalse(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_LEGACY, decision.getSelectedSource());
    }

    @Test
    void assistModeShouldAllowWhenLegacyIncomplete() {
        properties.getUnderstanding().getPlanner().setMode("assist");
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");

        QueryAnalysisResult legacy = new QueryAnalysisResult();
        legacy.setCategory("数码电子");
        legacy.setSubCategory("笔记本电脑");

        QueryPlanGateDecision decision = gatingService.decide(pr, legacy, null);
        assertTrue(decision.isAllowed());
        assertEquals(QueryPlanGateDecision.SELECTED_PLANNER, decision.getSelectedSource());
    }

    @Test
    void takeoverModeShouldAllowWhenPlannerValid() {
        properties.getUnderstanding().getPlanner().setMode("takeover");
        QueryPlanningResult pr = createValidResult("数码电子", "笔记本电脑", 0.95, "PRODUCT_SEARCH");
        QueryPlanGateDecision decision = gatingService.decide(pr, null, null);
        assertTrue(decision.isAllowed());
    }

    static class FakeLlmClient implements LlmClient {
        private String response = "{}";

        void setResponse(String r) { this.response = r; }

        @Override
        public void streamGenerate(String prompt, Consumer<String> onText,
                                    Runnable onComplete, Consumer<Throwable> onError) {
            onText.accept(response);
            onComplete.run();
        }
    }
}
