package com.ecommerce.rag.rag.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class QueryAnalyzerTest {

    private final QueryAnalyzer analyzer = new QueryAnalyzer();

    @Test
    void oilSkinCleanserShouldParseCategoryAndPersona() {
        QueryAnalysisResult r = analyzer.analyze("推荐一款适合油皮的洗面奶");
        assertEquals("美妆护肤", r.getCategory());
        assertEquals("洁面", r.getSubCategory());
        assertTrue(r.getPositiveKeywords().stream().anyMatch(k -> k.equals("控油") || k.equals("清爽")));
    }

    @Test
    void bluetoothHeadphoneWithPriceShouldParsePrice() {
        QueryAnalysisResult r = analyzer.analyze("200元以下蓝牙耳机");
        assertEquals("数码电子", r.getCategory());
        assertEquals("真无线耳机", r.getSubCategory());
        assertNotNull(r.getMaxPrice());
        assertEquals(new BigDecimal("200"), r.getMaxPrice());
        assertNotNull(r.getNormalizedQuery());
    }

    @Test
    void runningShoesWithPriceShouldParsePrice() {
        QueryAnalysisResult r = analyzer.analyze("500元以内跑鞋");
        assertEquals("服饰运动", r.getCategory());
        assertEquals("跑步鞋", r.getSubCategory());
        assertEquals(new BigDecimal("500"), r.getMaxPrice());
    }

    @Test
    void sportTopShouldParseMultiSubCategories() {
        QueryAnalysisResult r = analyzer.analyze("运动上衣");
        assertEquals("服饰运动", r.getCategory());
        assertNotNull(r.getSubCategories());
        assertTrue(r.getSubCategories().contains("短袖T恤"));
        assertTrue(r.getSubCategories().contains("速干T恤"));
    }

    @Test
    void noJapaneseBrandShouldExcludeBrands() {
        QueryAnalysisResult r = analyzer.analyze("不要日系品牌的护肤品");
        assertEquals("美妆护肤", r.getCategory());
        assertNotNull(r.getNegativeBrands());
        assertTrue(r.getNegativeBrands().size() >= 5,
                "Should contain Japanese brands, got: " + r.getNegativeBrands());
        assertTrue(r.getNegativeBrands().contains("SK-II"));
    }

    @Test
    void commuteBackpackShouldParseCategory() {
        QueryAnalysisResult r = analyzer.analyze("通勤双肩包");
        assertEquals("服饰运动", r.getCategory());
        assertEquals("背包", r.getSubCategory());
    }

    @Test
    void drinksShouldParseMultiSubCategories() {
        QueryAnalysisResult r = analyzer.analyze("好喝的饮料");
        assertEquals("食品饮料", r.getCategory());
        assertNotNull(r.getSubCategories());
        assertTrue(r.getSubCategories().size() >= 4);
    }

    @Test
    void priceRangeShouldParseMinMax() {
        QueryAnalysisResult r = analyzer.analyze("100到300元耳机");
        assertNotNull(r.getMinPrice());
        assertNotNull(r.getMaxPrice());
        assertEquals(new BigDecimal("100"), r.getMinPrice());
        assertEquals(new BigDecimal("300"), r.getMaxPrice());
    }

    @Test
    void studentHeadphoneShouldAddPersonaKeywords() {
        QueryAnalysisResult r = analyzer.analyze("适合学生党的耳机");
        assertEquals("数码电子", r.getCategory());
        assertTrue(r.getPositiveKeywords().stream().anyMatch(k -> k.equals("性价比") || k.equals("实惠")));
    }
}
