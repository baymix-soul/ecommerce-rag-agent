package com.ecommerce.rag.services.cart;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryCartStore implements CartStore {

    private final Map<String, Map<String, Integer>> store = new ConcurrentHashMap<>();

    @Override
    public Map<String, Integer> getItems(String userId) {
        return store.getOrDefault(userId, new HashMap<>());
    }

    @Override
    public void setItems(String userId, Map<String, Integer> items) {
        store.put(userId, new ConcurrentHashMap<>(items));
    }

    @Override
    public void clearCart(String userId) {
        store.remove(userId);
    }
}
