package com.ecommerce.rag.models.vo;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductCard {

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("reason")
    private String reason;

    public ProductCard() {
        this.reason = "";
    }

    public ProductCard(String productId, String name, BigDecimal price, String currency, String imageUrl, String reason) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.imageUrl = imageUrl;
        this.reason = reason != null ? reason : "";
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason != null ? reason : "";
    }
}
