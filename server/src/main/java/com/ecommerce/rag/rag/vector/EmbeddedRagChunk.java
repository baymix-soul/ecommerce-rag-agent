package com.ecommerce.rag.rag.vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddedRagChunk {

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("vector_point_id")
    private String vectorPointId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("chunk_type")
    private String chunkType;

    @JsonProperty("text")
    private String text;

    @JsonProperty("vector")
    private List<Double> vector;

    @JsonProperty("payload")
    private Map<String, Object> payload;

    public EmbeddedRagChunk() {
        this.payload = new LinkedHashMap<>();
    }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getVectorPointId() { return vectorPointId; }
    public void setVectorPointId(String vectorPointId) { this.vectorPointId = vectorPointId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<Double> getVector() { return vector; }
    public void setVector(List<Double> vector) { this.vector = vector; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload != null ? payload : new LinkedHashMap<>(); }
}
