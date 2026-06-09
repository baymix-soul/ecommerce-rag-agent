package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;

@SpringBootTest
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    @Test
    void shouldReturnSseEmitterForValidRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款适合油皮的洗面奶");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldReturnErrorEmitterForEmptyMessage() {
        ChatRequest request = new ChatRequest();
        request.setMessage("");

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldReturnErrorEmitterForNullMessage() {
        ChatRequest request = new ChatRequest();
        request.setMessage(null);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
    }

    @Test
    void shouldHandleChatWithCandidates() throws InterruptedException {
        ChatRequest request = new ChatRequest();
        request.setMessage("洗面奶");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void shouldHandleChatWithNoCandidates() throws InterruptedException {
        ChatRequest request = new ChatRequest();
        request.setMessage("zzzzz_nonexistent_product_xxxxx");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void shouldUseDefaultLimitWhenNotSet() {
        ChatRequest request = new ChatRequest();
        request.setMessage("测试");
        assertEquals(5, request.getEffectiveLimit());
    }

    @Test
    void shouldCapLimitAt10() {
        ChatRequest request = new ChatRequest();
        request.setMessage("测试");
        request.setLimit(50);
        assertEquals(10, request.getEffectiveLimit());
    }
}
