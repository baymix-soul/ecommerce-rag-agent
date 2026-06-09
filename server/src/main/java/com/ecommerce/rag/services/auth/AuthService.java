package com.ecommerce.rag.services.auth;

import java.security.SecureRandom;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppProperties appProperties;
    private final TokenStore tokenStore;

    public AuthService(AppProperties appProperties, TokenStore tokenStore) {
        this.appProperties = appProperties;
        this.tokenStore = tokenStore;
    }

    public LoginResponse login(LoginRequest request) {
        String demoUsername = appProperties.getAuth().getDemoUsername();
        String demoPassword = appProperties.getAuth().getDemoPassword();
        int ttlSeconds = appProperties.getAuth().getTokenTtlSeconds();

        if (!demoUsername.equals(request.getUsername()) || !demoPassword.equals(request.getPassword())) {
            log.warn("Login failed for username: {}", request.getUsername());
            throw new AuthenticationException("用户名或密码错误");
        }

        String token = generateToken();
        String userId = "demo-user";

        tokenStore.saveToken(token, userId, ttlSeconds);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(ttlSeconds);
        response.setUserId(userId);
        response.setUsername(demoUsername);

        log.info("Login successful for userId={}", userId);
        return response;
    }

    public String validateToken(String token) {
        return tokenStore.getUserIdByToken(token);
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
