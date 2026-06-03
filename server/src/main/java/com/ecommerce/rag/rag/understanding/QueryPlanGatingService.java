package com.ecommerce.rag.rag.understanding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.memory.ConversationState;

@Component
public class QueryPlanGatingService {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanGatingService.class);

    private final AppProperties appProperties;

    public QueryPlanGatingService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public QueryPlanGateDecision decide(QueryPlanningResult planningResult,
                                         QueryAnalysisResult legacyAnalysis,
                                         ConversationState conversationState) {
        QueryPlanGateDecision decision = new QueryPlanGateDecision();

        var plannerProps = appProperties.getUnderstanding() != null
                ? appProperties.getUnderstanding().getPlanner() : null;

        if (plannerProps == null || !plannerProps.isEnabled()) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_DISABLED);
            decision.setReason("planner disabled");
            decision.getFallbackReasons().add("planner disabled");
            decision.setMode("disabled");
            return decision;
        }

        String mode = plannerProps.getMode();
        if (mode == null || mode.isBlank()) mode = "shadow";
        decision.setMode(mode);

        if ("disabled".equals(mode)) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_DISABLED);
            decision.setReason("planner mode is disabled");
            return decision;
        }

        if ("shadow".equals(mode)) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_SHADOW_ONLY);
            decision.setReason("shadow mode — planner runs but does not override retrieval");
            return decision;
        }

        if (planningResult == null || planningResult.getSource() == null
                || QueryPlanningResult.SOURCE_DISABLED.equals(planningResult.getSource())) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_DISABLED);
            decision.setReason("planner returned DISABLED");
            return decision;
        }

        if (QueryPlanningResult.SOURCE_ERROR.equals(planningResult.getSource())) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("planner error");
            if (planningResult.getErrors() != null) {
                decision.getFallbackReasons().addAll(planningResult.getErrors());
            }
            return decision;
        }

        if (planningResult.getParseSuccess() == null || !planningResult.getParseSuccess()) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("planner parse failed");
            return decision;
        }

        if (planningResult.getValid() == null || !planningResult.getValid()) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("planner validation returned invalid");
            decision.setValid(false);
            return decision;
        }

        QueryPlan validatedPlan = planningResult.getValidatedPlan();
        if (validatedPlan == null) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("validated plan is null");
            return decision;
        }

        double confidence = validatedPlan.getConfidence() != null ? validatedPlan.getConfidence() : 0.0;
        decision.setConfidence(confidence);

        double minConfidence = plannerProps.getMinConfidence();
        if (confidence < minConfidence) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("confidence " + confidence + " < min " + minConfidence);
            return decision;
        }

        String intent = validatedPlan.getIntent();
        if (intent != null) {
            Set<String> allowedIntents = parseIntentList(plannerProps.getAllowTakeoverIntents());
            if (!allowedIntents.contains(intent)) {
                decision.setAllowed(false);
                decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
                decision.setReason("intent " + intent + " not in allow list");
                return decision;
            }
        }

        if (QueryPlan.INTENT_UNKNOWN.equals(intent)) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("intent is UNKNOWN");
            return decision;
        }

        if (validatedPlan.getNeedsClarification() != null && validatedPlan.getNeedsClarification()) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("plan indicates needs clarification");
            return decision;
        }

        QueryPlanValidationResult validationResult = planningResult.getValidationResult();
        boolean hasErrors = validationResult != null && validationResult.getErrors() != null
                && !validationResult.getErrors().isEmpty();
        decision.setHasErrors(hasErrors);
        if (hasErrors) {
            decision.setAllowed(false);
            decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
            decision.setReason("validation errors exist");
            return decision;
        }

        boolean hasSevereWarnings = false;

        if (plannerProps.isFallbackOnWarnings() && validationResult != null
                && validationResult.getWarnings() != null) {
            for (String w : validationResult.getWarnings()) {
                if (w.contains("unknown_category") || w.contains("unknown_sub_category")
                        || w.contains("sub_category_not_under_category")
                        || w.contains("invalid_price")) {
                    hasSevereWarnings = true;
                    if (w.contains("unknown_category") && plannerProps.isFallbackOnUnknownCategory()) {
                        decision.setAllowed(false);
                        decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
                        decision.setReason("unknown category warning: " + w);
                        decision.setHasSevereWarnings(true);
                        return decision;
                    }
                    if (w.contains("unknown_sub_category") && plannerProps.isFallbackOnUnknownSubCategory()) {
                        decision.setAllowed(false);
                        decision.setSelectedSource(QueryPlanGateDecision.SELECTED_FALLBACK);
                        decision.setReason("unknown subCategory warning: " + w);
                        decision.setHasSevereWarnings(true);
                        return decision;
                    }
                }
            }
        }
        decision.setHasSevereWarnings(hasSevereWarnings);
        decision.setValid(true);

        if ("assist".equals(mode)) {
            boolean legacyIncomplete = isLegacyIncomplete(legacyAnalysis, validatedPlan);
            if (!legacyIncomplete) {
                decision.setAllowed(false);
                decision.setSelectedSource(QueryPlanGateDecision.SELECTED_LEGACY);
                decision.setReason("assist mode: legacy analysis is already complete");
                return decision;
            }
        }

        decision.setAllowed(true);
        decision.setSelectedSource(QueryPlanGateDecision.SELECTED_PLANNER);
        decision.setReason("planner passed all gates");
        return decision;
    }

    private Set<String> parseIntentList(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return new HashSet<>(Arrays.asList(csv.trim().split("\\s*,\\s*")));
    }

    private boolean isLegacyIncomplete(QueryAnalysisResult analysis, QueryPlan validatedPlan) {
        if (analysis == null) return true;

        boolean hasCategory = analysis.getCategory() != null && !analysis.getCategory().isBlank();
        boolean hasSubCategory = analysis.getSubCategory() != null && !analysis.getSubCategory().isBlank();

        if (!hasCategory && !hasSubCategory) return true;

        boolean hasPositiveKeywords = analysis.getPositiveKeywords() != null
                && !analysis.getPositiveKeywords().isEmpty();

        if (!hasPositiveKeywords && validatedPlan != null
                && validatedPlan.getSoftKeywords() != null
                && !validatedPlan.getSoftKeywords().isEmpty()) {
            return true;
        }

        if (validatedPlan != null && validatedPlan.getPrice() != null
                && validatedPlan.getPrice().getMax() != null) {
            if (analysis.getMaxPrice() == null) {
                return true;
            }
        }

        if (validatedPlan != null && validatedPlan.getIntent() != null
                && analysis.getIntent() != null) {
            String plannerIntent = validatedPlan.getIntent();
            String legacyIntentName = analysis.getIntent().name();
            if (!plannerIntent.equals(legacyIntentName)
                    && ("REFINE_PREVIOUS_QUERY".equals(plannerIntent)
                        || "NEGATIVE_CONSTRAINT".equals(plannerIntent))) {
                return true;
            }
        }

        return !hasPositiveKeywords;
    }
}
