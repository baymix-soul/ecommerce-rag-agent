package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.document.ChunkType;
import com.ecommerce.rag.rag.document.RagDocumentBuilder;
import com.ecommerce.rag.rag.document.RagDocumentService;
import com.ecommerce.rag.rag.embedding.MockEmbeddingProvider;
import com.ecommerce.rag.services.ProductService;

class RagVectorIndexEnhancedChunkTest {

    private RagVectorIndexService vectorIndexService;

    @BeforeEach
    void setUp() {
        List<Product> products = List.of(
                createProductWithExtendedFields("p_enc_001", "测试精华", "品牌A", "美妆护肤", "精华",
                        "大多数用户反馈温和不刺激，清洁力好",
                        "Q:适合油皮吗？A:质地清爽，适合油皮使用",
                        "年度热销TOP1，抗初老首选精华"),
                createProductWithExtendedFields("p_enc_002", "测试面霜", "品牌B", "美妆护肤", "面霜",
                        "保湿效果一流，回购率极高",
                        "Q:孕妇能用吗？A:建议咨询医生后使用",
                        "口碑爆款，销量突破百万"),
                createProductNoExtendedFields("p_enc_003", "基础商品", "品牌C", "数码电子", "耳机")
        );

        ProductService mockProductService = mock(ProductService.class);
        when(mockProductService.listAll()).thenReturn(products);

        RagDocumentBuilder builder = new RagDocumentBuilder();
        RagDocumentService docService = new RagDocumentService(mockProductService, builder);

        AppProperties appProperties = new AppProperties();
        appProperties.getEmbedding().setMockDimension(64);
        appProperties.getVector().getQdrant().setVectorSize(64);

        MockEmbeddingProvider embeddingProvider = new MockEmbeddingProvider(appProperties);
        InMemoryVectorStoreService vectorStore = new InMemoryVectorStoreService();

        vectorIndexService = new RagVectorIndexService(
                docService, embeddingProvider, vectorStore, appProperties);
    }

    @Test
    void rebuildIndexShouldIndexProductsWithNewChunkTypes() {
        int count = vectorIndexService.rebuildIndex();

        assertTrue(count > 0, "Should have indexed chunks");
        assertEquals(count, vectorIndexService.count());
    }

    @Test
    void vectorSearchHitShouldReturnCorrectChunkTypeForReviewSummary() {
        vectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = vectorIndexService.search("温和不刺激 清洁力好", 20);

        boolean foundReview = hits.stream()
                .anyMatch(h -> ChunkType.REVIEW_SUMMARY.name().equals(h.getChunkType()));
        assertTrue(foundReview, "Should find REVIEW_SUMMARY chunk for review-related query");
    }

    @Test
    void vectorSearchHitShouldReturnCorrectChunkTypeForFaq() {
        vectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = vectorIndexService.search("适合油皮吗 孕妇能用吗", 20);

        boolean foundFaq = hits.stream()
                .anyMatch(h -> ChunkType.FAQ.name().equals(h.getChunkType()));
        assertTrue(foundFaq, "Should find FAQ chunk for faq-related query");
    }

    @Test
    void vectorSearchHitShouldReturnCorrectChunkTypeForMarketingCopy() {
        vectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = vectorIndexService.search("热销TOP1 首选精华 爆款", 20);

        boolean foundMarketing = hits.stream()
                .anyMatch(h -> ChunkType.MARKETING_COPY.name().equals(h.getChunkType()));
        assertTrue(foundMarketing, "Should find MARKETING_COPY chunk for marketing-related query");
    }

    @Test
    void vectorSearchCanFindChunksByNewChunkTypes() {
        vectorIndexService.rebuildIndex();

        Map<String, Object> reviewFilter = new LinkedHashMap<>();
        reviewFilter.put("chunk_type", ChunkType.REVIEW_SUMMARY.name());
        List<VectorSearchHit> reviewHits = vectorIndexService.search("温和 清洁", 10, reviewFilter);
        assertTrue(reviewHits.size() > 0, "Should find REVIEW_SUMMARY chunks by filter");

        Map<String, Object> faqFilter = new LinkedHashMap<>();
        faqFilter.put("chunk_type", ChunkType.FAQ.name());
        List<VectorSearchHit> faqHits = vectorIndexService.search("适合 孕妇", 10, faqFilter);
        assertTrue(faqHits.size() > 0, "Should find FAQ chunks by filter");

        Map<String, Object> marketingFilter = new LinkedHashMap<>();
        marketingFilter.put("chunk_type", ChunkType.MARKETING_COPY.name());
        List<VectorSearchHit> marketingHits = vectorIndexService.search("热销 爆款", 10, marketingFilter);
        assertTrue(marketingHits.size() > 0, "Should find MARKETING_COPY chunks by filter");
    }

    @Test
    void searchHitsShouldHaveRequiredFieldsForNewChunkTypes() {
        vectorIndexService.rebuildIndex();

        List<VectorSearchHit> allHits = vectorIndexService.search("温和 不刺激 热销", 20);

        for (VectorSearchHit hit : allHits) {
            assertNotNull(hit.getChunkId());
            assertNotNull(hit.getVectorPointId());
            assertNotNull(hit.getProductId());
            assertNotNull(hit.getChunkType());
            assertNotNull(hit.getScore());
            assertNotNull(hit.getText());
        }
    }

    @Test
    void rebuildIndexShouldNotCallRealArkApi() {
        int count = vectorIndexService.rebuildIndex();

        assertTrue(count > 0, "Should complete rebuild without real API calls");
        assertEquals(count, vectorIndexService.count());
    }

    @Test
    void rebuildIndexShouldNotConnectRealQdrant() {
        int count = vectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = vectorIndexService.search("测试", 5);
        assertNotNull(hits);
        assertTrue(vectorIndexService.count() > 0,
                "Should use InMemoryVectorStoreService, not real Qdrant");
    }

    private Product createProductWithExtendedFields(String id, String name, String brand,
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
        return createBaseProduct(id, name, brand, category, subCategory);
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
