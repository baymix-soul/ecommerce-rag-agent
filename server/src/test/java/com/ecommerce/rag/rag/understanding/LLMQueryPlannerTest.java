package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class LLMQueryPlannerTest {

    private LLMQueryPlanner planner;
    private AppProperties properties;
    private CatalogTaxonomyService taxonomyService;
    private FakeLlmClient fakeLlmClient;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("shadow");
        plannerProps.setTimeoutSeconds(5);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);

        fakeLlmClient = new FakeLlmClient();

        taxonomyService = new FakeTaxonomyService();

        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(properties);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();

        planner = new LLMQueryPlanner(fakeLlmClient, properties, promptBuilder,
                jsonParser, validator, taxonomyService);
    }

    @Test
    void disabledShouldReturnDisabled() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryPlanningResult result = planner.plan("test", null, null);
        assertNotNull(result);
        assertFalse(result.getPlannerEnabled());
        assertEquals(QueryPlanningResult.SOURCE_DISABLED, result.getSource());
    }

    @Test
    void validLlmJsonShouldProduceValidatedPlan() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u63a8\u8350\u51e0\u6b3e\u9002\u5408\u7a0b\u5e8f\u5458\u7684\u7535\u8111",
                  "intent": "PRODUCT_SEARCH",
                  "needsRetrieval": true,
                  "target": {
                    "category": "\u6570\u7801\u7535\u5b50",
                    "subCategory": "\u7b14\u8bb0\u672c\u7535\u8111"
                  },
                  "price": {
                    "min": null,
                    "max": null,
                    "currency": "CNY",
                    "strict": true
                  },
                  "softKeywords": ["\u7a0b\u5e8f\u5458", "\u7f16\u7a0b", "\u5f00\u53d1"],
                  "queryVariants": ["\u9002\u5408\u7a0b\u5e8f\u5458\u7684\u7b14\u8bb0\u672c\u7535\u8111"],
                  "source": "LLM"
                }""");

        QueryPlanningResult result = planner.plan("推荐几款适合程序员的电脑", null, null);

        assertTrue(result.getPlannerEnabled());
        assertEquals(QueryPlanningResult.SOURCE_LLM, result.getSource());
        assertTrue(result.getParseSuccess());
        assertNotNull(result.getRawPlan());
        assertNotNull(result.getValidatedPlan());
        assertNotNull(result.getValidatedPlan().getTarget());
        assertEquals("数码电子", result.getValidatedPlan().getTarget().getCategory());
        assertEquals("笔记本电脑", result.getValidatedPlan().getTarget().getSubCategory());
        assertTrue(result.getValidatedPlan().getSoftKeywords().contains("程序员"));
    }

    @Test
    void llmOutputWithPriceConstraintShouldMapCorrectly() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u4e00\u4e07\u5143\u4ee5\u4e0b\u7684",
                  "intent": "REFINE_PREVIOUS_QUERY",
                  "contextAction": "REFINE_PREVIOUS_SEARCH",
                  "target": {
                    "category": "\u6570\u7801\u7535\u5b50",
                    "subCategory": "\u7b14\u8bb0\u672c\u7535\u8111"
                  },
                  "price": {
                    "max": 10000,
                    "currency": "CNY",
                    "strict": true
                  },
                  "source": "LLM"
                }""");

        QueryPlanningResult result = planner.plan("一万元以下的", null, null);

        assertTrue(result.getParseSuccess());
        assertNotNull(result.getValidatedPlan());
        assertEquals("REFINE_PREVIOUS_QUERY", result.getValidatedPlan().getIntent());
        assertNotNull(result.getValidatedPlan().getPrice());
        assertEquals(new BigDecimal("10000"), result.getValidatedPlan().getPrice().getMax());
    }

    @Test
    void invalidJsonShouldReturnError() {
        fakeLlmClient.setResponse("这不是合法的JSON");

        QueryPlanningResult result = planner.plan("test", null, null);

        assertEquals(QueryPlanningResult.SOURCE_ERROR, result.getSource());
        assertFalse(result.getParseSuccess());
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void llmExceptionShouldReturnError() {
        fakeLlmClient.setThrowOnGenerate(true);

        QueryPlanningResult result = planner.plan("test", null, null);

        assertEquals(QueryPlanningResult.SOURCE_ERROR, result.getSource());
        assertFalse(result.getParseSuccess());
    }

    @Test
    void validationWarningsShouldBePassedToResult() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u63a8\u8350",
                  "intent": "PRODUCT_SEARCH",
                  "target": {
                    "category": "\u6c7d\u8f66\u7528\u54c1"
                  },
                  "softKeywords": ["\u4ef7\u683c", "\u4ef7\u683c", "\u4ef7\u683c"],
                  "source": "LLM"
                }""");

        QueryPlanningResult result = planner.plan("推荐", null, null);

        assertTrue(result.getParseSuccess());
        assertNotNull(result.getValidatedPlan());
        assertNotNull(result.getWarnings());
    }

    @Test
    void latencyMsShouldBeSet() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\"}");
        QueryPlanningResult result = planner.plan("test", null, null);
        assertNotNull(result.getLatencyMs());
        assertTrue(result.getLatencyMs() >= 0);
    }

    static class FakeLlmClient implements LlmClient {
        private String response = "{}";
        private boolean throwOnGenerate = false;

        void setResponse(String response) { this.response = response; }
        void setThrowOnGenerate(boolean t) { this.throwOnGenerate = t; }

        @Override
        public void streamGenerate(String prompt, Consumer<String> onText,
                                    Runnable onComplete, Consumer<Throwable> onError) {
            if (throwOnGenerate) {
                onError.accept(new RuntimeException("fake LLM error"));
                return;
            }
            onText.accept(response);
            onComplete.run();
        }
    }

    static class FakeTaxonomyService extends CatalogTaxonomyService {
        FakeTaxonomyService() {
            super(null);
        }

        @Override
        public CatalogTaxonomySnapshot getSnapshot() {
            CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
            snap.setCategories(List.of("美妆护肤", "数码电子", "服饰运动", "食品饮料"));
            snap.setAllSubCategories(List.of("笔记本电脑", "智能手机", "真无线耳机", "跑步鞋", "背包", "洁面", "精华"));
            snap.setSubCategoriesByCategory(Map.of(
                    "数码电子", List.of("笔记本电脑", "智能手机", "真无线耳机"),
                    "服饰运动", List.of("跑步鞋", "背包")
            ));
            snap.setBrands(List.of("Apple", "Nike", "华为"));
            snap.setMinPrice(new BigDecimal("29"));
            snap.setMaxPrice(new BigDecimal("12999"));
            snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
            snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));
            return snap;
        }
    }
}
