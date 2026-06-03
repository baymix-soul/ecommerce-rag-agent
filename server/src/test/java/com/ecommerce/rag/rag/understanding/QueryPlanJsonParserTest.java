package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class QueryPlanJsonParserTest {

    private final QueryPlanJsonParser parser = new QueryPlanJsonParser();

    @Test
    void shouldParsePureJson() {
        String json = """
                {
                  "originalQuery": "test",
                  "intent": "PRODUCT_SEARCH"
                }""";

        Optional<QueryPlan> result = parser.parse(json);
        assertTrue(result.isPresent());
        assertEquals("test", result.get().getOriginalQuery());
        assertEquals("PRODUCT_SEARCH", result.get().getIntent());
    }

    @Test
    void shouldParseJsonFenced() {
        String json = """
                ```json
                {
                  "originalQuery": "test",
                  "intent": "PRODUCT_SEARCH"
                }
                ```""";

        Optional<QueryPlan> result = parser.parse(json);
        assertTrue(result.isPresent());
        assertEquals("test", result.get().getOriginalQuery());
    }

    @Test
    void shouldParseJsonWithSurroundingText() {
        String output = "好的，我来为你解析查询：\n{\"originalQuery\":\"test\",\"intent\":\"PRODUCT_SEARCH\"}\n以上就是结果。";

        Optional<QueryPlan> result = parser.parse(output);
        assertTrue(result.isPresent());
        assertEquals("test", result.get().getOriginalQuery());
    }

    @Test
    void shouldReturnEmptyForInvalidJson() {
        Optional<QueryPlan> result = parser.parse("这不是JSON");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyString() {
        Optional<QueryPlan> result = parser.parse("");
        assertTrue(result.isEmpty());
        Optional<QueryPlan> result2 = parser.parse(null);
        assertTrue(result2.isEmpty());
    }

    @Test
    void shouldParseNestedStructures() {
        String json = """
                {
                  "originalQuery": "推荐跑鞋",
                  "target": {
                    "category": "服饰运动",
                    "subCategory": "跑步鞋"
                  },
                  "price": {
                    "max": 1000,
                    "currency": "CNY"
                  },
                  "brands": {
                    "include": ["Nike"],
                    "exclude": ["Adidas"]
                  }
                }""";

        Optional<QueryPlan> result = parser.parse(json);
        assertTrue(result.isPresent());
        QueryPlan plan = result.get();
        assertNotNull(plan.getTarget());
        assertEquals("服饰运动", plan.getTarget().getCategory());
        assertEquals("跑步鞋", plan.getTarget().getSubCategory());
        assertNotNull(plan.getPrice());
        assertEquals(new java.math.BigDecimal("1000"), plan.getPrice().getMax());
        assertNotNull(plan.getBrands());
        assertTrue(plan.getBrands().getInclude().contains("Nike"));
    }
}
