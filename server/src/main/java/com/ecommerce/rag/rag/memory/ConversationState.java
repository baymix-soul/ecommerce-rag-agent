package com.ecommerce.rag.rag.memory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ConversationState {

    private String sessionId;
    private String lastUserQuery;
    private String lastResolvedQuery;
    private String category;
    private String subCategory;
    private List<String> subCategories = new ArrayList<>();
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> positiveKeywords = new ArrayList<>();
    private List<String> negativeKeywords = new ArrayList<>();
    private List<String> negativeBrands = new ArrayList<>();
    private List<String> recommendedProductIds = new ArrayList<>();
    private List<String> candidateProductIds = new ArrayList<>();
    private int turnCount;
    private Instant updatedAt;

    public ConversationState() {
        this.updatedAt = Instant.now();
    }

    public ConversationState(String sessionId) {
        this.sessionId = sessionId;
        this.updatedAt = Instant.now();
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getLastUserQuery() { return lastUserQuery; }
    public void setLastUserQuery(String lastUserQuery) { this.lastUserQuery = lastUserQuery; }

    public String getLastResolvedQuery() { return lastResolvedQuery; }
    public void setLastResolvedQuery(String lastResolvedQuery) { this.lastResolvedQuery = lastResolvedQuery; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public List<String> getSubCategories() { return subCategories; }
    public void setSubCategories(List<String> subCategories) {
        this.subCategories = subCategories != null ? subCategories : new ArrayList<>();
    }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public List<String> getPositiveKeywords() { return positiveKeywords; }
    public void setPositiveKeywords(List<String> positiveKeywords) {
        this.positiveKeywords = positiveKeywords != null ? positiveKeywords : new ArrayList<>();
    }

    public List<String> getNegativeKeywords() { return negativeKeywords; }
    public void setNegativeKeywords(List<String> negativeKeywords) {
        this.negativeKeywords = negativeKeywords != null ? negativeKeywords : new ArrayList<>();
    }

    public List<String> getNegativeBrands() { return negativeBrands; }
    public void setNegativeBrands(List<String> negativeBrands) {
        this.negativeBrands = negativeBrands != null ? negativeBrands : new ArrayList<>();
    }

    public List<String> getRecommendedProductIds() { return recommendedProductIds; }
    public void setRecommendedProductIds(List<String> recommendedProductIds) {
        this.recommendedProductIds = recommendedProductIds != null ? recommendedProductIds : new ArrayList<>();
    }

    public List<String> getCandidateProductIds() { return candidateProductIds; }
    public void setCandidateProductIds(List<String> candidateProductIds) {
        this.candidateProductIds = candidateProductIds != null ? candidateProductIds : new ArrayList<>();
    }

    public int getTurnCount() { return turnCount; }
    public void setTurnCount(int turnCount) { this.turnCount = turnCount; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public void touch() {
        this.updatedAt = Instant.now();
        this.turnCount++;
    }
}
