package com.ecommerce.rag.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.rag.embedding.EmbeddingProvider;
import com.ecommerce.rag.rag.vector.RagVectorIndexService;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

@RestController
public class RagVectorController {

    private final RagVectorIndexService ragVectorIndexService;
    private final EmbeddingProvider embeddingProvider;

    public RagVectorController(RagVectorIndexService ragVectorIndexService,
                               EmbeddingProvider embeddingProvider) {
        this.ragVectorIndexService = ragVectorIndexService;
        this.embeddingProvider = embeddingProvider;
    }

    @PostMapping("/api/rag/vector-index/rebuild")
    public ResponseEntity<Map<String, Object>> rebuild() {
        int indexedChunks = ragVectorIndexService.rebuildIndex();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("indexed_chunks", indexedChunks);
        result.put("vector_store_count", ragVectorIndexService.count());
        result.put("embedding_model", embeddingProvider.modelName());
        result.put("dimension", embeddingProvider.dimension());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/rag/vector-index/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", ragVectorIndexService.count());
        result.put("embedding_model", embeddingProvider.modelName());
        result.put("dimension", embeddingProvider.dimension());

        return ResponseEntity.ok(result);
    }

    @PostMapping({"/api/rag/vector-search", "/api/rag/vector-index/search"})
    public ResponseEntity<Map<String, Object>> vectorSearch(
            @RequestBody VectorSearchBody body) {
        String query = body.query != null ? body.query : "";
        int limit = body.limit > 0 ? Math.min(body.limit, 50) : 10;

        List<VectorSearchHit> hits = ragVectorIndexService.search(query, limit);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("total", hits.size());
        result.put("hits", hits);

        return ResponseEntity.ok(result);
    }

    public static class VectorSearchBody {
        public String query;
        public int limit = 10;
    }
}
