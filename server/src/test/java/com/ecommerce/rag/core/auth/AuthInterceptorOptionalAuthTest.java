package com.ecommerce.rag.core.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;
import com.ecommerce.rag.services.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthInterceptorOptionalAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Test
    void shouldReturn401WhenCartWithoutToken() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenCartWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCartWithValidToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse response = authService.login(loginRequest);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + response.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowChatStreamWithoutToken() throws Exception {
        String requestBody = "{\"message\":\"推荐跑鞋\",\"limit\":1}";

        mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowChatStreamWithValidToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse response = authService.login(loginRequest);

        String requestBody = "{\"message\":\"推荐跑鞋\",\"limit\":1}";

        mockMvc.perform(post("/api/chat/stream")
                        .header("Authorization", "Bearer " + response.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowChatStreamWithInvalidToken() throws Exception {
        String requestBody = "{\"message\":\"推荐跑鞋\",\"limit\":1}";

        mockMvc.perform(post("/api/chat/stream")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowProductsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/products").param("limit", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAuthLoginWithoutToken() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"demo123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }
}
