package com.ecommerce.rag.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.response.RecommendationCountResolver;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.NoMatchRecoveryResult;
import com.ecommerce.rag.rag.retriever.NoMatchRecoveryService;
import com.ecommerce.rag.rag.retriever.RetrievedProductCandidate;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.CartTopUpRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;

class ChatServiceContextNoMatchRecoveryTest {

    private HybridCandidateRetriever retriever;
    private RagPromptBuilder promptBuilder;
    private LlmClient llmClient;
    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private RetrievalRouter retrievalRouter;
    private ConversationMemoryService memoryService;
    private QueryAnalyzer queryAnalyzer;
    private PageContextResolver pageContextResolver;
    private StrictProductConstraintFilter constraintFilter;
    private ProductService productService;
    private RecommendationCountResolver countResolver;
    private QueryUnderstandingService queryUnderstandingService;
    private CartService cartService;
    private CartTopUpRecommendationService cartTopUpRecommendationService;
    private PerformanceTraceService perfService;
    private NoMatchRecoveryService noMatchRecoveryService;
    private com.ecommerce.rag.services.recommendation.RecommendationReasonService recommendationReasonService;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        retriever = mock(HybridCandidateRetriever.class);
        promptBuilder = mock(RagPromptBuilder.class);
        llmClient = mock(LlmClient.class);
        appProperties = mock(AppProperties.class);
        objectMapper = new ObjectMapper();
        retrievalRouter = mock(RetrievalRouter.class);
        memoryService = mock(ConversationMemoryService.class);
        queryAnalyzer = mock(QueryAnalyzer.class);
        pageContextResolver = mock(PageContextResolver.class);
        constraintFilter = mock(StrictProductConstraintFilter.class);
        productService = mock(ProductService.class);
        countResolver = mock(RecommendationCountResolver.class);
        queryUnderstandingService = mock(QueryUnderstandingService.class);
        cartService = mock(CartService.class);
        cartTopUpRecommendationService = mock(CartTopUpRecommendationService.class);
        perfService = mock(PerformanceTraceService.class);
        noMatchRecoveryService = mock(NoMatchRecoveryService.class);
        recommendationReasonService = mock(com.ecommerce.rag.services.recommendation.RecommendationReasonService.class);

        when(appProperties.getChat()).thenReturn(mock(AppProperties.ChatProperties.class));
        when(appProperties.getChat().getMaxProductCardLimit()).thenReturn(10);
        when(appProperties.getRetrieval()).thenReturn(mock(AppProperties.RetrievalProperties.class));
        when(appProperties.getRetrieval().getMode()).thenReturn("hybrid");
        when(appProperties.getUnderstanding()).thenReturn(mock(AppProperties.UnderstandingProperties.class));
        when(appProperties.getUnderstanding().getPlanner()).thenReturn(mock(AppProperties.PlannerProperties.class));
        when(appProperties.getUnderstanding().getPlanner().isEnabled()).thenReturn(false);
        when(appProperties.getUnderstanding().getPlanner().getMode()).thenReturn("disabled");

        when(perfService.beginTrace(any(), any(), any())).thenReturn(mock(com.ecommerce.rag.core.perf.PerfTrace.class));
        doNothing().when(perfService).finishTrace(any());
        doNothing().when(perfService).finishTraceWithError(any(), any());

        chatService = new ChatService(
                retriever, promptBuilder, llmClient, appProperties, objectMapper,
                retrievalRouter, memoryService, queryAnalyzer, pageContextResolver,
                constraintFilter, productService, countResolver, queryUnderstandingService,
                cartService, cartTopUpRecommendationService, perfService, noMatchRecoveryService,
                recommendationReasonService);
    }

    @Test
    void testNoMatchRecoveryReturnsRelaxedCandidates() {
        String sessionId = "ctx-v2-1";
        String message = "一千块以下的";

        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);

        when(retrievalRouter.route(message)).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "search"));
        when(countResolver.resolve(message)).thenReturn(3);
        when(pageContextResolver.resolve(any())).thenReturn(new PageContextResolution());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));
        analysis.setSoftKeywords(List.of("轻量"));

        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        understanding.setEffectiveAnalysis(analysis);
        understanding.setSelectedSource("LEGACY");
        understanding.setPlannerUsedForRetrieval(false);

        com.ecommerce.rag.rag.memory.ActiveSearchContext activeContext = new com.ecommerce.rag.rag.memory.ActiveSearchContext(sessionId);
        activeContext.setCategory("服饰运动");
        activeContext.setSubCategory("跑步鞋");
        activeContext.setMaxPrice(new BigDecimal("1000"));
        activeContext.setSoftPreferences(new ArrayList<>(List.of("轻量")));
        understanding.setActiveSearchContext(activeContext);

        when(queryUnderstandingService.understandForRetrieval(any(), any(), any())).thenReturn(understanding);

        // First retrieval returns empty (exact match fails)
        when(retriever.retrieveWithAnalysis(any(), anyInt(), any())).thenReturn(List.of());
        when(retriever.getLastRetrievedCandidates()).thenReturn(List.of());

        ChatCandidate relaxed = new ChatCandidate();
        relaxed.setProductId("P001");
        relaxed.setName("跑鞋");
        relaxed.setPrice(new BigDecimal("899"));
        relaxed.setCategory("服饰运动");
        relaxed.setSubCategory("跑步鞋");

        NoMatchRecoveryResult recovery = NoMatchRecoveryResult.recovered(
                NoMatchRecoveryService.RECOVERY_TYPE_RELAX_SOFT,
                List.of(relaxed),
                "同时满足'轻量'和您的其他条件的商品暂时没有。",
                List.of("轻量"));

        when(noMatchRecoveryService.tryRecover(any(), any(), any(), anyInt())).thenReturn(recovery);

        when(promptBuilder.build(any(), any(), any(), any())).thenReturn("prompt");

        var emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());

        assertNotNull(emitter);
        verify(noMatchRecoveryService).tryRecover(any(), any(), any(), anyInt());
        verify(memoryService).updateAfterNoMatch(eq(sessionId), any(), eq(true));
    }

    @Test
    void testNoMatchDoesNotClearActiveContext() {
        String sessionId = "ctx-v2-2";
        String message = "预算1元以内";

        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);

        when(retrievalRouter.route(message)).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "search"));
        when(countResolver.resolve(message)).thenReturn(3);
        when(pageContextResolver.resolve(any())).thenReturn(new PageContextResolution());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1"));

        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        understanding.setEffectiveAnalysis(analysis);
        understanding.setSelectedSource("LEGACY");

        com.ecommerce.rag.rag.memory.ActiveSearchContext activeContext = new com.ecommerce.rag.rag.memory.ActiveSearchContext(sessionId);
        activeContext.setCategory("服饰运动");
        activeContext.setSubCategory("跑步鞋");
        activeContext.setMaxPrice(new BigDecimal("1"));
        understanding.setActiveSearchContext(activeContext);

        when(queryUnderstandingService.understandForRetrieval(any(), any(), any())).thenReturn(understanding);
        when(retriever.retrieveWithAnalysis(any(), anyInt(), any())).thenReturn(List.of());
        when(retriever.getLastRetrievedCandidates()).thenReturn(List.of());

        NoMatchRecoveryResult recovery = NoMatchRecoveryResult.notRecovered("当前没有1元以下的跑鞋");
        when(noMatchRecoveryService.tryRecover(any(), any(), any(), anyInt())).thenReturn(recovery);

        when(promptBuilder.build(any(), any(), any(), any())).thenReturn("prompt");

        var emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());

        assertNotNull(emitter);
        verify(memoryService).updateAfterNoMatch(eq(sessionId), any(), eq(false));
        verify(memoryService, never()).updateAfterRetrieval(eq(sessionId), any(), any(), any());
    }

    @Test
    void testNoMatchWithNegativeBrandDoesNotRecommendExcluded() {
        String sessionId = "ctx-v2-3";
        String message = "不要耐克 一千块以下的";

        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setSessionId(sessionId);

        when(retrievalRouter.route(message)).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "search"));
        when(countResolver.resolve(message)).thenReturn(3);
        when(pageContextResolver.resolve(any())).thenReturn(new PageContextResolution());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));
        analysis.setNegativeBrands(List.of("Nike"));

        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        understanding.setEffectiveAnalysis(analysis);
        understanding.setSelectedSource("LEGACY");

        com.ecommerce.rag.rag.memory.ActiveSearchContext activeContext = new com.ecommerce.rag.rag.memory.ActiveSearchContext(sessionId);
        activeContext.setCategory("服饰运动");
        activeContext.setSubCategory("跑步鞋");
        activeContext.setMaxPrice(new BigDecimal("1000"));
        activeContext.setNegativeBrands(new ArrayList<>(List.of("Nike")));
        understanding.setActiveSearchContext(activeContext);

        when(queryUnderstandingService.understandForRetrieval(any(), any(), any())).thenReturn(understanding);
        when(retriever.retrieveWithAnalysis(any(), anyInt(), any())).thenReturn(List.of());
        when(retriever.getLastRetrievedCandidates()).thenReturn(List.of());

        // Recovery should fail because negative brand is hard constraint
        NoMatchRecoveryResult recovery = NoMatchRecoveryResult.notRecovered("没有符合条件的商品");
        when(noMatchRecoveryService.tryRecover(any(), any(), any(), anyInt())).thenReturn(recovery);

        when(promptBuilder.build(any(), any(), any(), any())).thenReturn("prompt");

        var emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());

        assertNotNull(emitter);
        verify(noMatchRecoveryService).tryRecover(any(), any(), any(), anyInt());
    }
}
