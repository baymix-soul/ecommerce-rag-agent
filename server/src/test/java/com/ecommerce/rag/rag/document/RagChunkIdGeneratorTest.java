package com.ecommerce.rag.rag.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RagChunkIdGeneratorTest {

    private final RagChunkIdGenerator generator = new RagChunkIdGenerator();

    @Test
    void shouldGenerateSameChunkIdForSameInputs() {
        String id1 = generator.buildChunkId("p_001", ChunkType.PRODUCT_PROFILE, 0);
        String id2 = generator.buildChunkId("p_001", ChunkType.PRODUCT_PROFILE, 0);

        assertEquals(id1, id2);
    }

    @Test
    void shouldGenerateDifferentChunkIdsForDifferentProductIds() {
        String id1 = generator.buildChunkId("p_001", ChunkType.DESCRIPTION, 0);
        String id2 = generator.buildChunkId("p_002", ChunkType.DESCRIPTION, 0);

        assertTrue(!id1.equals(id2));
    }

    @Test
    void shouldGenerateDifferentChunkIdsForDifferentChunkTypes() {
        String id1 = generator.buildChunkId("p_001", ChunkType.PRODUCT_PROFILE, 0);
        String id2 = generator.buildChunkId("p_001", ChunkType.DESCRIPTION, 0);

        assertTrue(!id1.equals(id2));
    }

    @Test
    void shouldGenerateDifferentChunkIdsForDifferentIndices() {
        String id1 = generator.buildChunkId("p_001", ChunkType.DESCRIPTION, 0);
        String id2 = generator.buildChunkId("p_001", ChunkType.DESCRIPTION, 1);

        assertTrue(!id1.equals(id2));
    }

    @Test
    void chunkIdShouldContainProductId() {
        String chunkId = generator.buildChunkId("p_beauty_001", ChunkType.SEARCH_SUMMARY, 0);

        assertTrue(chunkId.startsWith("p_beauty_001::"));
        assertTrue(chunkId.contains("SEARCH_SUMMARY"));
    }

    @Test
    void chunkIdShouldBeHumanReadable() {
        String chunkId = generator.buildChunkId("p_beauty_001", ChunkType.PRODUCT_PROFILE, 0);

        assertEquals("p_beauty_001::PRODUCT_PROFILE::0", chunkId);
    }

    @Test
    void shouldThrowOnNullProductId() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildChunkId(null, ChunkType.DESCRIPTION, 0));
    }

    @Test
    void shouldThrowOnBlankProductId() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildChunkId("  ", ChunkType.DESCRIPTION, 0));
    }

    @Test
    void shouldThrowOnNullChunkType() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildChunkId("p_001", null, 0));
    }

    @Test
    void shouldThrowOnNegativeIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildChunkId("p_001", ChunkType.DESCRIPTION, -1));
    }

    @Test
    void vectorPointIdShouldBeValidUuid() {
        String chunkId = generator.buildChunkId("p_001", ChunkType.DESCRIPTION, 0);
        String vectorPointId = generator.buildVectorPointId(chunkId);

        assertNotNull(vectorPointId);
        UUID uuid = UUID.fromString(vectorPointId);
        assertNotNull(uuid);
    }

    @Test
    void sameChunkIdShouldGenerateSameVectorPointId() {
        String chunkId = "p_001::DESCRIPTION::0";
        String v1 = generator.buildVectorPointId(chunkId);
        String v2 = generator.buildVectorPointId(chunkId);

        assertEquals(v1, v2);
    }

    @Test
    void differentChunkIdsShouldGenerateDifferentVectorPointIds() {
        String v1 = generator.buildVectorPointId("p_001::DESCRIPTION::0");
        String v2 = generator.buildVectorPointId("p_002::DESCRIPTION::0");

        assertTrue(!v1.equals(v2));
    }

    @Test
    void shouldThrowOnNullVectorPointIdInput() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildVectorPointId(null));
    }

    @Test
    void shouldThrowOnBlankVectorPointIdInput() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.buildVectorPointId("  "));
    }
}
