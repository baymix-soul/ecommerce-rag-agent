package com.ecommerce.rag.services.cart;

public class InvalidQuantityException extends RuntimeException {
    private final String code = "INVALID_QUANTITY";

    public InvalidQuantityException(String message) {
        super(message);
    }

    public String getCode() { return code; }
}
