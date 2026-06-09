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
class RetrievalDebugConstraintTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debugShouldOutputMaxPrice() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=200元以下蓝牙耳机&limit=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effective_analysis.max_price").value(200));
    }

    @Test
    void debugShouldOutputFinalCandidates() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=推荐一款跑鞋&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.final_candidates").isArray())
                .andExpect(jsonPath("$.final_candidate_count").isNumber())
                .andExpect(jsonPath("$.raw_candidate_count").isNumber());
    }

    @Test
    void debugShouldOutputFilteredOutCandidates() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=推荐跑鞋&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtered_out_candidates").isArray())
                .andExpect(jsonPath("$.final_candidates").isArray());
    }

    @Test
    void debugShouldOutputFailedRulesWhenConstraintsApplied() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=推荐跑鞋&limit=10&session_id=debug-failed-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effective_analysis.category").value("服饰运动"))
                .andExpect(jsonPath("$.effective_analysis.sub_category").value("跑步鞋"))
                .andExpect(jsonPath("$.filtered_out_candidates").isArray());
    }

    @Test
    void debugShouldOutputPriceAndCategoryInQueryAnalysis() throws Exception {
        mockMvc.perform(get("/api/rag/retrieval/debug?query=500以内跑鞋&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effective_analysis.category").value("服饰运动"))
                .andExpect(jsonPath("$.effective_analysis.sub_category").value("跑步鞋"))
                .andExpect(jsonPath("$.effective_analysis.max_price").isNumber())
                .andExpect(jsonPath("$.effective_analysis.normalized_query").exists());
    }
}
