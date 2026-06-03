package com.ecommerce.rag.rag.vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VectorSearchRequest {

    @JsonProperty("query_vector")
    private List<Double> queryVector;

    @JsonProperty("query_text")
    private String queryText;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("filters")
    private Map<String, Object> filters;

    @JsonProperty("min_score")
    private Double minScore;

    public VectorSearchRequest() {
        this.filters = new LinkedHashMap<>();
    }

    public List<Double> getQueryVector() { return queryVector; }
    public void setQueryVector(List<Double> queryVector) { this.queryVector = queryVector; }

    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public int getEffectiveLimit() {
        if (limit == null || limit <= 0) return 10;
        return Math.min(limit, 50);
    }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters != null ? filters : new LinkedHashMap<>(); }

    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }
}
