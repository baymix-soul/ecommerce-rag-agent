package com.ecommerce.rag.rag.document;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RagChunkIdGenerator {

    private static final String SEPARATOR = "::";
    private static final String NAMESPACE_PREFIX = "rag-chunk:";

    public String buildChunkId(String productId, ChunkType chunkType, int index) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be null or blank");
        }
        if (chunkType == null) {
            throw new IllegalArgumentException("chunkType must not be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        return productId + SEPARATOR + chunkType.name() + SEPARATOR + index;
    }

    public String buildVectorPointId(String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be null or blank");
        }
        String name = NAMESPACE_PREFIX + chunkId;
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
