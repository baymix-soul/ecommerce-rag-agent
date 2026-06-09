package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.core.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryUnderstandingCartAssistTest {

    @Mock
    private QueryAnalyzer queryAnalyzer;

    @Mock
    private RetrievalRouter retrievalRouter;

    @Mock
    private ConversationMemoryService memoryService;

    @Mock
    private PageContextResolver pageContextResolver;

    @Mock
    private LLMQueryPlanner llmQueryPlanner;

    @Mock
    private QueryPlanGatingService gatingService;

    @Mock
    private QueryPlanToAnalysisMapper mapper;

    @Mock
    private com.ecommerce.rag.rag.memory.ConversationContextMerger conversationContextMerger;

    private AppProperties appProperties;

    private CartSemanticFrameCatalog catalog;
    private CartSemanticFrameMatcher matcher;
    private ConversationState state;
    private QueryAnalysisResult legacyAnalysis;
    private QueryUnderstandingService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        catalog = new CartSemanticFrameCatalog();
        matcher = new CartSemanticFrameMatcher(catalog, null);
        state = new ConversationState();
        legacyAnalysis = new QueryAnalysisResult();
        legacyAnalysis.setOriginalQuery("test");
        legacyAnalysis.setIntent(RetrievalIntent.PRODUCT_SEARCH);

        service = new QueryUnderstandingService(
                queryAnalyzer, retrievalRouter, memoryService, pageContextResolver,
                llmQueryPlanner, gatingService, mapper, appProperties, matcher,
                conversationContextMerger);
    }

    private void setupCommonMocks() {
        when(memoryService.getOrCreate(anyString())).thenReturn(state);
        when(retrievalRouter.route(anyString()))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "test"));
        when(queryAnalyzer.analyze(anyString(), any(), any())).thenReturn(legacyAnalysis);
    }

    @Test
    void testCartSemanticMatchResultPopulatedInResult() {
        setupCommonMocks();
        QueryPlanningResult planningResult = QueryPlanningResult.disabled();
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);
        when(gatingService.decide(any(), any(), any())).thenReturn(buildNotAllowedDecision());

        QueryUnderstandingResult result = service.understand("离2000还差多少", "test-session", null);

        assertNotNull(result.getCartSemanticMatch());
        assertTrue(result.getCartSemanticMatch().isCartRelated());
        assertEquals("cart.amount_gap_query", result.getCartSemanticMatch().getMatchedFrameId());
    }

    @Test
    void testNonCartQueryHasNOneMatchResult() {
        setupCommonMocks();
        QueryPlanningResult planningResult = QueryPlanningResult.disabled();
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);
        when(gatingService.decide(any(), any(), any())).thenReturn(buildNotAllowedDecision());

        QueryUnderstandingResult result = service.understand("推荐一款跑鞋", "test-session", null);

        assertNotNull(result.getCartSemanticMatch());
        assertFalse(result.getCartSemanticMatch().isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getCartSemanticMatch().getMatchLevel());
    }

    @Test
    void testComputerSearchNotTriggerCartPath() {
        setupCommonMocks();
        QueryPlanningResult planningResult = QueryPlanningResult.disabled();
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);
        when(gatingService.decide(any(), any(), any())).thenReturn(buildNotAllowedDecision());

        QueryUnderstandingResult result = service.understand("推荐2000元以内的电脑", "test-session", null);

        assertNotNull(result.getCartSemanticMatch());
        assertFalse(result.getCartSemanticMatch().isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getCartSemanticMatch().getMatchLevel());
    }

    @Test
    void testPlannerDisabledDoesNotBreakCartSemanticMatch() {
        setupCommonMocks();
        QueryPlanningResult planningResult = QueryPlanningResult.disabled();
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);
        when(gatingService.decide(any(), any(), any())).thenReturn(buildNotAllowedDecision());

        QueryUnderstandingResult result = service.understand("离2000还差多少", "test-session", null);

        assertFalse(result.getPlannerUsedForRetrieval());
        assertNotNull(result.getCartSemanticMatch());
        assertTrue(result.getCartSemanticMatch().isCartRelated());
    }

    @Test
    void testPlannerErrorStillReturnsCartMatch() {
        setupCommonMocks();
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenThrow(new RuntimeException("planner down"));
        when(gatingService.decide(any(), any(), any())).thenReturn(buildNotAllowedDecision());

        QueryUnderstandingResult result = service.understand("凑单推荐一下", "test-session", null);

        assertNotNull(result.getCartSemanticMatch());
        assertTrue(result.getCartSemanticMatch().isCartRelated());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("planner error")));
    }

    private QueryPlanGateDecision buildNotAllowedDecision() {
        QueryPlanGateDecision decision = new QueryPlanGateDecision();
        decision.setAllowed(false);
        decision.setSelectedSource(QueryPlanGateDecision.SELECTED_DISABLED);
        decision.setReason("planner disabled");
        return decision;
    }
}
