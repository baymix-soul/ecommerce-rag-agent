package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class QueryPlanningResult {

    public static final String SOURCE_DISABLED = "DISABLED";
    public static final String SOURCE_LLM = "LLM";
    public static final String SOURCE_FALLBACK = "FALLBACK";
    public static final String SOURCE_ERROR = "ERROR";

    private String originalQuery;
    private Boolean plannerEnabled;
    private String mode;
    private QueryPlan rawPlan;
    private QueryPlan validatedPlan;
    private QueryPlanValidationResult validationResult;
    private Boolean parseSuccess;
    private Boolean valid;
    private String rawLlmOutput;
    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private Long latencyMs;
    private String source;

    public static QueryPlanningResult disabled() {
        QueryPlanningResult r = new QueryPlanningResult();
        r.plannerEnabled = false;
        r.source = SOURCE_DISABLED;
        r.parseSuccess = false;
        r.valid = false;
        return r;
    }

    public static QueryPlanningResult error(String originalQuery, String errorMessage) {
        QueryPlanningResult r = new QueryPlanningResult();
        r.originalQuery = originalQuery;
        r.plannerEnabled = true;
        r.source = SOURCE_ERROR;
        r.parseSuccess = false;
        r.valid = false;
        r.errors.add(errorMessage);
        return r;
    }

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public Boolean getPlannerEnabled() { return plannerEnabled; }
    public void setPlannerEnabled(Boolean plannerEnabled) { this.plannerEnabled = plannerEnabled; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public QueryPlan getRawPlan() { return rawPlan; }
    public void setRawPlan(QueryPlan rawPlan) { this.rawPlan = rawPlan; }

    public QueryPlan getValidatedPlan() { return validatedPlan; }
    public void setValidatedPlan(QueryPlan validatedPlan) { this.validatedPlan = validatedPlan; }

    public QueryPlanValidationResult getValidationResult() { return validationResult; }
    public void setValidationResult(QueryPlanValidationResult validationResult) { this.validationResult = validationResult; }

    public Boolean getParseSuccess() { return parseSuccess; }
    public void setParseSuccess(Boolean parseSuccess) { this.parseSuccess = parseSuccess; }

    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }

    public String getRawLlmOutput() { return rawLlmOutput; }
    public void setRawLlmOutput(String rawLlmOutput) { this.rawLlmOutput = rawLlmOutput; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors != null ? errors : new ArrayList<>(); }

    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
