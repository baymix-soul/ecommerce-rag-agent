package com.ecommerce.rag.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private LlmProperties llm = new LlmProperties();
    private VectorProperties vector = new VectorProperties();
    private ProductProperties product = new ProductProperties();
    private ChatProperties chat = new ChatProperties();
    private EmbeddingProperties embedding = new EmbeddingProperties();
    private RetrievalProperties retrieval = new RetrievalProperties();
    private RewriteProperties rewrite = new RewriteProperties();
    private UnderstandingProperties understanding = new UnderstandingProperties();
    private RedisProperties redis = new RedisProperties();
    private AuthProperties auth = new AuthProperties();
    private CartProperties cart = new CartProperties();
    private TtsProperties tts = new TtsProperties();
    private PerfProperties perf = new PerfProperties();

    public LlmProperties getLlm() { return llm; }
    public void setLlm(LlmProperties llm) { this.llm = llm; }

    public VectorProperties getVector() { return vector; }
    public void setVector(VectorProperties vector) { this.vector = vector; }

    public ProductProperties getProduct() { return product; }
    public void setProduct(ProductProperties product) { this.product = product; }

    public ChatProperties getChat() { return chat; }
    public void setChat(ChatProperties chat) { this.chat = chat; }

    public EmbeddingProperties getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingProperties embedding) { this.embedding = embedding; }

    public RetrievalProperties getRetrieval() { return retrieval; }
    public void setRetrieval(RetrievalProperties retrieval) { this.retrieval = retrieval; }

    public RewriteProperties getRewrite() { return rewrite; }
    public void setRewrite(RewriteProperties rewrite) { this.rewrite = rewrite; }

    public UnderstandingProperties getUnderstanding() { return understanding; }
    public void setUnderstanding(UnderstandingProperties understanding) { this.understanding = understanding; }

    public RedisProperties getRedis() { return redis; }
    public void setRedis(RedisProperties redis) { this.redis = redis; }

    public AuthProperties getAuth() { return auth; }
    public void setAuth(AuthProperties auth) { this.auth = auth; }

    public CartProperties getCart() { return cart; }
    public void setCart(CartProperties cart) { this.cart = cart; }

    public TtsProperties getTts() { return tts; }
    public void setTts(TtsProperties tts) { this.tts = tts; }

    public PerfProperties getPerf() { return perf; }
    public void setPerf(PerfProperties perf) { this.perf = perf; }

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
        private String store = "in-memory";
        private QdrantProperties qdrant = new QdrantProperties();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getStore() { return store; }
        public void setStore(String store) { this.store = store; }
        public QdrantProperties getQdrant() { return qdrant; }
        public void setQdrant(QdrantProperties qdrant) { this.qdrant = qdrant; }
    }

    public static class QdrantProperties {
        private String url = "http://localhost:6333";
        private String apiKey = "";
        private String collectionName = "ecommerce_rag_chunks_mock";
        private int vectorSize = 64;
        private String distance = "Cosine";
        private boolean recreateOnStart = false;
        private int timeoutSeconds = 10;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public int getVectorSize() { return vectorSize; }
        public void setVectorSize(int vectorSize) { this.vectorSize = vectorSize; }
        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }
        public boolean isRecreateOnStart() { return recreateOnStart; }
        public void setRecreateOnStart(boolean recreateOnStart) { this.recreateOnStart = recreateOnStart; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class ProductProperties {
        private String dataPath = "classpath:data/products.json";

        public String getDataPath() { return dataPath; }
        public void setDataPath(String dataPath) { this.dataPath = dataPath; }
    }

    public static class ChatProperties {
        private boolean mockLlmEnabled = true;
        private int defaultCandidateLimit = 5;
        private int defaultProductCardLimit = 3;
        private int maxProductCardLimit = 3;

        public boolean isMockLlmEnabled() { return mockLlmEnabled; }
        public void setMockLlmEnabled(boolean mockLlmEnabled) { this.mockLlmEnabled = mockLlmEnabled; }
        public int getDefaultCandidateLimit() { return defaultCandidateLimit; }
        public void setDefaultCandidateLimit(int defaultCandidateLimit) { this.defaultCandidateLimit = defaultCandidateLimit; }
        public int getDefaultProductCardLimit() { return defaultProductCardLimit; }
        public void setDefaultProductCardLimit(int defaultProductCardLimit) { this.defaultProductCardLimit = defaultProductCardLimit; }
        public int getMaxProductCardLimit() { return maxProductCardLimit; }
        public void setMaxProductCardLimit(int maxProductCardLimit) { this.maxProductCardLimit = maxProductCardLimit; }
    }

    public static class EmbeddingProperties {
        private String provider = "mock";
        private int mockDimension = 64;
        private String baseUrl = "";
        private String apiKey = "";
        private String model = "";
        private int dimension = 64;
        private int timeoutSeconds = 30;
        private int batchSize = 16;
        private String arkMultimodalPath = "/embeddings/multimodal";

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public int getMockDimension() { return mockDimension; }
        public void setMockDimension(int mockDimension) { this.mockDimension = mockDimension; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public String getArkMultimodalPath() { return arkMultimodalPath; }
        public void setArkMultimodalPath(String arkMultimodalPath) { this.arkMultimodalPath = arkMultimodalPath; }
    }

    public static class RetrievalProperties {
        private String mode = "hybrid";
        private boolean vectorEnabled = true;
        private boolean keywordEnabled = true;
        private boolean autoFallbackToKeyword = true;
        private int defaultCandidateLimit = 5;

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public boolean isVectorEnabled() { return vectorEnabled; }
        public void setVectorEnabled(boolean vectorEnabled) { this.vectorEnabled = vectorEnabled; }

        public boolean isKeywordEnabled() { return keywordEnabled; }
        public void setKeywordEnabled(boolean keywordEnabled) { this.keywordEnabled = keywordEnabled; }

        public boolean isAutoFallbackToKeyword() { return autoFallbackToKeyword; }
        public void setAutoFallbackToKeyword(boolean autoFallbackToKeyword) { this.autoFallbackToKeyword = autoFallbackToKeyword; }

        public int getDefaultCandidateLimit() { return defaultCandidateLimit; }
        public void setDefaultCandidateLimit(int defaultCandidateLimit) { this.defaultCandidateLimit = defaultCandidateLimit; }
    }

    public static class RewriteProperties {
        private boolean enabled = false;
        private String provider = "lexicon";
        private int timeoutSeconds = 10;
        private int maxVariants = 3;
        private int maxSoftKeywords = 8;
        private boolean cacheEnabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public int getMaxVariants() { return maxVariants; }
        public void setMaxVariants(int maxVariants) { this.maxVariants = maxVariants; }

        public int getMaxSoftKeywords() { return maxSoftKeywords; }
        public void setMaxSoftKeywords(int maxSoftKeywords) { this.maxSoftKeywords = maxSoftKeywords; }

        public boolean isCacheEnabled() { return cacheEnabled; }
        public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
    }

    public static class UnderstandingProperties {
        private PlannerProperties planner = new PlannerProperties();

        public PlannerProperties getPlanner() { return planner; }
        public void setPlanner(PlannerProperties planner) { this.planner = planner; }
    }

    public static class PlannerProperties {
        private boolean enabled = false;
        private String mode = "shadow";
        private int timeoutSeconds = 10;
        private int maxTaxonomyItems = 80;
        private boolean includeBrands = true;
        private boolean cacheEnabled = true;
        private double minConfidence = 0.85;
        private String allowTakeoverIntents = "PRODUCT_SEARCH,REFINE_PREVIOUS_QUERY,NEGATIVE_CONSTRAINT,CHANGE_OR_MORE,CURRENT_PRODUCT_QA";
        private boolean fallbackOnWarnings = true;
        private boolean fallbackOnUnknownCategory = true;
        private boolean fallbackOnUnknownSubCategory = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public int getMaxTaxonomyItems() { return maxTaxonomyItems; }
        public void setMaxTaxonomyItems(int maxTaxonomyItems) { this.maxTaxonomyItems = maxTaxonomyItems; }

        public boolean isIncludeBrands() { return includeBrands; }
        public void setIncludeBrands(boolean includeBrands) { this.includeBrands = includeBrands; }

        public boolean isCacheEnabled() { return cacheEnabled; }
        public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }

        public double getMinConfidence() { return minConfidence; }
        public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }

        public String getAllowTakeoverIntents() { return allowTakeoverIntents; }
        public void setAllowTakeoverIntents(String allowTakeoverIntents) { this.allowTakeoverIntents = allowTakeoverIntents; }

        public boolean isFallbackOnWarnings() { return fallbackOnWarnings; }
        public void setFallbackOnWarnings(boolean fallbackOnWarnings) { this.fallbackOnWarnings = fallbackOnWarnings; }

        public boolean isFallbackOnUnknownCategory() { return fallbackOnUnknownCategory; }
        public void setFallbackOnUnknownCategory(boolean fallbackOnUnknownCategory) { this.fallbackOnUnknownCategory = fallbackOnUnknownCategory; }

        public boolean isFallbackOnUnknownSubCategory() { return fallbackOnUnknownSubCategory; }
        public void setFallbackOnUnknownSubCategory(boolean fallbackOnUnknownSubCategory) { this.fallbackOnUnknownSubCategory = fallbackOnUnknownSubCategory; }
    }

    public static class RedisProperties {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public int getDatabase() { return database; }
        public void setDatabase(int database) { this.database = database; }
    }

    public static class AuthProperties {
        private String demoUsername = "demo";
        private String demoPassword = "demo123";
        private int tokenTtlSeconds = 604800;

        public String getDemoUsername() { return demoUsername; }
        public void setDemoUsername(String demoUsername) { this.demoUsername = demoUsername; }

        public String getDemoPassword() { return demoPassword; }
        public void setDemoPassword(String demoPassword) { this.demoPassword = demoPassword; }

        public int getTokenTtlSeconds() { return tokenTtlSeconds; }
        public void setTokenTtlSeconds(int tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }
    }

    public static class CartProperties {
        private int topUpTolerance = 100;

        public int getTopUpTolerance() { return topUpTolerance; }
        public void setTopUpTolerance(int topUpTolerance) { this.topUpTolerance = topUpTolerance; }
    }

    public static class TtsProperties {
        private boolean enabled = true;
        private String provider = "edge-tts-cli";
        private String audioDir = "./data/tts";
        private String publicBaseUrl = "";
        private int maxTextLength = 500;
        private String defaultVoice = "zh-CN-XiaoxiaoNeural";
        private java.util.List<String> allowedVoices = java.util.List.of(
                "zh-CN-XiaoxiaoNeural",
                "zh-CN-YunxiNeural",
                "zh-CN-XiaoyiNeural",
                "zh-CN-YunjianNeural"
        );
        private String defaultFormat = "mp3";
        private int cleanupMaxAgeHours = 24;
        private int commandTimeoutSeconds = 30;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getAudioDir() { return audioDir; }
        public void setAudioDir(String audioDir) { this.audioDir = audioDir; }

        public String getPublicBaseUrl() { return publicBaseUrl; }
        public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }

        public int getMaxTextLength() { return maxTextLength; }
        public void setMaxTextLength(int maxTextLength) { this.maxTextLength = maxTextLength; }

        public String getDefaultVoice() { return defaultVoice; }
        public void setDefaultVoice(String defaultVoice) { this.defaultVoice = defaultVoice; }

        public java.util.List<String> getAllowedVoices() { return allowedVoices; }
        public void setAllowedVoices(java.util.List<String> allowedVoices) { this.allowedVoices = allowedVoices; }

        public String getDefaultFormat() { return defaultFormat; }
        public void setDefaultFormat(String defaultFormat) { this.defaultFormat = defaultFormat; }

        public int getCleanupMaxAgeHours() { return cleanupMaxAgeHours; }
        public void setCleanupMaxAgeHours(int cleanupMaxAgeHours) { this.cleanupMaxAgeHours = cleanupMaxAgeHours; }

        public int getCommandTimeoutSeconds() { return commandTimeoutSeconds; }
        public void setCommandTimeoutSeconds(int commandTimeoutSeconds) { this.commandTimeoutSeconds = commandTimeoutSeconds; }
    }

    public static class PerfProperties {
        private boolean enabled = true;
        private boolean logEnabled = true;
        private boolean recentEnabled = true;
        private int recentSize = 100;
        private int slowThresholdMs = 3000;
        private boolean logQueryPreview = true;
        private int queryPreviewLength = 40;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isLogEnabled() { return logEnabled; }
        public void setLogEnabled(boolean logEnabled) { this.logEnabled = logEnabled; }

        public boolean isRecentEnabled() { return recentEnabled; }
        public void setRecentEnabled(boolean recentEnabled) { this.recentEnabled = recentEnabled; }

        public int getRecentSize() { return recentSize; }
        public void setRecentSize(int recentSize) { this.recentSize = recentSize; }

        public int getSlowThresholdMs() { return slowThresholdMs; }
        public void setSlowThresholdMs(int slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }

        public boolean isLogQueryPreview() { return logQueryPreview; }
        public void setLogQueryPreview(boolean logQueryPreview) { this.logQueryPreview = logQueryPreview; }

        public int getQueryPreviewLength() { return queryPreviewLength; }
        public void setQueryPreviewLength(int queryPreviewLength) { this.queryPreviewLength = queryPreviewLength; }
    }
}
