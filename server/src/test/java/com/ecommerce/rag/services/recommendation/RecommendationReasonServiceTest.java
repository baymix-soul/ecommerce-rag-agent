package com.ecommerce.rag.services.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class RecommendationReasonServiceTest {

    private RecommendationReasonService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationReasonService();
    }

    @Test
    void softKeywordsHitProductText_shouldIncludeKeywords() {
        Product product = createProduct("p1", "HOKA Clifton 9 男子缓震公路跑鞋", "HOKA", "服饰运动", "跑步鞋",
                new BigDecimal("899"), "轻量缓震，适合日常跑步训练");
        ChatCandidate candidate = createCandidate("p1", "HOKA Clifton 9");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("轻量", "缓震", "透气"));
        analysis.setSubCategory("跑步鞋");

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("缓震"), "Reason should contain matched keyword: " + reason);
        assertFalse(reason.contains("由LLM推荐"), "Reason must not contain placeholder");
        assertTrue(reason.length() <= 60, "Reason length should be <= 60: " + reason.length());
    }

    @Test
    void multipleSoftKeywordsHit_shouldShowAtMostTwo() {
        Product product = createProduct("p1", "高性能编程笔记本", "ThinkPad", "数码电子", "笔记本电脑",
                new BigDecimal("6999"), "高性能大内存，适合编程开发多任务");
        ChatCandidate candidate = createCandidate("p1", "ThinkPad");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("高性能", "大内存"));
        analysis.setSubCategory("笔记本电脑");

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("高性能") || reason.contains("大内存"), "Reason should contain matched keywords: " + reason);
        assertTrue(reason.length() <= 60, "Reason length should be <= 60: " + reason.length());
    }

    @Test
    void emptySoftKeywords_withSubCategory_shouldFallbackToSubCategory() {
        Product product = createProduct("p1", "Nike Air Zoom", "Nike", "服饰运动", "跑步鞋",
                new BigDecimal("799"), "专业跑鞋");
        ChatCandidate candidate = createCandidate("p1", "Nike Air Zoom");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSubCategory("跑步鞋");

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("跑步鞋"), "Reason should mention subCategory: " + reason);
        assertFalse(reason.contains("由LLM推荐"), "Reason must not contain placeholder");
    }

    @Test
    void maxPriceExistsAndProductSatisfies_shouldMentionBudget() {
        Product product = createProduct("p1", "轻量跑鞋", "李宁", "服饰运动", "跑步鞋",
                new BigDecimal("599"), "轻量透气");
        ChatCandidate candidate = createCandidate("p1", "轻量跑鞋");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("1000") || reason.contains("预算"), "Reason should mention budget: " + reason);
        assertTrue(reason.length() <= 60, "Reason length should be <= 60: " + reason.length());
    }

    @Test
    void reasonShouldNotContainProductId() {
        Product product = createProduct("p_clothes_007", "跑鞋", "Nike", "服饰运动", "跑步鞋",
                new BigDecimal("899"), "轻量");
        ChatCandidate candidate = createCandidate("p_clothes_007", "跑鞋");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("轻量"));

        String reason = service.generateReason(candidate, product, analysis);

        assertFalse(reason.contains("p_clothes_007"), "Reason must not contain product_id");
    }

    @Test
    void reasonShouldNotBePlaceholder() {
        Product product = createProduct("p1", "跑鞋", "Nike", "服饰运动", "跑步鞋",
                new BigDecimal("899"), "轻量");
        ChatCandidate candidate = createCandidate("p1", "跑鞋");
        QueryAnalysisResult analysis = new QueryAnalysisResult();

        String reason = service.generateReason(candidate, product, analysis);

        assertFalse(service.isPlaceholder(reason), "Reason must not be placeholder: " + reason);
    }

    @Test
    void reasonLengthShouldNotExceed60() {
        Product product = createProduct("p1", "跑鞋", "Nike", "服饰运动", "跑步鞋",
                new BigDecimal("899"), "轻量缓震透气舒适专业马拉松训练鞋");
        ChatCandidate candidate = createCandidate("p1", "跑鞋");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("轻量", "缓震", "透气", "舒适", "专业"));
        analysis.setMaxPrice(new BigDecimal("1000"));
        analysis.setSubCategory("跑步鞋");

        String reason = service.generateReason(candidate, product, analysis);

        assertTrue(reason.length() <= 60, "Reason length should be <= 60, but was: " + reason.length() + " -> " + reason);
    }

    @Test
    void keywordNotInProductText_shouldNotForceKeyword() {
        Product product = createProduct("p1", "普通跑鞋", "品牌A", "服饰运动", "跑步鞋",
                new BigDecimal("399"), "基础款跑鞋");
        ChatCandidate candidate = createCandidate("p1", "普通跑鞋");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("碳纤维", "气垫", "智能芯片"));
        analysis.setSubCategory("跑步鞋");

        String reason = service.generateReason(candidate, product, analysis);

        assertFalse(reason.contains("碳纤维"), "Reason should not force unmatched keyword");
        assertFalse(reason.contains("气垫"), "Reason should not force unmatched keyword");
        assertTrue(reason.contains("跑步鞋"), "Reason should fallback to subCategory: " + reason);
    }

    @Test
    void programmerLaptopScenario_shouldGenerateProgrammingReason() {
        Product product = createProduct("p1", "MacBook Pro 16 英寸", "Apple", "数码电子", "笔记本电脑",
                new BigDecimal("19999"), "高性能大内存，适合编程开发");
        ChatCandidate candidate = createCandidate("p1", "MacBook Pro");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("高性能", "大内存", "编程"));
        analysis.setSubCategory("笔记本电脑");

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("高性能") || reason.contains("大内存") || reason.contains("编程"),
                "Reason should mention programming-related keywords: " + reason);
    }

    @Test
    void runningShoeLightweightScenario_shouldGenerateRunningReason() {
        Product product = createProduct("p1", "HOKA Clifton 9", "HOKA", "服饰运动", "跑步鞋",
                new BigDecimal("899"), "轻量缓震，适合跑步训练");
        ChatCandidate candidate = createCandidate("p1", "HOKA Clifton 9");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("轻量", "缓震", "透气"));
        analysis.setSubCategory("跑步鞋");

        String reason = service.generateReason(candidate, product, analysis);

        assertNotNull(reason);
        assertTrue(reason.contains("跑步训练") || reason.contains("跑步"), "Reason should mention running scenario: " + reason);
    }

    @Test
    void topUpReason_shouldContainTopUpHint() {
        Product product = createProduct("p1", "小零食", "品牌", "食品饮料", "坚果/零食",
                new BigDecimal("29"), "美味零食");

        String reason = service.generateTopUpReason(product, new BigDecimal("50"), new BigDecimal("100"));

        assertNotNull(reason);
        assertTrue(reason.contains("凑单"), "Top-up reason should mention 凑单: " + reason);
    }

    private Product createProduct(String id, String name, String brand, String category, String subCategory,
                                   BigDecimal price, String description) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setBrand(brand);
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(price);
        p.setCurrency("CNY");
        p.setDescription(description);
        return p;
    }

    private ChatCandidate createCandidate(String productId, String name) {
        ChatCandidate c = new ChatCandidate();
        c.setProductId(productId);
        c.setName(name);
        return c;
    }
}
