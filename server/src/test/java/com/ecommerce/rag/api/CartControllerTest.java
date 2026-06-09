package com.ecommerce.rag.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ecommerce.rag.services.auth.AuthService;
import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("demo");
        loginRequest.setPassword("demo123");
        LoginResponse response = authService.login(loginRequest);
        accessToken = response.getAccessToken();
    }

    @Test
    void shouldReturn401ForUnauthenticatedCartAccess() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCartForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("demo-user"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total_quantity").isNumber());
    }

    @Test
    void shouldAddItemToCartForAuthenticatedUser() throws Exception {
        String requestBody = "{\"product_id\":\"p_beauty_010\",\"quantity\":1}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total_quantity").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void shouldReturn404ForNonexistentProduct() throws Exception {
        String requestBody = "{\"product_id\":\"p_nonexistent_999\",\"quantity\":1}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForEmptyProductId() throws Exception {
        String requestBody = "{\"product_id\":\"\",\"quantity\":1}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateItemQuantity() throws Exception {
        cartService_addTestItem();

        String requestBody = "{\"quantity\":3}";

        mockMvc.perform(patch("/api/cart/items/p_beauty_010")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void shouldDeleteCartItem() throws Exception {
        cartService_addTestItem();

        mockMvc.perform(delete("/api/cart/items/p_beauty_010")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_quantity").value(0));
    }

    @Test
    void shouldClearCart() throws Exception {
        cartService_addTestItem();

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_quantity").value(0));
    }

    private void cartService_addTestItem() throws Exception {
        String requestBody = "{\"product_id\":\"p_beauty_010\",\"quantity\":1}";
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }
}
