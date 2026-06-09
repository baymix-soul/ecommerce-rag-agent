package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.ecommerce.rag.rag.vector.RagVectorIndexService;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

@Component
public class VectorRetriever {

    private static final Logger log = LoggerFactory.getLogger(VectorRetriever.class);

    private final RagVectorIndexService vectorIndexService;

    public VectorRetriever(RagVectorIndexService vectorIndexService) {
        this.vectorIndexService = vectorIndexService;
    }

    public List<VectorSearchHit> retrieve(String query, int limit) {
        return retrieveWithFilters(query, limit, null);
    }

    public List<VectorSearchHit> retrieveWithFilters(String query, int limit, Map<String, Object> filters) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        if (vectorIndexService.count() == 0) {
            log.debug("Vector index is empty, returning no hits");
            return Collections.emptyList();
        }
        PerfTraceContext.startSpan("vector.query_embedding");
        List<VectorSearchHit> hits = vectorIndexService.search(query, limit, filters);
        PerfTraceContext.endSpan("vector.query_embedding");
        log.debug("Vector retrieval returned {} hits for query: {}", hits.size(), query);
        PerfTraceContext.addAttribute("vector_hit_count", hits.size());
        return hits;
    }

    public List<VectorSearchHit> retrieveMultiQuery(String mainQuery, List<String> queryVariants,
                                                     int limit, Map<String, Object> filters) {
        Map<String, VectorSearchHit> dedupMap = new LinkedHashMap<>();

        int perQueryLimit = Math.max(3, limit / Math.max(1, (queryVariants != null ? queryVariants.size() : 0) + 1));

        List<VectorSearchHit> mainHits = retrieveWithFilters(mainQuery, limit, filters);
        for (VectorSearchHit hit : mainHits) {
            if (hit.getChunkId() != null) {
                dedupMap.putIfAbsent(hit.getChunkId(), hit);
            }
        }

        if (queryVariants != null) {
            for (String variant : queryVariants) {
                if (variant == null || variant.isBlank() || variant.equals(mainQuery)) continue;
                List<VectorSearchHit> variantHits = retrieveWithFilters(variant, perQueryLimit, filters);
                for (VectorSearchHit hit : variantHits) {
                    if (hit.getChunkId() != null) {
                        dedupMap.putIfAbsent(hit.getChunkId(), hit);
                    }
                }
            }
        }

        List<VectorSearchHit> result = new ArrayList<>(dedupMap.values());
        result.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));

        log.debug("Multi-query vector retrieval: {} hits from main + {} variants",
                result.size(), queryVariants != null ? queryVariants.size() : 0);
        return result;
    }

    public long getVectorStoreCount() {
        return vectorIndexService.count();
    }
}
