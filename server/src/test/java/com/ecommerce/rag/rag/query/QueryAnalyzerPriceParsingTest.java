package com.ecommerce.rag.rag.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class QueryAnalyzerPriceParsingTest {

    private final QueryAnalyzer analyzer = new QueryAnalyzer();

    @Test
    void budget1000YineiShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("预算1000以内");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
        assertNotNull(r.getNormalizedQuery());
        assertFalse(r.getNormalizedQuery().contains("1000"),
                "normalizedQuery should not contain price number");
    }

    @Test
    void yinei1000ShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("1000以内");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void budget1000ShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("预算1000");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
        assertNotNull(r.getNormalizedQuery());
        assertFalse(r.getNormalizedQuery().contains("1000"),
                "normalizedQuery should not contain price number");
    }

    @Test
    void price1000yixiaShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("1000元以下");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void buchaoguo1000ShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("不超过1000");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void diyu1000ShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("低于1000");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void range500To1000ShouldSetMinMax() {
        QueryAnalysisResult r = analyzer.analyze("500到1000元");
        assertEquals(new BigDecimal("500"), r.getMinPrice());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void range500Dash1000ShouldSetMinMax() {
        QueryAnalysisResult r = analyzer.analyze("500-1000元");
        assertEquals(new BigDecimal("500"), r.getMinPrice());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void range500Tilde1000ShouldSetMinMax() {
        QueryAnalysisResult r = analyzer.analyze("500~1000元");
        assertEquals(new BigDecimal("500"), r.getMinPrice());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void buchaoguo1000yuanShouldSetMaxPrice1000() {
        QueryAnalysisResult r = analyzer.analyze("不超过 1000 元");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void yixia1000ShouldSetMaxPrice() {
        QueryAnalysisResult r = analyzer.analyze("1000以下");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void budget1000WithSpaceShouldSetMaxPrice() {
        QueryAnalysisResult r = analyzer.analyze("预算 1000");
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void priceRemovedFromNormalizedQueryBudget() {
        QueryAnalysisResult r = analyzer.analyze("推荐跑鞋 预算1000以内");
        assertEquals("推荐跑鞋", r.getNormalizedQuery());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }

    @Test
    void priceRemovedFromNormalizedQueryLess() {
        QueryAnalysisResult r = analyzer.analyze("200元以下蓝牙耳机");
        assertEquals("蓝牙耳机", r.getNormalizedQuery());
        assertEquals(new BigDecimal("200"), r.getMaxPrice());
    }

    @Test
    void priceRemovedFromNormalizedQueryBefore() {
        QueryAnalysisResult r = analyzer.analyze("不超过1000跑鞋");
        assertEquals("跑鞋", r.getNormalizedQuery());
        assertEquals(new BigDecimal("1000"), r.getMaxPrice());
    }
}
