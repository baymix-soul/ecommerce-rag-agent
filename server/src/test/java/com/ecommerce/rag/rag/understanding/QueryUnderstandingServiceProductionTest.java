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
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.router.RetrievalRouter;

class QueryUnderstandingServiceProductionTest {

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
        plannerProps.setEnabled(false);
        plannerProps.setMode("shadow");
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

        QueryPlannerPromptBuilder promptBuilder = new QueryPlannerPromptBuilder(properties);
        QueryPlanJsonParser jsonParser = new QueryPlanJsonParser();
        QueryPlanValidator validator = new QueryPlanValidator();
        planner = new LLMQueryPlanner(fakeLlmClient, properties, promptBuilder, jsonParser, validator, taxonomyService);

        memoryService = new FakeMemoryService();

        QueryAnalyzer queryAnalyzer = new QueryAnalyzer();
        RetrievalRouter retrievalRouter = new RetrievalRouter();
        QueryPlanGatingService gatingService = new QueryPlanGatingService(properties);
        QueryPlanToAnalysisMapper mapper = new QueryPlanToAnalysisMapper();

        service = new QueryUnderstandingService(queryAnalyzer, retrievalRouter, memoryService,
                null, planner, gatingService, mapper, properties);
    }

    @Test
    void understandForRetrievalShouldReturnEffectiveAnalysisNonNull() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
    }

    @Test
    void understandForRetrievalDisabledShouldReturnLegacyAsEffective() {
        properties.getUnderstanding().getPlanner().setEnabled(false);
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertEquals("DISABLED", result.getSelectedSource());
    }

    @Test
    void understandForRetrievalShadowShouldReturnLegacyAsEffective() {
        properties.getUnderstanding().getPlanner().setEnabled(true);
        properties.getUnderstanding().getPlanner().setMode("shadow");
        fakeLlmClient.setResponse("{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\",\"confidence\":0.9}");
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertEquals("SHADOW_ONLY", result.getSelectedSource());
    }

    @Test
    void understandForRetrievalPlannerErrorShouldFallbackLegacy() {
        properties.getUnderstanding().getPlanner().setEnabled(true);
        properties.getUnderstanding().getPlanner().setMode("shadow");
        fakeLlmClient.setThrowOnGenerate(true);
        QueryUnderstandingResult result = service.understandForRetrieval("推荐电脑", "s1", null);
        assertNotNull(result.getEffectiveAnalysis());
        assertNotNull(result.getPlanningResult());
        assertEquals("ERROR", result.getPlanningResult().getSource());
        assertFalse(result.getPlannerUsedForRetrieval());
    }

    @Test
    void understandForRetrievalNullPageContextShouldNotThrow() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
    }

    @Test
    void understandForRetrievalNullSessionIdShouldNotThrow() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", null, null);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
    }

    @Test
    void understandForRetrievalEmptyQueryShouldNotThrow() {
        QueryUnderstandingResult result = service.understandForRetrieval("", "s1", null);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
        assertEquals("EMPTY_QUERY", result.getSelectedSource());
    }

    @Test
    void understandForRetrievalNullQueryShouldNotThrow() {
        QueryUnderstandingResult result = service.understandForRetrieval(null, "s1", null);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
        assertEquals("EMPTY_QUERY", result.getSelectedSource());
    }

    @Test
    void understandForRetrievalSelectedSourceShouldBeSet() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result.getSelectedSource());
    }

    @Test
    void understandForRetrievalEffectiveAnalysisShouldHaveOriginalQuery() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result.getEffectiveAnalysis().getOriginalQuery());
        assertEquals("推荐洗面奶", result.getEffectiveAnalysis().getOriginalQuery());
    }

    @Test
    void understandForRetrievalWithPageContextShouldNotThrow() {
        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_digital_001");
        QueryUnderstandingResult result = service.understandForRetrieval("这个适合程序员吗", "s1", pageContext);
        assertNotNull(result);
        assertNotNull(result.getEffectiveAnalysis());
    }

    @Test
    void understandForRetrievalShouldSetLegacyAnalysis() {
        QueryUnderstandingResult result = service.understandForRetrieval("推荐洗面奶", "s1", null);
        assertNotNull(result.getLegacyAnalysis());
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
        public void clearSession(String sessionId) {}

        @Override
        public boolean hasSession(String sessionId) { return false; }

        @Override
        public ConversationState getState(String sessionId) {
            return new ConversationState(sessionId);
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
