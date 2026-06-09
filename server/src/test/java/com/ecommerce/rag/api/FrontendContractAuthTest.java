package com.ecommerce.rag.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FrontendContractAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoginSuccessWithSnakeCaseFields() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"demo123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andExpect(jsonPath("$.user_id").value("demo-user"))
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void shouldNotContainCamelCaseFields() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"demo123\"}";

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("accessToken"),
                "Response should not contain camelCase accessToken");
        org.junit.jupiter.api.Assertions.assertFalse(
                response.contains("userId"),
                "Response should not contain camelCase userId");
    }

    @Test
    void shouldFailLoginWith401() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"wrong\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFailLoginWithInvalidCredentialsCode() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"wrong\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void demoDemo123IsAvailable() throws Exception {
        String requestBody = "{\"username\":\"demo\",\"password\":\"demo123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isString())
                .andExpect(jsonPath("$.user_id").value("demo-user"));
    }
}
