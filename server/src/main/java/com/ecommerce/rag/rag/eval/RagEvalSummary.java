package com.ecommerce.rag.rag.eval;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagEvalSummary {

    @JsonProperty("embedding_model")
    private String embeddingModel;

    @JsonProperty("vector_store")
    private String vectorStore;

    @JsonProperty("retrieval_mode")
    private String retrievalMode;

    @JsonProperty("total_queries")
    private int totalQueries;

    @JsonProperty("evaluated_queries")
    private int evaluatedQueries;

    @JsonProperty("unsupported_queries")
    private int unsupportedQueries;

    @JsonProperty("unsupported_query_list")
    private List<String> unsupportedQueryList = new ArrayList<>();

    @JsonProperty("passed_queries")
    private int passedQueries;

    @JsonProperty("failed_queries")
    private int failedQueries;

    @JsonProperty("pass_rate")
    private double passRate;

    @JsonProperty("avg_hit_count")
    private double avgHitCount;

    @JsonProperty("failed_query_list")
    private List<String> failedQueryList = new ArrayList<>();

    @JsonProperty("results")
    private List<RagEvalResult> results = new ArrayList<>();

    @JsonProperty("failed_reasons")
    private List<String> failedReasons = new ArrayList<>();

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getVectorStore() { return vectorStore; }
    public void setVectorStore(String vectorStore) { this.vectorStore = vectorStore; }

    public String getRetrievalMode() { return retrievalMode; }
    public void setRetrievalMode(String retrievalMode) { this.retrievalMode = retrievalMode; }

    public int getTotalQueries() { return totalQueries; }
    public void setTotalQueries(int totalQueries) { this.totalQueries = totalQueries; }

    public int getEvaluatedQueries() { return evaluatedQueries; }
    public void setEvaluatedQueries(int evaluatedQueries) { this.evaluatedQueries = evaluatedQueries; }

    public int getUnsupportedQueries() { return unsupportedQueries; }
    public void setUnsupportedQueries(int unsupportedQueries) { this.unsupportedQueries = unsupportedQueries; }

    public List<String> getUnsupportedQueryList() { return unsupportedQueryList; }
    public void setUnsupportedQueryList(List<String> unsupportedQueryList) { this.unsupportedQueryList = unsupportedQueryList != null ? unsupportedQueryList : new ArrayList<>(); }

    public int getPassedQueries() { return passedQueries; }
    public void setPassedQueries(int passedQueries) { this.passedQueries = passedQueries; }

    public int getFailedQueries() { return failedQueries; }
    public void setFailedQueries(int failedQueries) { this.failedQueries = failedQueries; }

    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }

    public double getAvgHitCount() { return avgHitCount; }
    public void setAvgHitCount(double avgHitCount) { this.avgHitCount = avgHitCount; }

    public List<String> getFailedQueryList() { return failedQueryList; }
    public void setFailedQueryList(List<String> failedQueryList) { this.failedQueryList = failedQueryList != null ? failedQueryList : new ArrayList<>(); }

    public List<RagEvalResult> getResults() { return results; }
    public void setResults(List<RagEvalResult> results) { this.results = results != null ? results : new ArrayList<>(); }

    public List<String> getFailedReasons() { return failedReasons; }
    public void setFailedReasons(List<String> failedReasons) { this.failedReasons = failedReasons != null ? failedReasons : new ArrayList<>(); }
}
