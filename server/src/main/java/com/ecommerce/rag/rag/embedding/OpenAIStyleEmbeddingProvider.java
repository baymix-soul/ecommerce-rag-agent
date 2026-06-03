package com.ecommerce.rag.rag.embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class OpenAIStyleEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIStyleEmbeddingProvider.class);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIStyleEmbeddingProvider(String baseUrl,
                                         String apiKey,
                                         String model,
                                         int dimension,
                                         int batchSize,
                                         int timeoutSeconds) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_BASE_URL must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_API_KEY must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_MODEL must not be blank");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("EMBEDDING_DIMENSION must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("EMBEDDING_BATCH_SIZE must be positive");
        }

        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.batchSize = batchSize;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or blank");
        }
        List<List<Double>> batch = embedBatch(List.of(text));
        return batch.get(0);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be null or empty");
        }

        List<List<Double>> allVectors = new ArrayList<>(texts.size());

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            List<List<Double>> batchVectors = callEmbeddingApi(batch);
            allVectors.addAll(batchVectors);
        }

        return allVectors;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return model;
    }

    private List<List<Double>> callEmbeddingApi(List<String> inputs) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            ArrayNode inputArray = objectMapper.createArrayNode();
            for (String input : inputs) {
                inputArray.add(input);
            }
            body.set("input", inputArray);

            String url = baseUrl.endsWith("/") ? baseUrl + "embeddings" : baseUrl + "/embeddings";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            log.debug("Calling embedding API: model={}, inputs_count={}", model, inputs.size());

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 401) {
                throw new RuntimeException("Embedding API authentication failed (401). Check EMBEDDING_API_KEY.");
            }
            if (status == 429) {
                throw new RuntimeException("Embedding API rate limit exceeded (429). Retry later or reduce batch size.");
            }
            if (status >= 500) {
                throw new RuntimeException("Embedding API server error (" + status + "). Try again later.");
            }
            if (status != 200) {
                String truncated = response.body().length() > 500
                        ? response.body().substring(0, 500) : response.body();
                throw new RuntimeException("Embedding API error (" + status + "): " + truncated);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                throw new RuntimeException("Unexpected embedding API response: 'data' is not an array");
            }

            List<List<Double>> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.path("embedding");
                if (!embedding.isArray()) {
                    throw new RuntimeException("Unexpected embedding API response: 'embedding' is not an array");
                }

                List<Double> vec = new ArrayList<>(embedding.size());
                for (JsonNode val : embedding) {
                    vec.add(val.asDouble());
                }

                if (vec.size() != dimension) {
                    throw new RuntimeException(
                            "Embedding dimension mismatch. API returned " + vec.size()
                                    + " but configured dimension is " + dimension
                                    + ". Update EMBEDDING_DIMENSION and QDRANT_VECTOR_SIZE.");
                }

                vectors.add(vec);
            }

            if (vectors.size() != inputs.size()) {
                throw new RuntimeException("Embedding API returned " + vectors.size()
                        + " vectors but expected " + inputs.size());
            }

            log.debug("Embedding API returned {} vectors", vectors.size());
            return vectors;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call embedding API: " + e.getMessage(), e);
        }
    }
}
