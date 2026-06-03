package com.ecommerce.rag.rag.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class ConversationMemoryServiceTest {

    private final InMemoryConversationMemoryService memoryService = new InMemoryConversationMemoryService();

    @Test
    void shouldGetOrCreateSession() {
        ConversationState state = memoryService.getOrCreate("test-session");
        assertNotNull(state);
        assertEquals("test-session", state.getSessionId());
        assertEquals(0, state.getTurnCount());
    }

    @Test
    void shouldReturnDefaultSessionForNullId() {
        ConversationState state = memoryService.getOrCreate(null);
        assertNotNull(state);
        assertEquals("default", state.getSessionId());
    }

    @Test
    void shouldReuseExistingSession() {
        ConversationState state1 = memoryService.getOrCreate("test-session");
        ConversationState state2 = memoryService.getOrCreate("test-session");
        assertEquals(state1, state2);
    }

    @Test
    void shouldUpdateAfterRetrieval() {
        String sessionId = "test-update";
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("500"));

        ChatCandidate c1 = new ChatCandidate();
        c1.setProductId("p_clothes_001");
        ChatCandidate c2 = new ChatCandidate();
        c2.setProductId("p_clothes_002");

        memoryService.updateAfterRetrieval(sessionId, "推荐跑鞋", analysis, List.of(c1, c2));

        ConversationState state = memoryService.getState(sessionId);
        assertNotNull(state);
        assertEquals("推荐跑鞋", state.getLastUserQuery());
        assertEquals("服饰运动", state.getCategory());
        assertEquals("跑步鞋", state.getSubCategory());
        assertEquals(new BigDecimal("500"), state.getMaxPrice());
        assertEquals(1, state.getTurnCount());
        assertTrue(state.getRecommendedProductIds().contains("p_clothes_001"));
        assertTrue(state.getRecommendedProductIds().contains("p_clothes_002"));
    }

    @Test
    void shouldClearSession() {
        memoryService.getOrCreate("test-clear");
        assertTrue(memoryService.hasSession("test-clear"));

        memoryService.clearSession("test-clear");
        assertNull(memoryService.getState("test-clear"));
    }

    @Test
    void shouldIncrementTurnCount() {
        String sessionId = "test-turn";
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("数码电子");

        memoryService.updateAfterRetrieval(sessionId, "q1", analysis, List.of());
        assertEquals(1, memoryService.getState(sessionId).getTurnCount());

        memoryService.updateAfterRetrieval(sessionId, "q2", analysis, List.of());
        assertEquals(2, memoryService.getState(sessionId).getTurnCount());
    }
}
