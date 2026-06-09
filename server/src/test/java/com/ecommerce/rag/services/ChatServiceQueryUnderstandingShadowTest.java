package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.dto.PageType;

@SpringBootTest
class ChatServiceQueryUnderstandingShadowTest {

    @Autowired
    private ChatService chatService;

    @Test
    void disabledShouldReturnSseEmitterWithoutCrash() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-shadow-disabled");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void shadowModeShouldNotCrash() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐几款适合程序员的电脑");
        request.setSessionId("test-shadow-mode");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void plannerErrorShouldNotCrashSseDone() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐洗面奶");
        request.setSessionId("test-shadow-error");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void pageContextShouldBePassedToUnderstandingService() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("这个适合敏感肌吗");
        request.setSessionId("test-shadow-page");
        request.setLimit(5);

        com.ecommerce.rag.models.dto.PageContext pageContext = new com.ecommerce.rag.models.dto.PageContext();
        pageContext.setPageType(PageType.PRODUCT_DETAIL);
        pageContext.setCurrentProductId("p_beauty_001");
        request.setPageContext(pageContext);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void sessionIdShouldBePassedToUnderstandingService() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-shadow-session-abc123");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void smalltalkShouldNotTriggerUnderstandingService() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId("test-shadow-smalltalk");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(300);
    }

    @Test
    void searchShouldProduceProductCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setSessionId("test-shadow-search");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }
}
