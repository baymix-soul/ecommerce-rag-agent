package com.ecommerce.rag.rag.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class ConversationStateUpdateTest {

    private final InMemoryConversationMemoryService memoryService = new InMemoryConversationMemoryService();

    @Test
    void firstTurnRunningShoesShouldSaveCategoryAndSubCategory() {
        String sessionId = "test-turn1-running";
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        ChatCandidate c1 = new ChatCandidate();
        c1.setProductId("p_clothes_001");
        ChatCandidate c2 = new ChatCandidate();
        c2.setProductId("p_clothes_002");

        memoryService.updateAfterRetrieval(sessionId, "推荐跑鞋", analysis, List.of(c1, c2));

        ConversationState state = memoryService.getState(sessionId);
        assertNotNull(state);
        assertEquals("服饰运动", state.getCategory());
        assertEquals("跑步鞋", state.getSubCategory());
        assertEquals(1, state.getTurnCount());
        assertEquals(2, state.getRecommendedProductIds().size());
        assertTrue(state.getRecommendedProductIds().contains("p_clothes_001"));
    }

    @Test
    void secondTurnLightweightShouldReadRunningShoesContext() {
        String sessionId = "test-turn2-lightweight";

        QueryAnalysisResult analysis1 = new QueryAnalysisResult();
        analysis1.setCategory("服饰运动");
        analysis1.setSubCategory("跑步鞋");

        ChatCandidate c1 = new ChatCandidate();
        c1.setProductId("p_clothes_001");
        c1.setBrand("Nike");

        memoryService.updateAfterRetrieval(sessionId, "推荐跑鞋", analysis1, List.of(c1));

        ConversationState state = memoryService.getState(sessionId);
        assertEquals("服饰运动", state.getCategory());
        assertEquals("跑步鞋", state.getSubCategory());

        QueryAnalysisResult analysis2 = new QueryAnalysisResult();
        analysis2.setCategory("服饰运动");
        analysis2.setSubCategory("跑步鞋");
        analysis2.getPositiveKeywords().add("轻量");

        ChatCandidate c2 = new ChatCandidate();
        c2.setProductId("p_clothes_003");
        c2.setBrand("Adidas");

        memoryService.updateAfterRetrieval(sessionId, "要轻量的", analysis2, List.of(c2));

        state = memoryService.getState(sessionId);
        assertNotNull(state);
        assertEquals(2, state.getTurnCount());
        assertEquals("服饰运动", state.getCategory());
        assertEquals("跑步鞋", state.getSubCategory());
        assertEquals(1, state.getRecommendedProductIds().size());
        assertTrue(state.getRecommendedProductIds().contains("p_clothes_003"));
    }

    @Test
    void thirdTurnBudget1000ShouldReadRunningShoesContextAndSaveMaxPrice() {
        String sessionId = "test-turn3-budget";

        QueryAnalysisResult analysis1 = new QueryAnalysisResult();
        analysis1.setCategory("服饰运动");
        analysis1.setSubCategory("跑步鞋");
        memoryService.updateAfterRetrieval(sessionId, "推荐跑鞋", analysis1, List.of());

        QueryAnalysisResult analysis2 = new QueryAnalysisResult();
        analysis2.setCategory("服饰运动");
        analysis2.setSubCategory("跑步鞋");
        analysis2.getPositiveKeywords().add("轻量");
        memoryService.updateAfterRetrieval(sessionId, "要轻量的", analysis2, List.of());

        QueryAnalysisResult analysis3 = new QueryAnalysisResult();
        analysis3.setCategory("服饰运动");
        analysis3.setSubCategory("跑步鞋");
        analysis3.setMaxPrice(new BigDecimal("1000"));

        ChatCandidate c3 = new ChatCandidate();
        c3.setProductId("p_clothes_005");
        c3.setPrice(new BigDecimal("899"));

        memoryService.updateAfterRetrieval(sessionId, "预算1000以内", analysis3, List.of(c3));

        ConversationState state = memoryService.getState(sessionId);
        assertNotNull(state);
        assertEquals(3, state.getTurnCount(), "Turn count should be 3 after third call");
        assertEquals("服饰运动", state.getCategory());
        assertEquals("跑步鞋", state.getSubCategory());
        assertEquals(new BigDecimal("1000"), state.getMaxPrice(),
                "Should save maxPrice=1000 from third turn");
    }

    @Test
    void recommendedProductIdsShouldOnlySaveSentCards() {
        String sessionId = "test-sent-cards-only";

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        ChatCandidate c1 = new ChatCandidate();
        c1.setProductId("p_sent_001");
        ChatCandidate c2 = new ChatCandidate();
        c2.setProductId("p_sent_002");

        memoryService.updateAfterRetrieval(sessionId, "推荐跑鞋", analysis, List.of(c1, c2));

        ConversationState state = memoryService.getState(sessionId);
        assertEquals(2, state.getRecommendedProductIds().size());
        assertTrue(state.getRecommendedProductIds().contains("p_sent_001"));
        assertTrue(state.getRecommendedProductIds().contains("p_sent_002"));
        assertEquals(2, state.getCandidateProductIds().size(),
                "candidateProductIds from latest call should match sent count");
    }

    @Test
    void differentSessionIdsShouldNotPollute() {
        memoryService.updateAfterRetrieval("session-a", "推荐跑鞋",
                createAnalysis("服饰运动", "跑步鞋"), List.of());

        memoryService.updateAfterRetrieval("session-b", "推荐洗面奶",
                createAnalysis("美妆护肤", "洁面"), List.of());

        ConversationState stateA = memoryService.getState("session-a");
        ConversationState stateB = memoryService.getState("session-b");

        assertEquals("服饰运动", stateA.getCategory());
        assertEquals("美妆护肤", stateB.getCategory());
    }

    @Test
    void missingSessionIdShouldUseDefault() {
        memoryService.updateAfterRetrieval(null, "推荐跑鞋",
                createAnalysis("服饰运动", "跑步鞋"), List.of());

        ConversationState state = memoryService.getState("default");
        assertNotNull(state);
        assertEquals("服饰运动", state.getCategory());
    }

    @Test
    void negativeKeywordsShouldBeSavedToState() {
        String sessionId = "test-negative-kw";
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("美妆护肤");
        analysis.getNegativeKeywords().add("酒精");
        analysis.getNegativeKeywords().add("乙醇");
        analysis.getAvoidIngredientsOrTerms().add("酒精");

        memoryService.updateAfterRetrieval(sessionId, "不要含酒精", analysis, List.of());

        ConversationState state = memoryService.getState(sessionId);
        assertNotNull(state);
        assertTrue(state.getNegativeKeywords().contains("酒精"));
        assertTrue(state.getNegativeKeywords().contains("乙醇"));
    }

    private QueryAnalysisResult createAnalysis(String category, String subCategory) {
        QueryAnalysisResult a = new QueryAnalysisResult();
        a.setCategory(category);
        a.setSubCategory(subCategory);
        return a;
    }
}
