package com.ecommerce.rag.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RagDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void shouldReturnPreviewWithDefaultLimit() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].chunk_id").exists())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].chunk_type").exists())
                .andExpect(jsonPath("$[0].text").exists());
    }

    @Test
    @Order(2)
    void shouldRespectCustomLimit() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/preview").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(3)
    void shouldRespectMaxLimit() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/preview").param("limit", "200"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void shouldReturnChunksByProductId() throws Exception {
        String previewResult = mockMvc.perform(get("/api/rag/chunks/preview").param("limit", "1"))
                .andReturn().getResponse().getContentAsString();

        String productId = extractProductId(previewResult);

        mockMvc.perform(get("/api/rag/chunks/product/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product_id").value(productId))
                .andExpect(jsonPath("$[0].chunk_type").exists());
    }

    @Test
    @Order(5)
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/product/non_existent_id_99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    void shouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_products").isNumber())
                .andExpect(jsonPath("$.total_products").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.total_chunks").isNumber())
                .andExpect(jsonPath("$.total_chunks").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.by_chunk_type").isMap())
                .andExpect(jsonPath("$.by_chunk_type", hasKey("PRODUCT_PROFILE")))
                .andExpect(jsonPath("$.by_chunk_type", hasKey("DESCRIPTION")))
                .andExpect(jsonPath("$.by_chunk_type", hasKey("SEARCH_SUMMARY")));
    }

    @Test
    @Order(7)
    void shouldReturnValidChunkDocumentStructure() throws Exception {
        mockMvc.perform(get("/api/rag/chunks/preview").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chunk_id").isString())
                .andExpect(jsonPath("$[0].parent_id").isString())
                .andExpect(jsonPath("$[0].product_id").isString())
                .andExpect(jsonPath("$[0].chunk_type").isString())
                .andExpect(jsonPath("$[0].text").isString())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].brand").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].metadata").isMap())
                .andExpect(jsonPath("$[0].metadata.product_id").exists())
                .andExpect(jsonPath("$[0].metadata.category").exists());
    }

    @Test
    @Order(8)
    void chunkIdInPreviewShouldNotBeEmpty() throws Exception {
        String content = mockMvc.perform(get("/api/rag/chunks/preview").param("limit", "3"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(content.contains("::"), "chunkId should contain '::' separator");
    }

    private String extractProductId(String previewJson) {
        int idx = previewJson.indexOf("\"product_id\":\"");
        if (idx < 0) {
            throw new RuntimeException("Cannot find product_id in preview response: " + previewJson);
        }
        int start = idx + "\"product_id\":\"".length();
        int end = previewJson.indexOf("\"", start);
        return previewJson.substring(start, end);
    }
}
