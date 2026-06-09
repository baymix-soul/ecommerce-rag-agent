package com.ecommerce.rag.rag.rewrite;

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
class RetrievalDebugRewriteTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debugShouldIncludeRewriteResultWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug")
                        .param("query", "学生党耳机")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("学生党耳机"));
    }

    @Test
    void debugShouldReturnValidResponse() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug")
                        .param("query", "通勤双肩包")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("通勤双肩包"))
                .andExpect(jsonPath("$.final_candidates").isArray());
    }

    @Test
    void debugShouldIncludeLegacyAnalysis() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug")
                        .param("query", "学生党耳机")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacy_analysis").exists());
    }
}
