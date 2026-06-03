package com.ecommerce.rag.rag.understanding;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
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

    public QueryUnderstandingService(QueryAnalyzer queryAnalyzer,
                                      RetrievalRouter retrievalRouter,
                                      ConversationMemoryService memoryService,
                                      PageContextResolver pageContextResolver,
                                      LLMQueryPlanner llmQueryPlanner,
                                      QueryPlanGatingService gatingService,
                                      QueryPlanToAnalysisMapper mapper,
                                      AppProperties appProperties) {
        this.queryAnalyzer = queryAnalyzer;
        this.retrievalRouter = retrievalRouter;
        this.memoryService = memoryService;
        this.pageContextResolver = pageContextResolver;
        this.llmQueryPlanner = llmQueryPlanner;
        this.gatingService = gatingService;
        this.mapper = mapper;
        this.appProperties = appProperties;
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

        var routeResult = retrievalRouter.route(query);
        QueryAnalysisResult legacyAnalysis = queryAnalyzer.analyze(query, state, pageCtx);
        if (legacyAnalysis.getIntent() == null) {
            legacyAnalysis.setIntent(routeResult.getIntent());
        }
        legacyAnalysis.setSessionId(sid);

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
            planningResult = llmQueryPlanner.plan(query, state, pageCtx);
            result.setPlanningResult(planningResult);
            if (planningResult.getValidatedPlan() != null) {
                result.setValidatedPlan(planningResult.getValidatedPlan());
            }
        } catch (Exception e) {
            log.warn("QueryUnderstandingService: planner error: {}", e.getMessage());
            result.getWarnings().add("planner error: " + e.getMessage());
            planningResult = QueryPlanningResult.disabled();
            result.setPlanningResult(planningResult);
        }

        QueryPlanGateDecision gateDecision = gatingService.decide(planningResult, legacyAnalysis, state);
        result.setGateDecision(gateDecision);

        if (gateDecision.isAllowed()) {
            QueryPlan validatedPlan = planningResult.getValidatedPlan();
            QueryPlanValidationResult validationResult = planningResult.getValidationResult();
            QueryAnalysisResult plannerAnalysis = mapper.map(validatedPlan, validationResult);
            plannerAnalysis.setSessionId(sid);
            if (legacyAnalysis.getIntent() != null) {
                plannerAnalysis.setIntent(legacyAnalysis.getIntent());
            }
            plannerAnalysis.setInheritedFromContext(legacyAnalysis.getInheritedFromContext());
            plannerAnalysis.setInheritedFromPageContext(legacyAnalysis.getInheritedFromPageContext());
            plannerAnalysis.setCurrentProductId(legacyAnalysis.getCurrentProductId());
            plannerAnalysis.setBoostedProductIds(new java.util.ArrayList<>(legacyAnalysis.getBoostedProductIds()));
            plannerAnalysis.setScopeProductIds(new java.util.ArrayList<>(legacyAnalysis.getScopeProductIds()));

            result.setEffectiveAnalysis(plannerAnalysis);
            result.setPlannerUsedForRetrieval(true);
            result.setSelectedSource(QueryPlanGateDecision.SELECTED_PLANNER);

            log.info("QueryUnderstandingService: PLANNER takeover — query='{}', mode={}, cat={}, sub={}, maxPrice={}, confidence={}",
                    query, mode,
                    plannerAnalysis.getCategory(), plannerAnalysis.getSubCategory(),
                    plannerAnalysis.getMaxPrice(), gateDecision.getConfidence());
        } else {
            result.setEffectiveAnalysis(legacyAnalysis);
            result.setPlannerUsedForRetrieval(false);
            result.setSelectedSource(gateDecision.getSelectedSource());
            result.setFallbackReason(gateDecision.getReason());

            if (!"disabled".equals(mode) && !"shadow".equals(mode)) {
                log.info("QueryUnderstandingService: fallback to LEGACY — query='{}', mode={}, reason={}",
                        query, mode, gateDecision.getReason());
            }
        }

        return result;
    }
}
