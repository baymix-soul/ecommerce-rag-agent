package com.ecommerce.rag.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MockEmbeddingProviderTest {

    private final MockEmbeddingProvider provider = new MockEmbeddingProvider(64);

    @Test
    void shouldProduceSameEmbeddingForSameText() {
        List<Double> v1 = provider.embed("油皮洗面奶");
        List<Double> v2 = provider.embed("油皮洗面奶");

        assertEquals(v1, v2);
    }

    @Test
    void shouldProduceDifferentEmbeddingForDifferentText() {
        List<Double> v1 = provider.embed("油皮洗面奶");
        List<Double> v2 = provider.embed("保湿面霜");

        assertTrue(!v1.equals(v2));
    }

    @Test
    void dimensionShouldMatch() {
        assertEquals(64, provider.dimension());
        assertEquals(64, provider.embed("测试文本").size());
    }

    @Test
    void customDimensionShouldWork() {
        MockEmbeddingProvider p128 = new MockEmbeddingProvider(128);
        assertEquals(128, p128.dimension());
        assertEquals(128, p128.embed("测试").size());
    }

    @Test
    void vectorShouldBeNormalized() {
        List<Double> vector = provider.embed("测试文本");
        double sumSq = 0.0;
        for (double v : vector) {
            sumSq += v * v;
        }
        assertEquals(1.0, sumSq, 0.0001, "L2 norm should be 1.0");
    }

    @Test
    void shouldThrowOnNullText() {
        assertThrows(IllegalArgumentException.class, () -> provider.embed(null));
    }

    @Test
    void shouldThrowOnBlankText() {
        assertThrows(IllegalArgumentException.class, () -> provider.embed("  "));
    }

    @Test
    void embedBatchShouldWork() {
        List<List<Double>> vectors = provider.embedBatch(List.of("A", "B", "C"));

        assertEquals(3, vectors.size());
        for (List<Double> v : vectors) {
            assertEquals(64, v.size());
        }
    }

    @Test
    void embedBatchSameTextsMatchSingleEmbed() {
        List<Double> single = provider.embed("hello");
        List<List<Double>> batch = provider.embedBatch(List.of("hello"));

        assertEquals(single, batch.get(0));
    }

    @Test
    void shouldThrowOnNullBatchTexts() {
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(null));
    }

    @Test
    void shouldThrowOnEmptyBatchTexts() {
        assertThrows(IllegalArgumentException.class, () -> provider.embedBatch(List.of()));
    }

    @Test
    void modelNameShouldBeMockHashEmbedding() {
        assertEquals("mock-hash-embedding", provider.modelName());
    }

    @Test
    void defaultDimensionShouldBe64() {
        MockEmbeddingProvider defaultProvider = new MockEmbeddingProvider();
        assertEquals(64, defaultProvider.dimension());
    }
}
