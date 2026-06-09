package com.ecommerce.rag.services.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Test
    void shouldLoginSuccessfullyWithDemoCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("demo123");
        
        LoginResponse response = authService.login(request);
        
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("demo-user", response.getUserId());
        assertEquals("demo", response.getUsername());
        assertEquals(604800, response.getExpiresIn());
    }

    @Test
    void shouldFailLoginWithWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("wrong_password");
        
        assertThrows(AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void shouldFailLoginWithWrongUsername() {
        LoginRequest request = new LoginRequest();
        request.setUsername("wrong_user");
        request.setPassword("demo123");
        
        assertThrows(AuthenticationException.class, () -> authService.login(request));
    }

    @Test
    void shouldGenerateNonEmptyToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("demo123");
        
        LoginResponse response = authService.login(request);
        
        assertNotNull(response.getAccessToken());
        assertFalse(response.getAccessToken().isBlank());
        assertEquals(64, response.getAccessToken().length()); // 32 bytes hex = 64 chars
    }

    @Test
    void shouldValidateReturnedToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("demo");
        request.setPassword("demo123");
        
        LoginResponse response = authService.login(request);
        String userId = authService.validateToken(response.getAccessToken());
        
        assertEquals("demo-user", userId);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        String userId = authService.validateToken("invalid-token-12345");
        assertNull(userId);
    }

    @Test
    void shouldReturnNullForEmptyToken() {
        String userId = authService.validateToken("");
        assertNull(userId);
    }

    @Test
    void shouldReturnNullForNullToken() {
        String userId = authService.validateToken(null);
        assertNull(userId);
    }
}
