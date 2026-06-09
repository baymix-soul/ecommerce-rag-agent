package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

public class QueryUnderstandingResult {

    private String query;
    private String sessionId;
    private QueryPlan validatedPlan;
    private Boolean plannerUsedForRetrieval;
    private List<String> warnings = new ArrayList<>();
    private Object legacyAnalysis;
    private QueryPlanningResult planningResult;
    private QueryPlanGateDecision gateDecision;
    private QueryAnalysisResult effectiveAnalysis;
    private String selectedSource;
    private String fallbackReason;
    private CartSemanticMatchResult cartSemanticMatch;
    private ActiveSearchContext activeSearchContext;
    private String contextAction;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public QueryPlan getValidatedPlan() { return validatedPlan; }
    public void setValidatedPlan(QueryPlan validatedPlan) { this.validatedPlan = validatedPlan; }

    public Boolean getPlannerUsedForRetrieval() { return plannerUsedForRetrieval; }
    public void setPlannerUsedForRetrieval(Boolean plannerUsedForRetrieval) { this.plannerUsedForRetrieval = plannerUsedForRetrieval; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings != null ? warnings : new ArrayList<>(); }

    public Object getLegacyAnalysis() { return legacyAnalysis; }
    public void setLegacyAnalysis(Object legacyAnalysis) { this.legacyAnalysis = legacyAnalysis; }

    public QueryPlanningResult getPlanningResult() { return planningResult; }
    public void setPlanningResult(QueryPlanningResult planningResult) { this.planningResult = planningResult; }

    public QueryPlanGateDecision getGateDecision() { return gateDecision; }
    public void setGateDecision(QueryPlanGateDecision gateDecision) { this.gateDecision = gateDecision; }

    public QueryAnalysisResult getEffectiveAnalysis() { return effectiveAnalysis; }
    public void setEffectiveAnalysis(QueryAnalysisResult effectiveAnalysis) { this.effectiveAnalysis = effectiveAnalysis; }

    public String getSelectedSource() { return selectedSource; }
    public void setSelectedSource(String selectedSource) { this.selectedSource = selectedSource; }

    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }

    public CartSemanticMatchResult getCartSemanticMatch() { return cartSemanticMatch; }
    public void setCartSemanticMatch(CartSemanticMatchResult cartSemanticMatch) { this.cartSemanticMatch = cartSemanticMatch; }

    public ActiveSearchContext getActiveSearchContext() { return activeSearchContext; }
    public void setActiveSearchContext(ActiveSearchContext activeSearchContext) { this.activeSearchContext = activeSearchContext; }

    public String getContextAction() { return contextAction; }
    public void setContextAction(String contextAction) { this.contextAction = contextAction; }
}
