package com.ecommerce.rag.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private LlmProperties llm = new LlmProperties();
    private VectorProperties vector = new VectorProperties();
    private ProductProperties product = new ProductProperties();
    private ChatProperties chat = new ChatProperties();

    public LlmProperties getLlm() {
        return llm;
    }

    public void setLlm(LlmProperties llm) {
        this.llm = llm;
    }

    public VectorProperties getVector() {
        return vector;
    }

    public void setVector(VectorProperties vector) {
        this.vector = vector;
    }

    public ProductProperties getProduct() {
        return product;
    }

    public void setProduct(ProductProperties product) {
        this.product = product;
    }

    public ChatProperties getChat() {
        return chat;
    }

    public void setChat(ChatProperties chat) {
        this.chat = chat;
    }

    public static class LlmProperties {
        private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
        private String apiKey = "";
        private String model = "";
        private int maxTokens = 2048;
        private double temperature = 0.7;
        private int timeoutSeconds = 30;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class VectorProperties {
        private boolean enabled = false;
        private String qdrantUrl = "http://localhost:6333";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getQdrantUrl() { return qdrantUrl; }
        public void setQdrantUrl(String qdrantUrl) { this.qdrantUrl = qdrantUrl; }
    }

    public static class ProductProperties {
        private String dataPath = "classpath:data/products.json";

        public String getDataPath() { return dataPath; }
        public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    }

    public static class ChatProperties {
        private boolean mockLlmEnabled = true;
        private int defaultCandidateLimit = 5;

        public boolean isMockLlmEnabled() { return mockLlmEnabled; }
        public void setMockLlmEnabled(boolean mockLlmEnabled) { this.mockLlmEnabled = mockLlmEnabled; }
        public int getDefaultCandidateLimit() { return defaultCandidateLimit; }
        public void setDefaultCandidateLimit(int defaultCandidateLimit) { this.defaultCandidateLimit = defaultCandidateLimit; }
    }
}
