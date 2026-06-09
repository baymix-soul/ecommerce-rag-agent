package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class QueryUnderstandingServiceTest {

    private QueryUnderstandingService service;
    private AppProperties properties;
    private FakeMemoryService memoryService;
    private FakeLlmClient fakeLlmClient;
    private LLMQueryPlanner planner;
    private CatalogTaxonomyService taxonomyService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        plannerProps.setTimeoutSeconds(5);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);

        fakeLlmClient = new FakeLlmClient();

        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(List.of("数码电子"));
        snap.setAllSubCategories(List.of("笔记本电脑"));
        snap.setSubCategoriesByCategory(Map.of("数码电子", List.of("笔记本电脑")));
        snap.setBrands(List.of("Apple", "华为"));
        snap.setFilterableFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_FILTERABLE_FIELDS));
        snap.setTextFields(new ArrayList<>(CatalogTaxonomySnapshot.DEFAULT_TEXT_FIELDS));

        taxonomyService = new CatalogTaxonomyService(null) {
            @Override
            public CatalogTaxonomySnapshot getSnapshot() { return snap; }
        };

        CartSemanticFrameCatalog catalog = new CartSemanticFrameCatalog();
        CartSemanticFrameMatcher matcher = new CartSemanticFrameMatcher(catalog, new CartSemanticHintService());
        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(properties, catalog, matcher);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();
        planner = new LLMQueryPlanner(fakeLlmClient, properties, promptBuilder, jsonParser, validator, taxonomyService);

        memoryService = new FakeMemoryService();

        QueryAnalyzer queryAnalyzer = new QueryAnalyzer();
        RetrievalRouter retrievalRouter = new RetrievalRouter();
        QueryPlanGatingService gatingService = new QueryPlanGatingService(properties);
        QueryPlanToAnalysisMapper mapper = new QueryPlanToAnalysisMapper();

        service = new QueryUnderstandingService(queryAnalyzer, retrievalRouter, memoryService,
                null, planner, gatingService, mapper, properties, matcher,
                new com.ecommerce.rag.rag.memory.ConversationContextMerger());
    }

    @Test
    void shouldReturnLegacyAnalysis() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.9}");
        QueryUnderstandingResult result = service.understand("推荐洗面奶", "s1", null);
        assertNotNull(result);
        assertNotNull(result.getLegacyAnalysis());
        assertNotNull(result.getEffectiveAnalysis());
    }

    @Test
    void shouldReturnPlanningResult() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"\u63a8\u8350\u6d17\u9762\u5976\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.9}");
        QueryUnderstandingResult result = service.understand("推荐洗面奶", "s1", null);
        assertNotNull(result.getPlanningResult());
    }

    @Test
    void plannerUsedForRetrievalShouldBeSet() {
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.9}");
        QueryUnderstandingResult result = service.understand("test", "s1", null);
        assertNotNull(result.getPlannerUsedForRetrieval());
    }

    @Test
    void legacyAnalysisShouldWorkWhenPlannerDisabled() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryUnderstandingResult result = service.understand("推荐电脑", "s1", null);
        assertNotNull(result.getLegacyAnalysis());
        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertEquals("DISABLED", result.getSelectedSource());
    }

    @Test
    void plannerErrorShouldNotAffectLegacyAnalysis() {
        fakeLlmClient.setThrowOnGenerate(true);
        QueryUnderstandingResult result = service.understand("test", null, null);
        assertNotNull(result.getLegacyAnalysis());
        assertNotNull(result.getPlanningResult());
        assertNotNull(result.getEffectiveAnalysis());
    }

    static class FakeMemoryService implements ConversationMemoryService {
        @Override
        public ConversationState getOrCreate(String sessionId) {
            ConversationState state = new ConversationState(sessionId);
            state.setTurnCount(0);
            return state;
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
        public ConversationState getState(String sessionId) {
            return new ConversationState(sessionId);
        }

        @Override
        public String buildMemoryKey(String sessionId, String authenticatedUserId) {
            return sessionId != null && !sessionId.isBlank() ? sessionId : "default";
        }
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
}
