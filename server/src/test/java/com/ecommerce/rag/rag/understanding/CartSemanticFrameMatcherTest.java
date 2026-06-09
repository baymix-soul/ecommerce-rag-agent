package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartSemanticFrameMatcherTest {

    private CartSemanticFrameMatcher matcher;
    private CartSemanticFrameCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new CartSemanticFrameCatalog();
        matcher = new CartSemanticFrameMatcher(catalog, null);
    }

    @Test
    void testAmountGapQueryExact() {
        CartSemanticMatchResult result = matcher.match("离2000还差多少", null, null);

        assertTrue(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_EXACT, result.getMatchLevel());
        assertEquals("cart.amount_gap_query", result.getMatchedFrameId());
        assertEquals(new BigDecimal("2000"), result.getRuleParsedTargetAmount());
        assertTrue(result.getMissingSlots().isEmpty());
    }

    @Test
    void testAmountGapQueryDistancePattern() {
        CartSemanticMatchResult result = matcher.match("距离2000还差多少", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.amount_gap_query", result.getMatchedFrameId());
        assertEquals(new BigDecimal("2000"), result.getRuleParsedTargetAmount());
    }

    @Test
    void testAmountGapQueryChineseNumber() {
        CartSemanticMatchResult result = matcher.match("还差多少到两千", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.amount_gap_query", result.getMatchedFrameId());
        assertEquals(new BigDecimal("2000"), result.getRuleParsedTargetAmount());
    }

    @Test
    void testAmountGapQueryReversePattern() {
        CartSemanticMatchResult result = matcher.match("差多少到2000", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.amount_gap_query", result.getMatchedFrameId());
        assertEquals(new BigDecimal("2000"), result.getRuleParsedTargetAmount());
    }

    @Test
    void testCompletionRecommendExact() {
        CartSemanticMatchResult result = matcher.match("帮我凑到1000元", null, null);

        assertTrue(result.isCartRelated());
        assertEquals(new BigDecimal("1000"), result.getRuleParsedTargetAmount());
    }

    @Test
    void testCompletionRecommendWithQuestion() {
        CartSemanticMatchResult result = matcher.match("如果要凑1000块，有没有推荐商品", null, null);

        assertTrue(result.isCartRelated());
        assertEquals(new BigDecimal("1000"), result.getRuleParsedTargetAmount());
    }

    @Test
    void testCompletionClarifyPartial() {
        CartSemanticMatchResult result = matcher.match("凑单推荐一下", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.completion_clarify", result.getMatchedFrameId());
        assertNull(result.getRuleParsedTargetAmount());
    }

    @Test
    void testProductSearchNotCart() {
        CartSemanticMatchResult result = matcher.match("推荐2000元以内的电脑", null, null);

        assertFalse(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getMatchLevel());
    }

    @Test
    void testBudgetBuyPhoneNotCart() {
        CartSemanticMatchResult result = matcher.match("预算2000买手机", null, null);

        assertFalse(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getMatchLevel());
    }

    @Test
    void testCartSummaryHint() {
        CartSemanticMatchResult result = matcher.match("当前购物车多少钱了", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.summary", result.getMatchedFrameId());
    }

    @Test
    void testAddToCart() {
        CartSemanticMatchResult result = matcher.match("把这个加入购物车", null, null);

        assertTrue(result.isCartRelated());
        assertEquals("cart.add_item", result.getMatchedFrameId());
    }

    @Test
    void testNormalProductSearchNotCart() {
        CartSemanticMatchResult result = matcher.match("推荐一款跑鞋", null, null);

        assertFalse(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getMatchLevel());
    }

    @Test
    void testNullQuery() {
        CartSemanticMatchResult result = matcher.match(null, null, null);

        assertFalse(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getMatchLevel());
    }

    @Test
    void testBlankQuery() {
        CartSemanticMatchResult result = matcher.match("   ", null, null);

        assertFalse(result.isCartRelated());
        assertEquals(CartSemanticMatchResult.LEVEL_NONE, result.getMatchLevel());
    }
}
