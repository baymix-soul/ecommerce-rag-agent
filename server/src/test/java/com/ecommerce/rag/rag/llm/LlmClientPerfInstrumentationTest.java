package com.ecommerce.rag.rag.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTrace;
import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.ecommerce.rag.core.perf.PerformanceTraceService;

class LlmClientPerfInstrumentationTest {

    PerformanceTraceService perfService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        AppProperties.PerfProperties perfProps = new AppProperties.PerfProperties();
        perfProps.setEnabled(true);
        perfProps.setLogEnabled(false);
        perfProps.setRecentEnabled(true);
        perfProps.setRecentSize(10);
        appProperties.setPerf(perfProps);
        perfService = new PerformanceTraceService(appProperties);
    }

    @Test
    void testFirstTokenAndStreamTotalRecorded() {
        PerfTrace trace = perfService.beginTrace("/api/chat/stream", "s1", "test");
        PerfTraceContext.set(trace);

        PerfTraceContext.startSpan("llm.http_connect_or_request");
        PerfTraceContext.endSpan("llm.http_connect_or_request");

        AtomicBoolean firstTokenRecorded = new AtomicBoolean(false);
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger outputCharCount = new AtomicInteger(0);

        // Simulate receiving chunks
        for (int i = 0; i < 3; i++) {
            String content = "chunk" + i;
            if (firstTokenRecorded.compareAndSet(false, true)) {
                PerfTraceContext.mark("llm.first_token");
            }
            chunkCount.incrementAndGet();
            outputCharCount.addAndGet(content.length());
        }

        PerfTraceContext.addAttribute("llm_model", "mock-model");
        PerfTraceContext.addAttribute("llm_chunk_count", chunkCount.get());
        PerfTraceContext.addAttribute("llm_output_chars", outputCharCount.get());

        perfService.finishTrace(trace);

        assertTrue(trace.getSpans().stream().anyMatch(s -> s.getName().equals("llm.first_token")));
        assertTrue(trace.getAttributes().containsKey("llm_chunk_count"));
        assertEquals(3, trace.getAttributes().get("llm_chunk_count"));
        assertTrue(trace.getAttributes().containsKey("llm_output_chars"));
    }

    @Test
    void testLlmErrorRecorded() {
        PerfTrace trace = perfService.beginTrace("/api/chat/stream", "s1", "test");
        PerfTraceContext.set(trace);

        PerfTraceContext.startSpan("llm.http_connect_or_request");
        PerfTraceContext.endSpan("llm.http_connect_or_request");

        PerfTraceContext.addAttribute("llm_http_status", 500);
        PerfTraceContext.addAttribute("llm_error", "RuntimeException");

        perfService.finishTraceWithError(trace, "LLM_ERROR");

        assertTrue(trace.isFailed());
        assertEquals("LLM_ERROR", trace.getErrorCode());
        assertEquals(500, trace.getAttributes().get("llm_http_status"));
    }
}
