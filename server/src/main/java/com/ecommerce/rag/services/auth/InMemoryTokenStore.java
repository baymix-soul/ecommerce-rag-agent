package com.ecommerce.rag.services.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false")
public class InMemoryTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryTokenStore.class);

    private final Map<String, TokenEntry> store = new ConcurrentHashMap<>();

    @Override
    public void saveToken(String token, String userId, int ttlSeconds) {
        store.put(token, new TokenEntry(userId, System.currentTimeMillis() + ttlSeconds * 1000L));
        log.debug("Token saved in-memory for userId={}", userId);
    }

    @Override
    public String getUserIdByToken(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(token);
            return null;
        }
        return entry.userId;
    }

    @Override
    public void removeToken(String token) {
        store.remove(token);
    }

    private record TokenEntry(String userId, long expiresAt) {}
}
