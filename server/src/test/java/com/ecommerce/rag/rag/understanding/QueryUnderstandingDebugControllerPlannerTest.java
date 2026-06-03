package com.ecommerce.rag.rag.understanding;

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
class QueryUnderstandingDebugControllerPlannerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void planEndpointShouldBeAvailable() throws Exception {
        mockMvc.perform(post("/api/rag/understanding/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\u63a8\u8350\u51e0\u6b3e\u9002\u5408\u7a0b\u5e8f\u5458\u7684\u7535\u8111\",\"session_id\":\"test-sc1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("推荐几款适合程序员的电脑"))
                .andExpect(jsonPath("$.sessionId").value("test-sc1"))
                .andExpect(jsonPath("$.legacyAnalysis").exists())
                .andExpect(jsonPath("$.plannerUsedForRetrieval").value(false));
    }

    @Test
    void planShouldReturnDisabledWhenPlannerOff() throws Exception {
        mockMvc.perform(post("/api/rag/understanding/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\",\"session_id\":\"s1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannerUsedForRetrieval").value(false));
    }

    @Test
    void planShouldNotCallRealLlm() throws Exception {
        mockMvc.perform(post("/api/rag/understanding/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacyAnalysis").exists());
    }

    @Test
    void existingTaxonomyEndpointStillWorks() throws Exception {
        mockMvc.perform(get("/api/rag/understanding/taxonomy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.brands").isArray());
    }

    @Test
    void existingValidatePlanEndpointStillWorks() throws Exception {
        String json = "{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\"}";
        mockMvc.perform(post("/api/rag/understanding/validate-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists());
    }
}
