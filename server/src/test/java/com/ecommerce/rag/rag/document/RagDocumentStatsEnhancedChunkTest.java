package com.ecommerce.rag.rag.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

class RagDocumentStatsEnhancedChunkTest {

    private RagDocumentService ragDocumentService;

    @BeforeEach
    void setUp() {
        List<Product> products = List.of(
                createProductWithAllFields("p_001", "商品A", "品牌A", "美妆护肤", "精华",
                        "用户好评如潮", "适合油皮吗？适合的", "明星单品热卖中"),
                createProductWithAllFields("p_002", "商品B", "品牌B", "美妆护肤", "面霜",
                        "回购率很高", "敏感肌能用吗？可以", "口碑爆款"),
                createProductNoExtendedFields("p_003", "商品C", "品牌C", "数码电子", "耳机"),
                createProductWithReviewOnly("p_004", "商品D", "品牌D", "美妆护肤", "洁面", "温和不刺激"),
                createProductWithFaqOnly("p_005", "商品E", "品牌E", "美妆护肤", "面膜", "多久用一次？每周2-3次")
        );

        ProductService mockProductService = mock(ProductService.class);
        when(mockProductService.listAll()).thenReturn(products);

        RagDocumentBuilder builder = new RagDocumentBuilder();
        ragDocumentService = new RagDocumentService(mockProductService, builder);
    }

    @Test
    void statsShouldIncludeReviewSummaryCount() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        assertNotNull(counts);
        assertTrue(counts.containsKey(ChunkType.REVIEW_SUMMARY.name()));

        long reviewCount = counts.getOrDefault(ChunkType.REVIEW_SUMMARY.name(), 0L);
        assertEquals(3, reviewCount, "3 products have reviewSummary: p_001, p_002, p_004");
    }

    @Test
    void statsShouldIncludeFaqCount() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        assertNotNull(counts);
        assertTrue(counts.containsKey(ChunkType.FAQ.name()));

        long faqCount = counts.getOrDefault(ChunkType.FAQ.name(), 0L);
        assertEquals(3, faqCount, "3 products have faqSummary: p_001, p_002, p_005");
    }

    @Test
    void statsShouldIncludeMarketingCopyCount() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        assertNotNull(counts);
        assertTrue(counts.containsKey(ChunkType.MARKETING_COPY.name()));

        long mcCount = counts.getOrDefault(ChunkType.MARKETING_COPY.name(), 0L);
        assertEquals(2, mcCount, "2 products have marketingCopy: p_001, p_002");
    }

    @Test
    void totalChunksShouldEqualSumOfAllChunkTypes() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        long expectedProfile = 5;
        long expectedDesc = 5;
        long expectedSpecs = 2;
        long expectedSearch = 5;
        long expectedReview = 3;
        long expectedFaq = 3;
        long expectedMarketing = 2;
        long expectedTotal = expectedProfile + expectedDesc + expectedSpecs
                + expectedSearch + expectedReview + expectedFaq + expectedMarketing;

        assertEquals(expectedTotal, total, "Total chunks should equal sum of all chunk types");
    }

    @Test
    void statsShouldShowCorrectNumbersForAll7ChunkTypes() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        assertEquals(7, counts.size(), "Should have exactly 7 chunk types");

        assertEquals(5L, counts.get(ChunkType.PRODUCT_PROFILE.name()));
        assertEquals(5L, counts.get(ChunkType.DESCRIPTION.name()));
        assertEquals(2L, counts.get(ChunkType.SPECS.name()));
        assertEquals(5L, counts.get(ChunkType.SEARCH_SUMMARY.name()));
        assertEquals(3L, counts.get(ChunkType.REVIEW_SUMMARY.name()));
        assertEquals(3L, counts.get(ChunkType.FAQ.name()));
        assertEquals(2L, counts.get(ChunkType.MARKETING_COPY.name()));
    }

    private Product createProductWithAllFields(String id, String name, String brand,
                                                String category, String subCategory,
                                                String review, String faq, String marketing) {
        Product p = createBaseProduct(id, name, brand, category, subCategory);
        p.setReviewSummary(review);
        p.setFaqSummary(faq);
        p.setMarketingCopy(marketing);
        return p;
    }

    private Product createProductNoExtendedFields(String id, String name, String brand,
                                                   String category, String subCategory) {
        Product p = createBaseProduct(id, name, brand, category, subCategory);
        p.setSpecs(null);
        return p;
    }

    private Product createProductWithReviewOnly(String id, String name, String brand,
                                                 String category, String subCategory,
                                                 String review) {
        Product p = createBaseProduct(id, name, brand, category, subCategory);
        p.setSpecs(null);
        p.setReviewSummary(review);
        return p;
    }

    private Product createProductWithFaqOnly(String id, String name, String brand,
                                              String category, String subCategory,
                                              String faq) {
        Product p = createBaseProduct(id, name, brand, category, subCategory);
        p.setSpecs(null);
        p.setFaqSummary(faq);
        return p;
    }

    private Product createBaseProduct(String id, String name, String brand,
                                       String category, String subCategory) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setBrand(brand);
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(new BigDecimal("100"));
        p.setCurrency("CNY");
        p.setAvgRating(4.0);
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("容量", "100ml");
        p.setSpecs(specs);
        return p;
    }
}
