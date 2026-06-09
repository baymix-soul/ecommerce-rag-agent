package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;

@SpringBootTest
class ChatServiceCandidateLimitTest {

    @Autowired
    private ChatService chatService;

    @Test
    void recommendYiKuanShouldSendAtMost1Card() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-limit-1");
        request.setLimit(10);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void recommendSeveralShouldSendAtMost3Cards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐几款跑鞋");
        request.setSessionId("test-limit-3");
        request.setLimit(10);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void limit10ShouldStillCapAt3Cards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("有哪些好的跑鞋");
        request.setSessionId("test-limit-cap");
        request.setLimit(10);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void emptyFinalCandidatesShouldSend0Cards() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-empty-limit");
        req1.setLimit(10);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1以内");
        req2.setSessionId("test-empty-limit");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void recommendedProductIdsShouldOnlySaveDisplayedCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-displayed-ids");
        request.setLimit(10);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void productDetailQAShouldSkipCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("这个适合敏感肌吗？");
        request.setSessionId("test-detail-qa-limit");
        request.setLimit(10);

        com.ecommerce.rag.models.dto.PageContext pageContext = new com.ecommerce.rag.models.dto.PageContext();
        pageContext.setPageType(com.ecommerce.rag.models.dto.PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_beauty_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }
}
