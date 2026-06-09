package com.ecommerce.rag.core.perf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PerfTrace {

    private final String traceId;
    private String sessionId;
    private String endpoint;
    private String queryPreview;
    private final long startNanos;
    private long endNanos;
    private long totalMs;
    private boolean slow;
    private boolean failed;
    private String errorCode;
    private final List<PerfSpan> spans = new ArrayList<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final Map<String, PerfSpan> openSpans = new LinkedHashMap<>();

    public PerfTrace(String traceId) {
        this.traceId = traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        this.startNanos = System.nanoTime();
    }

    public static PerfTrace create(String endpoint, String sessionId, String queryPreview, int queryPreviewLength) {
        PerfTrace trace = new PerfTrace(null);
        trace.setEndpoint(endpoint);
        trace.setSessionId(sessionId);
        trace.setQueryPreview(truncate(queryPreview, queryPreviewLength));
        return trace;
    }

    public PerfSpan startSpan(String name) {
        PerfSpan span = new PerfSpan(name, System.nanoTime());
        openSpans.put(name, span);
        return span;
    }

    public void endSpan(String name) {
        PerfSpan span = openSpans.remove(name);
        if (span != null) {
            span.end(System.nanoTime());
            spans.add(span);
        }
    }

    public void mark(String name) {
        long now = System.nanoTime();
        PerfSpan span = new PerfSpan(name, now);
        span.end(now);
        spans.add(span);
    }

    public void addAttribute(String key, Object value) {
        if (key != null && value != null) {
            attributes.put(key, value);
        }
    }

    public void finish(long slowThresholdMs) {
        this.endNanos = System.nanoTime();
        this.totalMs = (endNanos - startNanos) / 1_000_000L;
        this.slow = totalMs >= slowThresholdMs;
        for (PerfSpan span : openSpans.values()) {
            span.end(endNanos);
            spans.add(span);
        }
        openSpans.clear();
    }

    public void setFailed(String errorCode) {
        this.failed = true;
        this.errorCode = errorCode;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("PERF_TRACE trace_id=").append(traceId);
        if (endpoint != null) sb.append(" endpoint=").append(endpoint);
        if (sessionId != null) sb.append(" session_id=").append(sessionId);
        sb.append(" total_ms=").append(totalMs);
        if (slow) sb.append(" slow=true");
        if (failed) sb.append(" failed=true");
        if (errorCode != null) sb.append(" error_code=").append(errorCode);
        if (queryPreview != null) sb.append(" query=\"").append(escape(queryPreview)).append("\"");

        Map<String, Long> spanMap = new LinkedHashMap<>();
        for (PerfSpan span : spans) {
            spanMap.put(span.getName(), span.getDurationMs());
        }
        if (!spanMap.isEmpty()) {
            sb.append(" spans=").append(spanMap);
        }
        if (!attributes.isEmpty()) {
            sb.append(" attrs=").append(attributes);
        }
        return sb.toString();
    }

    public Map<String, Object> toSummaryMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("trace_id", traceId);
        if (endpoint != null) map.put("endpoint", endpoint);
        if (sessionId != null) map.put("session_id", sessionId);
        if (queryPreview != null) map.put("query_preview", queryPreview);
        map.put("total_ms", totalMs);
        map.put("slow", slow);
        map.put("failed", failed);
        if (errorCode != null) map.put("error_code", errorCode);

        Map<String, Long> spanMap = new LinkedHashMap<>();
        for (PerfSpan span : spans) {
            spanMap.put(span.getName(), span.getDurationMs());
        }
        if (!spanMap.isEmpty()) {
            map.put("spans", spanMap);
        }
        if (!attributes.isEmpty()) {
            Map<String, Object> safeAttrs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : attributes.entrySet()) {
                safeAttrs.put(e.getKey(), e.getValue());
            }
            map.put("attributes", safeAttrs);
        }
        return map;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, maxLength) + "...";
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\"", "'").replace("\n", " ");
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getQueryPreview() {
        return queryPreview;
    }

    public void setQueryPreview(String queryPreview) {
        this.queryPreview = queryPreview;
    }

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
    }

    public long getTotalMs() {
        return totalMs;
    }

    public boolean isSlow() {
        return slow;
    }

    public boolean isFailed() {
        return failed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<PerfSpan> getSpans() {
        return spans;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
