package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanTarget {

    private String category;
    private String subCategory;
    private List<String> subCategories = new ArrayList<>();
    private String currentProductId;
    private List<String> scopeProductIds = new ArrayList<>();
    private List<String> excludeProductIds = new ArrayList<>();

    public QueryPlanTarget() {
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public List<String> getSubCategories() { return subCategories; }
    public void setSubCategories(List<String> subCategories) { this.subCategories = subCategories != null ? subCategories : new ArrayList<>(); }

    public String getCurrentProductId() { return currentProductId; }
    public void setCurrentProductId(String currentProductId) { this.currentProductId = currentProductId; }

    public List<String> getScopeProductIds() { return scopeProductIds; }
    public void setScopeProductIds(List<String> scopeProductIds) { this.scopeProductIds = scopeProductIds != null ? scopeProductIds : new ArrayList<>(); }

    public List<String> getExcludeProductIds() { return excludeProductIds; }
    public void setExcludeProductIds(List<String> excludeProductIds) { this.excludeProductIds = excludeProductIds != null ? excludeProductIds : new ArrayList<>(); }
}
