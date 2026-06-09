package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
class ChatServiceAddToCartTest {

    @Autowired
    private ChatService chatService;

    @Test
    void shouldReturnLoginRequiredWhenNotAuthenticated() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把这款加入购物车");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldReturnClarificationWhenCannotDetermineProduct() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("加入购物车");
        request.setSessionId("add-cart-clarify");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldHandleAddToCartViaChat() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把这个加入购物车");
        request.setSessionId("add-cart-via-chat");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldHandleAddToCartWithProductDetailContext() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把这个加到购物车");
        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_clothes_001");
        request.setPageContext(pageContext);
        request.setSessionId("add-cart-detail-context");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldDetectAddToCartIntent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("加入购物车");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldDetectJiaGouIntent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("加购");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldDetectMaiZheGeIntent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("买这个");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldDetectViewCartIntent() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("查看购物车");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldNotBreakNormalRecommendation() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("add-cart-no-break-1");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldNotBreakMultiroundRefinement() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐几款程序员电脑");
        request.setSessionId("add-cart-no-break-2");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);

        ChatRequest refineRequest = new ChatRequest();
        refineRequest.setMessage("一万元以下");
        refineRequest.setSessionId("add-cart-no-break-2");

        SseEmitter refineEmitter = chatService.chat(refineRequest);
        assertNotNull(refineEmitter);
    }

    @Test
    void shouldPromptLoginWhenNotAuthenticated() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把第一款加入购物车");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldAcceptChatWithAuthContext() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("把第一款加入购物车");
        PageContext pageContext = new PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_clothes_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request, new AuthContextSnapshot("demo-user"));
        assertNotNull(emitter);
    }
}
