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
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.router.RetrievalRouter;

class QueryUnderstandingServiceTakeoverTest {

    private QueryUnderstandingService service;
    private AppProperties properties;
    private FakeLlmClient fakeLlmClient;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        plannerProps.setTimeoutSeconds(5);
        plannerProps.setMinConfidence(0.85);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);

        fakeLlmClient = new FakeLlmClient();
        LLMQueryPlanner planner = buildPlanner(fakeLlmClient, properties);

        FakeMemoryService memoryService = new FakeMemoryService();
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer();
        RetrievalRouter retrievalRouter = new RetrievalRouter();
        QueryPlanGatingService gatingService = new QueryPlanGatingService(properties);
        QueryPlanToAnalysisMapper mapper = new QueryPlanToAnalysisMapper();
        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, new CartSemanticHintService());

        service = new QueryUnderstandingService(queryAnalyzer, retrievalRouter, memoryService,
                null, planner, gatingService, mapper, properties, matcher,
                new com.ecommerce.rag.rag.memory.ConversationContextMerger());
    }

    private LLMQueryPlanner buildPlanner(FakeLlmClient fakeLlm, AppProperties props) {
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

        AppProperties plannerProps = new AppProperties();
        AppProperties.PlannerProperties pp = new AppProperties.PlannerProperties();
        pp.setEnabled(true);
        AppProperties.UnderstandingProperties up = new AppProperties.UnderstandingProperties();
        up.setPlanner(pp);
        plannerProps.setUnderstanding(up);

        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, new CartSemanticHintService());
        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(plannerProps, catalog, matcher);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();
        return new LLMQueryPlanner(fakeLlm, plannerProps, promptBuilder, jsonParser, validator, taxonomyService);
    }

    @Test
    void disabledEffectiveAnalysisShouldBeLegacy() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertEquals("DISABLED", result.getSelectedSource());
    }

    @Test
    void shadowEffectiveAnalysisShouldBeLegacy() {
        properties.getUnderstanding().getPlanner().setMode("shadow");
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.95}");
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertFalse(result.getPlannerUsedForRetrieval());
    }

    @Test
    void plannerErrorEffectiveAnalysisShouldBeLegacy() {
        fakeLlmClient.setThrowOnGenerate(true);
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertFalse(result.getPlannerUsedForRetrieval());
    }

    @Test
    void highConfidenceLegalPlanShouldUsePlanner() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.95,\"target\":{\"category\":\"\u6570\u7801\u7535\u5b50\",\"subCategory\":\"\u7b14\u8bb0\u672c\u7535\u8111\"}}");
        QueryUnderstandingResult result = service.understand("推荐电脑", "s1", null);
        assertTrue(result.getPlannerUsedForRetrieval());
        assertEquals("PLANNER", result.getSelectedSource());
    }

    @Test
    void lowConfidenceShouldFallbackLegacy() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.5,\"target\":{\"category\":\"\u6570\u7801\u7535\u5b50\",\"subCategory\":\"\u7b14\u8bb0\u672c\u7535\u8111\"}}");
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertFalse(result.getPlannerUsedForRetrieval());
    }

    @Test
    void assistModeCompleteLegacyShouldFallback() {
        properties.getUnderstanding().getPlanner().setMode("assist");
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.95,\"target\":{\"category\":\"\u6570\u7801\u7535\u5b50\",\"subCategory\":\"\u7b14\u8bb0\u672c\u7535\u8111\"}}");
        QueryAnalysisResult result = service.understand("推荐洗面奶", "s1", null).getEffectiveAnalysis();
        assertNotNull(result);
    }

    @Test
    void selectedSourceShouldBeCorrect() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertEquals("DISABLED", result.getSelectedSource());
    }

    static class FakeLlmClient implements LlmClient {
        private String response = "{}";
        private boolean throwOnGenerate = false;

        void setResponse(String r) { this.response = r; }
        void setThrowOnGenerate(boolean t) { this.throwOnGenerate = t; }

        @Override
        public void streamGenerate(String prompt, Consumer<String> onText,
                                    Runnable onComplete, Consumer<Throwable> onError) {
            if (throwOnGenerate) {
                onError.accept(new RuntimeException("fake error"));
                return;
            }
            onText.accept(response);
            onComplete.run();
        }
    }

    static class FakeMemoryService implements ConversationMemoryService {
        @Override
        public ConversationState getOrCreate(String sessionId) {
            return new ConversationState(sessionId);
        }

        @Override
        public void updateAfterRetrieval(String sessionId, String query,
                                          QueryAnalysisResult analysis, List<com.ecommerce.rag.models.dto.ChatCandidate> candidates) {}

        @Override
        public void updateAfterNoMatch(String sessionId, QueryAnalysisResult analysis, boolean recovered) {}

        @Override
        public void save(String sessionId, ConversationState state) {}

        @Override
        public void clearSession(String sessionId) {}

        @Override
        public boolean hasSession(String sessionId) { return false; }

        @Override
        public ConversationState getState(String sessionId) { return new ConversationState(sessionId); }

        @Override
        public String buildMemoryKey(String sessionId, String authenticatedUserId) {
            return sessionId != null && !sessionId.isBlank() ? sessionId : "default";
        }
    }
}
