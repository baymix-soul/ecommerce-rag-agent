package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryPlan {

    public static final String INTENT_SMALLTALK = "SMALLTALK";
    public static final String INTENT_HELP = "HELP";
    public static final String INTENT_THANKS = "THANKS";
    public static final String INTENT_PRODUCT_SEARCH = "PRODUCT_SEARCH";
    public static final String INTENT_REFINE_PREVIOUS_QUERY = "REFINE_PREVIOUS_QUERY";
    public static final String INTENT_NEGATIVE_CONSTRAINT = "NEGATIVE_CONSTRAINT";
    public static final String INTENT_CHANGE_OR_MORE = "CHANGE_OR_MORE";
    public static final String INTENT_CURRENT_PRODUCT_QA = "CURRENT_PRODUCT_QA";
    public static final String INTENT_COMPARE_PRODUCTS = "COMPARE_PRODUCTS";
    public static final String INTENT_UNKNOWN = "UNKNOWN";

    public static final String CONTEXT_ACTION_NONE = "NONE";
    public static final String CONTEXT_ACTION_NEW_SEARCH = "NEW_SEARCH";
    public static final String CONTEXT_ACTION_REFINE_PREVIOUS_SEARCH = "REFINE_PREVIOUS_SEARCH";
    public static final String CONTEXT_ACTION_REPLACE_PREVIOUS_SEARCH = "REPLACE_PREVIOUS_SEARCH";
    public static final String CONTEXT_ACTION_EXCLUDE_FROM_PREVIOUS = "EXCLUDE_FROM_PREVIOUS";
    public static final String CONTEXT_ACTION_CURRENT_PRODUCT_REFERENCE = "CURRENT_PRODUCT_REFERENCE";
    public static final String CONTEXT_ACTION_ASK_CLARIFICATION = "ASK_CLARIFICATION";

    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_HYBRID = "HYBRID";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_DEBUG = "DEBUG";

    private String originalQuery;
    private String normalizedQuery;
    private String intent;
    private Boolean needsRetrieval;
    private String contextAction;
    private QueryPlanTarget target;
    private QueryPlanPrice price;
    private QueryPlanBrands brands;
    private QueryPlanAttributes attributes;
    private List<String> softKeywords = new ArrayList<>();
    private List<String> queryVariants = new ArrayList<>();
    private Integer requestedProductCount;
    private String answerMode;
    private Boolean needsClarification;
    private Double confidence;
    private List<String> warnings = new ArrayList<>();
    private String source;

    public QueryPlan() {
    }

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public String getNormalizedQuery() { return normalizedQuery; }
    public void setNormalizedQuery(String normalizedQuery) { this.normalizedQuery = normalizedQuery; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public Boolean getNeedsRetrieval() { return needsRetrieval; }
    public void setNeedsRetrieval(Boolean needsRetrieval) { this.needsRetrieval = needsRetrieval; }

    public String getContextAction() { return contextAction; }
    public void setContextAction(String contextAction) { this.contextAction = contextAction; }

    public QueryPlanTarget getTarget() { return target; }
    public void setTarget(QueryPlanTarget target) { this.target = target; }

    public QueryPlanPrice getPrice() { return price; }
    public void setPrice(QueryPlanPrice price) { this.price = price; }

    public QueryPlanBrands getBrands() { return brands; }
    public void setBrands(QueryPlanBrands brands) { this.brands = brands; }

    public QueryPlanAttributes getAttributes() { return attributes; }
    public void setAttributes(QueryPlanAttributes attributes) { this.attributes = attributes; }

    public List<String> getSoftKeywords() { return softKeywords; }
    public void setSoftKeywords(List<String> softKeywords) { this.softKeywords = softKeywords != null ? softKeywords : new ArrayList<>(); }

    public List<String> getQueryVariants() { return queryVariants; }
    public void setQueryVariants(List<String> queryVariants) { this.queryVariants = queryVariants != null ? queryVariants : new ArrayList<>(); }

    public Integer getRequestedProductCount() { return requestedProductCount; }
    public void setRequestedProductCount(Integer requestedProductCount) { this.requestedProductCount = requestedProductCount; }

    public String getAnswerMode() { return answerMode; }
    public void setAnswerMode(String answerMode) { this.answerMode = answerMode; }

    public Boolean getNeedsClarification() { return needsClarification; }
    public void setNeedsClarification(Boolean needsClarification) { this.needsClarification = needsClarification; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public QueryPlan deepCopy() {
        QueryPlan copy = new QueryPlan();
        copy.setOriginalQuery(this.originalQuery);
        copy.setNormalizedQuery(this.normalizedQuery);
        copy.setIntent(this.intent);
        copy.setNeedsRetrieval(this.needsRetrieval);
        copy.setContextAction(this.contextAction);
        if (this.target != null) {
            QueryPlanTarget t = new QueryPlanTarget();
            t.setCategory(this.target.getCategory());
            t.setSubCategory(this.target.getSubCategory());
            t.setSubCategories(new ArrayList<>(this.target.getSubCategories()));
            t.setCurrentProductId(this.target.getCurrentProductId());
            t.setScopeProductIds(new ArrayList<>(this.target.getScopeProductIds()));
            t.setExcludeProductIds(new ArrayList<>(this.target.getExcludeProductIds()));
            copy.setTarget(t);
        }
        if (this.price != null) {
            QueryPlanPrice p = new QueryPlanPrice();
            p.setMin(this.price.getMin());
            p.setMax(this.price.getMax());
            p.setCurrency(this.price.getCurrency());
            p.setStrict(this.price.getStrict());
            copy.setPrice(p);
        }
        if (this.brands != null) {
            QueryPlanBrands b = new QueryPlanBrands();
            b.setInclude(new ArrayList<>(this.brands.getInclude()));
            b.setExclude(new ArrayList<>(this.brands.getExclude()));
            copy.setBrands(b);
        }
        if (this.attributes != null) {
            QueryPlanAttributes a = new QueryPlanAttributes();
            a.setInclude(new ArrayList<>(this.attributes.getInclude()));
            a.setExclude(new ArrayList<>(this.attributes.getExclude()));
            copy.setAttributes(a);
        }
        copy.setSoftKeywords(new ArrayList<>(this.softKeywords));
        copy.setQueryVariants(new ArrayList<>(this.queryVariants));
        copy.setRequestedProductCount(this.requestedProductCount);
        copy.setAnswerMode(this.answerMode);
        copy.setNeedsClarification(this.needsClarification);
        copy.setConfidence(this.confidence);
        copy.setWarnings(new ArrayList<>(this.warnings));
        copy.setSource(this.source);
        return copy;
    }
}
