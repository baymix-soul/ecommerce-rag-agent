package com.ecommerce.rag.rag.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RagDocumentServiceTest {

    @Autowired
    private RagDocumentService ragDocumentService;

    @Autowired
    private RagDocumentBuilder documentBuilder;

    @Test
    void shouldBuildAllChunksFromAllProducts() {
        List<RagChunkDocument> allChunks = ragDocumentService.buildAllChunks();

        assertNotNull(allChunks);
        assertTrue(allChunks.size() > 0, "Should have at least some chunks");
    }

    @Test
    void shouldGenerateReasonableNumberOfChunksFor100Products() {
        List<RagChunkDocument> allChunks = ragDocumentService.buildAllChunks();

        long profileChunks = allChunks.stream()
                .filter(c -> ChunkType.PRODUCT_PROFILE.name().equals(c.getChunkType())).count();
        long descChunks = allChunks.stream()
                .filter(c -> ChunkType.DESCRIPTION.name().equals(c.getChunkType())).count();
        long summaryChunks = allChunks.stream()
                .filter(c -> ChunkType.SEARCH_SUMMARY.name().equals(c.getChunkType())).count();

        assertTrue(profileChunks >= 50, "Should have at least 50 PRODUCT_PROFILE chunks");
        assertTrue(descChunks >= 50, "Should have at least 50 DESCRIPTION chunks");
        assertTrue(summaryChunks >= 50, "Should have at least 50 SEARCH_SUMMARY chunks");
    }

    @Test
    void shouldCountByChunkType() {
        Map<String, Long> counts = ragDocumentService.countByChunkType();

        assertNotNull(counts);
        assertTrue(counts.containsKey(ChunkType.PRODUCT_PROFILE.name()));
        assertTrue(counts.containsKey(ChunkType.DESCRIPTION.name()));
        assertTrue(counts.containsKey(ChunkType.SEARCH_SUMMARY.name()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        assertTrue(total > 0, "Total chunks should be positive");

        long profileCount = counts.getOrDefault(ChunkType.PRODUCT_PROFILE.name(), 0L);
        long descCount = counts.getOrDefault(ChunkType.DESCRIPTION.name(), 0L);
        assertEquals(profileCount, descCount,
                "PRODUCT_PROFILE and DESCRIPTION counts should match");
    }

    @Test
    void shouldBuildChunksByExistingProductId() {
        List<RagChunkDocument> firstChunks = ragDocumentService.buildAllChunks();
        String firstProductId = firstChunks.get(0).getProductId();

        List<RagChunkDocument> chunks = ragDocumentService.buildChunksByProductId(firstProductId);

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 3);
        for (RagChunkDocument chunk : chunks) {
            assertEquals(firstProductId, chunk.getProductId());
        }
    }

    @Test
    void shouldThrowExceptionForNonExistentProductId() {
        assertThrows(
                com.ecommerce.rag.core.exception.ProductNotFoundException.class,
                () -> ragDocumentService.buildChunksByProductId("non_existent_id_99999")
        );
    }
}
