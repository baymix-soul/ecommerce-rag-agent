package com.ecommerce.rag.rag.vector;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.rag.core.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QdrantCollectionManager {

    private static final Logger log = LoggerFactory.getLogger(QdrantCollectionManager.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties.QdrantProperties config;

    public QdrantCollectionManager(AppProperties.QdrantProperties config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    public boolean collectionExists() {
        try {
            HttpRequest request = buildRequest("GET", "/collections/" + config.getCollectionName(), null);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode body = objectMapper.readTree(response.body());
                return "green".equals(body.path("result").path("status").asText());
            }
            return false;
        } catch (IOException e) {
            log.debug("Collection existence check failed: {}", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void createCollectionIfAbsent() {
        if (collectionExists()) {
            log.debug("Collection '{}' already exists", config.getCollectionName());
            return;
        }
        createCollection();
    }

    public void recreateCollection() {
        try {
            HttpRequest deleteReq = buildRequest("DELETE", "/collections/" + config.getCollectionName(), null);
            httpClient.send(deleteReq, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.debug("Delete collection before recreate: {}", e.getMessage());
        }
        createCollection();
    }

    public long countPoints() {
        try {
            HttpRequest request = buildRequest("GET", "/collections/" + config.getCollectionName(), null);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode body = objectMapper.readTree(response.body());
                return body.path("result").path("points_count").asLong(0);
            }
            return 0;
        } catch (Exception e) {
            log.debug("Failed to count points: {}", e.getMessage());
            return 0;
        }
    }

    private void createCollection() {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            Map<String, Object> vectors = new LinkedHashMap<>();
            vectors.put("size", config.getVectorSize());
            vectors.put("distance", config.getDistance());
            body.put("vectors", vectors);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = buildRequest("PUT", "/collections/" + config.getCollectionName(), json);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Collection '{}' created successfully", config.getCollectionName());
            } else {
                log.warn("Failed to create collection: {} {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to create collection: {}", e.getMessage());
            throw new RuntimeException("Failed to create Qdrant collection: " + e.getMessage(), e);
        }
    }

    private HttpRequest buildRequest(String method, String path, String bodyJson) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl() + path));

        switch (method) {
            case "GET" -> builder.GET();
            case "PUT" -> builder.PUT(bodyJson == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(bodyJson));
            case "DELETE" -> builder.DELETE();
            default -> builder.method(method, bodyJson == null
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
}
