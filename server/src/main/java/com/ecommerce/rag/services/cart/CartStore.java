package com.ecommerce.rag.services.cart;

import java.util.Map;

import com.ecommerce.rag.models.dto.CartView;

public interface CartStore {
    Map<String, Integer> getItems(String userId);
    void setItems(String userId, Map<String, Integer> items);
    void clearCart(String userId);
}
