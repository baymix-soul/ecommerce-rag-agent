package com.ecommerce.rag.core.perf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;

@Service
public class PerformanceTraceService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTraceService.class);

    private final AppProperties appProperties;
    private final ConcurrentLinkedDeque<PerfTrace> recentTraces = new ConcurrentLinkedDeque<>();

    public PerformanceTraceService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public boolean isEnabled() {
        AppProperties.PerfProperties perf = appProperties.getPerf();
        return perf != null && perf.isEnabled();
    }

    public boolean isLogEnabled() {
        AppProperties.PerfProperties perf = appProperties.getPerf();
        return perf != null && perf.isLogEnabled();
    }

    public boolean isRecentEnabled() {
        AppProperties.PerfProperties perf = appProperties.getPerf();
        return perf != null && perf.isRecentEnabled();
    }

    public PerfTrace beginTrace(String endpoint, String sessionId, String query) {
        if (!isEnabled()) {
            return null;
        }
        int previewLength = appProperties.getPerf() != null
                ? appProperties.getPerf().getQueryPreviewLength() : 40;
        PerfTrace trace = PerfTrace.create(endpoint, sessionId, query, previewLength);
        PerfTraceContext.set(trace);
        MDC.put("traceId", trace.getTraceId());
        return trace;
    }

    public void finishTrace(PerfTrace trace) {
        if (trace == null) {
            return;
        }
        int slowThreshold = appProperties.getPerf() != null
                ? appProperties.getPerf().getSlowThresholdMs() : 3000;
        trace.finish(slowThreshold);
        PerfTraceContext.clear();
        MDC.remove("traceId");

        if (isLogEnabled()) {
            String summary = trace.summary();
            if (trace.isSlow() || trace.isFailed()) {
                log.warn(summary);
            } else {
                log.info(summary);
            }
        }

        if (isRecentEnabled()) {
            saveRecent(trace);
        }
    }

    public void finishTraceWithError(PerfTrace trace, String errorCode) {
        if (trace != null) {
            trace.setFailed(errorCode);
        }
        finishTrace(trace);
    }

    private void saveRecent(PerfTrace trace) {
        int size = appProperties.getPerf() != null ? appProperties.getPerf().getRecentSize() : 100;
        recentTraces.addFirst(trace);
        while (recentTraces.size() > size) {
            recentTraces.pollLast();
        }
    }

    public List<PerfTrace> getRecentTraces(int limit) {
        if (!isRecentEnabled()) {
            return Collections.emptyList();
        }
        List<PerfTrace> result = new ArrayList<>();
        for (PerfTrace trace : recentTraces) {
            if (result.size() >= limit) break;
            result.add(trace);
        }
        return result;
    }

    public List<Map<String, Object>> getRecentTraceSummaries(int limit) {
        List<PerfTrace> traces = getRecentTraces(limit);
        List<Map<String, Object>> summaries = new ArrayList<>(traces.size());
        for (PerfTrace trace : traces) {
            summaries.add(trace.toSummaryMap());
        }
        return summaries;
    }
}
