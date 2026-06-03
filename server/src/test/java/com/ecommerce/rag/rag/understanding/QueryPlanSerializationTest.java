package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class QueryPlanSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
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
              "softKeywords": ["程序员", "编程", "开发", "大内存"],
              "queryVariants": ["适合程序员的笔记本电脑"]
            }
            """;

        QueryPlan plan = mapper.readValue(json, QueryPlan.class);

        assertEquals("推荐几款适合程序员的电脑", plan.getOriginalQuery());
        assertEquals("PRODUCT_SEARCH", plan.getIntent());
        assertTrue(plan.getNeedsRetrieval());
        assertNotNull(plan.getTarget());
        assertEquals("数码电子", plan.getTarget().getCategory());
        assertEquals("笔记本电脑", plan.getTarget().getSubCategory());
        assertNotNull(plan.getPrice());
        assertEquals(new BigDecimal("10000"), plan.getPrice().getMax());
        assertEquals("CNY", plan.getPrice().getCurrency());
        assertTrue(plan.getPrice().getStrict());
        assertEquals(4, plan.getSoftKeywords().size());
        assertTrue(plan.getSoftKeywords().contains("程序员"));
        assertEquals(1, plan.getQueryVariants().size());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        QueryPlan plan = new QueryPlan();
        plan.setOriginalQuery("推荐跑鞋");
        plan.setIntent("PRODUCT_SEARCH");
        plan.setSource("RULE");

        QueryPlanTarget target = new QueryPlanTarget();
        target.setCategory("服饰运动");
        target.setSubCategory("跑步鞋");
        plan.setTarget(target);

        QueryPlanPrice price = new QueryPlanPrice();
        price.setMax(new BigDecimal("1000"));
        price.setCurrency("CNY");
        plan.setPrice(price);

        String json = mapper.writeValueAsString(plan);

        assertTrue(json.contains("推荐跑鞋"));
        assertTrue(json.contains("PRODUCT_SEARCH"));
        assertTrue(json.contains("服饰运动"));
        assertTrue(json.contains("跑步鞋"));
        assertTrue(json.contains("1000"));
    }

    @Test
    void missingFieldsShouldDefaultToEmptyList() throws Exception {
        String json = """
            {
              "originalQuery": "hello"
            }
            """;

        QueryPlan plan = mapper.readValue(json, QueryPlan.class);

        assertEquals("hello", plan.getOriginalQuery());
        assertNotNull(plan.getSoftKeywords());
        assertTrue(plan.getSoftKeywords().isEmpty());
        assertNotNull(plan.getQueryVariants());
        assertTrue(plan.getQueryVariants().isEmpty());
    }

    @Test
    void nestedStructuresShouldDeserialize() throws Exception {
        String json = """
            {
              "originalQuery": "找便宜货",
              "target": {
                "category": "美妆护肤",
                "excludeProductIds": ["p_beauty_001"]
              },
              "brands": {
                "include": ["兰蔻", "雅诗兰黛"],
                "exclude": ["资生堂"]
              },
              "attributes": {
                "include": ["温和", "保湿"],
                "exclude": ["含酒精"]
              }
            }
            """;

        QueryPlan plan = mapper.readValue(json, QueryPlan.class);

        assertNotNull(plan.getTarget());
        assertEquals("美妆护肤", plan.getTarget().getCategory());
        assertEquals(1, plan.getTarget().getExcludeProductIds().size());
        assertTrue(plan.getTarget().getExcludeProductIds().contains("p_beauty_001"));

        assertNotNull(plan.getBrands());
        assertEquals(2, plan.getBrands().getInclude().size());
        assertTrue(plan.getBrands().getInclude().contains("兰蔻"));
        assertEquals(1, plan.getBrands().getExclude().size());

        assertNotNull(plan.getAttributes());
        assertEquals(2, plan.getAttributes().getInclude().size());
        assertTrue(plan.getAttributes().getInclude().contains("保湿"));
    }
}
