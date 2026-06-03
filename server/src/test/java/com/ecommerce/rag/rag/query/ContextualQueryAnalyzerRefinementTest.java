package com.ecommerce.rag.rag.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.memory.ConversationState;

class ContextualQueryAnalyzerRefinementTest {

    private final QueryAnalyzer analyzer = new QueryAnalyzer();

    @Test
    void lightweightShouldInheritRunningShoesAndBuildNormalizedQuery() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("要轻量的", context);

        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertTrue(r.getInheritedFromContext());

        assertNotNull(r.getNormalizedQuery());
        assertTrue(r.getNormalizedQuery().contains("跑步鞋"),
                "normalizedQuery should contain context subCategory: " + r.getNormalizedQuery());
        assertTrue(r.getNormalizedQuery().contains("轻量"),
                "normalizedQuery should contain query keyword: " + r.getNormalizedQuery());
    }

    @Test
    void budget1000ShouldInheritRunningShoesAndSetMaxPrice() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("预算1000以内", context);

        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
        assertTrue(r.getInheritedFromContext());

        assertNotNull(r.getNormalizedQuery());
        assertTrue(r.getNormalizedQuery().contains("跑步鞋"),
                "normalizedQuery should contain context subCategory: " + r.getNormalizedQuery());
    }

    @Test
    void excludeNikeShouldInheritRunningShoesAndSetNegativeBrands() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("除了耐克", context);

        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertTrue(r.getNegativeBrands().contains("Nike") || r.getNegativeBrands().contains("耐克"),
                "Should exclude Nike: " + r.getNegativeBrands());
        assertTrue(r.getInheritedFromContext());

        assertNotNull(r.getNormalizedQuery());
        assertTrue(r.getNormalizedQuery().contains("跑步鞋"),
                "normalizedQuery should contain context subCategory: " + r.getNormalizedQuery());
    }

    @Test
    void changeOneShouldInheritContextAndExcludeRecommended() {
        ConversationState context = new ConversationState("test");
        context.setCategory("美妆护肤");
        context.setSubCategory("防晒");
        context.getRecommendedProductIds().add("p_beauty_020");
        context.getRecommendedProductIds().add("p_beauty_021");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("换一个", context);

        assertEquals("美妆护肤", r.getCategory());
        assertEquals("防晒", r.getSubCategory());
        assertTrue(r.getExcludeProductIds().contains("p_beauty_020"),
                "Should exclude previously recommended product");
        assertTrue(r.getExcludeProductIds().contains("p_beauty_021"));
        assertTrue(r.getInheritedFromContext());

        assertNotNull(r.getNormalizedQuery());
        assertTrue(r.getNormalizedQuery().contains("防晒"),
                "normalizedQuery should contain context subCategory: " + r.getNormalizedQuery());
    }
}
