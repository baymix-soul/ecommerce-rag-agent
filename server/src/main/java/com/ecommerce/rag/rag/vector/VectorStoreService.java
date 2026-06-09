package com.ecommerce.rag.rag.vector;

import java.util.List;

public interface VectorStoreService {

    void upsert(List<EmbeddedRagChunk> chunks);

    List<VectorSearchHit> search(VectorSearchRequest request);

    long count();

    void clear();
}
