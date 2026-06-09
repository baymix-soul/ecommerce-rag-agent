package com.ecommerce.rag.services.auth;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);
    private static final String KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void saveToken(String token, String userId, int ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, userId, Duration.ofSeconds(ttlSeconds));
            log.debug("Token saved for userId={}, ttl={}s", userId, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to save token to Redis", e);
        }
    }

    @Override
    public String getUserIdByToken(String token) {
        try {
            return redisTemplate.opsForValue().get(KEY_PREFIX + token);
        } catch (Exception e) {
            log.error("Failed to get token from Redis", e);
            return null;
        }
    }

    @Override
    public void removeToken(String token) {
        try {
            redisTemplate.delete(KEY_PREFIX + token);
        } catch (Exception e) {
            log.error("Failed to remove token from Redis", e);
        }
    }
}
