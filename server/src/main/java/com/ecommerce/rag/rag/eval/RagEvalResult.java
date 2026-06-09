package com.ecommerce.rag.rag.eval;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RagEvalResult {

    @JsonProperty("query")
    private String query;

    @JsonProperty("expected_category")
    private String expectedCategory;

    @JsonProperty("expected_sub_category")
    private String expectedSubCategory;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("top_k")
    private int topK;

    @JsonProperty("hits")
    private List<RagEvalHit> hits = new ArrayList<>();

    @JsonProperty("pass")
    private boolean pass;

    @JsonProperty("reasons")
    private List<String> reasons = new ArrayList<>();

    @JsonProperty("total_hits")
    private int totalHits;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getExpectedCategory() { return expectedCategory; }
    public void setExpectedCategory(String expectedCategory) { this.expectedCategory = expectedCategory; }

    public String getExpectedSubCategory() { return expectedSubCategory; }
    public void setExpectedSubCategory(String expectedSubCategory) { this.expectedSubCategory = expectedSubCategory; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public List<RagEvalHit> getHits() { return hits; }
    public void setHits(List<RagEvalHit> hits) { this.hits = hits != null ? hits : new ArrayList<>(); }

    public boolean isPass() { return pass; }
    public void setPass(boolean pass) { this.pass = pass; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons != null ? reasons : new ArrayList<>(); }

    public int getTotalHits() { return totalHits; }
    public void setTotalHits(int totalHits) { this.totalHits = totalHits; }
}
