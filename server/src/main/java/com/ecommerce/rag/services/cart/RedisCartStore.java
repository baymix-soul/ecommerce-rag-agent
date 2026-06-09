package com.ecommerce.rag.services.cart;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCartStore implements CartStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCartStore.class);
    private static final String CART_KEY_PREFIX = "cart:";

    private final StringRedisTemplate redisTemplate;

    public RedisCartStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<String, Integer> getItems(String userId) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(CART_KEY_PREFIX + userId);
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                try {
                    result.put(entry.getKey().toString(), Integer.parseInt(entry.getValue().toString()));
                } catch (NumberFormatException e) {
                    log.warn("Invalid quantity in cart for userId={}, productId={}", userId, entry.getKey());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get cart from Redis for userId={}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    public void setItems(String userId, Map<String, Integer> items) {
        if (items.isEmpty()) {
            clearCart(userId);
            return;
        }
        try {
            String key = CART_KEY_PREFIX + userId;
            redisTemplate.delete(key);
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                stringMap.put(entry.getKey(), entry.getValue().toString());
            }
            redisTemplate.opsForHash().putAll(key, stringMap);
        } catch (Exception e) {
            log.error("Failed to save cart to Redis for userId={}", userId, e);
        }
    }

    @Override
    public void clearCart(String userId) {
        try {
            redisTemplate.delete(CART_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.error("Failed to clear cart from Redis for userId={}", userId, e);
        }
    }
}
