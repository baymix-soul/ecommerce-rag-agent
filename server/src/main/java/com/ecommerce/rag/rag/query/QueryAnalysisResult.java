package com.ecommerce.rag.rag.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ecommerce.rag.rag.router.RetrievalIntent;

public class QueryAnalysisResult {

    private String originalQuery;
    private String normalizedQuery;
    private String resolvedQuery;
    private Boolean inheritedFromContext;
    private String category;
    private String subCategory;
    private List<String> subCategories = new ArrayList<>();
    private String brand;
    private List<String> negativeBrands = new ArrayList<>();
    private List<String> excludeBrands = new ArrayList<>();
    private List<String> positiveKeywords = new ArrayList<>();
    private List<String> negativeKeywords = new ArrayList<>();
    private List<String> avoidIngredientsOrTerms = new ArrayList<>();
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> excludeProductIds = new ArrayList<>();
    private Map<String, Object> filters = new LinkedHashMap<>();
    private List<String> warnings = new ArrayList<>();
    private String sessionId;
    private RetrievalIntent intent;
    private boolean supported = true;

    private String currentProductId;
    private List<String> scopeProductIds = new ArrayList<>();
    private List<String> boostedProductIds = new ArrayList<>();
    private Boolean inheritedFromPageContext;
    private String pageSearchQuery;
    private Map<String, Object> pageFilters = new LinkedHashMap<>();
    private List<String> pageWarnings = new ArrayList<>();

    private Integer requestedProductCount;
    private String responseStyle;
    private List<String> queryVariants = new ArrayList<>();
    private List<String> softKeywords = new ArrayList<>();
    private com.ecommerce.rag.rag.rewrite.QueryRewriteResult rewriteResult;

    public static final String SINGLE_RECOMMENDATION = "SINGLE_RECOMMENDATION";
    public static final String MULTI_RECOMMENDATION = "MULTI_RECOMMENDATION";
    public static final String CURRENT_PRODUCT_QA = "CURRENT_PRODUCT_QA";
    public static final String NO_MATCH = "NO_MATCH";

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public String getNormalizedQuery() { return normalizedQuery; }
    public void setNormalizedQuery(String normalizedQuery) { this.normalizedQuery = normalizedQuery; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public List<String> getSubCategories() { return subCategories; }
    public void setSubCategories(List<String> subCategories) { this.subCategories = subCategories != null ? subCategories : new ArrayList<>(); }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public List<String> getNegativeBrands() { return negativeBrands; }
    public void setNegativeBrands(List<String> negativeBrands) { this.negativeBrands = negativeBrands != null ? negativeBrands : new ArrayList<>(); }

    public List<String> getPositiveKeywords() { return positiveKeywords; }
    public void setPositiveKeywords(List<String> positiveKeywords) { this.positiveKeywords = positiveKeywords != null ? positiveKeywords : new ArrayList<>(); }

    public List<String> getNegativeKeywords() { return negativeKeywords; }
    public void setNegativeKeywords(List<String> negativeKeywords) { this.negativeKeywords = negativeKeywords != null ? negativeKeywords : new ArrayList<>(); }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Map<String, Object> getFilters() { return filters; }
    public void setFilters(Map<String, Object> filters) { this.filters = filters != null ? filters : new LinkedHashMap<>(); }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public String getResolvedQuery() { return resolvedQuery; }
    public void setResolvedQuery(String resolvedQuery) { this.resolvedQuery = resolvedQuery; }

    public Boolean getInheritedFromContext() { return inheritedFromContext; }
    public void setInheritedFromContext(Boolean inheritedFromContext) { this.inheritedFromContext = inheritedFromContext; }

    public List<String> getExcludeBrands() { return excludeBrands; }
    public void setExcludeBrands(List<String> excludeBrands) { this.excludeBrands = excludeBrands != null ? excludeBrands : new ArrayList<>(); }

    public List<String> getAvoidIngredientsOrTerms() { return avoidIngredientsOrTerms; }
    public void setAvoidIngredientsOrTerms(List<String> avoidIngredientsOrTerms) { this.avoidIngredientsOrTerms = avoidIngredientsOrTerms != null ? avoidIngredientsOrTerms : new ArrayList<>(); }

    public List<String> getExcludeProductIds() { return excludeProductIds; }
    public void setExcludeProductIds(List<String> excludeProductIds) { this.excludeProductIds = excludeProductIds != null ? excludeProductIds : new ArrayList<>(); }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public RetrievalIntent getIntent() { return intent; }
    public void setIntent(RetrievalIntent intent) { this.intent = intent; }

    public boolean isSupported() { return supported; }
    public void setSupported(boolean supported) { this.supported = supported; }

    public String getCurrentProductId() { return currentProductId; }
    public void setCurrentProductId(String currentProductId) { this.currentProductId = currentProductId; }

    public List<String> getScopeProductIds() { return scopeProductIds; }
    public void setScopeProductIds(List<String> scopeProductIds) { this.scopeProductIds = scopeProductIds != null ? scopeProductIds : new ArrayList<>(); }

    public List<String> getBoostedProductIds() { return boostedProductIds; }
    public void setBoostedProductIds(List<String> boostedProductIds) { this.boostedProductIds = boostedProductIds != null ? boostedProductIds : new ArrayList<>(); }

    public Boolean getInheritedFromPageContext() { return inheritedFromPageContext; }
    public void setInheritedFromPageContext(Boolean inheritedFromPageContext) { this.inheritedFromPageContext = inheritedFromPageContext; }

    public String getPageSearchQuery() { return pageSearchQuery; }
    public void setPageSearchQuery(String pageSearchQuery) { this.pageSearchQuery = pageSearchQuery; }

    public Map<String, Object> getPageFilters() { return pageFilters; }
    public void setPageFilters(Map<String, Object> pageFilters) { this.pageFilters = pageFilters != null ? pageFilters : new LinkedHashMap<>(); }

    public List<String> getPageWarnings() { return pageWarnings; }
    public void setPageWarnings(List<String> pageWarnings) { this.pageWarnings = pageWarnings != null ? pageWarnings : new ArrayList<>(); }

    public Integer getRequestedProductCount() { return requestedProductCount; }
    public void setRequestedProductCount(Integer requestedProductCount) { this.requestedProductCount = requestedProductCount; }

    public String getResponseStyle() { return responseStyle; }
    public void setResponseStyle(String responseStyle) { this.responseStyle = responseStyle; }

    public List<String> getQueryVariants() { return queryVariants; }
    public void setQueryVariants(List<String> queryVariants) { this.queryVariants = queryVariants != null ? queryVariants : new ArrayList<>(); }

    public List<String> getSoftKeywords() { return softKeywords; }
    public void setSoftKeywords(List<String> softKeywords) { this.softKeywords = softKeywords != null ? softKeywords : new ArrayList<>(); }

    public com.ecommerce.rag.rag.rewrite.QueryRewriteResult getRewriteResult() { return rewriteResult; }
    public void setRewriteResult(com.ecommerce.rag.rag.rewrite.QueryRewriteResult rewriteResult) { this.rewriteResult = rewriteResult; }

    public boolean hasHardConstraints() {
        return category != null || subCategory != null || !subCategories.isEmpty()
                || !negativeBrands.isEmpty() || minPrice != null || maxPrice != null
                || !excludeBrands.isEmpty() || !excludeProductIds.isEmpty();
    }
}
