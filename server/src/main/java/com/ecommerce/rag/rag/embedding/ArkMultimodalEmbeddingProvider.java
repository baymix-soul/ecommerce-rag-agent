package com.ecommerce.rag.rag.embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ArkMultimodalEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(ArkMultimodalEmbeddingProvider.class);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final int batchSize;
    private final String multimodalPath;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ArkMultimodalEmbeddingProvider(String baseUrl,
                                           String apiKey,
                                           String model,
                                           int dimension,
                                           int batchSize,
                                           int timeoutSeconds,
                                           String multimodalPath) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_BASE_URL must not be blank when provider=ark-multimodal");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_API_KEY must not be blank when provider=ark-multimodal");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_MODEL must not be blank when provider=ark-multimodal");
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
        this.multimodalPath = (multimodalPath != null && !multimodalPath.isBlank())
                ? multimodalPath : "/embeddings/multimodal";
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
        return callArkMultimodalApi(text);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be null or empty");
        }

        List<List<Double>> allVectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            allVectors.add(callArkMultimodalApi(text));
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

    private List<Double> callArkMultimodalApi(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            ArrayNode inputArray = objectMapper.createArrayNode();
            ObjectNode item = objectMapper.createObjectNode();
            item.put("type", "text");
            item.put("text", text);
            inputArray.add(item);
            body.set("input", inputArray);

            String url = baseUrl + multimodalPath;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            log.debug("Calling Ark multimodal embedding API: model={}, path={}",
                    model, multimodalPath);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 401) {
                throw new RuntimeException(
                        "Ark multimodal embedding API authentication failed (401). Check EMBEDDING_API_KEY.");
            }
            if (status == 429) {
                throw new RuntimeException(
                        "Ark multimodal embedding API rate limit exceeded (429). Retry later or reduce batch size.");
            }
            if (status >= 500) {
                throw new RuntimeException(
                        "Ark multimodal embedding API server error (" + status + "). Try again later.");
            }
            if (status != 200) {
                String truncated = response.body().length() > 500
                        ? response.body().substring(0, 500) : response.body();
                throw new RuntimeException(
                        "Ark multimodal embedding API error (" + status + "): " + truncated);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            JsonNode embedding;

            if (data.isArray()) {
                if (data.size() == 0) {
                    throw new RuntimeException(
                            "Unexpected Ark multimodal embedding API response: 'data' is an empty array");
                }
                embedding = data.get(0).path("embedding");
            } else {
                embedding = data.path("embedding");
            }

            if (!embedding.isArray()) {
                throw new RuntimeException(
                        "Unexpected Ark multimodal embedding API response: 'embedding' is not an array");
            }

            List<Double> vec = new ArrayList<>(embedding.size());
            for (JsonNode val : embedding) {
                vec.add(val.asDouble());
            }

            if (vec.size() != dimension) {
                throw new RuntimeException(
                        "Embedding dimension returned by Ark API is " + vec.size()
                                + ", but configured EMBEDDING_DIMENSION is " + dimension
                                + ". Please update EMBEDDING_DIMENSION and QDRANT_VECTOR_SIZE.");
            }

            return vec;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Ark multimodal embedding API: " + e.getMessage(), e);
        }
    }
}
