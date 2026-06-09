package com.ecommerce.rag.models.dto;

import java.util.List;

import com.ecommerce.rag.models.vo.ProductCard;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductSearchResponse {

    @JsonProperty("query")
    private String query;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("products")
    private List<ProductCard> products;

    public ProductSearchResponse() {
    }

    public ProductSearchResponse(String query, Integer total, List<ProductCard> products) {
        this.query = query;
        this.total = total;
        this.products = products;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<ProductCard> getProducts() {
        return products;
    }

    public void setProducts(List<ProductCard> products) {
        this.products = products;
    }
}
