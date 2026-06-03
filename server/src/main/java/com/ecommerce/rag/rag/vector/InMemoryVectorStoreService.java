package com.ecommerce.rag.rag.vector;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InMemoryVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorStoreService.class);

    private final ConcurrentHashMap<String, EmbeddedRagChunk> store = new ConcurrentHashMap<>();

    @Override
    public void upsert(List<EmbeddedRagChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (EmbeddedRagChunk chunk : chunks) {
            if (chunk.getVectorPointId() != null && !chunk.getVectorPointId().isBlank()) {
                store.put(chunk.getVectorPointId(), chunk);
            }
        }
        log.info("Upserted {} chunks, store now has {} entries", chunks.size(), store.size());
    }

    @Override
    public List<VectorSearchHit> search(VectorSearchRequest request) {
        if (request.getQueryVector() == null || request.getQueryVector().isEmpty()) {
            return List.of();
        }

        List<VectorSearchHit> scored = new ArrayList<>();
        for (EmbeddedRagChunk storedChunk : store.values()) {
            if (storedChunk.getVector() == null || storedChunk.getVector().isEmpty()) {
                continue;
            }
            if (!passesFilter(storedChunk, request.getFilters())) {
                continue;
            }
            double score = cosineSimilarity(request.getQueryVector(), storedChunk.getVector());
            if (request.getMinScore() != null && score < request.getMinScore()) {
                continue;
            }
            scored.add(toHit(storedChunk, score));
        }

        scored.sort(Comparator.comparingDouble(VectorSearchHit::getScore).reversed());

        int limit = request.getEffectiveLimit();
        return scored.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void clear() {
        store.clear();
        log.info("Vector store cleared");
    }

    private boolean passesFilter(EmbeddedRagChunk chunk, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        Map<String, Object> payload = chunk.getPayload();

        if (filters.containsKey("category")) {
            String expected = filters.get("category").toString();
            if (!expected.equals(payload.get("category"))) return false;
        }
        if (filters.containsKey("sub_category")) {
            String expected = filters.get("sub_category").toString();
            if (!expected.equals(payload.get("sub_category"))) return false;
        }
        if (filters.containsKey("brand")) {
            String expected = filters.get("brand").toString();
            if (!expected.equals(payload.get("brand"))) return false;
        }
        if (filters.containsKey("product_id")) {
            String expected = filters.get("product_id").toString();
            if (!expected.equals(payload.get("product_id"))) return false;
        }
        if (filters.containsKey("chunk_type")) {
            String expected = filters.get("chunk_type").toString();
            if (!expected.equals(payload.get("chunk_type"))) return false;
        }
        if (filters.containsKey("max_price")) {
            double maxPrice = toDouble(filters.get("max_price"));
            Object priceObj = payload.get("price");
            if (priceObj != null) {
                double price = toDouble(priceObj);
                if (price > maxPrice) return false;
            }
        }
        if (filters.containsKey("min_price")) {
            double minPrice = toDouble(filters.get("min_price"));
            Object priceObj = payload.get("price");
            if (priceObj != null) {
                double price = toDouble(priceObj);
                if (price < minPrice) return false;
            }
        }

        return true;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return 0.0;
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private VectorSearchHit toHit(EmbeddedRagChunk chunk, double score) {
        VectorSearchHit hit = new VectorSearchHit();
        hit.setChunkId(chunk.getChunkId());
        hit.setVectorPointId(chunk.getVectorPointId());
        hit.setProductId(chunk.getProductId());
        hit.setChunkType(chunk.getChunkType());
        hit.setScore(score);
        hit.setText(chunk.getText());
        hit.setSourceField(objectToString(chunk.getPayload().get("source_field")));
        hit.setPayload(chunk.getPayload());
        return hit;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private String objectToString(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}
