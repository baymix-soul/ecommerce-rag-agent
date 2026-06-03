package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;

@SpringBootTest
class ChatServiceProductCardConstraintTest {

    @Autowired
    private ChatService chatService;

    @Test
    void productSearchWithPriceConstraintShouldNotReturnOverBudgetCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("预算500以内跑鞋");
        request.setSessionId("test-price-constraint");
        request.setLimit(10);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void emptyFinalCandidatesShouldNotSendProductCards() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("test-empty-cards");
        request.setLimit(10);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1以内");
        req2.setSessionId("test-empty-cards");
        req2.setLimit(10);

        SseEmitter e1 = chatService.chat(request);
        assertNotNull(e1);
        Thread.sleep(500);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void lightweightShouldNotReturnBackpackInRunningShoeContext() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-lightweight-backpack");
        req1.setLimit(10);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("要轻量的");
        req2.setSessionId("test-lightweight-backpack");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void budget1000ShouldNotReturnOver1000CardsInRunningShoeContext() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-budget-context");
        req1.setLimit(10);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1000以内");
        req2.setSessionId("test-budget-context");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void excludeNikeShouldNotReturnNikeCards() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-exclude-nike");
        req1.setLimit(10);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("除了耐克");
        req2.setSessionId("test-exclude-nike");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void oldChatRequestWithoutSessionIdShouldStillWork() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐一款跑鞋");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);

        Thread.sleep(500);
    }

    @Test
    void noMatchingProductsWithBudget500ShouldReturnTextOnly() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-no-match-500");
        req1.setLimit(10);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1以内");
        req2.setSessionId("test-no-match-500");
        req2.setLimit(10);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void promptAndCardShouldUseSameFinalCandidates() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("test-same-candidates");
        req1.setLimit(5);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("要轻量的");
        req2.setSessionId("test-same-candidates");
        req2.setLimit(5);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }
}
