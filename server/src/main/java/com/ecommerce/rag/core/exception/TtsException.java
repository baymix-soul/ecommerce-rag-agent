package com.ecommerce.rag.core.exception;

public class TtsException extends RuntimeException {

    private final String code;

    public TtsException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
