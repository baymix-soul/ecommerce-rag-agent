package com.ecommerce.rag.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateCartItemRequest {

    @JsonProperty("quantity")
    private int quantity;

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
