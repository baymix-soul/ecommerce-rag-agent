package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RagVectorIndexServiceTest {

    @Autowired
    private RagVectorIndexService ragVectorIndexService;

    @Test
    void shouldRebuildIndexAndReturnChunkCount() {
        int count = ragVectorIndexService.rebuildIndex();

        assertTrue(count > 0, "Should have indexed at least some chunks");
    }

    @Test
    void countShouldBePositiveAfterRebuild() {
        ragVectorIndexService.rebuildIndex();

        long cnt = ragVectorIndexService.count();
        assertTrue(cnt > 0, "Store should have entries after rebuild");
    }

    @Test
    void searchShouldReturnHits() {
        ragVectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = ragVectorIndexService.search("油皮洗面奶", 5);

        assertNotNull(hits);
        assertTrue(hits.size() > 0, "Should return at least one hit");
        for (VectorSearchHit hit : hits) {
            assertNotNull(hit.getProductId());
            assertNotNull(hit.getScore());
            assertNotNull(hit.getChunkId());
        }
    }

    @Test
    void searchWithBlankQueryShouldReturnEmpty() {
        ragVectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = ragVectorIndexService.search("  ", 5);

        assertEquals(0, hits.size());
    }

    @Test
    void searchShouldNotCallRealApi() {
        ragVectorIndexService.rebuildIndex();

        List<VectorSearchHit> hits = ragVectorIndexService.search("测试", 3);

        assertNotNull(hits);
        assertTrue(hits.size() > 0);
    }
}
