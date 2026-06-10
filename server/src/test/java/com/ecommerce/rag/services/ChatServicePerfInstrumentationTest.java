package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.response.RecommendationCountResolver;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.ProductCardSafetyFilter;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.CartTopUpRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatServicePerfInstrumentationTest {

    @Mock HybridCandidateRetriever retriever;
    @Mock RagPromptBuilder promptBuilder;
    @Mock LlmClient llmClient;
    @Mock RetrievalRouter retrievalRouter;
    @Mock ConversationMemoryService memoryService;
    @Mock QueryAnalyzer queryAnalyzer;
    @Mock PageContextResolver pageContextResolver;
    @Mock StrictProductConstraintFilter constraintFilter;
    @Mock ProductService productService;
    @Mock RecommendationCountResolver countResolver;
    @Mock QueryUnderstandingService queryUnderstandingService;
    @Mock CartService cartService;
    @Mock CartTopUpRecommendationService cartTopUpRecommendationService;
    @Mock com.ecommerce.rag.rag.retriever.NoMatchRecoveryService noMatchRecoveryService;
    @Mock ProductCardSafetyFilter cardSafetyFilter;
    @Mock com.ecommerce.rag.services.recommendation.RecommendationReasonService recommendationReasonService;

    ChatService chatService;
    PerformanceTraceService perfService;
    AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        AppProperties.ChatProperties chatProps = new AppProperties.ChatProperties();
        chatProps.setMockLlmEnabled(true);
        chatProps.setMaxProductCardLimit(5);
        appProperties.setChat(chatProps);
        appProperties.setRetrieval(new AppProperties.RetrievalProperties());
        appProperties.getRetrieval().setMode("hybrid");
        appProperties.setUnderstanding(new AppProperties.UnderstandingProperties());
        appProperties.getUnderstanding().setPlanner(new AppProperties.PlannerProperties());
        appProperties.getUnderstanding().getPlanner().setEnabled(false);

        AppProperties.PerfProperties perfProps = new AppProperties.PerfProperties();
        perfProps.setEnabled(true);
        perfProps.setLogEnabled(false);
        perfProps.setRecentEnabled(true);
        perfProps.setRecentSize(10);
        perfProps.setSlowThresholdMs(3000);
        appProperties.setPerf(perfProps);

        perfService = new PerformanceTraceService(appProperties);

        chatService = new ChatService(
                retriever, promptBuilder, llmClient, appProperties,
                new ObjectMapper(), retrievalRouter, memoryService, queryAnalyzer,
                pageContextResolver, constraintFilter, productService, countResolver,
                queryUnderstandingService, cartService, cartTopUpRecommendationService,
                perfService, noMatchRecoveryService, cardSafetyFilter, recommendationReasonService);
    }

    @Test
    void testNormalChatProducesTrace() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("perf-test-1");
        request.setLimit(3);

        when(retrievalRouter.route(anyString())).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "product search"));

        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        understanding.setEffectiveAnalysis(analysis);
        when(queryUnderstandingService.understandForRetrieval(anyString(), any(), any()))
                .thenReturn(understanding);

        when(pageContextResolver.resolve(any())).thenReturn(new PageContextResolution());
        when(countResolver.resolve(anyString())).thenReturn(3);
        when(retriever.retrieveWithAnalysis(anyString(), anyInt(), any()))
                .thenReturn(Collections.emptyList());
        when(promptBuilder.build(anyString(), anyList(), any(), anyString()))
                .thenReturn("prompt");

        doAnswer(inv -> {
            Consumer<String> onText = inv.getArgument(1);
            Runnable onComplete = inv.getArgument(2);
            onText.accept("hi");
            onComplete.run();
            return null;
        }).when(llmClient).streamGenerate(anyString(), any(), any(), any());

        SseEmitter emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());
        assertNotNull(emitter);

        Thread.sleep(500);

        List<Map<String, Object>> recent = perfService.getRecentTraceSummaries(10);
        assertEquals(1, recent.size());
        Map<String, Object> trace = recent.get(0);
        assertEquals("/api/chat/stream", trace.get("endpoint"));
        assertEquals("perf-test-1", trace.get("session_id"));
    }

    @Test
    void testCartSummaryBypassesRag() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("购物车多少钱");
        request.setSessionId("perf-cart-1");

        when(retrievalRouter.route(anyString())).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.CART_SUMMARY, false, "cart summary"));

        SseEmitter emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());
        assertNotNull(emitter);

        Thread.sleep(500);

        List<Map<String, Object>> recent = perfService.getRecentTraceSummaries(10);
        assertEquals(1, recent.size());
        Map<String, Object> trace = recent.get(0);
        assertEquals("/api/chat/stream", trace.get("endpoint"));
    }

    @Test
    void testAddToCartProducesTrace() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("加入购物车 p1");
        request.setSessionId("perf-add-1");

        when(retrievalRouter.route(anyString())).thenReturn(
                new RetrievalRouteResult(RetrievalIntent.ADD_TO_CART, false, "add to cart"));

        SseEmitter emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());
        assertNotNull(emitter);

        Thread.sleep(500);

        List<Map<String, Object>> recent = perfService.getRecentTraceSummaries(10);
        assertEquals(1, recent.size());
    }
}
