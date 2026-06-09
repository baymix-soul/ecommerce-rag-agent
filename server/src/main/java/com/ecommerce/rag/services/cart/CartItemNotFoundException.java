package com.ecommerce.rag.services.cart;

public class CartItemNotFoundException extends RuntimeException {
    private final String code = "CART_ITEM_NOT_FOUND";

    public CartItemNotFoundException(String message) {
        super(message);
    }

    public String getCode() { return code; }
}
