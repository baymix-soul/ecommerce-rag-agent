package com.ecommerce.rag.models.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CartView {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("items")
    private List<CartItem> items = new ArrayList<>();

    @JsonProperty("total_quantity")
    private int totalQuantity;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("currency")
    private String currency = "CNY";

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
