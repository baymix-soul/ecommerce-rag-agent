package com.ecommerce.rag.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.rag.models.dto.LoginRequest;
import com.ecommerce.rag.models.dto.LoginResponse;
import com.ecommerce.rag.services.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FrontendContractCartTest {

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
    @Order(1)
    void shouldReturn401ForUnauthenticatedGetCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @Order(2)
    void shouldReturn401ForUnauthenticatedPostCart() throws Exception {
        String body = "{\"product_id\":\"p_beauty_010\",\"quantity\":1}";
        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void emptyCartReturns200WithEmptyItems() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("demo-user"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total_quantity").value(0))
                .andExpect(jsonPath("$.total_amount").value(0))
                .andExpect(jsonPath("$.currency").value("CNY"));
    }

    @Test
    @Order(4)
    void cartViewHasOnlySnakeCaseFields() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.total_quantity").exists())
                .andExpect(jsonPath("$.total_amount").exists());

        String response = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("userId"),
                "CartView should not contain camelCase userId");
        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("totalQuantity"),
                "CartView should not contain camelCase totalQuantity");
        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("totalAmount"),
                "CartView should not contain camelCase totalAmount");
    }

    @Test
    @Order(5)
    void addItemReturnsFullCartView() throws Exception {
        String body = "{\"product_id\":\"p_beauty_010\",\"quantity\":2}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("demo-user"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].product_id").value("p_beauty_010"))
                .andExpect(jsonPath("$.items[0].name").isString())
                .andExpect(jsonPath("$.items[0].price").isNumber())
                .andExpect(jsonPath("$.items[0].currency").value("CNY"))
                .andExpect(jsonPath("$.items[0].image_url").isString())
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").isNumber())
                .andExpect(jsonPath("$.total_quantity").value(2))
                .andExpect(jsonPath("$.total_amount").isNumber());
    }

    @Test
    @Order(6)
    void addItemQuantityDefaultsToOne() throws Exception {
        String body = "{\"product_id\":\"p_digital_001\"}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.total_quantity").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @Order(7)
    void updateItemReturnsFullCartView() throws Exception {
        addTestItem("p_beauty_010", 1);

        String body = "{\"quantity\":5}";

        mockMvc.perform(patch("/api/cart/items/p_beauty_010")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.total_quantity").value(5));
    }

    @Test
    @Order(8)
    void deleteItemReturnsFullCartView() throws Exception {
        addTestItem("p_beauty_010", 1);

        mockMvc.perform(delete("/api/cart/items/p_beauty_010")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total_quantity").value(0));
    }

    @Test
    @Order(9)
    void clearCartReturnsEmptyCartView() throws Exception {
        addTestItem("p_beauty_010", 1);

        mockMvc.perform(delete("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total_quantity").value(0))
                .andExpect(jsonPath("$.total_amount").value(0))
                .andExpect(jsonPath("$.currency").value("CNY"));
    }

    @Test
    @Order(10)
    void cartItemHasAllRequiredFields() throws Exception {
        addTestItem("p_beauty_010", 2);

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].product_id").exists())
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].price").exists())
                .andExpect(jsonPath("$.items[0].currency").exists())
                .andExpect(jsonPath("$.items[0].image_url").exists())
                .andExpect(jsonPath("$.items[0].quantity").exists())
                .andExpect(jsonPath("$.items[0].subtotal").exists());
    }

    @Test
    @Order(11)
    void cartItemNoCamelCaseFields() throws Exception {
        addTestItem("p_beauty_010", 1);

        String response = mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("productId"),
                "CartItem should not contain camelCase productId");
        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("imageUrl"),
                "CartItem should not contain camelCase imageUrl");
    }

    @Test
    @Order(12)
    void shouldReturn400ForInvalidQuantity() throws Exception {
        String body = "{\"product_id\":\"p_beauty_010\",\"quantity\":0}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
    }

    @Test
    @Order(13)
    void shouldReturn404ForNonexistentProduct() throws Exception {
        String body = "{\"product_id\":\"p_nonexistent_999\",\"quantity\":1}";

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品不存在")));
    }

    private void addTestItem(String productId, int quantity) throws Exception {
        String body = "{\"product_id\":\"" + productId + "\",\"quantity\":" + quantity + "}";
        mockMvc.perform(post("/api/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
