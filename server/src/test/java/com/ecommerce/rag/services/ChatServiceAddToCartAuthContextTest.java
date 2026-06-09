package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.services.auth.AuthService;
import com.ecommerce.rag.services.cart.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
class ChatServiceAddToCartAuthContextTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthService authService;

    @Test
    void shouldSucceedWhenAuthenticatedAndProductDetailContext() throws Exception {
        cartService.clearCart("demo-user");

        ChatRequest request = new ChatRequest();
        request.setMessage("把这个加到购物车");
        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_clothes_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request, new AuthContextSnapshot("demo-user"));
        assertNotNull(emitter);

        Thread.sleep(500);

        CartView cart = cartService.getCart("demo-user");
        assertEquals(1, cart.getTotalQuantity());
        assertEquals("p_clothes_001", cart.getItems().get(0).getProductId());

        cartService.clearCart("demo-user");
    }

    @Test
    void shouldPromptLoginWhenNotAuthenticated() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把第一款加入购物车");

        SseEmitter emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());
        assertNotNull(emitter);
    }

    @Test
    void shouldReturnClarificationWhenCannotDetermineProduct() throws Exception {
        cartService.clearCart("demo-user");

        ChatRequest request = new ChatRequest();
        request.setMessage("加入购物车");

        SseEmitter emitter = chatService.chat(request, new AuthContextSnapshot("demo-user"));
        assertNotNull(emitter);

        cartService.clearCart("demo-user");
    }

    @Test
    void shouldUseOrdinalFromRecommendedProducts() throws Exception {
        cartService.clearCart("demo-user");

        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐一款跑鞋");
        req1.setSessionId("ordinal-test");
        req1.setLimit(1);
        chatService.chat(req1);

        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("把第一款加入购物车");
        req2.setSessionId("ordinal-test");
        SseEmitter e2 = chatService.chat(req2, new AuthContextSnapshot("demo-user"));
        assertNotNull(e2);

        Thread.sleep(300);

        CartView cart = cartService.getCart("demo-user");
        assertTrue(cart.getTotalQuantity() > 0);

        cartService.clearCart("demo-user");
    }

    @Test
    void shouldNotBreakNormalRecommendation() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");

        SseEmitter emitter = chatService.chat(request, AuthContextSnapshot.unauthenticated());
        assertNotNull(emitter);
    }
}
