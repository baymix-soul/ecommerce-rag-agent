package com.ecommerce.rag.rag.router;

public class RetrievalRouteResult {

    private RetrievalIntent intent;
    private boolean needsRetrieval;
    private String reason;

    public RetrievalRouteResult() {}

    public RetrievalRouteResult(RetrievalIntent intent, boolean needsRetrieval, String reason) {
        this.intent = intent;
        this.needsRetrieval = needsRetrieval;
        this.reason = reason;
    }

    public RetrievalIntent getIntent() { return intent; }
    public void setIntent(RetrievalIntent intent) { this.intent = intent; }

    public boolean isNeedsRetrieval() { return needsRetrieval; }
    public void setNeedsRetrieval(boolean needsRetrieval) { this.needsRetrieval = needsRetrieval; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
