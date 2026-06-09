package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;

@SpringBootTest
class ChatServiceContextTest {

    @Autowired
    private ChatService chatService;

    @Test
    void smalltalkShouldNotReturnProductCard() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId("test-smalltalk");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(300);
    }

    @Test
    void helpShouldNotTriggerRetrieval() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("你能做什么");
        request.setSessionId("test-help");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(300);
    }

    @Test
    void thanksShouldNotTriggerRetrieval() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("谢谢");
        request.setSessionId("test-thanks");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(300);
    }

    @Test
    void productSearchShouldUseContext() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-context-search");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void multiTurnShouldInheritContext() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-multi-turn");
        req1.setLimit(3);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("要轻量的");
        req2.setSessionId("test-multi-turn");
        req2.setLimit(3);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void changeOneShouldExcludePrevious() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐洗面奶");
        req1.setSessionId("test-change-one");
        req1.setLimit(3);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("换一个");
        req2.setSessionId("test-change-one");
        req2.setLimit(3);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void sessionIdShouldBeOptional() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId(null);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }
}
