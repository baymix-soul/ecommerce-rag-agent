package com.ecommerce.rag.core.perf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PerfTraceTest {

    @Test
    void testCreateTrace() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "hello world", 40);
        assertNotNull(trace.getTraceId());
        assertEquals("/api/chat/stream", trace.getEndpoint());
        assertEquals("s1", trace.getSessionId());
        assertEquals("hello world", trace.getQueryPreview());
    }

    @Test
    void testStartEndSpan() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        trace.startSpan("span1");
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        trace.endSpan("span1");

        trace.finish(3000);
        assertFalse(trace.getSpans().isEmpty());
        PerfSpan span = trace.getSpans().get(0);
        assertEquals("span1", span.getName());
        assertTrue(span.getDurationMs() >= 0);
    }

    @Test
    void testFinishTotalMs() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {}
        trace.finish(3000);
        assertTrue(trace.getTotalMs() >= 10, "totalMs should be >= 10 but was " + trace.getTotalMs());
    }

    @Test
    void testQueryPreviewTruncation() {
        String longQuery = "a".repeat(100);
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", longQuery, 40);
        assertEquals(43, trace.getQueryPreview().length());
        assertTrue(trace.getQueryPreview().endsWith("..."));
    }

    @Test
    void testDisabledNoException() {
        PerfTrace trace = null;
        assertDoesNotThrow(() -> {
            if (trace != null) {
                trace.startSpan("x");
            }
        });
    }

    @Test
    void testAttributes() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        trace.addAttribute("key1", "value1");
        trace.addAttribute("key2", 123);
        assertEquals("value1", trace.getAttributes().get("key1"));
        assertEquals(123, trace.getAttributes().get("key2"));
    }

    @Test
    void testSummaryContainsTraceId() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        trace.finish(3000);
        String summary = trace.summary();
        assertTrue(summary.contains("PERF_TRACE"));
        assertTrue(summary.contains(trace.getTraceId()));
    }

    @Test
    void testSlowThreshold() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        trace.finish(0);
        assertTrue(trace.isSlow());
    }

    @Test
    void testFailedTrace() {
        PerfTrace trace = PerfTrace.create("/api/chat/stream", "s1", "test", 40);
        trace.setFailed("ERR_CODE");
        trace.finish(3000);
        assertTrue(trace.isFailed());
        assertEquals("ERR_CODE", trace.getErrorCode());
        assertTrue(trace.summary().contains("failed=true"));
    }
}
