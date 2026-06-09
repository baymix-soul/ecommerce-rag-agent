package com.ecommerce.rag.core.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.rag.services.auth.AuthenticationException;
import com.ecommerce.rag.services.cart.CartItemNotFoundException;
import com.ecommerce.rag.services.cart.InvalidQuantityException;
import com.ecommerce.rag.services.cart.ProductNotFoundInCartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, String>> handleAppException(AppException ex) {
        log.warn("AppException [{}]: {}", ex.getCode(), ex.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof ProductNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "INVALID_CREDENTIALS",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ProductNotFoundInCartException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFoundInCart(ProductNotFoundInCartException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCartItemNotFound(CartItemNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<Map<String, String>> handleInvalidQuantity(InvalidQuantityException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(TtsException.class)
    public ResponseEntity<Map<String, String>> handleTtsException(TtsException ex) {
        log.warn("TTS exception [{}]: {}", ex.getCode(), ex.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("TTS_PROVIDER_UNAVAILABLE".equals(ex.getCode())) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if ("TTS_SYNTHESIS_FAILED".equals(ex.getCode())) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "code", ex.getCode(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "code", "INTERNAL_ERROR",
                        "message", "An unexpected error occurred"
                ));
    }
}
