package com.ecommerce.rag.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProductCardsList() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void shouldRespectLimitParameter() throws Exception {
        mockMvc.perform(get("/api/products").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void shouldReturnProductDetail() throws Exception {
        mockMvc.perform(get("/api/products/p_beauty_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value("p_beauty_001"))
                .andExpect(jsonPath("$.name").value(notNullValue()))
                .andExpect(jsonPath("$.price").value(notNullValue()))
                .andExpect(jsonPath("$.currency").value("CNY"))
                .andExpect(jsonPath("$.image_url").value(notNullValue()))
                .andExpect(jsonPath("$.description").value(notNullValue()));
    }

    @Test
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/non_existent_id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void shouldSearchByQuery() throws Exception {
        String requestBody = "{\"query\":\"油皮 洗面奶\"}";
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("油皮 洗面奶"))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void shouldSearchByCategoryAndPrice() throws Exception {
        String requestBody = "{\"category\":\"数码电子\",\"max_price\":200}";
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void shouldSearchWithEmptyQuery() throws Exception {
        String requestBody = "{\"category\":\"美妆护肤\",\"limit\":5}";
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.products.length()").value(5));
    }
}
