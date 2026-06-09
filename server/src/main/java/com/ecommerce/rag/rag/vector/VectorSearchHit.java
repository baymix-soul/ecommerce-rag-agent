package com.ecommerce.rag.rag.vector;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VectorSearchHit {

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("vector_point_id")
    private String vectorPointId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("chunk_type")
    private String chunkType;

    @JsonProperty("source_field")
    private String sourceField;

    @JsonProperty("text")
    private String text;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("payload")
    private Map<String, Object> payload;

    public VectorSearchHit() {
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

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload != null ? payload : new LinkedHashMap<>(); }
}
