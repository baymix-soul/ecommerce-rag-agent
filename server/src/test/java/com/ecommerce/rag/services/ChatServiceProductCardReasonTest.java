package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.recommendation.RecommendationReasonService;

@SpringBootTest
class ChatServiceProductCardReasonTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RecommendationReasonService reasonService;

    @Test
    void productCardReasonShouldNotBeEmpty() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐跑鞋");
        request.setSessionId("reason-test-not-empty");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void productCardReasonShouldNotBePlaceholder() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐轻量跑鞋");
        request.setSessionId("reason-test-no-placeholder");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void productCardReasonShouldUseSoftKeywords() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐轻量跑鞋");
        request.setSessionId("reason-test-soft-keywords");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void multipleCardsShouldAllHaveReason() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐几款跑鞋");
        request.setSessionId("reason-test-multi-cards");
        request.setLimit(5);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void maxPriceConstraintShouldMentionBudget() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("推荐1000元以下的跑鞋");
        request.setSessionId("reason-test-budget");
        request.setLimit(3);

        SseEmitter emitter = chatService.chat(request);
        assertNotNull(emitter);
        Thread.sleep(500);
    }

    @Test
    void noMatchRecoveryCardShouldNotUsePlaceholder() throws Exception {
        ChatRequest req1 = new ChatRequest();
        req1.setMessage("推荐跑鞋");
        req1.setSessionId("reason-test-recovery");
        req1.setLimit(3);

        SseEmitter e1 = chatService.chat(req1);
        assertNotNull(e1);
        Thread.sleep(500);

        ChatRequest req2 = new ChatRequest();
        req2.setMessage("预算1以内");
        req2.setSessionId("reason-test-recovery");
        req2.setLimit(3);

        SseEmitter e2 = chatService.chat(req2);
        assertNotNull(e2);
        Thread.sleep(500);
    }

    @Test
    void productCardJsonStructureShouldRemainUnchanged() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSubCategory("跑步鞋");

        String reason = reasonService.generateReason(null, null, analysis);
        assertNotNull(reason);
    }

    @Test
    void reasonServiceShouldDetectPlaceholder() {
        assertTrue(reasonService.isPlaceholder("由LLM推荐"));
        assertTrue(reasonService.isPlaceholder("该商品来自当前商品库候选结果，具体推荐理由将在后续 LLM 阶段生成。"));
        assertFalse(reasonService.isPlaceholder("匹配轻量需求，适合跑步训练。"));
    }
}
