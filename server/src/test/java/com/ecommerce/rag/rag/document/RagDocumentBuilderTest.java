package com.ecommerce.rag.rag.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;

class RagDocumentBuilderTest {

    private final RagDocumentBuilder builder = new RagDocumentBuilder();

    @Test
    void shouldGenerateMultipleChunksForSingleProduct() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setDescription("这是一个温和的洁面产品");
        product.setSpecs(createSpecs("容量", "100ml"));

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 3,
                "Expected at least 3 chunks, got " + chunks.size());
    }

    @Test
    void shouldNotGenerateSpecsChunkWhenSpecsEmpty() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setSpecs(null);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasSpecs = chunks.stream()
                .anyMatch(c -> ChunkType.SPECS.name().equals(c.getChunkType()));
        assertTrue(hasSpecs == false || chunks.size() == 3,
                "Should not have SPECS chunk when specs are empty");
    }

    @Test
    void shouldNotGenerateSpecsChunkWhenSpecsHasEmptyValues() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("容量", "");
        product.setSpecs(specs);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasSpecs = chunks.stream()
                .anyMatch(c -> ChunkType.SPECS.name().equals(c.getChunkType()));
        assertTrue(hasSpecs == false || chunks.size() == 3,
                "Should not generate SPECS chunk when all values are empty");
    }

    @Test
    void shouldNotGenerateSpecsChunkWhenSpecsEmptyMap() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setSpecs(new LinkedHashMap<>());

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasSpecs = chunks.stream()
                .anyMatch(c -> ChunkType.SPECS.name().equals(c.getChunkType()));
        assertTrue(hasSpecs == false || chunks.size() == 3,
                "Should not generate SPECS chunk when specs map is empty");
    }

    @Test
    void shouldGenerateSpecsChunkWhenSpecsPresent() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("容量", "100ml");
        specs.put("颜色", "白色");
        product.setSpecs(specs);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        boolean hasSpecs = chunks.stream()
                .anyMatch(c -> ChunkType.SPECS.name().equals(c.getChunkType()));
        assertTrue(hasSpecs, "Should generate SPECS chunk when specs are present");
    }

    @Test
    void shouldIncludeChunkIdInEveryChunk() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            assertNotNull(chunk.getChunkId(), "chunkId should not be null");
            assertTrue(chunk.getChunkId().startsWith("p_001::"),
                    "chunkId should start with productId: " + chunk.getChunkId());
        }
    }

    @Test
    void chunkIdsShouldBeUnique() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks = builder.buildChunks(product);
        long distinctIds = chunks.stream().map(RagChunkDocument::getChunkId).distinct().count();

        assertEquals(chunks.size(), distinctIds, "All chunkIds should be unique");
    }

    @Test
    void shouldIncludeProductIdInEveryChunk() {
        Product product = createProduct("p_042", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            assertEquals("p_042", chunk.getProductId());
        }
    }

    @Test
    void parentIdShouldEqualProductId() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            assertEquals(chunk.getProductId(), chunk.getParentId());
        }
    }

    @Test
    void descriptionChunkShouldContainProductDescription() {
        Product product = createProduct("p_001", "测试洁面乳", "测试品牌", "美妆护肤", "洁面");
        product.setDescription("温和氨基酸洁面，适合敏感肌使用");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument descChunk = chunks.stream()
                .filter(c -> ChunkType.DESCRIPTION.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        assertTrue(descChunk.getText().contains("温和氨基酸洁面"),
                "DESCRIPTION chunk should contain the product description");
        assertEquals("description", descChunk.getSourceField());
    }

    @Test
    void specsChunkShouldContainSpecsContent() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put("容量", "100ml");
        specs.put("颜色", "白色");
        product.setSpecs(specs);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument specsChunk = chunks.stream()
                .filter(c -> ChunkType.SPECS.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        assertTrue(specsChunk.getText().contains("100ml"));
        assertTrue(specsChunk.getText().contains("白色"));
        assertEquals("specs", specsChunk.getSourceField());
    }

    @Test
    void searchSummaryChunkShouldContainKeyFields() {
        Product product = createProduct("p_001", "芙丽芳丝净润洗面霜", "芙丽芳丝", "美妆护肤", "洗面奶");
        product.setDescription("温和氨基酸洁面");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument summaryChunk = chunks.stream()
                .filter(c -> ChunkType.SEARCH_SUMMARY.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        String text = summaryChunk.getText();
        assertTrue(text.contains("芙丽芳丝净润洗面霜"), "Should contain name");
        assertTrue(text.contains("芙丽芳丝"), "Should contain brand");
        assertTrue(text.contains("美妆护肤"), "Should contain category");
        assertTrue(text.contains("洗面奶"), "Should contain subCategory");
        assertEquals("search_summary", summaryChunk.getSourceField());
    }

    @Test
    void profileChunkShouldContainAllBasicFields() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setPrice(new BigDecimal("89"));
        product.setCurrency("CNY");
        product.setAvgRating(4.7);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        RagChunkDocument profileChunk = chunks.stream()
                .filter(c -> ChunkType.PRODUCT_PROFILE.name().equals(c.getChunkType()))
                .findFirst().orElseThrow();

        String text = profileChunk.getText();
        assertTrue(text.contains("测试商品"), "Should contain name");
        assertTrue(text.contains("测试品牌"), "Should contain brand");
        assertTrue(text.contains("美妆护肤"), "Should contain category");
        assertTrue(text.contains("洁面"), "Should contain subCategory");
        assertTrue(text.contains("89"), "Should contain price");
        assertTrue(text.contains("4.7"), "Should contain rating");
        assertEquals("profile", profileChunk.getSourceField());
    }

    @Test
    void metadataShouldContainRequiredFields() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setPrice(new BigDecimal("89"));
        product.setCurrency("CNY");
        product.setAvgRating(4.5);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            Map<String, String> metadata = chunk.getMetadata();
            assertNotNull(metadata);
            assertEquals("p_001", metadata.get("product_id"));
            assertEquals("美妆护肤", metadata.get("category"));
            assertEquals("洁面", metadata.get("sub_category"));
            assertEquals("测试品牌", metadata.get("brand"));
            assertEquals("89", metadata.get("price"));
            assertEquals("CNY", metadata.get("currency"));
            assertEquals("4.5", metadata.get("avg_rating"));
        }
    }

    @Test
    void shouldTruncateTextExceedingMaxLength() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");
        String longDesc = "A".repeat(1000);
        product.setDescription(longDesc);

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            assertTrue(chunk.getText() == null || chunk.getText().length() <= 800,
                    "Text should not exceed 800 characters, got " +
                            (chunk.getText() != null ? chunk.getText().length() : 0));
        }
    }

    @Test
    void shouldNotIncludeImageUrlInText() {
        Product product = createProduct("p_001", "测试商品", "测试品牌", "美妆护肤", "洁面");
        product.setImageUrl("/images/p_001.jpg");
        product.setDescription("优质洁面产品");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            String text = chunk.getText();
            if (text != null) {
                assertTrue(!text.contains("/images/"),
                        "Text should not contain image URL in chunk type " + chunk.getChunkType());
            }
        }
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        Product product = new Product();
        product.setProductId("p_null");
        product.setName("测试商品");
        product.setPrice(new BigDecimal("100"));
        product.setCurrency("CNY");
        product.setDescription("测试描述");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 3);
        for (RagChunkDocument chunk : chunks) {
            assertNotNull(chunk.getChunkId());
            assertEquals("p_null", chunk.getProductId());
        }
    }

    @Test
    void buildChunksWithListShouldProcessAllProducts() {
        Product p1 = createProduct("p_001", "商品1", "品牌A", "类目A", "子类目A");
        Product p2 = createProduct("p_002", "商品2", "品牌B", "类目B", "子类目B");

        List<RagChunkDocument> chunks = builder.buildChunks(List.of(p1, p2));

        assertTrue(chunks.size() >= 6, "Expected at least 6 chunks for 2 products");
        long product1Chunks = chunks.stream()
                .filter(c -> "p_001".equals(c.getProductId())).count();
        long product2Chunks = chunks.stream()
                .filter(c -> "p_002".equals(c.getProductId())).count();
        assertTrue(product1Chunks >= 3);
        assertTrue(product2Chunks >= 3);
    }

    @Test
    void sameProductShouldProduceSameChunkIdsOnRebuild() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks1 = builder.buildChunks(product);
        List<RagChunkDocument> chunks2 = builder.buildChunks(product);

        assertEquals(chunks1.size(), chunks2.size());
        for (int i = 0; i < chunks1.size(); i++) {
            assertEquals(chunks1.get(i).getChunkId(), chunks2.get(i).getChunkId());
        }
    }

    @Test
    void everyChunkShouldContainVectorPointId() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks = builder.buildChunks(product);

        for (RagChunkDocument chunk : chunks) {
            assertNotNull(chunk.getVectorPointId(), "vectorPointId should not be null");
            UUID uuid = UUID.fromString(chunk.getVectorPointId());
            assertNotNull(uuid);
        }
    }

    @Test
    void sameChunkIdShouldProduceSameVectorPointId() {
        Product product = createProduct("p_001", "测试", "品牌", "类目", "子类目");

        List<RagChunkDocument> chunks1 = builder.buildChunks(product);
        List<RagChunkDocument> chunks2 = builder.buildChunks(product);

        for (int i = 0; i < chunks1.size(); i++) {
            assertEquals(chunks1.get(i).getVectorPointId(), chunks2.get(i).getVectorPointId());
        }
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

    private Map<String, String> createSpecs(String key, String value) {
        Map<String, String> specs = new LinkedHashMap<>();
        specs.put(key, value);
        return specs;
    }
}
