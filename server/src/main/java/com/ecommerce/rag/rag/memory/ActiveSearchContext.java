package com.ecommerce.rag.rag.memory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;

public class ActiveSearchContext {

    private String sessionId;
    private String activeTask;

    private String category;
    private String subCategory;
    private List<String> subCategories = new ArrayList<>();

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private List<String> positiveKeywords = new ArrayList<>();
    private List<String> softPreferences = new ArrayList<>();
    private List<String> negativeKeywords = new ArrayList<>();
    private List<String> negativeBrands = new ArrayList<>();
    private List<String> excludeProductIds = new ArrayList<>();

    private String audience;
    private String scenario;

    private List<String> lastRecommendedProductIds = new ArrayList<>();
    private List<String> lastCandidateProductIds = new ArrayList<>();
    private List<String> lastSuccessfulCandidateIds = new ArrayList<>();

    private List<ContextConstraint> constraints = new ArrayList<>();

    private String lastContextAction;
    private String lastNoMatchReason;
    private int turnCount;
    private Instant updatedAt;

    public ActiveSearchContext() {
        this.updatedAt = Instant.now();
    }

    public ActiveSearchContext(String sessionId) {
        this.sessionId = sessionId;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
        this.turnCount++;
    }

    public void addConstraint(String field, String value, ConstraintStrength strength,
                              String sourceQuery, int sourceTurn) {
        ContextConstraint constraint = new ContextConstraint(field, value, strength, sourceQuery, sourceTurn);
        this.constraints.add(constraint);
        this.updatedAt = Instant.now();
    }

    public void deactivateSoftPreferences() {
        for (ContextConstraint c : constraints) {
            if (c.getStrength() == ConstraintStrength.SOFT && "softPreferences".equals(c.getField())) {
                c.setActive(false);
            }
        }
    }

    public List<ContextConstraint> getActiveConstraints() {
        return constraints.stream().filter(ContextConstraint::isActive).toList();
    }

    public List<ContextConstraint> getActiveHardConstraints() {
        return constraints.stream()
                .filter(c -> c.isActive() && c.getStrength() == ConstraintStrength.HARD)
                .toList();
    }

    public List<ContextConstraint> getActiveSoftPreferences() {
        return constraints.stream()
                .filter(c -> c.isActive() && c.getStrength() == ConstraintStrength.SOFT)
                .toList();
    }

    public List<ContextConstraint> getActiveExclusions() {
        return constraints.stream()
                .filter(c -> c.isActive() && c.getStrength() == ConstraintStrength.EXCLUSION)
                .toList();
    }

    public QueryAnalysisResult toQueryAnalysisResult() {
        QueryAnalysisResult result = new QueryAnalysisResult();
        result.setCategory(this.category);
        result.setSubCategory(this.subCategory);
        result.setSubCategories(new ArrayList<>(this.subCategories));
        result.setMinPrice(this.minPrice);
        result.setMaxPrice(this.maxPrice);
        result.setPositiveKeywords(new ArrayList<>(this.positiveKeywords));
        result.setNegativeKeywords(new ArrayList<>(this.negativeKeywords));
        result.setNegativeBrands(new ArrayList<>(this.negativeBrands));
        result.setExcludeProductIds(new ArrayList<>(this.excludeProductIds));
        result.setSoftKeywords(new ArrayList<>(this.softPreferences));

        if (this.audience != null && !this.audience.isBlank()) {
            if (!result.getPositiveKeywords().contains(this.audience)) {
                result.getPositiveKeywords().add(this.audience);
            }
        }

        return result;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getActiveTask() { return activeTask; }
    public void setActiveTask(String activeTask) { this.activeTask = activeTask; }

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

    public List<String> getSoftPreferences() { return softPreferences; }
    public void setSoftPreferences(List<String> softPreferences) {
        this.softPreferences = softPreferences != null ? softPreferences : new ArrayList<>();
    }

    public List<String> getNegativeKeywords() { return negativeKeywords; }
    public void setNegativeKeywords(List<String> negativeKeywords) {
        this.negativeKeywords = negativeKeywords != null ? negativeKeywords : new ArrayList<>();
    }

    public List<String> getNegativeBrands() { return negativeBrands; }
    public void setNegativeBrands(List<String> negativeBrands) {
        this.negativeBrands = negativeBrands != null ? negativeBrands : new ArrayList<>();
    }

    public List<String> getExcludeProductIds() { return excludeProductIds; }
    public void setExcludeProductIds(List<String> excludeProductIds) {
        this.excludeProductIds = excludeProductIds != null ? excludeProductIds : new ArrayList<>();
    }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }

    public List<String> getLastRecommendedProductIds() { return lastRecommendedProductIds; }
    public void setLastRecommendedProductIds(List<String> lastRecommendedProductIds) {
        this.lastRecommendedProductIds = lastRecommendedProductIds != null ? lastRecommendedProductIds : new ArrayList<>();
    }

    public List<String> getLastCandidateProductIds() { return lastCandidateProductIds; }
    public void setLastCandidateProductIds(List<String> lastCandidateProductIds) {
        this.lastCandidateProductIds = lastCandidateProductIds != null ? lastCandidateProductIds : new ArrayList<>();
    }

    public List<String> getLastSuccessfulCandidateIds() { return lastSuccessfulCandidateIds; }
    public void setLastSuccessfulCandidateIds(List<String> lastSuccessfulCandidateIds) {
        this.lastSuccessfulCandidateIds = lastSuccessfulCandidateIds != null ? lastSuccessfulCandidateIds : new ArrayList<>();
    }

    public List<ContextConstraint> getConstraints() { return constraints; }
    public void setConstraints(List<ContextConstraint> constraints) {
        this.constraints = constraints != null ? constraints : new ArrayList<>();
    }

    public String getLastContextAction() { return lastContextAction; }
    public void setLastContextAction(String lastContextAction) { this.lastContextAction = lastContextAction; }

    public String getLastNoMatchReason() { return lastNoMatchReason; }
    public void setLastNoMatchReason(String lastNoMatchReason) { this.lastNoMatchReason = lastNoMatchReason; }

    public int getTurnCount() { return turnCount; }
    public void setTurnCount(int turnCount) { this.turnCount = turnCount; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
