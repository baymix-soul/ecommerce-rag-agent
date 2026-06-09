package com.ecommerce.rag.core.exception;

public class ProductNotFoundException extends AppException {

    public ProductNotFoundException(String productId) {
        super("PRODUCT_NOT_FOUND", "商品不存在: " + productId);
    }
}
