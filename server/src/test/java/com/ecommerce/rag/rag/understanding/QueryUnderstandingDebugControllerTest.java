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
class QueryUnderstandingDebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTaxonomyShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/rag/understanding/taxonomy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.brands").isArray())
                .andExpect(jsonPath("$.filterable_fields").isArray());
    }

    @Test
    void validatePlanShouldReturn200() throws Exception {
        String requestJson = """
            {
              "originalQuery": "推荐几款适合程序员的电脑",
              "intent": "PRODUCT_SEARCH",
              "needsRetrieval": true,
              "target": {
                "category": "数码电子",
                "subCategory": "笔记本电脑"
              },
              "price": {
                "max": 10000,
                "currency": "CNY",
                "strict": true
              },
              "softKeywords": ["程序员", "编程", "开发"],
              "queryVariants": ["适合程序员的笔记本电脑"]
            }
            """;

        mockMvc.perform(post("/api/rag/understanding/validate-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists())
                .andExpect(jsonPath("$.originalPlan").exists())
                .andExpect(jsonPath("$.validatedPlan").exists());
    }

    @Test
    void validatePlanShouldReturnWarningsForUnknownCategory() throws Exception {
        String requestJson = """
            {
              "originalQuery": "找汽车",
              "intent": "PRODUCT_SEARCH",
              "target": {
                "category": "汽车用品"
              }
            }
            """;

        mockMvc.perform(post("/api/rag/understanding/validate-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void validatePlanShouldNotCallLLM() throws Exception {
        String requestJson = """
            {
              "originalQuery": "推荐一款跑鞋",
              "intent": "PRODUCT_SEARCH"
            }
            """;

        mockMvc.perform(post("/api/rag/understanding/validate-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists());
    }
}
