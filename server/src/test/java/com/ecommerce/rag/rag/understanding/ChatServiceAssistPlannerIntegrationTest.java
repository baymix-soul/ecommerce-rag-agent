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

class ChatServiceAssistPlannerIntegrationTest {

    private QueryUnderstandingService service;
    private AppProperties properties;
    private FakeMemoryService memoryService;
    private FakeLlmClient fakeLlmClient;
    private CatalogTaxonomyService taxonomyService;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("assist");
        plannerProps.setTimeoutSeconds(5);
        plannerProps.setMinConfidence(0.85);
        plannerProps.setFallbackOnUnknownCategory(true);
        plannerProps.setFallbackOnUnknownSubCategory(true);
        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        understandingProps.setPlanner(plannerProps);
        properties.setUnderstanding(understandingProps);

        fakeLlmClient = new FakeLlmClient();

        CatalogTaxonomySnapshot snap = new CatalogTaxonomySnapshot();
        snap.setCategories(List.of("数码电子", "美妆护肤", "服饰运动"));
        snap.setAllSubCategories(List.of("笔记本电脑", "洁面", "跑步鞋"));
        snap.setSubCategoriesByCategory(Map.of(
                "数码电子", List.of("笔记本电脑"),
                "美妆护肤", List.of("洁面"),
                "服饰运动", List.of("跑步鞋")));
        snap.setBrands(List.of("Apple", "华为", "Nike"));
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
        LLMQueryPlanner planner = new LLMQueryPlanner(fakeLlmClient, properties, promptBuilder, jsonParser, validator, taxonomyService);

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
    void assistModeLegacyIncompletePlannerValidShouldUsePlannerEffectiveAnalysis() {
        String plannerJson = "{"
                + "\"originalQuery\":\"推荐几款适合程序员的电脑\","
                + "\"intent\":\"PRODUCT_SEARCH\","
                + "\"needsRetrieval\":true,"
                + "\"confidence\":0.9,"
                + "\"target\":{\"category\":\"数码电子\",\"subCategory\":\"笔记本电脑\"},"
                + "\"softKeywords\":[\"程序员\",\"编程\",\"开发\",\"大内存\"],"
                + "\"queryVariants\":[\"适合程序员的笔记本电脑\"]"
                + "}";
        fakeLlmClient.setResponse(plannerJson);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款适合程序员的电脑", "s1", null);

        assertNotNull(result.getEffectiveAnalysis());
        assertTrue(result.getPlannerUsedForRetrieval());
        assertEquals("PLANNER", result.getSelectedSource());
        assertEquals("数码电子", result.getEffectiveAnalysis().getCategory());
        assertEquals("笔记本电脑", result.getEffectiveAnalysis().getSubCategory());
        assertFalse(result.getEffectiveAnalysis().getPositiveKeywords().isEmpty());
        assertTrue(result.getEffectiveAnalysis().getPositiveKeywords().contains("程序员"));
        assertTrue(result.getEffectiveAnalysis().getSoftKeywords().contains("程序员"));
    }

    @Test
    void assistModePriceRefinementShouldInheritAndAddMaxPrice() {
        ConversationState prevState = new ConversationState("s2");
        prevState.setCategory("数码电子");
        prevState.setSubCategory("笔记本电脑");
        prevState.setTurnCount(1);
        memoryService.setPreExistingState("s2", prevState);

        String plannerJson = "{"
                + "\"originalQuery\":\"一万元以下的\","
                + "\"intent\":\"REFINE_PREVIOUS_QUERY\","
                + "\"needsRetrieval\":true,"
                + "\"confidence\":0.92,"
                + "\"contextAction\":\"REFINE_PREVIOUS_SEARCH\","
                + "\"target\":{\"category\":\"数码电子\",\"subCategory\":\"笔记本电脑\"},"
                + "\"price\":{\"max\":10000,\"currency\":\"CNY\",\"strict\":true},"
                + "\"queryVariants\":[\"10000元以内笔记本电脑\"]"
                + "}";
        fakeLlmClient.setResponse(plannerJson);

        QueryUnderstandingResult result = service.understandForRetrieval("一万元以下的", "s2", null);

        assertNotNull(result.getEffectiveAnalysis());
        assertTrue(result.getPlannerUsedForRetrieval());
        assertEquals("PLANNER", result.getSelectedSource());
        assertEquals("数码电子", result.getEffectiveAnalysis().getCategory());
        assertEquals("笔记本电脑", result.getEffectiveAnalysis().getSubCategory());
        assertNotNull(result.getEffectiveAnalysis().getMaxPrice());
        assertEquals(new BigDecimal("10000"), result.getEffectiveAnalysis().getMaxPrice());
    }

    @Test
    void assistModeLowConfidenceShouldFallbackLegacy() {
        properties.getUnderstanding().getPlanner().setMinConfidence(0.99);

        String plannerJson = "{"
                + "\"originalQuery\":\"推荐几款适合程序员的电脑\","
                + "\"intent\":\"PRODUCT_SEARCH\","
                + "\"confidence\":0.88,"
                + "\"target\":{\"category\":\"数码电子\",\"subCategory\":\"笔记本电脑\"}"
                + "}";
        fakeLlmClient.setResponse(plannerJson);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款适合程序员的电脑", "s1", null);

        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertTrue(result.getSelectedSource().contains("FALLBACK") || result.getSelectedSource().contains("LEGACY"));
    }

    @Test
    void assistModePlannerErrorShouldFallbackLegacy() {
        fakeLlmClient.setThrowOnGenerate(true);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款适合程序员的电脑", "s1", null);

        assertNotNull(result.getEffectiveAnalysis());
        assertFalse(result.getPlannerUsedForRetrieval());
        assertEquals("ERROR", result.getPlanningResult().getSource());
    }

    @Test
    void assistModeLegacyCompleteShouldPreferLegacy() {
        ConversationState prevState = new ConversationState("s3");
        prevState.setTurnCount(1);
        memoryService.setPreExistingState("s3", prevState);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款跑鞋", "s3", null);

        assertNotNull(result.getEffectiveAnalysis());
        assertEquals("服饰运动", result.getEffectiveAnalysis().getCategory());
        assertEquals("跑步鞋", result.getEffectiveAnalysis().getSubCategory());
    }

    @Test
    void assistModePlannerUsedForRetrievalFlagCorrectWhenTakeover() {
        String plannerJson = "{"
                + "\"originalQuery\":\"推荐几款适合程序员的电脑\","
                + "\"intent\":\"PRODUCT_SEARCH\","
                + "\"confidence\":0.9,"
                + "\"target\":{\"category\":\"数码电子\",\"subCategory\":\"笔记本电脑\"},"
                + "\"softKeywords\":[\"程序员\",\"编程\"]"
                + "}";
        fakeLlmClient.setResponse(plannerJson);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款适合程序员的电脑", "s1", null);

        assertNotNull(result.getPlannerUsedForRetrieval());
        assertTrue(result.getPlannerUsedForRetrieval());
    }

    @Test
    void assistModeSelectedSourceCorrect() {
        String plannerJson = "{"
                + "\"originalQuery\":\"推荐几款适合程序员的电脑\","
                + "\"intent\":\"PRODUCT_SEARCH\","
                + "\"confidence\":0.9,"
                + "\"target\":{\"category\":\"数码电子\",\"subCategory\":\"笔记本电脑\"},"
                + "\"softKeywords\":[\"程序员\",\"编程\"]"
                + "}";
        fakeLlmClient.setResponse(plannerJson);

        QueryUnderstandingResult result = service.understandForRetrieval("推荐几款适合程序员的电脑", "s1", null);

        assertEquals("PLANNER", result.getSelectedSource());
    }

    static class FakeMemoryService implements ConversationMemoryService {
        private final java.util.Map<String, ConversationState> states = new java.util.concurrent.ConcurrentHashMap<>();

        void setPreExistingState(String sessionId, ConversationState state) {
            states.put(sessionId != null && !sessionId.isBlank() ? sessionId : "default", state);
        }

        @Override
        public ConversationState getOrCreate(String sessionId) {
            String sid = sessionId != null && !sessionId.isBlank() ? sessionId : "default";
            return states.computeIfAbsent(sid, k -> {
                ConversationState state = new ConversationState(k);
                state.setTurnCount(0);
                return state;
            });
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
            String sid = sessionId != null && !sessionId.isBlank() ? sessionId : "default";
            return states.get(sid);
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
                onError.accept(new RuntimeException("fake LLM error"));
                return;
            }
            onText.accept(response);
            onComplete.run();
        }
    }
}
