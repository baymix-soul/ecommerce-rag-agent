package com.ecommerce.rag.rag.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.memory.ConversationState;

class ContextualQueryAnalyzerTest {

    private final QueryAnalyzer analyzer = new QueryAnalyzer();

    @Test
    void runningShoesShouldParseCategory() {
        QueryAnalysisResult r = analyzer.analyze("推荐跑鞋", (ConversationState) null);
        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
    }

    @Test
    void lightweightShouldInheritRunningShoesContext() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("要轻量的", context);
        assertEquals("服饰运动", r.getCategory(), "Should inherit category from context");
        assertEquals("跑步鞋", r.getSubCategory(), "Should inherit subCategory from context");
        assertTrue(r.getInheritedFromContext() != null && r.getInheritedFromContext());
    }

    @Test
    void budget500ShouldInheritRunningShoesContext() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("预算500", context);
        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertEquals(new BigDecimal("500"), r.getMaxPrice());
        assertTrue(r.getInheritedFromContext() != null && r.getInheritedFromContext());
    }

    @Test
    void excludeNikeShouldInheritRunningShoesContext() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("除了耐克还有什么", context);
        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertTrue(r.getNegativeBrands().contains("Nike") || r.getNegativeBrands().contains("耐克"),
                "Should exclude Nike: " + r.getNegativeBrands());
    }

    @Test
    void changeOneShouldExcludeRecommended() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.getRecommendedProductIds().add("p_clothes_001");
        context.getRecommendedProductIds().add("p_clothes_002");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("换一个", context);
        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertTrue(r.getExcludeProductIds().contains("p_clothes_001"),
                "Should exclude previously recommended product");
        assertTrue(r.getExcludeProductIds().contains("p_clothes_002"));
    }

    @Test
    void noAlcoholShouldAddNegativeKeywords() {
        ConversationState context = new ConversationState("test");
        context.setCategory("美妆护肤");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("不要含酒精的", context);
        assertEquals("美妆护肤", r.getCategory(), "Should inherit category from context");
        assertTrue(r.getNegativeKeywords().contains("酒精"), "Should contain 酒精 keyword");
        assertTrue(r.getAvoidIngredientsOrTerms().contains("酒精"));
        assertTrue(r.getWarnings().stream().anyMatch(w -> w.contains("成分")));
    }

    @Test
    void noJapaneseShouldAddNegativeBrands() {
        QueryAnalysisResult r = analyzer.analyze("不要日系品牌的护肤品");
        assertNotNull(r.getNegativeBrands());
        assertTrue(r.getNegativeBrands().size() >= 5);
        assertTrue(r.getNegativeBrands().contains("SK-II"));
    }

    @Test
    void cheaperShouldReduceMaxPrice() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setSubCategory("跑步鞋");
        context.setMaxPrice(new BigDecimal("500"));
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("再便宜点", context);
        assertEquals("服饰运动", r.getCategory());
        assertNotNull(r.getMaxPrice());
        assertTrue(r.getMaxPrice().compareTo(new BigDecimal("500")) < 0,
                "Should reduce maxPrice from 500: " + r.getMaxPrice());
    }

    @Test
    void cheaperWithoutPriceContextShouldAddKeywords() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("再便宜点", context);
        assertTrue(r.getPositiveKeywords().contains("性价比"), "Should add 性价比 keyword");
        assertTrue(r.getPositiveKeywords().contains("低价"));
    }

    @Test
    void studentShouldAddBudgetKeywords() {
        ConversationState context = new ConversationState("test");
        context.setTurnCount(1);

        QueryAnalysisResult r = analyzer.analyze("适合学生党的耳机", context);
        assertEquals("数码电子", r.getCategory());
        assertTrue(r.getPositiveKeywords().contains("性价比"));
        assertTrue(r.getPositiveKeywords().contains("低价"));
        assertTrue(r.getPositiveKeywords().contains("实用"));
    }

    @Test
    void nonSupplementQueryShouldNotInheritFromZeroTurnContext() {
        ConversationState context = new ConversationState("test");
        context.setCategory("服饰运动");
        context.setTurnCount(0);

        QueryAnalysisResult r = analyzer.analyze("推荐洗面奶", context);
        assertEquals("美妆护肤", r.getCategory(), "Should NOT inherit from zero-turn context");
    }
}
