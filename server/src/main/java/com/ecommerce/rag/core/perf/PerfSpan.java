package com.ecommerce.rag.core.perf;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerfSpan {

    private final String name;
    private final long startNanos;
    private long endNanos;
    private long durationMs;
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public PerfSpan(String name, long startNanos) {
        this.name = name;
        this.startNanos = startNanos;
    }

    public void end(long endNanos) {
        this.endNanos = endNanos;
        this.durationMs = (endNanos - startNanos) / 1_000_000L;
    }

    public void addAttribute(String key, Object value) {
        if (key != null && value != null) {
            attributes.put(key, value);
        }
    }

    public String getName() {
        return name;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
