package com.ecommerce.rag.rag.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.models.entity.Product;

public class PageContextResolution {

    private PageType pageType = PageType.UNKNOWN;
    private Product currentProduct;
    private List<Product> visibleProducts = new ArrayList<>();
    private List<Product> recentlyViewedProducts = new ArrayList<>();
    private String pageSearchQuery;
    private Map<String, Object> selectedFilters = new LinkedHashMap<>();
    private boolean hasValidCurrentProduct;
    private List<String> warnings = new ArrayList<>();

    public PageType getPageType() {
        return pageType;
    }

    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }

    public Product getCurrentProduct() {
        return currentProduct;
    }

    public void setCurrentProduct(Product currentProduct) {
        this.currentProduct = currentProduct;
        this.hasValidCurrentProduct = currentProduct != null;
    }

    public List<Product> getVisibleProducts() {
        return visibleProducts;
    }

    public void setVisibleProducts(List<Product> visibleProducts) {
        this.visibleProducts = visibleProducts != null ? visibleProducts : new ArrayList<>();
    }

    public List<Product> getRecentlyViewedProducts() {
        return recentlyViewedProducts;
    }

    public void setRecentlyViewedProducts(List<Product> recentlyViewedProducts) {
        this.recentlyViewedProducts = recentlyViewedProducts != null ? recentlyViewedProducts : new ArrayList<>();
    }

    public String getPageSearchQuery() {
        return pageSearchQuery;
    }

    public void setPageSearchQuery(String pageSearchQuery) {
        this.pageSearchQuery = pageSearchQuery;
    }

    public Map<String, Object> getSelectedFilters() {
        return selectedFilters;
    }

    public void setSelectedFilters(Map<String, Object> selectedFilters) {
        this.selectedFilters = selectedFilters != null ? selectedFilters : new LinkedHashMap<>();
    }

    public boolean isHasValidCurrentProduct() {
        return hasValidCurrentProduct;
    }

    public void setHasValidCurrentProduct(boolean hasValidCurrentProduct) {
        this.hasValidCurrentProduct = hasValidCurrentProduct;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public boolean hasPageContext() {
        return pageType != null && pageType != PageType.UNKNOWN;
    }

    public boolean isProductDetail() {
        return pageType == PageType.PRODUCT_DETAIL;
    }

    public boolean isProductList() {
        return pageType == PageType.PRODUCT_LIST;
    }
}
