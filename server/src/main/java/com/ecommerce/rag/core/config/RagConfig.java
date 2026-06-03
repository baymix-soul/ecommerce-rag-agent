package com.ecommerce.rag.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.rag.rag.embedding.ArkMultimodalEmbeddingProvider;
import com.ecommerce.rag.rag.embedding.EmbeddingProvider;
import com.ecommerce.rag.rag.embedding.MockEmbeddingProvider;
import com.ecommerce.rag.rag.embedding.OpenAIStyleEmbeddingProvider;
import com.ecommerce.rag.rag.vector.InMemoryVectorStoreService;
import com.ecommerce.rag.rag.vector.QdrantVectorStoreService;
import com.ecommerce.rag.rag.vector.VectorStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public RagConfig(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public EmbeddingProvider embeddingProvider() {
        var embedding = appProperties.getEmbedding();
        String provider = embedding.getProvider();
        log.info("Initializing EmbeddingProvider: provider={}", provider);

        return switch (provider.toLowerCase()) {
            case "mock" -> new MockEmbeddingProvider(embedding.getMockDimension());
            case "openai-style" -> {
                if (embedding.getBaseUrl() == null || embedding.getBaseUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_BASE_URL must not be blank when provider=openai-style");
                }
                if (embedding.getApiKey() == null || embedding.getApiKey().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_API_KEY must not be blank when provider=openai-style");
                }
                if (embedding.getModel() == null || embedding.getModel().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_MODEL must not be blank when provider=openai-style");
                }
                yield new OpenAIStyleEmbeddingProvider(
                        embedding.getBaseUrl(),
                        embedding.getApiKey(),
                        embedding.getModel(),
                        embedding.getDimension(),
                        embedding.getBatchSize(),
                        embedding.getTimeoutSeconds()
                );
            }
            case "ark-multimodal" -> {
                if (embedding.getBaseUrl() == null || embedding.getBaseUrl().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_BASE_URL must not be blank when provider=ark-multimodal");
                }
                if (embedding.getApiKey() == null || embedding.getApiKey().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_API_KEY must not be blank when provider=ark-multimodal");
                }
                if (embedding.getModel() == null || embedding.getModel().isBlank()) {
                    throw new IllegalArgumentException(
                            "EMBEDDING_MODEL must not be blank when provider=ark-multimodal");
                }
                yield new ArkMultimodalEmbeddingProvider(
                        embedding.getBaseUrl(),
                        embedding.getApiKey(),
                        embedding.getModel(),
                        embedding.getDimension(),
                        embedding.getBatchSize(),
                        embedding.getTimeoutSeconds(),
                        embedding.getArkMultimodalPath()
                );
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported embedding provider: " + provider
                            + ". Valid values: mock, openai-style, ark-multimodal");
        };
    }

    @Bean
    public VectorStoreService vectorStoreService() {
        String store = appProperties.getVector().getStore();
        log.info("Initializing VectorStoreService: store={}", store);

        return switch (store.toLowerCase()) {
            case "in-memory" -> new InMemoryVectorStoreService();
            case "qdrant" -> new QdrantVectorStoreService(appProperties.getVector().getQdrant(), objectMapper);
            default -> throw new IllegalArgumentException(
                    "Unsupported vector store type: " + store + ". Valid values: in-memory, qdrant");
        };
    }
}
