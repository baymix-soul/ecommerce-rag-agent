package com.ecommerce.rag.services.cart;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 99;

    private final CartStore cartStore;
    private final ProductService productService;

    public CartService(CartStore cartStore, ProductService productService) {
        this.cartStore = cartStore;
        this.productService = productService;
    }

    public CartView getCart(String userId) {
        Map<String, Integer> stored = cartStore.getItems(userId);
        List<CartItem> items = buildCartItems(stored);
        return buildCartView(userId, items);
    }

    public CartView addItem(String userId, String productId, int quantity) {
        validateQuantity(quantity);
        validateProductExists(productId);

        Map<String, Integer> stored = cartStore.getItems(userId);
        int currentQty = stored.getOrDefault(productId, 0);
        int newQty = Math.min(currentQty + quantity, MAX_QUANTITY);
        stored.put(productId, newQty);
        cartStore.setItems(userId, stored);

        log.info("Cart add: userId={}, productId={}, quantity={} (total={})", userId, productId, quantity, newQty);

        List<CartItem> items = buildCartItems(stored);
        return buildCartView(userId, items);
    }

    public CartView updateItemQuantity(String userId, String productId, int quantity) {
        validateQuantity(quantity);

        Map<String, Integer> stored = cartStore.getItems(userId);
        if (!stored.containsKey(productId)) {
            throw new CartItemNotFoundException("购物车中未找到该商品: " + productId);
        }
        stored.put(productId, quantity);
        cartStore.setItems(userId, stored);

        List<CartItem> items = buildCartItems(stored);
        return buildCartView(userId, items);
    }

    public CartView removeItem(String userId, String productId) {
        Map<String, Integer> stored = cartStore.getItems(userId);
        stored.remove(productId);
        cartStore.setItems(userId, stored);

        List<CartItem> items = buildCartItems(stored);
        return buildCartView(userId, items);
    }

    public CartView clearCart(String userId) {
        cartStore.clearCart(userId);
        return buildCartView(userId, new ArrayList<>());
    }

    public CartItem addItemAndReturn(String userId, String productId, int quantity) {
        addItem(userId, productId, quantity);
        Product product = productService.findById(productId).orElse(null);
        if (product == null) return null;
        Map<String, Integer> stored = cartStore.getItems(userId);
        int qty = stored.getOrDefault(productId, 1);
        return buildCartItem(product, qty);
    }

    private void validateQuantity(int quantity) {
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new InvalidQuantityException("数量必须在 " + MIN_QUANTITY + " 到 " + MAX_QUANTITY + " 之间");
        }
    }

    private void validateProductExists(String productId) {
        Optional<Product> product = productService.findById(productId);
        if (product.isEmpty()) {
            throw new ProductNotFoundInCartException("商品不存在: " + productId);
        }
    }

    private List<CartItem> buildCartItems(Map<String, Integer> stored) {
        List<CartItem> items = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : stored.entrySet()) {
            Optional<Product> productOpt = productService.findById(entry.getKey());
            if (productOpt.isEmpty()) {
                log.warn("Product not found for cart item: productId={}, will skip", entry.getKey());
                toRemove.add(entry.getKey());
                continue;
            }
            Product product = productOpt.get();
            int qty = entry.getValue();
            items.add(buildCartItem(product, qty));
        }

        if (!toRemove.isEmpty()) {
            stored.keySet().removeAll(toRemove);
        }

        return items;
    }

    private CartItem buildCartItem(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setProductId(product.getProductId());
        item.setName(product.getName());
        item.setPrice(product.getPrice());
        item.setCurrency(product.getCurrency() != null ? product.getCurrency() : "CNY");
        item.setImageUrl(product.getImageUrl());
        item.setQuantity(quantity);
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private CartView buildCartView(String userId, List<CartItem> items) {
        CartView view = new CartView();
        view.setUserId(userId);
        view.setItems(items);

        int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        view.setTotalQuantity(totalQuantity);
        view.setTotalAmount(totalAmount);

        String currency = items.isEmpty() ? "CNY" : items.get(0).getCurrency();
        view.setCurrency(currency);

        return view;
    }
}
