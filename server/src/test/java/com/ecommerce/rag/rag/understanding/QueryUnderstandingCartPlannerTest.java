package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;

@ExtendWith(MockitoExtension.class)
class QueryUnderstandingCartPlannerTest {

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
    private AppProperties appProperties;

    @Mock
    private CartSemanticFrameMatcher cartFrameMatcher;

    @InjectMocks
    private QueryUnderstandingService service;

    private ConversationState state;
    private QueryAnalysisResult legacyAnalysis;

    @BeforeEach
    void setUp() {
        state = new ConversationState();

        legacyAnalysis = new QueryAnalysisResult();
        legacyAnalysis.setOriginalQuery("test");
        legacyAnalysis.setIntent(RetrievalIntent.PRODUCT_SEARCH);
    }

    private void setupCommonMocks() {
        when(memoryService.getOrCreate(anyString())).thenReturn(state);
        when(retrievalRouter.route(anyString())).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "test"));
        when(queryAnalyzer.analyze(anyString(), any(), any())).thenReturn(legacyAnalysis);
    }

    private QueryPlan buildCartSummaryPlan() {
        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_SUMMARY);
        plan.setConfidence(0.94);
        plan.setNeedsRetrieval(false);
        CartPlan cartPlan = new CartPlan();
        cartPlan.setAction(CartPlan.ACTION_CART_SUMMARY);
        cartPlan.setNeedsCart(true);
        cartPlan.setNeedsRecommendation(false);
        plan.setCart(cartPlan);
        return plan;
    }

    private QueryPlanningResult buildPlanningResult(QueryPlan plan) {
        QueryPlanningResult planningResult = new QueryPlanningResult();
        planningResult.setValidatedPlan(plan);
        planningResult.setPlannerEnabled(true);
        planningResult.setParseSuccess(true);
        planningResult.setValid(true);
        planningResult.setSource(QueryPlanningResult.SOURCE_LLM);
        QueryPlanValidationResult validation = new QueryPlanValidationResult();
        validation.setValid(true);
        planningResult.setValidationResult(validation);
        return planningResult;
    }

    private QueryPlanGateDecision buildAllowedGateDecision() {
        QueryPlanGateDecision gateDecision = new QueryPlanGateDecision();
        gateDecision.setAllowed(true);
        gateDecision.setSelectedSource(QueryPlanGateDecision.SELECTED_PLANNER);
        gateDecision.setConfidence(0.94);
        return gateDecision;
    }

    @Test
    void testCartSummaryFromPlanner() {
        setupCommonMocks();

        QueryPlan plan = buildCartSummaryPlan();
        QueryPlanningResult planningResult = buildPlanningResult(plan);
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);

        QueryPlanGateDecision gateDecision = buildAllowedGateDecision();
        when(gatingService.decide(any(), any(), any())).thenReturn(gateDecision);

        QueryAnalysisResult plannerAnalysis = new QueryAnalysisResult();
        plannerAnalysis.setIntent(RetrievalIntent.CART_SUMMARY);
        when(mapper.map(any(), any())).thenReturn(plannerAnalysis);

        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        understandingProps.setPlanner(plannerProps);
        when(appProperties.getUnderstanding()).thenReturn(understandingProps);

        QueryUnderstandingResult result = service.understand("购物车多少钱了", "s1", null);

        assertNotNull(result);
        assertEquals("CART_SUMMARY", result.getValidatedPlan().getIntent());
    }

    @Test
    void testCartTopUpFromPlanner() {
        setupCommonMocks();

        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        plan.setConfidence(0.92);
        plan.setNeedsRetrieval(true);
        CartPlan cartPlan = new CartPlan();
        cartPlan.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cartPlan.setTargetAmount(new BigDecimal("1000"));
        cartPlan.setNeedsCart(true);
        cartPlan.setNeedsRecommendation(true);
        plan.setCart(cartPlan);

        QueryPlanningResult planningResult = buildPlanningResult(plan);
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);

        QueryPlanGateDecision gateDecision = buildAllowedGateDecision();
        when(gatingService.decide(any(), any(), any())).thenReturn(gateDecision);

        QueryAnalysisResult plannerAnalysis = new QueryAnalysisResult();
        plannerAnalysis.setIntent(RetrievalIntent.CART_TOP_UP);
        when(mapper.map(any(), any())).thenReturn(plannerAnalysis);

        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        understandingProps.setPlanner(plannerProps);
        when(appProperties.getUnderstanding()).thenReturn(understandingProps);

        QueryUnderstandingResult result = service.understand("帮我凑到1000块", "s1", null);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000"), result.getValidatedPlan().getCart().getTargetAmount());
    }

    @Test
    void testPlannerFailureDoesNotCause500() {
        setupCommonMocks();

        when(llmQueryPlanner.plan(anyString(), any(), any()))
                .thenThrow(new RuntimeException("LLM unavailable"));

        QueryPlanGateDecision gateDecision = new QueryPlanGateDecision();
        gateDecision.setAllowed(false);
        gateDecision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
        when(gatingService.decide(any(), any(), any())).thenReturn(gateDecision);

        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        understandingProps.setPlanner(plannerProps);
        when(appProperties.getUnderstanding()).thenReturn(understandingProps);

        QueryUnderstandingResult result = service.understand("test", "s1", null);

        assertNotNull(result);
        assertNotEquals("PLANNER", result.getSelectedSource());
    }

    @Test
    void testPlannerDisabledStrongHintFallback() {
        setupCommonMocks();

        when(llmQueryPlanner.plan(anyString(), any(), any()))
                .thenReturn(QueryPlanningResult.disabled());

        QueryPlanGateDecision gateDecision = new QueryPlanGateDecision();
        gateDecision.setAllowed(false);
        gateDecision.setSelectedSource(QueryPlanGateDecision.SELECTED_DISABLED);
        when(gatingService.decide(any(), any(), any())).thenReturn(gateDecision);

        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(false);
        plannerProps.setMode("disabled");
        understandingProps.setPlanner(plannerProps);
        when(appProperties.getUnderstanding()).thenReturn(understandingProps);

        QueryUnderstandingResult result = service.understand("购物车里有什么", "s1", null);

        assertNotNull(result);
        assertFalse(result.getPlannerUsedForRetrieval());
    }

    @Test
    void testSelectedSourceMarkedCorrectly() {
        setupCommonMocks();

        QueryPlan plan = buildCartSummaryPlan();
        QueryPlanningResult planningResult = buildPlanningResult(plan);
        when(llmQueryPlanner.plan(anyString(), any(), any())).thenReturn(planningResult);

        QueryPlanGateDecision gateDecision = buildAllowedGateDecision();
        when(gatingService.decide(any(), any(), any())).thenReturn(gateDecision);

        QueryAnalysisResult plannerAnalysis = new QueryAnalysisResult();
        plannerAnalysis.setIntent(RetrievalIntent.CART_SUMMARY);
        when(mapper.map(any(), any())).thenReturn(plannerAnalysis);

        AppProperties.UnderstandingProperties understandingProps = new AppProperties.UnderstandingProperties();
        AppProperties.PlannerProperties plannerProps = new AppProperties.PlannerProperties();
        plannerProps.setEnabled(true);
        plannerProps.setMode("takeover");
        understandingProps.setPlanner(plannerProps);
        when(appProperties.getUnderstanding()).thenReturn(understandingProps);

        QueryUnderstandingResult result = service.understand("购物车多少钱了", "s1", null);

        assertTrue(result.getPlannerUsedForRetrieval());
        assertEquals("PLANNER", result.getSelectedSource());
    }
}
