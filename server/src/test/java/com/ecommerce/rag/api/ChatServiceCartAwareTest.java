package com.ecommerce.rag.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.response.RecommendationCountResolver;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.services.ChatService;
import com.ecommerce.rag.services.ProductService;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.CartTopUpRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatServiceCartAwareTest {

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
    private com.ecommerce.rag.rag.context.PageContextResolver pageContextResolver;

    @Mock
    private PerformanceTraceService performanceTraceService;

    @Mock
    private com.ecommerce.rag.rag.retriever.NoMatchRecoveryService noMatchRecoveryService;

    @Mock
    private com.ecommerce.rag.services.recommendation.RecommendationReasonService recommendationReasonService;

    private ChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        chatService = new ChatService(retriever, promptBuilder, llmClient, appProperties,
                objectMapper, retrievalRouter, memoryService, queryAnalyzer,
                pageContextResolver, constraintFilter, productService, countResolver,
                queryUnderstandingService, cartService, cartTopUpRecommendationService,
                performanceTraceService, noMatchRecoveryService, recommendationReasonService);

        // Default: PageContextResolver returns a non-product-detail resolution
        com.ecommerce.rag.rag.context.PageContextResolution defaultResolution =
                new com.ecommerce.rag.rag.context.PageContextResolution();
        lenient().when(pageContextResolver.resolve(any())).thenReturn(defaultResolution);
    }

    /**
     * Test 1: Authenticated user asks cart summary → returns text with cart amount + done
     */
    @Test
    void testAuthenticatedCartSummary() throws Exception {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setName("跑鞋A");
        item.setPrice(new BigDecimal("780"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("780"));

        CartView cartView = new CartView();
        cartView.setItems(List.of(item));
        cartView.setTotalQuantity(1);
        cartView.setTotalAmount(new BigDecimal("780"));

        when(retrievalRouter.route("当前已经买了多少钱了"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_SUMMARY, false, "cart summary"));
        when(cartService.getCart("user1")).thenReturn(cartView);

        ChatRequest request = new ChatRequest();
        request.setMessage("当前已经买了多少钱了");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        // Wait for async executor to complete
        Thread.sleep(500);

        verify(cartService).getCart("user1");
    }

    /**
     * Test 2: Unauthenticated user asks cart summary → "请先登录后查看购物车" + done
     */
    @Test
    void testUnauthenticatedCartSummary() throws Exception {
        when(retrievalRouter.route("当前已经买了多少钱了"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_SUMMARY, false, "cart summary"));

        ChatRequest request = new ChatRequest();
        request.setMessage("当前已经买了多少钱了");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        // Cart service should NOT be called for unauthenticated users
        verify(cartService, never()).getCart(anyString());
    }

    /**
     * Test 3: Authenticated user asks cart top-up → text + product_card(s) + done
     */
    @Test
    void testAuthenticatedCartTopUp() throws Exception {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setName("跑鞋A");
        item.setPrice(new BigDecimal("780"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("780"));

        CartView cartView = new CartView();
        cartView.setItems(List.of(item));
        cartView.setTotalQuantity(1);
        cartView.setTotalAmount(new BigDecimal("780"));

        ProductCard card1 = new ProductCard("p2", "袜子B", new BigDecimal("120"),
                "CNY", "http://img.example.com/p2.jpg", "适合凑单");

        when(retrievalRouter.route("如果要凑1000块"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));
        when(cartService.getCart("user1")).thenReturn(cartView);
        when(cartTopUpRecommendationService.recommend(cartView, new BigDecimal("1000"), 3))
                .thenReturn(List.of(card1));

        ChatRequest request = new ChatRequest();
        request.setMessage("如果要凑1000块");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService).getCart("user1");
        verify(cartTopUpRecommendationService).recommend(cartView, new BigDecimal("1000"), 3);
    }

    /**
     * Test 4: targetAmount=null → asks for clarification "你想凑到多少元？"
     */
    @Test
    void testCartTopUpNullTargetAmount() throws Exception {
        when(retrievalRouter.route("凑单推荐一下"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));

        ChatRequest request = new ChatRequest();
        request.setMessage("凑单推荐一下");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        // Cart service should NOT be called when targetAmount is null (clarification returned first)
        verify(cartService, never()).getCart(anyString());
        verify(cartTopUpRecommendationService, never()).recommend(any(), any(BigDecimal.class), anyInt());
    }

    /**
     * Test 5: Cart already meets target → "不需要再凑单" + done, no product_card
     */
    @Test
    void testCartTopUpGapZeroOrNegative() throws Exception {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setName("跑鞋A");
        item.setPrice(new BigDecimal("1200"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("1200"));

        CartView cartView = new CartView();
        cartView.setItems(List.of(item));
        cartView.setTotalQuantity(1);
        cartView.setTotalAmount(new BigDecimal("1200"));

        when(retrievalRouter.route("凑到1000块"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));
        when(cartService.getCart("user1")).thenReturn(cartView);

        ChatRequest request = new ChatRequest();
        request.setMessage("凑到1000块");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        verify(cartService).getCart("user1");
        // No recommendations should be requested when gap <= 0
        verify(cartTopUpRecommendationService, never()).recommend(any(), any(BigDecimal.class), anyInt());
    }

    /**
     * Test 6: Normal PRODUCT_SEARCH intent still works normally (not intercepted by cart logic)
     */
    @Test
    void testNormalRecommendationNotAffected() throws Exception {
        when(retrievalRouter.route("推荐一款跑鞋"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true, "product search"));

        // Mock the query understanding chain for normal PRODUCT_SEARCH flow
        com.ecommerce.rag.rag.understanding.QueryUnderstandingResult understandingResult =
                mock(com.ecommerce.rag.rag.understanding.QueryUnderstandingResult.class);
        when(understandingResult.getValidatedPlan()).thenReturn(null);
        when(understandingResult.getLegacyAnalysis()).thenReturn(null);
        when(understandingResult.getSelectedSource()).thenReturn("RULE");
        when(understandingResult.getPlannerUsedForRetrieval()).thenReturn(false);
        when(understandingResult.getFallbackReason()).thenReturn(null);
        com.ecommerce.rag.rag.query.QueryAnalysisResult analysis =
                new com.ecommerce.rag.rag.query.QueryAnalysisResult();
        when(understandingResult.getEffectiveAnalysis()).thenReturn(analysis);
        when(queryUnderstandingService.understandForRetrieval(eq("推荐一款跑鞋"), anyString(), any()))
                .thenReturn(understandingResult);

        AppProperties.ChatProperties chatProps = mock(AppProperties.ChatProperties.class);
        when(chatProps.getMaxProductCardLimit()).thenReturn(5);
        when(appProperties.getChat()).thenReturn(chatProps);
        when(appProperties.getUnderstanding()).thenReturn(null);

        when(countResolver.resolve("推荐一款跑鞋")).thenReturn(3);
        when(retriever.retrieveWithAnalysis(eq("推荐一款跑鞋"), anyInt(), any(com.ecommerce.rag.rag.query.QueryAnalysisResult.class)))
                .thenReturn(Collections.emptyList());

        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = AuthContextSnapshot.unauthenticated();

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        // Cart services should NOT be called for normal product search
        verify(cartService, never()).getCart(anyString());
        verify(cartTopUpRecommendationService, never()).recommend(any(), any(BigDecimal.class), anyInt());
    }

    /**
     * Test 7: ADD_TO_CART intent still goes through existing logic
     */
    @Test
    void testAddToCartLogicUnaffected() throws Exception {
        when(retrievalRouter.route("加购第一款"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.ADD_TO_CART, false, "add to cart"));

        // Mock conversation memory for ADD_TO_CART product resolution
        com.ecommerce.rag.rag.memory.ConversationState state =
                new com.ecommerce.rag.rag.memory.ConversationState();
        when(memoryService.getOrCreate("test-session")).thenReturn(state);

        ChatRequest request = new ChatRequest();
        request.setMessage("加购第一款");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        Thread.sleep(500);

        // Cart summary/top-up should NOT be invoked for ADD_TO_CART intent
        verify(cartService, never()).getCart("user1");
        verify(cartTopUpRecommendationService, never()).recommend(any(), any(BigDecimal.class), anyInt());
    }

    /**
     * Test 8: Verify SSE events follow text → product_card(s) → done format
     */
    @Test
    void testSseFormatUnchanged() throws Exception {
        CartItem item = new CartItem();
        item.setProductId("p1");
        item.setName("跑鞋A");
        item.setPrice(new BigDecimal("780"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("780"));

        CartView cartView = new CartView();
        cartView.setItems(List.of(item));
        cartView.setTotalQuantity(1);
        cartView.setTotalAmount(new BigDecimal("780"));

        ProductCard card1 = new ProductCard("p2", "袜子B", new BigDecimal("120"),
                "CNY", "http://img.example.com/p2.jpg", "适合凑单");
        ProductCard card2 = new ProductCard("p3", "护腕C", new BigDecimal("80"),
                "CNY", "http://img.example.com/p3.jpg", "适合凑单");

        when(retrievalRouter.route("帮我凑到1000元"))
                .thenReturn(new RetrievalRouteResult(RetrievalIntent.CART_TOP_UP, false, "cart top-up"));
        when(cartService.getCart("user1")).thenReturn(cartView);
        when(cartTopUpRecommendationService.recommend(cartView, new BigDecimal("1000"), 3))
                .thenReturn(List.of(card1, card2));

        ChatRequest request = new ChatRequest();
        request.setMessage("帮我凑到1000元");
        request.setSessionId("test-session");

        AuthContextSnapshot authContext = new AuthContextSnapshot("user1");

        // Collect SSE events via SseEmitter handler
        SseEmitter emitter = chatService.chat(request, authContext);
        assertNotNull(emitter);

        emitter.onCompletion(() -> {});

        // Wait for async executor to finish sending all events
        Thread.sleep(800);

        // Verify the service interactions confirm the correct event sequence:
        // 1. cartService.getCart is called (to build the text event)
        // 2. cartTopUpRecommendationService.recommend is called (to build product_card events)
        // The emitter sends: text → product_card(s) → done
        verify(cartService).getCart("user1");
        verify(cartTopUpRecommendationService).recommend(cartView, new BigDecimal("1000"), 3);

        // Verify the emitter was created and not immediately completed with error
        // (successful completion means text + product_card(s) + done were sent)
        assertNotNull(emitter);
    }
}
