package com.ecommerce.rag.rag.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class ConversationStatePlannerUpdateTest {

    private InMemoryConversationMemoryService memoryService;

    @BeforeEach
    void setUp() {
        memoryService = new InMemoryConversationMemoryService();
    }

    @Test
    void plannerEffectiveAnalysisShouldSaveCategoryAndSubCategory() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐几款适合程序员的电脑");
        analysis.setCategory("数码电子");
        analysis.setSubCategory("笔记本电脑");
        analysis.getPositiveKeywords().addAll(List.of("程序员", "编程", "开发"));

        List<ChatCandidate> candidates = List.of(
                createCandidate("p_digital_001", "MacBook Pro"),
                createCandidate("p_digital_002", "ThinkPad X1")
        );

        memoryService.updateAfterRetrieval("planner-session-1", "推荐几款适合程序员的电脑", analysis, candidates);

        ConversationState state = memoryService.getState("planner-session-1");
        assertNotNull(state);
        assertEquals("数码电子", state.getCategory());
        assertEquals("笔记本电脑", state.getSubCategory());
        assertTrue(state.getPositiveKeywords().contains("程序员"));
        assertEquals(2, state.getRecommendedProductIds().size());
    }

    @Test
    void plannerEffectiveAnalysisShouldSaveMaxPrice() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("一万元以下的");
        analysis.setCategory("数码电子");
        analysis.setSubCategory("笔记本电脑");
        analysis.setMaxPrice(new BigDecimal("10000"));

        List<ChatCandidate> candidates = List.of(
                createCandidate("p_digital_001", "MacBook Pro")
        );

        memoryService.updateAfterRetrieval("planner-session-2", "一万元以下的", analysis, candidates);

        ConversationState state = memoryService.getState("planner-session-2");
        assertNotNull(state);
        assertEquals(new BigDecimal("10000"), state.getMaxPrice());
    }

    @Test
    void secondRoundShouldReadFirstRoundPlannerContext() {
        QueryAnalysisResult round1 = new QueryAnalysisResult();
        round1.setOriginalQuery("推荐几款适合程序员的电脑");
        round1.setCategory("数码电子");
        round1.setSubCategory("笔记本电脑");
        round1.getPositiveKeywords().addAll(List.of("程序员", "编程"));

        memoryService.updateAfterRetrieval("planner-multi", "推荐几款适合程序员的电脑", round1,
                List.of(createCandidate("p_digital_001", "MacBook Pro")));

        ConversationState state = memoryService.getOrCreate("planner-multi");
        assertEquals("数码电子", state.getCategory());
        assertEquals("笔记本电脑", state.getSubCategory());

        QueryAnalysisResult round2 = new QueryAnalysisResult();
        round2.setOriginalQuery("一万元以下的");
        round2.setCategory("数码电子");
        round2.setSubCategory("笔记本电脑");
        round2.setMaxPrice(new BigDecimal("10000"));

        memoryService.updateAfterRetrieval("planner-multi", "一万元以下的", round2,
                List.of(createCandidate("p_digital_001", "MacBook Pro")));

        ConversationState state2 = memoryService.getState("planner-multi");
        assertNotNull(state2);
        assertEquals(new BigDecimal("10000"), state2.getMaxPrice());
        assertEquals("数码电子", state2.getCategory());
    }

    @Test
    void recommendedProductIdsShouldOnlySaveActuallySentCards() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        List<ChatCandidate> sentCards = List.of(
                createCandidate("p_clothes_001", "Nike Air Zoom"),
                createCandidate("p_clothes_002", "Adidas Ultraboost"),
                createCandidate("p_clothes_003", "Asics Gel-Kayano")
        );

        memoryService.updateAfterRetrieval("planner-cards", "推荐跑鞋", analysis, sentCards);

        ConversationState state = memoryService.getState("planner-cards");
        assertNotNull(state);
        assertEquals(3, state.getRecommendedProductIds().size());
        assertTrue(state.getRecommendedProductIds().contains("p_clothes_001"));
    }

    @Test
    void differentSessionIdsShouldNotCrossContaminate() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        memoryService.updateAfterRetrieval("session-a", "推荐跑鞋", analysis,
                List.of(createCandidate("p_clothes_001", "Nike")));

        QueryAnalysisResult analysisB = new QueryAnalysisResult();
        analysisB.setOriginalQuery("推荐笔记本电脑");
        analysisB.setCategory("数码电子");
        analysisB.setSubCategory("笔记本电脑");

        memoryService.updateAfterRetrieval("session-b", "推荐笔记本电脑", analysisB,
                List.of(createCandidate("p_digital_001", "MacBook")));

        ConversationState stateA = memoryService.getState("session-a");
        assertEquals("服饰运动", stateA.getCategory());
        assertEquals("跑步鞋", stateA.getSubCategory());

        ConversationState stateB = memoryService.getState("session-b");
        assertEquals("数码电子", stateB.getCategory());
        assertEquals("笔记本电脑", stateB.getSubCategory());
    }

    private ChatCandidate createCandidate(String productId, String name) {
        ChatCandidate c = new ChatCandidate();
        c.setProductId(productId);
        c.setName(name);
        return c;
    }
}
