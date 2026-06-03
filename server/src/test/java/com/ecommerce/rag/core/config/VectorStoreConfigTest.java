package com.ecommerce.rag.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ecommerce.rag.rag.vector.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VectorStoreConfigTest {

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private AppProperties appProperties;

    @Test
    void shouldDefaultToInMemoryVectorStore() {
        assertEquals("in-memory", appProperties.getVector().getStore());
        assertNotNull(vectorStoreService);
    }

    @Test
    void shouldHaveDefaultQdrantConfig() {
        var qdrant = appProperties.getVector().getQdrant();
        assertEquals("http://localhost:6333", qdrant.getUrl());
        assertEquals("ecommerce_rag_chunks_mock", qdrant.getCollectionName());
        assertEquals(64, qdrant.getVectorSize());
        assertEquals("Cosine", qdrant.getDistance());
        assertFalse(qdrant.isRecreateOnStart());
        assertEquals(10, qdrant.getTimeoutSeconds());
    }

    @Test
    void shouldResolveStoreType() {
        String store = appProperties.getVector().getStore();
        assertNotNull(store);
        assertTrue(store.equals("in-memory") || store.equals("qdrant"));
    }
}
