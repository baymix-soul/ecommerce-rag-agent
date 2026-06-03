package com.ecommerce.rag.rag.vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QdrantFilterBuilder {

    public Map<String, Object> build(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> mustConditions = new ArrayList<>();

        if (filters.containsKey("category")) {
            mustConditions.add(buildMatch("category", filters.get("category")));
        }
        if (filters.containsKey("sub_category")) {
            mustConditions.add(buildMatch("sub_category", filters.get("sub_category")));
        }
        if (filters.containsKey("brand")) {
            mustConditions.add(buildMatch("brand", filters.get("brand")));
        }
        if (filters.containsKey("product_id")) {
            mustConditions.add(buildMatch("product_id", filters.get("product_id")));
        }
        if (filters.containsKey("chunk_type")) {
            mustConditions.add(buildMatch("chunk_type", filters.get("chunk_type")));
        }
        if (filters.containsKey("min_price")) {
            mustConditions.add(buildRange("price", "gte", filters.get("min_price")));
        }
        if (filters.containsKey("max_price")) {
            mustConditions.add(buildRange("price", "lte", filters.get("max_price")));
        }

        if (mustConditions.isEmpty()) {
            return null;
        }

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("must", mustConditions);
        return filter;
    }

    private Map<String, Object> buildMatch(String key, Object value) {
        Map<String, Object> match = new LinkedHashMap<>();
        match.put("key", key);
        match.put("match", Map.of("value", value));
        return match;
    }

    private Map<String, Object> buildRange(String key, String op, Object value) {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("key", key);
        double numValue = toDouble(value);
        range.put("range", Map.of(op, numValue));
        return range;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}
