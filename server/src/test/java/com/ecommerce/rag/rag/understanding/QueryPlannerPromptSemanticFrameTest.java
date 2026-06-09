package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.rag.core.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryPlannerPromptSemanticFrameTest {

    private QueryPlannerPromptBuilder promptBuilder;
    private CartSemanticFrameCatalog catalog;
    private CartSemanticFrameMatcher matcher;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        catalog = new CartSemanticFrameCatalog();
        matcher = new CartSemanticFrameMatcher(catalog, null);
        promptBuilder = new QueryPlannerPromptBuilder(appProperties, catalog, matcher);
    }

    @Test
    void testPromptContainsAvailableCartSemanticFrames() {
        String prompt = promptBuilder.build("推荐一款跑鞋", null, null, null);

        assertTrue(prompt.contains("可用购物车语义框架"));
        assertTrue(prompt.contains("cart.summary"));
        assertTrue(prompt.contains("cart.amount_gap_query"));
        assertTrue(prompt.contains("cart.completion_recommend"));
        assertTrue(prompt.contains("cart.completion_clarify"));
        assertTrue(prompt.contains("cart.add_item"));
    }

    @Test
    void testPromptContainsAmountGapQueryFrame() {
        String prompt = promptBuilder.build("离2000还差多少", null, null, null);

        assertTrue(prompt.contains("cart.amount_gap_query"));
        assertTrue(prompt.contains("AMOUNT_GAP_QUERY"));
        assertTrue(prompt.contains("needsRecommendation\": false"));
    }

    @Test
    void testPromptContainsCompletionRecommendFrame() {
        String prompt = promptBuilder.build("如果要凑1000块", null, null, null);

        assertTrue(prompt.contains("cart.completion_recommend"));
        assertTrue(prompt.contains("COMPLETION_RECOMMEND"));
    }

    @Test
    void testPromptContainsSemanticMatchHint() {
        String prompt = promptBuilder.build("离2000还差多少", null, null, null);

        assertTrue(prompt.contains("购物车语义匹配结果"));
        assertTrue(prompt.contains("匹配级别"));
        assertTrue(prompt.contains("cart.amount_gap_query"));
    }

    @Test
    void testPromptPartialMatchLlmFillSlots() {
        String prompt = promptBuilder.build("差多少到2000", null, null, null);

        assertTrue(prompt.contains("matchLevel=PARTIAL"));
        assertTrue(prompt.contains("判断 query 是否与候选 frame 语义等价"));
    }

    @Test
    void testPromptProhibitsInventedFrameId() {
        String prompt = promptBuilder.build("推荐一款跑鞋", null, null, null);

        assertTrue(prompt.contains("不允许发明新的"));
        assertTrue(prompt.contains("不允许发明未知 frameId"));
    }

    @Test
    void testPromptComputerSearchNotCartIntent() {
        String prompt = promptBuilder.build("推荐2000元以内的电脑", null, null, null);

        assertTrue(prompt.contains("推荐2000元以内的电脑"));
        assertTrue(prompt.contains("PRODUCT_SEARCH"));
        assertTrue(prompt.contains("不是购物车意图"));
    }
}
