package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.rag.vector.VectorSearchHit;

@SpringBootTest
class VectorRetrieverTest {

    @Autowired
    private VectorRetriever vectorRetriever;

    @Test
    void shouldReturnEmptyWhenIndexNotBuilt() {
        List<VectorSearchHit> hits = vectorRetriever.retrieve("油皮洗面奶", 5);

        assertNotNull(hits);
        assertEquals(0, hits.size());
    }

    @Test
    void shouldReturnEmptyForBlankQuery() {
        List<VectorSearchHit> hits = vectorRetriever.retrieve("", 5);

        assertNotNull(hits);
        assertTrue(hits.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullQuery() {
        List<VectorSearchHit> hits = vectorRetriever.retrieve(null, 5);

        assertNotNull(hits);
        assertTrue(hits.isEmpty());
    }

    @Test
    void vectorStoreCountShouldBeZeroWhenNotBuilt() {
        assertEquals(0, vectorRetriever.getVectorStoreCount());
    }
}
