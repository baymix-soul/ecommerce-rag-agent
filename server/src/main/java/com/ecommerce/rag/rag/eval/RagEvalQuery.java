package com.ecommerce.rag.rag.eval;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagEvalQuery {

    @JsonProperty("query")
    private String query;

    @JsonProperty("expected_category")
    private String expectedCategory;

    @JsonProperty("expected_sub_category")
    private String expectedSubCategory;

    @JsonProperty("expected_sub_categories")
    private List<String> expectedSubCategories;

    @JsonProperty("expected_keywords")
    private List<String> expectedKeywords;

    @JsonProperty("negative_keywords")
    private List<String> negativeKeywords;

    @JsonProperty("min_price")
    private BigDecimal minPrice;

    @JsonProperty("max_price")
    private BigDecimal maxPrice;

    @JsonProperty("min_relevant_count")
    private int minRelevantCount = 1;

    @JsonProperty("supported")
    private Boolean supported;

    @JsonProperty("unsupported_reason")
    private String unsupportedReason;

    @JsonProperty("notes")
    private String notes;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getExpectedCategory() { return expectedCategory; }
    public void setExpectedCategory(String expectedCategory) { this.expectedCategory = expectedCategory; }

    public String getExpectedSubCategory() { return expectedSubCategory; }
    public void setExpectedSubCategory(String expectedSubCategory) { this.expectedSubCategory = expectedSubCategory; }

    public List<String> getExpectedSubCategories() { return expectedSubCategories; }
    public void setExpectedSubCategories(List<String> expectedSubCategories) { this.expectedSubCategories = expectedSubCategories; }

    public List<String> getExpectedKeywords() { return expectedKeywords; }
    public void setExpectedKeywords(List<String> expectedKeywords) { this.expectedKeywords = expectedKeywords; }

    public List<String> getNegativeKeywords() { return negativeKeywords; }
    public void setNegativeKeywords(List<String> negativeKeywords) { this.negativeKeywords = negativeKeywords; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public int getMinRelevantCount() { return minRelevantCount; }
    public void setMinRelevantCount(int minRelevantCount) { this.minRelevantCount = minRelevantCount; }

    public boolean isSupported() { return supported == null || supported; }
    public void setSupported(Boolean supported) { this.supported = supported; }

    public String getUnsupportedReason() { return unsupportedReason; }
    public void setUnsupportedReason(String unsupportedReason) { this.unsupportedReason = unsupportedReason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
