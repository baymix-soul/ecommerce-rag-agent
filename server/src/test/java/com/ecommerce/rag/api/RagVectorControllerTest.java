package com.ecommerce.rag.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RagVectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void statsBeforeRebuildShouldReturnZero() throws Exception {
        mockMvc.perform(get("/api/rag/vector-index/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.embedding_model").value("mock-hash-embedding"))
                .andExpect(jsonPath("$.dimension").value(64));
    }

    @Test
    @Order(2)
    void searchBeforeRebuildShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/rag/vector-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"油皮洗面奶\",\"limit\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("油皮洗面奶"))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @Order(3)
    void rebuildShouldReturnIndexedChunks() throws Exception {
        mockMvc.perform(post("/api/rag/vector-index/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed_chunks").isNumber())
                .andExpect(jsonPath("$.indexed_chunks").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.embedding_model").value("mock-hash-embedding"))
                .andExpect(jsonPath("$.dimension").value(64));
    }

    @Test
    @Order(4)
    void statsAfterRebuildShouldReturnPositiveCount() throws Exception {
        mockMvc.perform(get("/api/rag/vector-index/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.count").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    @Order(5)
    void searchAfterRebuildShouldReturnHits() throws Exception {
        mockMvc.perform(post("/api/rag/vector-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"油皮洗面奶\",\"limit\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("油皮洗面奶"))
                .andExpect(jsonPath("$.hits").isArray())
                .andExpect(jsonPath("$.hits.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.hits[0].chunk_id").exists())
                .andExpect(jsonPath("$.hits[0].product_id").exists())
                .andExpect(jsonPath("$.hits[0].score").isNumber());
    }

    @Test
    @Order(6)
    void vectorSearchAliasShouldAlsoWork() throws Exception {
        mockMvc.perform(post("/api/rag/vector-index/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"油皮洗面奶\",\"limit\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("油皮洗面奶"))
                .andExpect(jsonPath("$.hits").isArray())
                .andExpect(jsonPath("$.hits.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
