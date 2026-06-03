package com.ecommerce.rag.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RetrievalDebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debugShouldReturnQueryAnalysis() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=200元以下蓝牙耳机&limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("200元以下蓝牙耳机"))
                .andExpect(jsonPath("$.effective_analysis").exists())
                .andExpect(jsonPath("$.effective_analysis.category").isString())
                .andExpect(jsonPath("$.effective_analysis.filters").exists());
    }

    @Test
    void debugShouldReturnCandidateScores() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=推荐一款适合油皮的洗面奶&limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.final_candidates").isArray())
                .andExpect(jsonPath("$.final_candidates[0].final_score").isNumber())
                .andExpect(jsonPath("$.final_candidates[0].matched_sources").isArray());
    }

    @Test
    void debugShouldReturnFiltersWhenApplicable() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=200元以下蓝牙耳机&limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effective_analysis.filters.max_price").isNumber());
    }

    @Test
    void debugShouldNot500WhenNoResults() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=xyz不存在的商品xyz&limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());
    }
}
