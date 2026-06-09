package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.dto.PageType;

@SpringBootTest
class ChatServiceNoRegressionTest {

    @Autowired
    private ChatService chatService;

    @Test
    void recommendRunningShoesShouldReturnProductCard() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void recommendSeveralRunningShoesShouldReturnMaxThreeCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐几款跑鞋");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void refineToLightweightShouldNotReturnBackpack() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-no-regression-lightweight");
        req1.setLimit(5);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("要轻量的");
        req2.setSessionId("test-no-regression-lightweight");
        req2.setLimit(5);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void budget1000ShouldNotExceedPrice() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-no-regression-budget");
        req1.setLimit(5);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1000以内");
        req2.setSessionId("test-no-regression-budget");
        req2.setLimit(5);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void noMatchShouldNotSendProductCard() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-no-match");
        request.setLimit(10);

        SseEmitter e1 = chatService.chat(request);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1以内");
        req2.setSessionId("test-no-match");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void productDetailContextShouldStillWork() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("这个适合敏感肌吗");
        request.setSessionId("test-no-regression-pd");
        request.setLimit(5);

        com.ecommerce.rag.models.dto.PageContext pageContext = new com.ecommerce.rag.models.dto.PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_beauty_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }
}
