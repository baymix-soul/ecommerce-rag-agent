package com.ecommerce.rag.rag.vector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QdrantVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreService.class);

    private static final int MAX_BATCH_SIZE = 500;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final QdrantCollectionManager collectionManager;
    private final QdrantFilterBuilder filterBuilder;
    private final AppProperties.QdrantProperties config;

    public QdrantVectorStoreService(AppProperties.QdrantProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.filterBuilder = new QdrantFilterBuilder();
        this.collectionManager = new QdrantCollectionManager(config, objectMapper);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    @Override
    public void upsert(List<EmbeddedRagChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        collectionManager.createCollectionIfAbsent();

        int total = chunks.size();
        for (int i = 0; i < total; i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, total);
            List<EmbeddedRagChunk> batch = chunks.subList(i, end);
            upsertBatch(batch);
        }
        log.info("Upserted {} chunks to Qdrant", total);
    }

    @Override
    public List<VectorSearchHit> search(VectorSearchRequest request) {
        if (request.getQueryVector() == null || request.getQueryVector().isEmpty()) {
            return List.of();
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode vectorArray = objectMapper.valueToTree(request.getQueryVector());
            body.set("vector", vectorArray);
            body.put("limit", request.getEffectiveLimit());
            body.put("with_payload", true);

            Map<String, Object> qdrantFilter = filterBuilder.build(request.getFilters());
            if (qdrantFilter != null) {
                body.set("filter", objectMapper.valueToTree(qdrantFilter));
            }
            if (request.getMinScore() != null) {
                body.put("score_threshold", request.getMinScore());
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = buildRequest("POST",
                    "/collections/" + config.getCollectionName() + "/points/search", json);

            PerfTraceContext.startSpan("qdrant.search_http");
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            PerfTraceContext.endSpan("qdrant.search_http");

            int statusCode = response.statusCode();
            PerfTraceContext.addAttribute("qdrant_status_code", statusCode);
            if (statusCode != 200) {
                log.warn("Qdrant search failed: {} {}", statusCode, response.body());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("result");

            List<VectorSearchHit> hits = new ArrayList<>();
            for (JsonNode point : results) {
                VectorSearchHit hit = new VectorSearchHit();
                hit.setVectorPointId(point.path("id").asText());
                hit.setScore(point.path("score").asDouble());

                JsonNode payload = point.path("payload");
                hit.setChunkId(stringOrNull(payload, "chunk_id"));
                hit.setProductId(stringOrNull(payload, "product_id"));
                hit.setChunkType(stringOrNull(payload, "chunk_type"));
                hit.setSourceField(stringOrNull(payload, "source_field"));
                hit.setText(stringOrNull(payload, "text"));

                Map<String, Object> payloadMap = new LinkedHashMap<>();
                payload.fields().forEachRemaining(entry ->
                        payloadMap.put(entry.getKey(), objectToJava(entry.getValue())));
                hit.setPayload(payloadMap);

                hits.add(hit);
            }

            PerfTraceContext.addAttribute("qdrant_point_count", hits.size());
            PerfTraceContext.addAttribute("qdrant_collection", config.getCollectionName());
            PerfTraceContext.addAttribute("qdrant_limit", request.getEffectiveLimit());
            PerfTraceContext.addAttribute("qdrant_filter_count", request.getFilters() != null ? request.getFilters().size() : 0);
            log.debug("Qdrant search returned {} hits", hits.size());
            return hits;

        } catch (Exception e) {
            log.error("Qdrant search error: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public long count() {
        return collectionManager.countPoints();
    }

    @Override
    public void clear() {
        try {
            HttpRequest request = buildRequest("DELETE",
                    "/collections/" + config.getCollectionName(), null);
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Qdrant collection '{}' cleared", config.getCollectionName());
        } catch (Exception e) {
            log.error("Failed to clear Qdrant collection: {}", e.getMessage());
            throw new RuntimeException("Failed to clear Qdrant collection: " + e.getMessage(), e);
        }
    }

    private void upsertBatch(List<EmbeddedRagChunk> batch) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode points = objectMapper.createArrayNode();

            for (EmbeddedRagChunk chunk : batch) {
                ObjectNode point = objectMapper.createObjectNode();
                try {
                    UUID uuid = UUID.fromString(chunk.getVectorPointId());
                    point.put("id", uuid.toString());
                } catch (IllegalArgumentException e) {
                    point.put("id", chunk.getVectorPointId());
                }
                point.set("vector", objectMapper.valueToTree(chunk.getVector()));
                point.set("payload", objectMapper.valueToTree(chunk.getPayload()));
                points.add(point);
            }

            body.set("points", points);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildRequest("PUT",
                    "/collections/" + config.getCollectionName() + "/points?wait=true", json);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Qdrant upsert failed: {} {}", response.statusCode(), response.body());
                throw new RuntimeException("Qdrant upsert failed: " + response.body());
            }
        } catch (IOException e) {
            log.error("Qdrant upsert IO error: {}", e.getMessage());
            throw new RuntimeException("Failed to upsert to Qdrant: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Qdrant upsert interrupted", e);
        }
    }

    private HttpRequest buildRequest(String method, String path, String bodyJson) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl() + path));

        if ("POST".equals(method)) {
            builder.POST(bodyJson == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(bodyJson));
        } else if ("PUT".equals(method)) {
            builder.PUT(bodyJson == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(bodyJson));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.method(method, bodyJson == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(bodyJson));
        }

        builder.header("Content-Type", "application/json");
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.header("api-key", config.getApiKey());
        }
        builder.timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        return builder.build();
    }

    private String stringOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() ? null : value.asText();
    }

    private Object objectToJava(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        return node.asText();
    }
}
