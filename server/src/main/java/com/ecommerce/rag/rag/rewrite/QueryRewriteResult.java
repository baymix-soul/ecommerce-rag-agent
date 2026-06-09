package com.ecommerce.rag.rag.rewrite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class QueryRewriteResult {

    public static final String SOURCE_NONE = "NONE";
    public static final String SOURCE_LEXICON = "LEXICON";
    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_HYBRID = "HYBRID";
    public static final String SOURCE_FALLBACK = "FALLBACK";

    private String originalQuery;
    private String normalizedQuery;
    private String expandedQuery;
    private List<String> queryVariants = new ArrayList<>();
    private List<String> softKeywords = new ArrayList<>();
    private List<String> inferredScenarios = new ArrayList<>();
    private Double confidence;
    private String source = SOURCE_NONE;
    private List<String> warnings = new ArrayList<>();

    public static QueryRewriteResult none() {
        QueryRewriteResult r = new QueryRewriteResult();
        r.source = SOURCE_NONE;
        r.confidence = 0.0;
        return r;
    }

    public static QueryRewriteResult fromLexicon(List<String> keywords, String query) {
        QueryRewriteResult r = new QueryRewriteResult();
        r.source = SOURCE_LEXICON;
        r.originalQuery = query;
        r.softKeywords = keywords != null ? keywords : new ArrayList<>();
        r.confidence = keywords != null && !keywords.isEmpty() ? 0.7 : 0.0;
        return r;
    }

    public static QueryRewriteResult fromLlm(String query, List<String> variants,
                                              List<String> keywords, List<String> scenarios,
                                              double confidence, String expandedQuery) {
        QueryRewriteResult r = new QueryRewriteResult();
        r.source = SOURCE_LLM;
        r.originalQuery = query;
        r.queryVariants = variants != null ? variants : new ArrayList<>();
        r.softKeywords = keywords != null ? keywords : new ArrayList<>();
        r.inferredScenarios = scenarios != null ? scenarios : new ArrayList<>();
        r.confidence = confidence;
        r.expandedQuery = expandedQuery;
        return r;
    }

    public static QueryRewriteResult hybrid(QueryRewriteResult lexicon, QueryRewriteResult llm) {
        QueryRewriteResult r = new QueryRewriteResult();
        r.source = SOURCE_HYBRID;
        r.originalQuery = lexicon != null ? lexicon.originalQuery
                : (llm != null ? llm.originalQuery : null);

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (lexicon != null) keywords.addAll(lexicon.getSoftKeywords());
        if (llm != null) keywords.addAll(llm.getSoftKeywords());
        r.softKeywords = new ArrayList<>(keywords);

        LinkedHashSet<String> variants = new LinkedHashSet<>();
        if (llm != null) variants.addAll(llm.getQueryVariants());
        r.queryVariants = new ArrayList<>(variants);

        if (llm != null) {
            r.inferredScenarios = llm.getInferredScenarios();
            r.expandedQuery = llm.getExpandedQuery();
        }

        double lexConf = lexicon != null && lexicon.getConfidence() != null ? lexicon.getConfidence() : 0.0;
        double llmConf = llm != null && llm.getConfidence() != null ? llm.getConfidence() : 0.0;
        r.confidence = Math.max(lexConf, llmConf);

        List<String> warnings = new ArrayList<>();
        if (lexicon != null) warnings.addAll(lexicon.getWarnings());
        if (llm != null) warnings.addAll(llm.getWarnings());
        r.setWarnings(warnings);

        return r;
    }

    public static QueryRewriteResult fallback(String reason) {
        QueryRewriteResult r = new QueryRewriteResult();
        r.source = SOURCE_FALLBACK;
        r.confidence = 0.0;
        r.warnings = new ArrayList<>();
        r.warnings.add(reason);
        return r;
    }

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    public String getNormalizedQuery() { return normalizedQuery; }
    public void setNormalizedQuery(String normalizedQuery) { this.normalizedQuery = normalizedQuery; }
    public String getExpandedQuery() { return expandedQuery; }
    public void setExpandedQuery(String expandedQuery) { this.expandedQuery = expandedQuery; }
    public List<String> getQueryVariants() { return queryVariants; }
    public void setQueryVariants(List<String> queryVariants) { this.queryVariants = queryVariants != null ? queryVariants : new ArrayList<>(); }
    public List<String> getSoftKeywords() { return softKeywords; }
    public void setSoftKeywords(List<String> softKeywords) { this.softKeywords = softKeywords != null ? softKeywords : new ArrayList<>(); }
    public List<String> getInferredScenarios() { return inferredScenarios; }
    public void setInferredScenarios(List<String> inferredScenarios) { this.inferredScenarios = inferredScenarios != null ? inferredScenarios : new ArrayList<>(); }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }
}
