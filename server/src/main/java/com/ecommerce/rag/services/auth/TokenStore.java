package com.ecommerce.rag.services.auth;

public interface TokenStore {
    void saveToken(String token, String userId, int ttlSeconds);
    String getUserIdByToken(String token);
    void removeToken(String token);
}
