package com.ecommerce.rag.rag.vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.document.RagChunkDocument;
import com.ecommerce.rag.rag.document.RagDocumentService;
import com.ecommerce.rag.rag.embedding.EmbeddingProvider;

@Service
public class RagVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(RagVectorIndexService.class);

    private final RagDocumentService ragDocumentService;
    private final EmbeddingProvider embeddingProvider;
    private final VectorStoreService vectorStoreService;
    private final AppProperties appProperties;

    public RagVectorIndexService(RagDocumentService ragDocumentService,
                                  EmbeddingProvider embeddingProvider,
                                  VectorStoreService vectorStoreService,
                                  AppProperties appProperties) {
        this.ragDocumentService = ragDocumentService;
        this.embeddingProvider = embeddingProvider;
        this.vectorStoreService = vectorStoreService;
        this.appProperties = appProperties;
    }

    public int rebuildIndex() {
        int embDim = embeddingProvider.dimension();
        int storeDim = appProperties.getVector().getQdrant().getVectorSize();

        if (embDim != storeDim) {
            throw new IllegalArgumentException(
                    "Embedding dimension " + embDim + " does not match vector store dimension " + storeDim
                            + ". Please update QDRANT_VECTOR_SIZE and rebuild collection.");
        }

        List<RagChunkDocument> allChunks = ragDocumentService.buildAllChunks();
        log.info("Rebuilding index with {} chunks, embedding dimension={}", allChunks.size(), embDim);

        List<String> texts = new ArrayList<>(allChunks.size());
        for (RagChunkDocument chunk : allChunks) {
            texts.add(chunk.getText());
        }

        List<List<Double>> vectors = embeddingProvider.embedBatch(texts);

        List<EmbeddedRagChunk> embeddedChunks = new ArrayList<>(allChunks.size());
        for (int i = 0; i < allChunks.size(); i++) {
            RagChunkDocument chunk = allChunks.get(i);
            EmbeddedRagChunk embedded = new EmbeddedRagChunk();
            embedded.setChunkId(chunk.getChunkId());
            embedded.setVectorPointId(chunk.getVectorPointId());
            embedded.setProductId(chunk.getProductId());
            embedded.setChunkType(chunk.getChunkType());
            embedded.setText(chunk.getText());
            embedded.setVector(vectors.get(i));
            embedded.setPayload(buildPayload(chunk));
            embeddedChunks.add(embedded);
        }

        vectorStoreService.clear();
        vectorStoreService.upsert(embeddedChunks);

        log.info("Index rebuilt with {} embedded chunks", embeddedChunks.size());
        return embeddedChunks.size();
    }

    public List<VectorSearchHit> search(String query, int limit) {
        return search(query, limit, null);
    }

    public List<VectorSearchHit> search(String query, int limit, Map<String, Object> filters) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Double> queryVector = embeddingProvider.embed(query);

        VectorSearchRequest request = new VectorSearchRequest();
        request.setQueryVector(queryVector);
        request.setQueryText(query);
        request.setLimit(limit);
        if (filters != null && !filters.isEmpty()) {
            request.setFilters(filters);
        }

        return vectorStoreService.search(request);
    }

    public long count() {
        return vectorStoreService.count();
    }

    private Map<String, Object> buildPayload(RagChunkDocument chunk) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chunk_id", chunk.getChunkId());
        payload.put("vector_point_id", chunk.getVectorPointId());
        payload.put("parent_id", chunk.getParentId());
        payload.put("product_id", chunk.getProductId());
        payload.put("chunk_type", chunk.getChunkType());
        payload.put("source_field", chunk.getSourceField());
        payload.put("text", chunk.getText());
        payload.put("name", chunk.getName());
        payload.put("brand", chunk.getBrand());
        payload.put("category", chunk.getCategory());
        payload.put("sub_category", chunk.getSubCategory());
        if (chunk.getPrice() != null) {
            payload.put("price", chunk.getPrice().doubleValue());
        }
        if (chunk.getCurrency() != null) {
            payload.put("currency", chunk.getCurrency());
        }
        if (chunk.getAvgRating() != null) {
            payload.put("avg_rating", chunk.getAvgRating());
        }
        if (chunk.getImageUrl() != null) {
            payload.put("image_url", chunk.getImageUrl());
        }
        return payload;
    }
}
