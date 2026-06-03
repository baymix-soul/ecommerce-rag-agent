package com.ecommerce.rag.api;

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
class RagEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getQueriesShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/rag/eval/queries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$[0].query").isString());
    }

    @Test
    void runAllShouldReturnSummary() throws Exception {
        mockMvc.perform(post("/api/rag/eval/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"top_k\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_queries").value(org.hamcrest.Matchers.greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.evaluated_queries").isNumber())
                .andExpect(jsonPath("$.unsupported_queries").isNumber())
                .andExpect(jsonPath("$.pass_rate").isNumber())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void runOneShouldReturnSingleResult() throws Exception {
        String body = """
            {
                "query": "油皮洗面奶",
                "expected_category": "美妆护肤",
                "expected_sub_category": "洁面",
                "min_relevant_count": 1
            }
            """;

        mockMvc.perform(post("/api/rag/eval/run-one?top_k=5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("油皮洗面奶"))
                .andExpect(jsonPath("$.hits").isArray())
                .andExpect(jsonPath("$.pass").isBoolean())
                .andExpect(jsonPath("$.reasons").isArray());
    }

    @Test
    void runOneWithEmptyQueryShouldReturnBadRequest() throws Exception {
        String body = "{\"query\": \"\", \"expected_category\": \"美妆护肤\"}";
        mockMvc.perform(post("/api/rag/eval/run-one?top_k=5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
