package com.ecommerce.rag.rag.understanding;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.memory.ConversationContextMerger;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.router.RetrievalRouter;

@Service
public class QueryUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(QueryUnderstandingService.class);

    private final QueryAnalyzer queryAnalyzer;
    private final RetrievalRouter retrievalRouter;
    private final ConversationMemoryService memoryService;
    private final PageContextResolver pageContextResolver;
    private final LLMQueryPlanner llmQueryPlanner;
    private final QueryPlanGatingService gatingService;
    private final QueryPlanToAnalysisMapper mapper;
    private final AppProperties appProperties;
    private final CartSemanticFrameMatcher cartFrameMatcher;
    private final ConversationContextMerger contextMerger;

    public QueryUnderstandingService(QueryAnalyzer queryAnalyzer,
                                      RetrievalRouter retrievalRouter,
                                      ConversationMemoryService memoryService,
                                      PageContextResolver pageContextResolver,
                                      LLMQueryPlanner llmQueryPlanner,
                                      QueryPlanGatingService gatingService,
                                      QueryPlanToAnalysisMapper mapper,
                                      AppProperties appProperties,
                                      CartSemanticFrameMatcher cartFrameMatcher,
                                      ConversationContextMerger contextMerger) {
        this.queryAnalyzer = queryAnalyzer;
        this.retrievalRouter = retrievalRouter;
        this.memoryService = memoryService;
        this.pageContextResolver = pageContextResolver;
        this.llmQueryPlanner = llmQueryPlanner;
        this.gatingService = gatingService;
        this.mapper = mapper;
        this.appProperties = appProperties;
        this.cartFrameMatcher = cartFrameMatcher;
        this.contextMerger = contextMerger;
    }

    public QueryUnderstandingResult understandForRetrieval(String query, String sessionId, PageContext pageContext) {
        if (query == null || query.isBlank()) {
            log.warn("QueryUnderstandingService: empty query, returning minimal result");
            QueryUnderstandingResult result = new QueryUnderstandingResult();
            result.setQuery(query);
            result.setSessionId(sessionId);
            result.setPlannerUsedForRetrieval(false);
            result.setSelectedSource("EMPTY_QUERY");
            result.setEffectiveAnalysis(new QueryAnalysisResult());
            result.getEffectiveAnalysis().setOriginalQuery(query);
            return result;
        }
        return understand(query, sessionId, pageContext);
    }

    public QueryUnderstandingResult understand(String query, String sessionId, PageContext pageContext) {
        QueryUnderstandingResult result = new QueryUnderstandingResult();
        result.setQuery(query);
        result.setSessionId(sessionId);

        String sid = sessionId != null && !sessionId.isBlank() ? sessionId : "default";
        ConversationState state = memoryService.getOrCreate(sid);

        PageContextResolution pageCtx = pageContextResolver != null
                ? pageContextResolver.resolve(pageContext)
                : null;

        PerfTraceContext.startSpan("understanding.legacy_analyzer");
        var routeResult = retrievalRouter.route(query);
        QueryAnalysisResult legacyAnalysis = queryAnalyzer.analyze(query, state, pageCtx);
        if (legacyAnalysis.getIntent() == null) {
            legacyAnalysis.setIntent(routeResult.getIntent());
        }
        legacyAnalysis.setSessionId(sid);
        PerfTraceContext.endSpan("understanding.legacy_analyzer");

        PerfTraceContext.startSpan("understanding.semantic_hint");
        CartSemanticMatchResult cartSemanticMatch = cartFrameMatcher.match(query, pageContext, state);
        result.setCartSemanticMatch(cartSemanticMatch);
        PerfTraceContext.endSpan("understanding.semantic_hint");

        Map<String, Object> legacyMap = new LinkedHashMap<>();
        legacyMap.put("original_query", legacyAnalysis.getOriginalQuery());
        legacyMap.put("normalized_query", legacyAnalysis.getNormalizedQuery());
        legacyMap.put("category", legacyAnalysis.getCategory());
        legacyMap.put("sub_category", legacyAnalysis.getSubCategory());
        legacyMap.put("max_price", legacyAnalysis.getMaxPrice());
        legacyMap.put("intent", legacyAnalysis.getIntent() != null ? legacyAnalysis.getIntent().name() : null);
        legacyMap.put("positive_keywords", legacyAnalysis.getPositiveKeywords());
        legacyMap.put("negative_brands", legacyAnalysis.getNegativeBrands());
        result.setLegacyAnalysis(legacyMap);

        var plannerProps = appProperties.getUnderstanding() != null
                ? appProperties.getUnderstanding().getPlanner() : null;
        String mode = plannerProps != null ? plannerProps.getMode() : "disabled";

        QueryPlanningResult planningResult = null;
        try {
            PerfTraceContext.startSpan("understanding.llm_planner");
            planningResult = llmQueryPlanner.plan(query, state, pageCtx);
            PerfTraceContext.endSpan("understanding.llm_planner");
            result.setPlanningResult(planningResult);
            if (planningResult.getValidatedPlan() != null) {
                result.setValidatedPlan(planningResult.getValidatedPlan());
            }
        } catch (Exception e) {
            PerfTraceContext.endSpan("understanding.llm_planner");
            log.warn("QueryUnderstandingService: planner error: {}", e.getMessage());
            result.getWarnings().add("planner error: " + e.getMessage());
            planningResult = QueryPlanningResult.disabled();
            result.setPlanningResult(planningResult);
        }

        PerfTraceContext.startSpan("understanding.gating");
        QueryPlanGateDecision gateDecision = gatingService.decide(planningResult, legacyAnalysis, state);
        result.setGateDecision(gateDecision);
        PerfTraceContext.endSpan("understanding.gating");

        QueryAnalysisResult selectedAnalysis;
        QueryPlan validatedPlan = null;
        if (gateDecision.isAllowed()) {
            validatedPlan = planningResult.getValidatedPlan();
            QueryPlanValidationResult validationResult = planningResult.getValidationResult();

            PerfTraceContext.startSpan("understanding.mapper");
            QueryAnalysisResult plannerAnalysis = mapper.map(validatedPlan, validationResult);
            PerfTraceContext.endSpan("understanding.mapper");

            plannerAnalysis.setSessionId(sid);
            if (legacyAnalysis.getIntent() != null) {
                plannerAnalysis.setIntent(legacyAnalysis.getIntent());
            }
            plannerAnalysis.setInheritedFromContext(legacyAnalysis.getInheritedFromContext());
            plannerAnalysis.setInheritedFromPageContext(legacyAnalysis.getInheritedFromPageContext());
            plannerAnalysis.setCurrentProductId(legacyAnalysis.getCurrentProductId());
            plannerAnalysis.setBoostedProductIds(new java.util.ArrayList<>(legacyAnalysis.getBoostedProductIds()));
            plannerAnalysis.setScopeProductIds(new java.util.ArrayList<>(legacyAnalysis.getScopeProductIds()));

            selectedAnalysis = plannerAnalysis;
            result.setPlannerUsedForRetrieval(true);
            result.setSelectedSource(QueryPlanGateDecision.SELECTED_PLANNER);

            log.info("QueryUnderstandingService: PLANNER takeover — query='{}', mode={}, cat={}, sub={}, maxPrice={}, confidence={}",
                    query, mode,
                    plannerAnalysis.getCategory(), plannerAnalysis.getSubCategory(),
                    plannerAnalysis.getMaxPrice(), gateDecision.getConfidence());
        } else {
            selectedAnalysis = legacyAnalysis;
            result.setPlannerUsedForRetrieval(false);
            result.setSelectedSource(gateDecision.getSelectedSource());
            result.setFallbackReason(gateDecision.getReason());

            if (!"disabled".equals(mode) && !"shadow".equals(mode)) {
                log.info("QueryUnderstandingService: fallback to LEGACY — query='{}', mode={}, reason={}",
                        query, mode, gateDecision.getReason());
            }
        }

        // Conversation Context v2: merge with ActiveSearchContext
        PerfTraceContext.startSpan("understanding.context_merge");
        ActiveSearchContext activeContext = null;
        try {
            ConversationState existingState = memoryService.getOrCreate(sid);
            activeContext = existingState != null ? existingState.getActiveSearchContext() : null;
        } catch (Exception e) {
            log.debug("QueryUnderstandingService: no active context for session={}", sid);
        }

        ConversationContextMerger.MergeResult mergeResult = contextMerger.merge(
                activeContext, selectedAnalysis, validatedPlan, query);
        QueryAnalysisResult effectiveAnalysis = mergeResult.getEffectiveAnalysis();
        result.setEffectiveAnalysis(effectiveAnalysis);
        result.setActiveSearchContext(mergeResult.getActiveContext());
        result.setContextAction(mergeResult.getContextAction());
        PerfTraceContext.endSpan("understanding.context_merge");

        // Update memory with merged context
        try {
            ConversationState mergedState = memoryService.getOrCreate(sid);
            mergedState.setActiveSearchContext(mergeResult.getActiveContext());
            mergedState.setLastQuery(query);
            mergedState.setLastAnalysis(effectiveAnalysis);
            mergedState.setTurnCount(mergedState.getTurnCount() + 1);
            memoryService.save(sid, mergedState);
        } catch (Exception e) {
            log.warn("QueryUnderstandingService: failed to save merged context: {}", e.getMessage());
        }

        PerfTraceContext.addAttribute("legacy_intent",
                legacyAnalysis.getIntent() != null ? legacyAnalysis.getIntent().name() : null);
        PerfTraceContext.addAttribute("effective_intent",
                effectiveAnalysis.getIntent() != null ? effectiveAnalysis.getIntent().name() : null);
        PerfTraceContext.addAttribute("category", effectiveAnalysis.getCategory());
        PerfTraceContext.addAttribute("sub_category", effectiveAnalysis.getSubCategory());
        if (effectiveAnalysis.getMaxPrice() != null) {
            PerfTraceContext.addAttribute("max_price", effectiveAnalysis.getMaxPrice());
        }
        PerfTraceContext.addAttribute("context_action", mergeResult.getContextAction());
        if (mergeResult.getActiveContext() != null) {
            int hard = mergeResult.getActiveContext().getActiveHardConstraints().size();
            int soft = mergeResult.getActiveContext().getActiveSoftPreferences().size();
            int excl = mergeResult.getActiveContext().getActiveExclusions().size();
            PerfTraceContext.addAttribute("active_constraint_count", hard + soft + excl);
            PerfTraceContext.addAttribute("hard_constraint_count", hard);
            PerfTraceContext.addAttribute("soft_preference_count", soft);
            PerfTraceContext.addAttribute("exclusion_count", excl);
        }

        return result;
    }
}
