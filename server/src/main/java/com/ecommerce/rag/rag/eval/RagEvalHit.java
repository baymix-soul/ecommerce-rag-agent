package com.ecommerce.rag.rag.eval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagEvalHit {

    @JsonProperty("rank")
    private int rank;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("sub_category")
    private String subCategory;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("vector_score")
    private Double vectorScore;

    @JsonProperty("keyword_score")
    private Double keywordScore;

    @JsonProperty("final_score")
    private Double finalScore;

    @JsonProperty("matched_sources")
    private List<String> matchedSources = new ArrayList<>();

    @JsonProperty("matched_chunk_types")
    private List<String> matchedChunkTypes = new ArrayList<>();

    @JsonProperty("text_snippet")
    private String textSnippet;

    @JsonProperty("judged_relevant")
    private boolean judgedRelevant;

    @JsonProperty("matched_rules")
    private List<String> matchedRules = new ArrayList<>();

    @JsonProperty("failed_rules")
    private List<String> failedRules = new ArrayList<>();

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Double getVectorScore() { return vectorScore; }
    public void setVectorScore(Double vectorScore) { this.vectorScore = vectorScore; }

    public Double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(Double keywordScore) { this.keywordScore = keywordScore; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public List<String> getMatchedSources() { return matchedSources; }
    public void setMatchedSources(List<String> matchedSources) { this.matchedSources = matchedSources != null ? matchedSources : new ArrayList<>(); }

    public List<String> getMatchedChunkTypes() { return matchedChunkTypes; }
    public void setMatchedChunkTypes(List<String> matchedChunkTypes) { this.matchedChunkTypes = matchedChunkTypes != null ? matchedChunkTypes : new ArrayList<>(); }

    public String getTextSnippet() { return textSnippet; }
    public void setTextSnippet(String textSnippet) { this.textSnippet = textSnippet; }

    public boolean isJudgedRelevant() { return judgedRelevant; }
    public void setJudgedRelevant(boolean judgedRelevant) { this.judgedRelevant = judgedRelevant; }

    public List<String> getMatchedRules() { return matchedRules; }
    public void setMatchedRules(List<String> matchedRules) { this.matchedRules = matchedRules != null ? matchedRules : new ArrayList<>(); }

    public List<String> getFailedRules() { return failedRules; }
    public void setFailedRules(List<String> failedRules) { this.failedRules = failedRules != null ? failedRules : new ArrayList<>(); }
}
