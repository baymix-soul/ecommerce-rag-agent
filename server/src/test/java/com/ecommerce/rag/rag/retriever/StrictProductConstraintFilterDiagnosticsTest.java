package com.ecommerce.rag.rag.retriever;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.eval.CategoryMatchService;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class StrictProductConstraintFilterDiagnosticsTest {

    private CategoryMatchService categoryMatchService;
    private StrictProductConstraintFilter filter;

    @BeforeEach
    void setUp() {
        categoryMatchService = mock(CategoryMatchService.class);
        when(categoryMatchService.categoryMatches(any(), any())).thenReturn(false);
        when(categoryMatchService.subCategoryMatches(any(), any())).thenReturn(false);
        when(categoryMatchService.subCategoryMatchesAny(any(), any())).thenReturn(false);
        filter = new StrictProductConstraintFilter(categoryMatchService);
    }

    @Test
    void testPriceGtMax() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("跑鞋");
        p.setPrice(new BigDecimal("1299"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");
        p.setBrand("Nike");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));

        ConstraintCheckResult result = filter.check(p, analysis);

        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("price_gt_max")));
        assertTrue(result.getFailures().stream().anyMatch(f -> ConstraintFailure.PRICE_GT_MAX.equals(f.getType())));
    }

    @Test
    void testSubCategoryMismatch() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("篮球鞋");
        p.setPrice(new BigDecimal("599"));
        p.setCategory("服饰运动");
        p.setSubCategory("篮球鞋");
        p.setBrand("Nike");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        ConstraintCheckResult result = filter.check(p, analysis);

        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("sub_category_mismatch")));
        assertTrue(result.getFailures().stream().anyMatch(f -> ConstraintFailure.SUB_CATEGORY_MISMATCH.equals(f.getType())));
    }

    @Test
    void testNegativeBrand() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("耐克跑鞋");
        p.setPrice(new BigDecimal("599"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");
        p.setBrand("Nike");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setNegativeBrands(List.of("Nike"));

        ConstraintCheckResult result = filter.check(p, analysis);

        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("negative_brand_hit")));
        assertTrue(result.getFailures().stream().anyMatch(f -> ConstraintFailure.NEGATIVE_BRAND.equals(f.getType())));
    }

    @Test
    void testNegativeKeyword() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("含酒精爽肤水");
        p.setDescription("含酒精配方");
        p.setPrice(new BigDecimal("99"));
        p.setCategory("美妆护肤");
        p.setSubCategory("爽肤水");
        p.setBrand("某品牌");
        p.setSpecs(Map.of("成分", "酒精"));

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("美妆护肤");
        analysis.setSubCategory("爽肤水");
        analysis.setNegativeKeywords(List.of("酒精"));

        ConstraintCheckResult result = filter.check(p, analysis);

        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("negative_keyword_hit")));
        assertTrue(result.getFailures().stream().anyMatch(f -> ConstraintFailure.NEGATIVE_KEYWORD.equals(f.getType())));
    }

    @Test
    void testSoftPreferenceMissingDoesNotCauseHardFail() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("普通跑鞋");
        p.setPrice(new BigDecimal("599"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");
        p.setBrand("某品牌");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        // 轻量 是 soft preference，不作为硬过滤

        ConstraintCheckResult result = filter.check(p, analysis);

        assertTrue(result.isPassed());
    }

    @Test
    void testFailedRulesInFilteredOutCandidates() {
        Product p = new Product();
        p.setProductId("P001");
        p.setName("昂贵跑鞋");
        p.setPrice(new BigDecimal("1299"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");
        p.setBrand("Nike");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));
        analysis.setNegativeBrands(List.of("Nike"));

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");

        List<RetrievedProductCandidate> filtered = filter.filterCandidates(List.of(candidate), analysis);

        assertTrue(filtered.isEmpty());
        assertFalse(candidate.getFailedRules().isEmpty());
        assertFalse(candidate.getFailures().isEmpty());
    }
}
