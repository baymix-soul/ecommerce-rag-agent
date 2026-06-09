package com.ecommerce.rag.core.perf;

public class PerfTraceContext {

    private static final ThreadLocal<PerfTrace> CURRENT = new ThreadLocal<>();

    private PerfTraceContext() {
    }

    public static void set(PerfTrace trace) {
        CURRENT.set(trace);
    }

    public static PerfTrace get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static PerfTrace startSpan(String name) {
        PerfTrace trace = CURRENT.get();
        if (trace != null) {
            trace.startSpan(name);
        }
        return trace;
    }

    public static void endSpan(String name) {
        PerfTrace trace = CURRENT.get();
        if (trace != null) {
            trace.endSpan(name);
        }
    }

    public static void mark(String name) {
        PerfTrace trace = CURRENT.get();
        if (trace != null) {
            trace.mark(name);
        }
    }

    public static void addAttribute(String key, Object value) {
        PerfTrace trace = CURRENT.get();
        if (trace != null) {
            trace.addAttribute(key, value);
        }
    }
}
