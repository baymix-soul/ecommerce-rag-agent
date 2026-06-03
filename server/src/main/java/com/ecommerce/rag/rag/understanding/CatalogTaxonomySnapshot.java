package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CatalogTaxonomySnapshot {

    private List<String> categories = new ArrayList<>();
    private Map<String, List<String>> subCategoriesByCategory = new LinkedHashMap<>();
    private List<String> allSubCategories = new ArrayList<>();
    private List<String> brands = new ArrayList<>();
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> filterableFields = new ArrayList<>();
    private List<String> textFields = new ArrayList<>();
    private Instant generatedAt;

    public static final List<String> DEFAULT_FILTERABLE_FIELDS = List.of(
            "category", "sub_category", "brand", "price", "product_id", "chunk_type"
    );

    public static final List<String> DEFAULT_TEXT_FIELDS = List.of(
            "name", "description", "specs", "review_summary",
            "faq_summary", "marketing_copy", "search_summary"
    );

    public CatalogTaxonomySnapshot() {
    }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories != null ? categories : new ArrayList<>(); }

    public Map<String, List<String>> getSubCategoriesByCategory() { return subCategoriesByCategory; }
    public void setSubCategoriesByCategory(Map<String, List<String>> subCategoriesByCategory) { this.subCategoriesByCategory = subCategoriesByCategory != null ? subCategoriesByCategory : new LinkedHashMap<>(); }

    public List<String> getAllSubCategories() { return allSubCategories; }
    public void setAllSubCategories(List<String> allSubCategories) { this.allSubCategories = allSubCategories != null ? allSubCategories : new ArrayList<>(); }

    public List<String> getBrands() { return brands; }
    public void setBrands(List<String> brands) { this.brands = brands != null ? brands : new ArrayList<>(); }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public List<String> getFilterableFields() { return filterableFields; }
    public void setFilterableFields(List<String> filterableFields) { this.filterableFields = filterableFields != null ? filterableFields : new ArrayList<>(); }

    public List<String> getTextFields() { return textFields; }
    public void setTextFields(List<String> textFields) { this.textFields = textFields != null ? textFields : new ArrayList<>(); }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public boolean isEmpty() {
        return categories.isEmpty() && allSubCategories.isEmpty() && brands.isEmpty();
    }
}
