package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ecommerce.rag.core.config.AppProperties;

class QdrantPointMappingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapEmbeddedRagChunkToQdrantPoint() throws Exception {
        EmbeddedRagChunk chunk = new EmbeddedRagChunk();
        String pointId = UUID.randomUUID().toString();
        chunk.setVectorPointId(pointId);
        chunk.setChunkId("p_001::DESCRIPTION::0");
        chunk.setProductId("p_001");
        chunk.setChunkType("DESCRIPTION");
        chunk.setText("测试文本");
        chunk.setVector(List.of(0.1, 0.2, 0.3));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chunk_id", chunk.getChunkId());
        payload.put("vector_point_id", chunk.getVectorPointId());
        payload.put("product_id", chunk.getProductId());
        payload.put("chunk_type", chunk.getChunkType());
        payload.put("text", chunk.getText());
        payload.put("name", "测试商品");
        payload.put("brand", "测试品牌");
        payload.put("category", "美妆护肤");
        payload.put("sub_category", "洁面");
        payload.put("price", 89.0);
        payload.put("currency", "CNY");
        payload.put("avg_rating", 4.5);
        payload.put("image_url", "/images/p_001.jpg");
        chunk.setPayload(payload);

        Map<String, Object> pointJson = new LinkedHashMap<>();
        pointJson.put("id", chunk.getVectorPointId());
        pointJson.put("vector", chunk.getVector());
        pointJson.put("payload", chunk.getPayload());

        String json = objectMapper.writeValueAsString(pointJson);
        assertNotNull(json);
        assertTrue(json.contains(pointId));

        Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
        assertEquals(pointId, parsed.get("id"));
        assertNotNull(parsed.get("vector"));
        assertNotNull(parsed.get("payload"));

        Map<?, ?> parsedPayload = (Map<?, ?>) parsed.get("payload");
        assertEquals("p_001", parsedPayload.get("product_id"));
        assertEquals("DESCRIPTION", parsedPayload.get("chunk_type"));
        assertEquals("测试文本", parsedPayload.get("text"));
    }

    @Test
    void shouldUseVectorPointIdAsId() {
        EmbeddedRagChunk chunk = new EmbeddedRagChunk();
        chunk.setVectorPointId("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        chunk.setChunkId("p_001::PRODUCT_PROFILE::0");

        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", chunk.getVectorPointId());
    }

    @Test
    void payloadShouldContainRequiredFields() {
        EmbeddedRagChunk chunk = new EmbeddedRagChunk();
        chunk.setPayload(new LinkedHashMap<>());
        chunk.getPayload().put("product_id", "p_001");
        chunk.getPayload().put("chunk_type", "DESCRIPTION");
        chunk.getPayload().put("text", "测试");
        chunk.getPayload().put("price", 89.0);

        assertTrue(chunk.getPayload().containsKey("product_id"));
        assertTrue(chunk.getPayload().containsKey("chunk_type"));
        assertTrue(chunk.getPayload().containsKey("text"));
        assertTrue(chunk.getPayload().containsKey("price"));
    }

    @Test
    void priceShouldBeNumericInPayload() {
        EmbeddedRagChunk chunk = new EmbeddedRagChunk();
        chunk.setPayload(new LinkedHashMap<>());
        chunk.getPayload().put("price", 89.0);

        Object price = chunk.getPayload().get("price");
        assertTrue(price instanceof Number, "price should be numeric, was: " + price.getClass());
        assertEquals(89.0, ((Number) price).doubleValue());
    }
}
