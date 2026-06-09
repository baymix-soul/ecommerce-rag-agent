package com.ecommerce.rag.rag.retriever;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.ProductService;

class NoMatchRecoveryServiceTest {

    private ProductService productService;
    private StrictProductConstraintFilter constraintFilter;
    private NoMatchRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        constraintFilter = mock(StrictProductConstraintFilter.class);
        recoveryService = new NoMatchRecoveryService(productService, constraintFilter);
    }

    @Test
    void testRecoverByRelaxingSoftPreferences() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));
        ctx.setSoftPreferences(List.of("轻量"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1000"));
        exactAnalysis.setSoftKeywords(List.of("轻量"));

        Product p = new Product();
        p.setProductId("P001");
        p.setName("跑鞋");
        p.setPrice(new BigDecimal("899"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");
        candidate.setFinalScore(0.9);

        when(constraintFilter.passes(any(), any())).thenAnswer(inv -> {
            QueryAnalysisResult a = inv.getArgument(1);
            // 模拟：有 softKeywords 时失败，无 softKeywords 时通过
            return a.getSoftKeywords() == null || a.getSoftKeywords().isEmpty();
        });

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(candidate), 3);

        assertTrue(result.isRecovered());
        assertEquals(NoMatchRecoveryService.RECOVERY_TYPE_RELAX_SOFT, result.getRecoveryType());
        assertFalse(result.getRelaxedCandidates().isEmpty());
        assertEquals("P001", result.getRelaxedCandidates().get(0).getProductId());
        assertTrue(result.getUserMessage().contains("轻量"));
    }

    @Test
    void testRelaxedCandidatesStillRespectMaxPrice() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));
        ctx.setSoftPreferences(List.of("轻量"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1000"));

        Product cheap = new Product();
        cheap.setProductId("P001");
        cheap.setName("便宜跑鞋");
        cheap.setPrice(new BigDecimal("899"));
        cheap.setCategory("服饰运动");
        cheap.setSubCategory("跑步鞋");

        Product expensive = new Product();
        expensive.setProductId("P002");
        expensive.setName("贵跑鞋");
        expensive.setPrice(new BigDecimal("1299"));
        expensive.setCategory("服饰运动");
        expensive.setSubCategory("跑步鞋");

        RetrievedProductCandidate c1 = new RetrievedProductCandidate();
        c1.setProduct(cheap);
        c1.setProductId("P001");

        RetrievedProductCandidate c2 = new RetrievedProductCandidate();
        c2.setProduct(expensive);
        c2.setProductId("P002");

        when(constraintFilter.passes(any(), any())).thenAnswer(inv -> {
            Product prod = inv.getArgument(0);
            QueryAnalysisResult a = inv.getArgument(1);
            if (a.getMaxPrice() != null && prod.getPrice().compareTo(a.getMaxPrice()) > 0) {
                return false;
            }
            return true;
        });

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(c1, c2), 3);

        assertTrue(result.isRecovered());
        for (ChatCandidate cc : result.getRelaxedCandidates()) {
            assertTrue(cc.getPrice().compareTo(new BigDecimal("1000")) <= 0,
                    "Candidate " + cc.getProductId() + " price " + cc.getPrice() + " exceeds maxPrice");
        }
    }

    @Test
    void testNegativeBrandsNotRelaxed() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));
        ctx.setNegativeBrands(List.of("Nike"));
        ctx.setSoftPreferences(List.of("轻量"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1000"));

        Product p = new Product();
        p.setProductId("P001");
        p.setName("Nike跑鞋");
        p.setPrice(new BigDecimal("899"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");
        p.setBrand("Nike");

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");

        when(constraintFilter.passes(any(), any())).thenAnswer(inv -> {
            QueryAnalysisResult a = inv.getArgument(1);
            Product prod = inv.getArgument(0);
            if (a.getNegativeBrands() != null && a.getNegativeBrands().contains(prod.getBrand())) {
                return false;
            }
            return true;
        });

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(candidate), 3);

        // 因为 negative brand 是硬约束，不应被放宽，所以恢复失败
        assertFalse(result.isRecovered());
    }

    @Test
    void testMaxPriceNotRelaxedAndNoCardsSent() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1"));
        ctx.setSoftPreferences(List.of("轻量"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1"));

        Product p = new Product();
        p.setProductId("P001");
        p.setName("跑鞋");
        p.setPrice(new BigDecimal("599"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");

        when(constraintFilter.passes(any(), any())).thenReturn(false);

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(candidate), 3);

        assertFalse(result.isRecovered());
        assertTrue(result.getRelaxedCandidates().isEmpty());
        assertNotNull(result.getUserMessage());
    }

    @Test
    void testHardConstraintsNoResultReturnsSuggestion() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1000"));

        Product p = new Product();
        p.setProductId("P001");
        p.setName("跑鞋");
        p.setPrice(new BigDecimal("1099"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");

        when(constraintFilter.passes(any(), any())).thenReturn(false);

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(candidate), 3);

        assertFalse(result.isRecovered());
        assertTrue(result.getUserMessage().contains("1099") || result.getUserMessage().contains("暂时没有"));
    }

    @Test
    void testUserMessageExplainsRelaxedConstraints() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));
        ctx.setSoftPreferences(List.of("轻量", "透气"));

        QueryAnalysisResult exactAnalysis = new QueryAnalysisResult();
        exactAnalysis.setCategory("服饰运动");
        exactAnalysis.setSubCategory("跑步鞋");
        exactAnalysis.setMaxPrice(new BigDecimal("1000"));

        Product p = new Product();
        p.setProductId("P001");
        p.setName("跑鞋");
        p.setPrice(new BigDecimal("899"));
        p.setCategory("服饰运动");
        p.setSubCategory("跑步鞋");

        RetrievedProductCandidate candidate = new RetrievedProductCandidate();
        candidate.setProduct(p);
        candidate.setProductId("P001");

        when(constraintFilter.passes(any(), any())).thenAnswer(inv -> {
            QueryAnalysisResult a = inv.getArgument(1);
            return a.getSoftKeywords() == null || a.getSoftKeywords().isEmpty();
        });

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(candidate), 3);

        assertTrue(result.isRecovered());
        assertTrue(result.getUserMessage().contains("轻量") || result.getUserMessage().contains("透气"));
        assertFalse(result.getRelaxedConstraints().isEmpty());
    }
}
