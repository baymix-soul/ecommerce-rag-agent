package com.ecommerce.rag.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.core.perf.PerformanceTraceService;

@RestController
@RequestMapping("/api/debug/perf")
public class DebugPerfController {

    private static final Logger log = LoggerFactory.getLogger(DebugPerfController.class);

    private final PerformanceTraceService perfService;

    public DebugPerfController(PerformanceTraceService perfService) {
        this.perfService = perfService;
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentTraces(
            @RequestParam(defaultValue = "20") int limit) {
        if (!perfService.isRecentEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "PERF_RECENT_DISABLED",
                            "message", "Performance recent trace is disabled"));
        }

        int cappedLimit = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> items = perfService.getRecentTraceSummaries(cappedLimit);
        return ResponseEntity.ok(Map.of("items", items));
    }
}
