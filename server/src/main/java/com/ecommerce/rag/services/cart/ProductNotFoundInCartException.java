package com.ecommerce.rag.services.cart;

public class ProductNotFoundInCartException extends RuntimeException {
    private final String code = "PRODUCT_NOT_FOUND";

    public ProductNotFoundInCartException(String message) {
        super(message);
    }

    public String getCode() { return code; }
}
