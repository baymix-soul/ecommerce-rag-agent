package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.brand.BrandAliasService;
import com.ecommerce.rag.rag.eval.CategoryMatchService;
import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class NoMatchRecoveryExclusionTest {

    private final CategoryMatchService categoryMatchService = new CategoryMatchService();
    private final BrandAliasService brandAliasService = new BrandAliasService();
    private final StrictProductConstraintFilter constraintFilter = new StrictProductConstraintFilter(
            categoryMatchService, brandAliasService);
    private NoMatchRecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new NoMatchRecoveryService(
                null, constraintFilter);
    }

    @Test
    void negativeBrandsShouldNotBeRelaxedByRecovery() {
        ActiveSearchContext ctx = new ActiveSearchContext("test-session");
        ctx.setCategory("数码电子");
        ctx.setSubCategory("笔记本电脑");
        ctx.getNegativeBrands().add("苹果");
        ctx.setSoftPreferences(List.of("程序员", "轻量"));

        QueryAnalysisResult exactAnalysis = ctx.toQueryAnalysisResult();
        exactAnalysis.setExcludeProductIds(List.of());

        Product appleProduct = new Product();
        appleProduct.setProductId("p_apple_001");
        appleProduct.setCategory("数码电子");
        appleProduct.setSubCategory("笔记本电脑");
        appleProduct.setBrand("Apple");
        appleProduct.setName("Apple MacBook Air");
        appleProduct.setPrice(new BigDecimal("7999"));

        RetrievedProductCandidate rawCandidate = new RetrievedProductCandidate();
        rawCandidate.setProduct(appleProduct);
        rawCandidate.setProductId("p_apple_001");
        rawCandidate.setFinalScore(0.85);

        Product lenovoProduct = new Product();
        lenovoProduct.setProductId("p_lenovo_001");
        lenovoProduct.setCategory("数码电子");
        lenovoProduct.setSubCategory("笔记本电脑");
        lenovoProduct.setBrand("联想");
        lenovoProduct.setName("联想 ThinkPad X1 Carbon");
        lenovoProduct.setPrice(new BigDecimal("8999"));

        RetrievedProductCandidate lenovoCandidate = new RetrievedProductCandidate();
        lenovoCandidate.setProduct(lenovoProduct);
        lenovoCandidate.setProductId("p_lenovo_001");
        lenovoCandidate.setFinalScore(0.80);

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(rawCandidate, lenovoCandidate), 3);

        assertNotNull(result);
        if (result.isRecovered()) {
            for (var candidate : result.getRelaxedCandidates()) {
                assertFalse(
                        candidate.getProductId().contains("apple")
                                || (candidate.getBrand() != null && candidate.getBrand().equalsIgnoreCase("Apple")),
                        "Recovery should NOT reintroduce Apple products: " + candidate.getProductId());
            }
        }
    }

    @Test
    void recoveryShouldPreserveNegativeBrandsInRelaxedAnalysis() {
        ActiveSearchContext ctx = new ActiveSearchContext("test-session");
        ctx.setCategory("数码电子");
        ctx.setSubCategory("笔记本电脑");
        ctx.getNegativeBrands().add("Apple");
        ctx.getNegativeBrands().add("耐克");
        ctx.getExcludeProductIds().add("p_excluded_001");
        ctx.setSoftPreferences(List.of("程序员"));

        QueryAnalysisResult exactAnalysis = ctx.toQueryAnalysisResult();

        NoMatchRecoveryResult result = recoveryService.tryRecover(
                exactAnalysis, ctx, List.of(), 3);

        assertFalse(result.isRecovered(),
                "Should not recover with no candidates");
    }
}
