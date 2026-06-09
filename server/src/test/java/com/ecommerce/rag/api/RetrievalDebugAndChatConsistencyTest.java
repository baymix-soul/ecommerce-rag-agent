package com.ecommerce.rag.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest
class RetrievalDebugAndChatConsistencyTest {

    @Autowired
    private RagRetrievalDebugController debugController;

    @Test
    void debugShouldReturnEffectiveAnalysis() {
        ResponseEntity<Map<String, Object>> response = debugController.debug("推荐洗面奶", 5, "test-consistency-1");

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("effective_analysis"));
        assertNotNull(response.getBody().get("selected_source"));
    }

    @Test
    void debugShouldReturnPlannerResult() {
        ResponseEntity<Map<String, Object>> response = debugController.debug("推荐跑鞋", 5, "test-consistency-2");

        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("legacy_analysis"));
        assertNotNull(response.getBody().get("selected_source"));
    }

    @Test
    void disabledModeShouldHaveSelectedSourceDisabled() {
        ResponseEntity<Map<String, Object>> response = debugController.debug("推荐洗面奶", 5, "test-consistency-3");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        String selectedSource = (String) body.get("selected_source");
        assertNotNull(selectedSource);
        assertEquals("DISABLED", selectedSource);
    }

    @Test
    void effectiveAnalysisShouldBeNonNull() {
        ResponseEntity<Map<String, Object>> response = debugController.debug("推荐跑鞋", 5, "test-consistency-4");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) body.get("effective_analysis");
        assertNotNull(effective);
        assertNotNull(effective.get("original_query"));
    }
}
