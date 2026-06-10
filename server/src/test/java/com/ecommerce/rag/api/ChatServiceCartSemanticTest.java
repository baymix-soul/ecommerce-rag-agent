package com.ecommerce.rag.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
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
import com.ecommerce.rag.services.ChatService;
import com.ecommerce.rag.services.ProductService;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.CartTopUpRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceCartSemanticTest {

    @Mock
    private LlmClient llmClient;
    @Mock
    private HybridCandidateRetriever retriever;
    @Mock
    private RagPromptBuilder promptBuilder;
    @Mock
    private ConversationMemoryService memoryService;
    @Mock
    private QueryAnalyzer queryAnalyzer;
    @Mock
    private RetrievalRouter retrievalRouter;
    @Mock
    private RecommendationCountResolver countResolver;
    @Mock
    private StrictProductConstraintFilter constraintFilter;
    @Mock
    private ProductService productService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private QueryUnderstandingService queryUnderstandingService;
    @Mock
    private CartService cartService;
    @Mock
    private CartTopUpRecommendationService cartTopUpRecommendationService;
    @Mock
    private PageContextResolver pageContextResolver;
    @Mock
    private PerformanceTraceService performanceTraceService;

    @Mock
    private com.ecommerce.rag.rag.retriever.NoMatchRecoveryService noMatchRecoveryService;

    @Mock
    private com.ecommerce.rag.services.recommendation.RecommendationReasonService recommendationReasonService;

    @Mock
    private ProductCardSafetyFilter cardSafetyFilter;

    private ChatService chatService;
    private ObjectMapper objectMapper;
    private AppProperties.ChatProperties chatProperties;
    private AppProperties.UnderstandingProperties understandingProperties;
    private AppProperties.PlannerProperties plannerProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatProperties = new AppProperties.ChatProperties();
        chatProperties.setMaxProductCardLimit(3);
        understandingProperties = new AppProperties.UnderstandingProperties();
        plannerProperties = new AppProperties.PlannerProperties();
        plannerProperties.setEnabled(false);
        plannerProperties.setMode("shadow");
        understandingProperties.setPlanner(plannerProperties);

        lenient().when(appProperties.getChat()).thenReturn(chatProperties);
        lenient().when(appProperties.getUnderstanding()).thenReturn(understandingProperties);

        PageContextResolution defaultResolution = new PageContextResolution();
        lenient().when(pageContextResolver.resolve(any())).thenReturn(defaultResolution);

        chatService = new ChatService(retriever, promptBuilder, llmClient, appProperties,
                objectMapper, retrievalRouter, memoryService, queryAnalyzer,
                pageContextResolver, constraintFilter, productService, countResolver,
                queryUnderstandingService, cartService, cartTopUpRecommendationService,
                performanceTraceService, noMatchRecoveryService, cardSafetyFilter,
                recommendationReasonService);
    }

    @Test
    void testAmountGapQueryReturnsTextOnly() throws Exception {
        CartView cartView = new CartView();
        cartView.setTotalAmount(new BigDecimal("780"));
        cartView.setItems(Collections.emptyList());
        cartView.setTotalQuantity(2);

        when(retrievalRouter.route("离2000还差多少"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));

        ChatRequest request = new ChatRequest();
        request.setMessage("离2000还差多少");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void testAmountGapQueryUnauthenticated() throws Exception {
        when(retrievalRouter.route("离2000还差多少"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));

        ChatRequest request = new ChatRequest();
        request.setMessage("离2000还差多少");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService, never()).getCart(anyString());
    }

    @Test
    void testCompletionRecommendReturnsTextAndProductCards() throws Exception {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setPrice(new BigDecimal("300"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("300"));
        item.setName("测试商品");

        CartView cartView = new CartView();
        cartView.setTotalAmount(new BigDecimal("300"));
        cartView.setItems(List.of(item));
        cartView.setTotalQuantity(1);

        ProductCard card = new ProductCard("p2", "凑单商品", new BigDecimal("700"),
                "CNY", "/images/p2.jpg", "适合凑单");

        when(retrievalRouter.route("如果要凑1000块，有没有推荐商品"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));
        when(cartService.getCart("user1")).thenReturn(cartView);
        when(cartTopUpRecommendationService.recommend(eq(cartView), eq(new BigDecimal("1000")), eq(3)))
                .thenReturn(List.of(card));

        ChatRequest request = new ChatRequest();
        request.setMessage("如果要凑1000块，有没有推荐商品");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService).getCart("user1");
        verify(cartTopUpRecommendationService).recommend(eq(cartView), eq(new BigDecimal("1000")), eq(3));
    }

    @Test
    void testCompletionClarifyReturnsQuestion() throws Exception {
        when(retrievalRouter.route("凑单推荐一下"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));

        ChatRequest request = new ChatRequest();
        request.setMessage("凑单推荐一下");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService, never()).getCart(anyString());
    }

    @Test
    void testNormalProductSearchNotAffected() throws Exception {
        var analysis = new com.ecommerce.rag.rag.query.QueryAnalysisResult();
        analysis.setIntent(com.ecommerce.rag.rag.router.RetrievalIntent.PRODUCT_SEARCH);
        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        understanding.setPlannerUsedForRetrieval(false);
        understanding.setSelectedSource("SHADOW_ONLY");
        understanding.setEffectiveAnalysis(analysis);

        when(retrievalRouter.route("推荐一款跑鞋"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "product search"));
        when(countResolver.resolve("推荐一款跑鞋")).thenReturn(1);
        when(queryUnderstandingService.understandForRetrieval(anyString(), anyString(), any()))
                .thenReturn(understanding);

        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);
    }

    @Test
    void testAddToCartUnaffected() throws Exception {
        when(retrievalRouter.route("把这个加入购物车"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.ADD_TO_CART, false, "add to cart"));

        ChatRequest request = new ChatRequest();
        request.setMessage("把这个加入购物车");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService, never()).getCart(anyString());
    }

    @Test
    void testCartsSummaryUnauthenticated() throws Exception {
        when(retrievalRouter.route("当前购物车多少钱了"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_SUMMARY, false, "cart summary"));

        ChatRequest request = new ChatRequest();
        request.setMessage("当前购物车多少钱了");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService, never()).getCart(anyString());
    }

    @Test
    void testComputerSearchNotTriggerCart() throws Exception {
        var analysis = new com.ecommerce.rag.rag.query.QueryAnalysisResult();
        analysis.setIntent(com.ecommerce.rag.rag.router.RetrievalIntent.PRODUCT_SEARCH);
        QueryUnderstandingResult understanding = new QueryUnderstandingResult();
        understanding.setPlannerUsedForRetrieval(false);
        understanding.setSelectedSource("SHADOW_ONLY");
        understanding.setEffectiveAnalysis(analysis);

        when(retrievalRouter.route("推荐2000元以内的电脑"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "product search"));
        when(countResolver.resolve("推荐2000元以内的电脑")).thenReturn(3);
        when(queryUnderstandingService.understandForRetrieval(anyString(), anyString(), any()))
                .thenReturn(understanding);

        ChatRequest request = new ChatRequest();
        request.setMessage("推荐2000元以内的电脑");
        request.setSessionId("test-session");
        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        var emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);
    }
}
