package com.ecommerce.rag.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;

class MockEmbeddingProviderTest {

    private final AppProperties appProperties64 = createAppProperties(64);
    private final MockEmbeddingProvider provider = new MockEmbeddingProvider(appProperties64);

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
        AppProperties appProperties128 = createAppProperties(128);
        MockEmbeddingProvider p128 = new MockEmbeddingProvider(appProperties128);
        assertEquals(128, p128.dimension());
        assertEquals(128, p128.embed("测试").size());
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
        assertThrows(NullPointerException.class, () -> provider.embedBatch(null));
    }

    @Test
    void shouldReturnEmptyForEmptyBatchTexts() {
        List<List<Double>> result = provider.embedBatch(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void modelNameShouldBeMockHashEmbedding() {
        assertEquals("mock-hash-embedding", provider.modelName());
    }

    private AppProperties createAppProperties(int mockDimension) {
        AppProperties props = new AppProperties();
        props.getEmbedding().setMockDimension(mockDimension);
        return props;
    }
}
