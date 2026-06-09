package com.ecommerce.rag.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ecommerce.rag.core.perf.PerformanceTraceService;

class DebugPerfControllerTest {

    @Test
    void testRecentReturnsSnakeCase() {
        PerformanceTraceService perfService = mock(PerformanceTraceService.class);
        when(perfService.isRecentEnabled()).thenReturn(true);
        when(perfService.getRecentTraceSummaries(anyInt())).thenReturn(List.of(
                Map.of("trace_id", "abc123", "endpoint", "/api/chat/stream",
                        "total_ms", 100L, "slow", false, "failed", false)
        ));

        DebugPerfController controller = new DebugPerfController(perfService);
        ResponseEntity<Map<String, Object>> response = controller.getRecentTraces(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        assertEquals(1, items.size());
        assertEquals("abc123", items.get(0).get("trace_id"));
    }

    @Test
    void testLimitCapped() {
        PerformanceTraceService perfService = mock(PerformanceTraceService.class);
        when(perfService.isRecentEnabled()).thenReturn(true);
        when(perfService.getRecentTraceSummaries(anyInt())).thenReturn(List.of());

        DebugPerfController controller = new DebugPerfController(perfService);
        controller.getRecentTraces(200);

        verify(perfService).getRecentTraceSummaries(100);
    }

    @Test
    void testNoSensitiveFields() {
        PerformanceTraceService perfService = mock(PerformanceTraceService.class);
        when(perfService.isRecentEnabled()).thenReturn(true);
        when(perfService.getRecentTraceSummaries(anyInt())).thenReturn(List.of(
                Map.of("trace_id", "abc", "endpoint", "/api/chat/stream")
        ));

        DebugPerfController controller = new DebugPerfController(perfService);
        ResponseEntity<Map<String, Object>> response = controller.getRecentTraces(10);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
        Map<String, Object> item = items.get(0);
        assertFalse(item.containsKey("api_key"));
        assertFalse(item.containsKey("authorization"));
        assertFalse(item.containsKey("prompt"));
    }

    @Test
    void testRecentDisabledReturns404() {
        PerformanceTraceService perfService = mock(PerformanceTraceService.class);
        when(perfService.isRecentEnabled()).thenReturn(false);

        DebugPerfController controller = new DebugPerfController(perfService);
        ResponseEntity<Map<String, Object>> response = controller.getRecentTraces(10);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("PERF_RECENT_DISABLED", response.getBody().get("code"));
    }
}
