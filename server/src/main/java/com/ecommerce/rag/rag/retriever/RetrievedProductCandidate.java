package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

public class RetrievedProductCandidate {

    private String productId;
    private Product product;
    private Double vectorScore;
    private Double keywordScore;
    private Double finalScore;
    private List<VectorSearchHit> matchedChunks = new ArrayList<>();
    private List<String> matchedSources = new ArrayList<>();
    private List<String> failedRules = new ArrayList<>();
    private List<ConstraintFailure> failures = new ArrayList<>();

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Double getVectorScore() { return vectorScore; }
    public void setVectorScore(Double vectorScore) { this.vectorScore = vectorScore; }

    public Double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(Double keywordScore) { this.keywordScore = keywordScore; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public List<VectorSearchHit> getMatchedChunks() { return matchedChunks; }
    public void setMatchedChunks(List<VectorSearchHit> matchedChunks) { this.matchedChunks = matchedChunks != null ? matchedChunks : new ArrayList<>(); }

    public List<String> getMatchedSources() { return matchedSources; }
    public void setMatchedSources(List<String> matchedSources) { this.matchedSources = matchedSources != null ? matchedSources : new ArrayList<>(); }

    public List<String> getFailedRules() { return failedRules; }
    public void setFailedRules(List<String> failedRules) { this.failedRules = failedRules != null ? failedRules : new ArrayList<>(); }

    public List<ConstraintFailure> getFailures() { return failures; }
    public void setFailures(List<ConstraintFailure> failures) { this.failures = failures != null ? failures : new ArrayList<>(); }
}
