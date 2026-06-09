package com.ecommerce.rag.core.perf;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;

class PerformanceTraceServiceTest {

    private AppProperties createPerfProperties(boolean enabled, boolean logEnabled, boolean recentEnabled, int size) {
        AppProperties props = new AppProperties();
        AppProperties.PerfProperties perf = new AppProperties.PerfProperties();
        perf.setEnabled(enabled);
        perf.setLogEnabled(logEnabled);
        perf.setRecentEnabled(recentEnabled);
        perf.setRecentSize(size);
        perf.setSlowThresholdMs(100);
        perf.setQueryPreviewLength(40);
        props.setPerf(perf);
        return props;
    }

    @Test
    void testRecentRingBufferLimit() {
        AppProperties props = createPerfProperties(true, false, true, 3);
        PerformanceTraceService service = new PerformanceTraceService(props);

        for (int i = 0; i < 5; i++) {
            PerfTrace trace = service.beginTrace("/api/test", "s" + i, "q" + i);
            service.finishTrace(trace);
        }

        List<PerfTrace> recent = service.getRecentTraces(10);
        assertEquals(3, recent.size());
    }

    @Test
    void testSlowThreshold() {
        AppProperties props = createPerfProperties(true, false, true, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "slow query");
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}
        service.finishTrace(trace);

        assertTrue(trace.isSlow());
    }

    @Test
    void testFailedTraceSaved() {
        AppProperties props = createPerfProperties(true, false, true, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "fail");
        service.finishTraceWithError(trace, "ERR");

        List<PerfTrace> recent = service.getRecentTraces(10);
        assertEquals(1, recent.size());
        assertTrue(recent.get(0).isFailed());
    }

    @Test
    void testSummaryNoSensitiveFields() {
        AppProperties props = createPerfProperties(true, false, true, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "hello");
        trace.addAttribute("safe_key", "safe_value");
        service.finishTrace(trace);

        String summary = trace.summary();
        assertFalse(summary.contains("api_key"));
        assertFalse(summary.contains("authorization"));
        assertFalse(summary.contains("token"));
    }

    @Test
    void testRecentDisabled() {
        AppProperties props = createPerfProperties(true, false, false, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "q");
        service.finishTrace(trace);

        assertTrue(service.getRecentTraces(10).isEmpty());
    }

    @Test
    void testEnabledFalse() {
        AppProperties props = createPerfProperties(false, false, false, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "q");
        assertNull(trace);
    }

    @Test
    void testGetRecentTraceSummaries() {
        AppProperties props = createPerfProperties(true, false, true, 10);
        PerformanceTraceService service = new PerformanceTraceService(props);

        PerfTrace trace = service.beginTrace("/api/test", "s1", "query");
        service.finishTrace(trace);

        List<Map<String, Object>> summaries = service.getRecentTraceSummaries(10);
        assertEquals(1, summaries.size());
        Map<String, Object> map = summaries.get(0);
        assertEquals("/api/test", map.get("endpoint"));
        assertEquals("query", map.get("query_preview"));
    }
}
