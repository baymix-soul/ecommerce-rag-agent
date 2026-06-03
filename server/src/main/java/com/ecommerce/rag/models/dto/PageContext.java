package com.ecommerce.rag.models.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PageContext {

    @JsonProperty("page_type")
    private PageType pageType;

    @JsonProperty("current_product_id")
    private String currentProductId;

    @JsonProperty("visible_product_ids")
    private List<String> visibleProductIds = new ArrayList<>();

    @JsonProperty("search_query")
    private String searchQuery;

    @JsonProperty("selected_filters")
    private Map<String, Object> selectedFilters = new LinkedHashMap<>();

    @JsonProperty("recently_viewed_product_ids")
    private List<String> recentlyViewedProductIds = new ArrayList<>();

    public PageContext() {
        this.pageType = PageType.UNKNOWN;
        this.visibleProductIds = new ArrayList<>();
        this.selectedFilters = new LinkedHashMap<>();
        this.recentlyViewedProductIds = new ArrayList<>();
    }

    public PageType getPageType() {
        return pageType != null ? pageType : PageType.UNKNOWN;
    }

    public void setPageType(PageType pageType) {
        this.pageType = pageType != null ? pageType : PageType.UNKNOWN;
    }

    public String getCurrentProductId() {
        return currentProductId;
    }

    public void setCurrentProductId(String currentProductId) {
        this.currentProductId = currentProductId;
    }

    public List<String> getVisibleProductIds() {
        return visibleProductIds != null ? visibleProductIds : new ArrayList<>();
    }

    public void setVisibleProductIds(List<String> visibleProductIds) {
        this.visibleProductIds = visibleProductIds != null ? visibleProductIds : new ArrayList<>();
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public Map<String, Object> getSelectedFilters() {
        return selectedFilters != null ? selectedFilters : new LinkedHashMap<>();
    }

    public void setSelectedFilters(Map<String, Object> selectedFilters) {
        this.selectedFilters = selectedFilters != null ? selectedFilters : new LinkedHashMap<>();
    }

    public List<String> getRecentlyViewedProductIds() {
        return recentlyViewedProductIds != null ? recentlyViewedProductIds : new ArrayList<>();
    }

    public void setRecentlyViewedProductIds(List<String> recentlyViewedProductIds) {
        this.recentlyViewedProductIds = recentlyViewedProductIds != null ? recentlyViewedProductIds : new ArrayList<>();
    }
}
