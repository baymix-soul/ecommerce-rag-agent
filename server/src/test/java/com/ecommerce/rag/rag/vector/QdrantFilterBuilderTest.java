package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class QdrantFilterBuilderTest {

    private final QdrantFilterBuilder builder = new QdrantFilterBuilder();

    @Test
    void shouldReturnNullForEmptyFilters() {
        assertNull(builder.build(null));
        assertNull(builder.build(Map.of()));
    }

    @Test
    void shouldBuildCategoryFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("category", "美妆护肤");

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(1, must.size());
    }

    @Test
    void shouldBuildBrandFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("brand", "芙丽芳丝");

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(1, must.size());
    }

    @Test
    void shouldBuildChunkTypeFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("chunk_type", "DESCRIPTION");

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(1, must.size());
    }

    @Test
    void shouldBuildMinPriceFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("min_price", 50.0);

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(1, must.size());
    }

    @Test
    void shouldBuildMaxPriceFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("max_price", 200.0);

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(1, must.size());
    }

    @Test
    void shouldBuildMultipleFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("category", "美妆护肤");
        filters.put("brand", "芙丽芳丝");
        filters.put("chunk_type", "DESCRIPTION");
        filters.put("min_price", 50.0);
        filters.put("max_price", 200.0);

        Map<String, Object> result = builder.build(filters);

        assertNotNull(result);
        List<?> must = (List<?>) result.get("must");
        assertEquals(5, must.size());
    }

    @Test
    void shouldReturnNullForUnknownFilterKeys() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("unknown_key", "value");

        Map<String, Object> result = builder.build(filters);

        assertNull(result);
    }
}
