package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanGateDecision {

    public static final String SELECTED_LEGACY = "LEGACY";
    public static final String SELECTED_PLANNER = "PLANNER";
    public static final String SELECTED_SHADOW_ONLY = "SHADOW_ONLY";
    public static final String SELECTED_DISABLED = "DISABLED";
    public static final String SELECTED_FALLBACK = "FALLBACK";

    private boolean allowed;
    private String selectedSource;
    private String reason;
    private List<String> fallbackReasons = new ArrayList<>();
    private Double confidence;
    private Boolean valid;
    private Boolean hasErrors;
    private Boolean hasSevereWarnings;
    private String mode;

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }

    public String getSelectedSource() { return selectedSource; }
    public void setSelectedSource(String selectedSource) { this.selectedSource = selectedSource; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<String> getFallbackReasons() { return fallbackReasons; }
    public void setFallbackReasons(List<String> fallbackReasons) { this.fallbackReasons = fallbackReasons != null ? fallbackReasons : new ArrayList<>(); }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }

    public Boolean getHasErrors() { return hasErrors; }
    public void setHasErrors(Boolean hasErrors) { this.hasErrors = hasErrors; }

    public Boolean getHasSevereWarnings() { return hasSevereWarnings; }
    public void setHasSevereWarnings(Boolean hasSevereWarnings) { this.hasSevereWarnings = hasSevereWarnings; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
