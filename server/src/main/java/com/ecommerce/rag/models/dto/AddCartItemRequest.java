package com.ecommerce.rag.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddCartItemRequest {

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("quantity")
    private int quantity = 1;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
