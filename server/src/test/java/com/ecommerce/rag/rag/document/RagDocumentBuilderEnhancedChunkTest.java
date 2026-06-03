package com.ecommerce.rag.rag.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;

class RagDocumentBuilderEnhancedChunkTest {

    private final RagDocumentBuilder builder = new RagDocumentBuilder();

    @Test
    void productWithReviewSummaryShouldGenerateReviewSummaryChunk() {
        Product product = createProduct("p_001", "测试精华", "测试品牌", "美妆护肤", "精华");
        product.setReviewSummary("大多数用户反馈清洁力好，温和不刺激，适合日常使用");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument reviewChunk = chunks.stream()
                .filter(c -> ChunkType.REVIEW_SUMMARY.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        assertEquals("p_001::REVIEW_SUMMARY::0", reviewChunk.getChunkId());
        assertEquals("review_summary", reviewChunk.getSourceField());
        assertTrue(reviewChunk.getText().contains("大多数用户反馈清洁力好"));
        assertTrue(reviewChunk.getText().contains("温和不刺激"));
    }

    @Test
    void productWithFaqSummaryShouldGenerateFaqChunk() {
        Product product = createProduct("p_002", "测试面霜", "测试品牌", "美妆护肤", "面霜");
        product.setFaqSummary("Q:适合油皮吗？A:质地清爽不油腻，油皮可用");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument faqChunk = chunks.stream()
                .filter(c -> ChunkType.FAQ.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        assertEquals("p_002::FAQ::0", faqChunk.getChunkId());
        assertEquals("faq_summary", faqChunk.getSourceField());
        assertTrue(faqChunk.getText().contains("Q:适合油皮吗？"));
        assertTrue(faqChunk.getText().contains("质地清爽不油腻"));
    }

    @Test
    void productWithMarketingCopyShouldGenerateMarketingCopyChunk() {
        Product product = createProduct("p_003", "测试面膜", "测试品牌", "美妆护肤", "面膜");
        product.setMarketingCopy("年度畅销TOP1，百万用户口碑之选，补水效果立竿见影");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument marketingChunk = chunks.stream()
                .filter(c -> ChunkType.MARKETING_COPY.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        assertEquals("p_003::MARKETING_COPY::0", marketingChunk.getChunkId());
        assertEquals("marketing_copy", marketingChunk.getSourceField());
        assertTrue(marketingChunk.getText().contains("年度畅销TOP1"));
        assertTrue(marketingChunk.getText().contains("百万用户口碑之选"));
    }

    @Test
    void productWithNullReviewSummaryShouldNotGenerateReviewSummaryChunk() {
        Product product = createProduct("p_004", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setReviewSummary(null);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasReview = chunks.stream()
                .anyMatch(c -> ChunkType.REVIEW_SUMMARY.name().equals(c.getChunkType()));
        assertFalse(hasReview, "Should not generate REVIEW_SUMMARY chunk when reviewSummary is null");
    }

    @Test
    void productWithNullFaqSummaryShouldNotGenerateFaqChunk() {
        Product product = createProduct("p_005", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setFaqSummary(null);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasFaq = chunks.stream()
                .anyMatch(c -> ChunkType.FAQ.name().equals(c.getChunkType()));
        assertFalse(hasFaq, "Should not generate FAQ chunk when faqSummary is null");
    }

    @Test
    void productWithNullMarketingCopyShouldNotGenerateMarketingCopyChunk() {
        Product product = createProduct("p_006", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setMarketingCopy(null);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasMarketing = chunks.stream()
                .anyMatch(c -> ChunkType.MARKETING_COPY.name().equals(c.getChunkType()));
        assertFalse(hasMarketing, "Should not generate MARKETING_COPY chunk when marketingCopy is null");
    }

    @Test
    void productWithEmptyStringReviewSummaryShouldNotGenerateChunk() {
        Product product = createProduct("p_007", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setReviewSummary("");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasReview = chunks.stream()
                .anyMatch(c -> ChunkType.REVIEW_SUMMARY.name().equals(c.getChunkType()));
        assertFalse(hasReview, "Should not generate REVIEW_SUMMARY chunk when reviewSummary is empty");
    }

    @Test
    void deterministicRebuildWithAllFieldsShouldProduceSameChunkIds() {
        Product product = createProduct("p_008", "全能商品", "测试品牌", "美妆护肤", "精华");
        product.setReviewSummary("用户评价很好，回购率高");
        product.setFaqSummary("Q:适合孕妇吗？A:建议咨询医生");
        product.setMarketingCopy("明星产品，限时特惠");

        List<RagChunkDocument> chunks1 = builder.buildChunks(product);
        List<RagChunkDocument> chunks2 = builder.buildChunks(product);

        assertEquals(chunks1.size(), chunks2.size());
        for (int i = 0; i < chunks1.size(); i++) {
            assertEquals(chunks1.get(i).getChunkId(), chunks2.get(i).getChunkId());
            assertEquals(chunks1.get(i).getChunkType(), chunks2.get(i).getChunkType());
        }

        boolean hasReview = chunks1.stream()
                .anyMatch(c -> ChunkType.REVIEW_SUMMARY.name().equals(c.getChunkType()));
        boolean hasFaq = chunks1.stream()
                .anyMatch(c -> ChunkType.FAQ.name().equals(c.getChunkType()));
        boolean hasMarketing = chunks1.stream()
                .anyMatch(c -> ChunkType.MARKETING_COPY.name().equals(c.getChunkType()));
        assertTrue(hasReview, "Product with reviewSummary should generate REVIEW_SUMMARY chunk");
        assertTrue(hasFaq, "Product with faqSummary should generate FAQ chunk");
        assertTrue(hasMarketing, "Product with marketingCopy should generate MARKETING_COPY chunk");
    }

    @Test
    void extendedChunksShouldHaveCorrectMetadata() {
        Product product = createProduct("p_009", "测试商品", "测试品牌", "美妆护肤", "精华");
        product.setReviewSummary("好评如潮");
        product.setFaqSummary("常见问题解答");
        product.setMarketingCopy("核心卖点文案");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument reviewChunk = chunks.stream()
                .filter(c -> ChunkType.REVIEW_SUMMARY.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();
        assertEquals("p_009", reviewChunk.getMetadata().get("product_id"));
        assertEquals("美妆护肤", reviewChunk.getMetadata().get("category"));
        assertNotNull(reviewChunk.getVectorPointId());

        RagChunkDocument faqChunk = chunks.stream()
                .filter(c -> ChunkType.FAQ.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();
        assertEquals("测试品牌", faqChunk.getMetadata().get("brand"));
        assertNotNull(faqChunk.getText());

        RagChunkDocument marketingChunk = chunks.stream()
                .filter(c -> ChunkType.MARKETING_COPY.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();
        assertEquals("p_009", marketingChunk.getProductId());
        assertEquals("marketing_copy", marketingChunk.getSourceField());
    }

    private Product createProduct(String productId, String name, String brand,
                                  String category, String subCategory) {
        Product product = new Product();
        product.setProductId(productId);
        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setPrice(new BigDecimal("100"));
        product.setCurrency("CNY");
        product.setAvgRating(4.0);
        return product;
    }
}
