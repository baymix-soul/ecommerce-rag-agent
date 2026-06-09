package com.ecommerce.rag.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.core.auth.CurrentUserContext;
import com.ecommerce.rag.models.dto.AddCartItemRequest;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.dto.UpdateCartItemRequest;
import com.ecommerce.rag.services.cart.CartItemNotFoundException;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.InvalidQuantityException;
import com.ecommerce.rag.services.cart.ProductNotFoundInCartException;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartView> getCart() {
        String userId = CurrentUserContext.getUserId();
        CartView cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(@RequestBody AddCartItemRequest request) {
        String userId = CurrentUserContext.getUserId();
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "INVALID_REQUEST", "message", "product_id 不能为空"));
        }
        try {
            CartView cart = cartService.addItem(userId, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (ProductNotFoundInCartException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        } catch (InvalidQuantityException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        }
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<?> updateItem(@PathVariable String productId,
                                         @RequestBody UpdateCartItemRequest request) {
        String userId = CurrentUserContext.getUserId();
        try {
            CartView cart = cartService.updateItemQuantity(userId, productId, request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (CartItemNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        } catch (InvalidQuantityException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", e.getCode(), "message", e.getMessage()));
        }
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartView> removeItem(@PathVariable String productId) {
        String userId = CurrentUserContext.getUserId();
        CartView cart = cartService.removeItem(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<CartView> clearCart() {
        String userId = CurrentUserContext.getUserId();
        CartView cart = cartService.clearCart(userId);
        return ResponseEntity.ok(cart);
    }
}
