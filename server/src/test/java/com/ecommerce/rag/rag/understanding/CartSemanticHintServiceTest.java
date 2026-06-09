package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartSemanticHintServiceTest {

    private final CartSemanticHintService service = new CartSemanticHintService();

    @Test
    @DisplayName("购物车总价查询 → cartRelated=true, possibleIntents 包含 CART_SUMMARY")
    void testCartSummaryHint() {
        CartSemanticHint hint = service.analyze("当前购物车多少钱了");

        assertTrue(hint.isCartRelated());
        assertTrue(hint.getPossibleIntents().contains("CART_SUMMARY"));
    }

    @Test
    @DisplayName("购物车里有什么 → CART_SUMMARY hint")
    void testCartSummaryWhatInCart() {
        CartSemanticHint hint = service.analyze("购物车里有什么");

        assertTrue(hint.isCartRelated());
        assertTrue(hint.getPossibleIntents().contains("CART_SUMMARY"));
    }

    @Test
    @DisplayName("凑1000块 → CART_TOP_UP hint, ruleParsedTargetAmount=1000")
    void testCartTopUpWithAmount() {
        CartSemanticHint hint = service.analyze("如果要凑1000块");

        assertTrue(hint.isCartRelated());
        assertTrue(hint.getPossibleIntents().contains("CART_TOP_UP"));
        assertEquals(new BigDecimal("1000"), hint.getRuleParsedTargetAmount());
    }

    @Test
    @DisplayName("帮我凑到一千 → 中文数字解析 targetAmount=1000")
    void testCartTopUpChineseAmount() {
        CartSemanticHint hint = service.analyze("帮我凑到一千");

        assertTrue(hint.isCartRelated());
        assertTrue(hint.getPossibleIntents().contains("CART_TOP_UP"));
        assertEquals(new BigDecimal("1000"), hint.getRuleParsedTargetAmount());
    }

    @Test
    @DisplayName("非购物车查询 → cartRelated=false")
    void testNonCartQuery() {
        CartSemanticHint hint = service.analyze("推荐一款跑鞋");

        assertFalse(hint.isCartRelated());
    }

    @Test
    @DisplayName("hint 的 possibleIntents 是列表而非单一意图，确认仅为 hint 而非最终判断")
    void testHintIsNotFinalIntent() {
        CartSemanticHint hint = service.analyze("当前购物车多少钱了");

        assertNotNull(hint.getPossibleIntents());
        // possibleIntents is a List, not a single string — verify it's iterable and sized
        assertInstanceOf(java.util.List.class, hint.getPossibleIntents());
        // A hint may contain multiple possible intents; it is not a final single-intent judgment
        assertTrue(hint.getPossibleIntents().size() >= 1);
    }
}
