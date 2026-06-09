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
import com.ecommerce.rag.rag.memory.ConversationState;

class LLMQueryPlannerContextRefinementTest {

    private LLMQueryPlanner planner;
    private FakeLlmClient fakeLlmClient;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("shadow");
        plannerProps.setTimeoutSeconds(5);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);

        fakeLlmClient = new FakeLlmClient();

        LLMQueryPlannerTest.FakeTaxonomyService taxonomyService = new LLMQueryPlannerTest.FakeTaxonomyService();
        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, new CartSemanticHintService());
        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(properties, catalog, matcher);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();

        planner = new LLMQueryPlanner(fakeLlmClient, properties, promptBuilder,
                jsonParser, validator, taxonomyService);
    }

    @Test
    void contextRefinementShouldInheritCategoryAndSetPriceMax() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u4e00\u4e07\u5143\u4ee5\u4e0b\u7684",
                  "normalizedQuery": "\u4e00\u4e07\u5143\u4ee5\u4e0b\u7684\u7b14\u8bb0\u672c\u7535\u8111",
                  "intent": "REFINE_PREVIOUS_QUERY",
                  "needsRetrieval": true,
                  "contextAction": "REFINE_PREVIOUS_SEARCH",
                  "target": {
                    "category": "\u6570\u7801\u7535\u5b50",
                    "subCategory": "\u7b14\u8bb0\u672c\u7535\u8111"
                  },
                  "price": {
                    "min": null,
                    "max": 10000,
                    "currency": "CNY",
                    "strict": true
                  },
                  "brands": { "include": [], "exclude": [] },
                  "softKeywords": ["\u7a0b\u5e8f\u5458","\u7f16\u7a0b","\u5f00\u53d1"],
                  "queryVariants": [
                    "\u4e00\u4e07\u5143\u4ee5\u4e0b\u9002\u5408\u7a0b\u5e8f\u5458\u7684\u7b14\u8bb0\u672c\u7535\u8111"
                  ],
                  "source": "LLM"
                }""");

        ConversationState state = new ConversationState("s1");
        state.setTurnCount(2);
        state.setLastUserQuery("推荐几款适合程序员的电脑");
        state.setCategory("数码电子");
        state.setSubCategory("笔记本电脑");
        state.setPositiveKeywords(List.of("程序员", "编程", "开发"));

        QueryPlanningResult result = planner.plan("一万元以下的", state, null);

        assertTrue(result.getParseSuccess());
        QueryPlan validated = result.getValidatedPlan();
        assertNotNull(validated);
        assertNotNull(validated.getTarget());
        assertEquals("REFINE_PREVIOUS_QUERY", validated.getIntent());
        assertEquals("REFINE_PREVIOUS_SEARCH", validated.getContextAction());
        assertEquals("数码电子", validated.getTarget().getCategory());
        assertEquals("笔记本电脑", validated.getTarget().getSubCategory());
        assertNotNull(validated.getPrice());
        assertEquals(new BigDecimal("10000"), validated.getPrice().getMax());
        assertTrue(validated.getSoftKeywords().contains("程序员"));
        assertTrue(validated.getSoftKeywords().contains("开发"));

        assertEquals(QueryPlanningResult.SOURCE_LLM, result.getSource());
    }

    @Test
    void validationResultShouldShowMatchedFlags() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u4e00\u4e07\u5143\u4ee5\u4e0b\u7684",
                  "intent": "REFINE_PREVIOUS_QUERY",
                  "contextAction": "REFINE_PREVIOUS_SEARCH",
                  "target": {
                    "category": "\u6570\u7801\u7535\u5b50",
                    "subCategory": "\u7b14\u8bb0\u672c\u7535\u8111"
                  },
                  "price": { "max": 10000, "currency": "CNY", "strict": true },
                  "softKeywords": ["\u7a0b\u5e8f\u5458"],
                  "source": "LLM"
                }""");

        ConversationState state = new ConversationState("s1");
        state.setTurnCount(2);
        state.setCategory("数码电子");
        state.setSubCategory("笔记本电脑");

        QueryPlanningResult result = planner.plan("一万元以下的", state, null);

        assertNotNull(result.getValidationResult());
        assertTrue(result.getValidationResult().getCategoryMatched());
        assertTrue(result.getValidationResult().getSubCategoryMatched());
    }

    @Test
    void firstRoundQueryShouldNotLeakContext() {
        fakeLlmClient.setResponse("""
                {
                  "originalQuery": "\u63a8\u8350\u7535\u8111",
                  "intent": "PRODUCT_SEARCH",
                  "contextAction": "NEW_SEARCH",
                  "target": {
                    "category": "\u6570\u7801\u7535\u5b50",
                    "subCategory": "\u7b14\u8bb0\u672c\u7535\u8111"
                  },
                  "price": { "currency": "CNY", "strict": true },
                  "softKeywords": ["\u7a0b\u5e8f\u5458"],
                  "source": "LLM"
                }""");

        ConversationState emptyState = new ConversationState("s2");
        emptyState.setTurnCount(0);

        QueryPlanningResult result = planner.plan("推荐电脑", emptyState, null);

        assertTrue(result.getParseSuccess());
        QueryPlan validated = result.getValidatedPlan();
        assertNotNull(validated);
        assertEquals("PRODUCT_SEARCH", validated.getIntent());
        assertEquals("数码电子", validated.getTarget().getCategory());
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
}
