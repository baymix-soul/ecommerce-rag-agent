package com.ecommerce.rag.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.retriever.ConstraintCheckResult;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.RetrievedProductCandidate;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.retriever.VectorRetriever;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

@RestController
@RequestMapping("/api/rag/retrieval")
public class RagRetrievalDebugController {

    private final HybridCandidateRetriever hybridRetriever;
    private final VectorRetriever vectorRetriever;
    private final AppProperties appProperties;
    private final RetrievalRouter retrievalRouter;
    private final ConversationMemoryService memoryService;
    private final PageContextResolver pageContextResolver;
    private final StrictProductConstraintFilter constraintFilter;
    private final QueryUnderstandingService understandingService;

    public RagRetrievalDebugController(HybridCandidateRetriever hybridRetriever,
                                        VectorRetriever vectorRetriever,
                                        AppProperties appProperties,
                                        RetrievalRouter retrievalRouter,
                                        ConversationMemoryService memoryService,
                                        PageContextResolver pageContextResolver,
                                        StrictProductConstraintFilter constraintFilter,
                                        QueryUnderstandingService understandingService) {
        this.hybridRetriever = hybridRetriever;
        this.vectorRetriever = vectorRetriever;
        this.appProperties = appProperties;
        this.retrievalRouter = retrievalRouter;
        this.memoryService = memoryService;
        this.pageContextResolver = pageContextResolver;
        this.constraintFilter = constraintFilter;
        this.understandingService = understandingService;
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit,
            @RequestParam(value = "session_id", required = false) String sessionId) {

        String sid = sessionId != null && !sessionId.isBlank() ? sessionId : "default";

        RetrievalRouteResult routeResult = retrievalRouter.route(query);

        ConversationState memoryBefore = memoryService.getState(sid);
        PageContextResolution pageContext = pageContextResolver.resolve(null);

        QueryUnderstandingResult understandingResult = understandingService.understand(query, sid, null);

        QueryAnalysisResult effectiveAnalysis = understandingResult.getEffectiveAnalysis();

        List<RetrievedProductCandidate> rawCandidates = hybridRetriever.retrieveRawWithAnalysis(
                query, limit, effectiveAnalysis);

        List<Map<String, Object>> finalCandidateMaps = new ArrayList<>();
        List<Map<String, Object>> filteredOutMaps = new ArrayList<>();

        for (RetrievedProductCandidate rc : rawCandidates) {
            Product p = rc.getProduct();
            if (p == null) continue;

            ConstraintCheckResult checkResult = constraintFilter.check(p, effectiveAnalysis);

            if (checkResult.isPassed()) {
                Map<String, Object> cMap = buildCandidateMap(rc, effectiveAnalysis, checkResult);
                finalCandidateMaps.add(cMap);
            } else {
                Map<String, Object> fMap = new LinkedHashMap<>();
                fMap.put("product_id", p.getProductId());
                fMap.put("name", p.getName());
                fMap.put("category", p.getCategory());
                fMap.put("sub_category", p.getSubCategory());
                fMap.put("price", p.getPrice());
                fMap.put("brand", p.getBrand());
                fMap.put("vector_score", rc.getVectorScore());
                fMap.put("keyword_score", rc.getKeywordScore());
                fMap.put("final_score", rc.getFinalScore());
                fMap.put("failed_rules", checkResult.getFailedRules());
                if (!checkResult.getFailures().isEmpty()) {
                    List<Map<String, Object>> failureMaps = new ArrayList<>();
                    for (var failure : checkResult.getFailures()) {
                        Map<String, Object> fm = new LinkedHashMap<>();
                        fm.put("type", failure.getType());
                        fm.put("field", failure.getField());
                        fm.put("expected", failure.getExpected());
                        fm.put("actual", failure.getActual());
                        fm.put("message", failure.getMessage());
                        failureMaps.add(fm);
                    }
                    fMap.put("failures", failureMaps);
                }
                filteredOutMaps.add(fMap);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("session_id", sid);
        result.put("intent", routeResult.getIntent().name());
        result.put("intent_reason", routeResult.getReason());
        result.put("mode", appProperties.getRetrieval().getMode());
        result.put("vector_index_count", vectorRetriever.getVectorStoreCount());
        result.put("total", rawCandidates.size());

        result.put("legacy_analysis", understandingResult.getLegacyAnalysis());

        if (understandingResult.getPlanningResult() != null) {
            result.put("planner_result", buildPlannerResultMap(understandingResult));
        }

        if (understandingResult.getValidatedPlan() != null) {
            result.put("validated_plan", understandingResult.getValidatedPlan());
        }

        if (understandingResult.getGateDecision() != null) {
            result.put("gate_decision", buildGateDecisionMap(understandingResult));
        }

        if (effectiveAnalysis != null) {
            result.put("effective_analysis", buildQueryAnalysisMap(effectiveAnalysis));
        }

        result.put("selected_source", understandingResult.getSelectedSource());
        result.put("planner_used_for_retrieval", understandingResult.getPlannerUsedForRetrieval());
        result.put("fallback_reason", understandingResult.getFallbackReason());

        if (memoryBefore != null) {
            result.put("memory_before", buildMemoryMap(memoryBefore));
        }
        result.put("memory_after", buildMemoryMap(memoryService.getState(sid)));

        result.put("page_context_resolution", buildPageContextMap(pageContext));

        result.put("raw_candidate_count", rawCandidates.size());
        result.put("final_candidate_count", finalCandidateMaps.size());
        result.put("final_candidates", finalCandidateMaps);
        result.put("filtered_out_candidates", filteredOutMaps);

        if (effectiveAnalysis != null && effectiveAnalysis.getRewriteResult() != null) {
            result.put("rewrite_result", buildRewriteResultMap(effectiveAnalysis.getRewriteResult()));
        }

        // Conversation Context v2 debug info
        if (understandingResult.getActiveSearchContext() != null) {
            result.put("active_search_context", buildActiveSearchContextMap(understandingResult.getActiveSearchContext()));
        }
        if (understandingResult.getContextAction() != null) {
            result.put("context_action", understandingResult.getContextAction());
        }

        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildPlannerResultMap(QueryUnderstandingResult ur) {
        Map<String, Object> map = new LinkedHashMap<>();
        var pr = ur.getPlanningResult();
        if (pr == null) return map;
        map.put("planner_enabled", pr.getPlannerEnabled());
        map.put("source", pr.getSource());
        map.put("parse_success", pr.getParseSuccess());
        map.put("valid", pr.getValid());
        map.put("latency_ms", pr.getLatencyMs());
        if (pr.getRawPlan() != null && pr.getRawPlan().getTarget() != null) {
            map.put("raw_category", pr.getRawPlan().getTarget().getCategory());
            map.put("raw_sub_category", pr.getRawPlan().getTarget().getSubCategory());
        }
        if (pr.getValidatedPlan() != null && pr.getValidatedPlan().getTarget() != null) {
            map.put("validated_category", pr.getValidatedPlan().getTarget().getCategory());
            map.put("validated_sub_category", pr.getValidatedPlan().getTarget().getSubCategory());
        }
        if (pr.getValidatedPlan() != null) {
            map.put("confidence", pr.getValidatedPlan().getConfidence());
        }
        if (pr.getValidationResult() != null) {
            map.put("validation_warnings", pr.getValidationResult().getWarnings());
            map.put("category_matched", pr.getValidationResult().getCategoryMatched());
            map.put("sub_category_matched", pr.getValidationResult().getSubCategoryMatched());
        }
        return map;
    }

    private Map<String, Object> buildGateDecisionMap(QueryUnderstandingResult ur) {
        Map<String, Object> map = new LinkedHashMap<>();
        var gd = ur.getGateDecision();
        if (gd == null) return map;
        map.put("allowed", gd.isAllowed());
        map.put("selected_source", gd.getSelectedSource());
        map.put("reason", gd.getReason());
        map.put("fallback_reasons", gd.getFallbackReasons());
        map.put("confidence", gd.getConfidence());
        map.put("valid", gd.getValid());
        map.put("has_errors", gd.getHasErrors());
        map.put("mode", gd.getMode());
        return map;
    }

    private Map<String, Object> buildCandidateMap(RetrievedProductCandidate rc,
                                                   QueryAnalysisResult analysis,
                                                   ConstraintCheckResult checkResult) {
        Map<String, Object> cMap = new LinkedHashMap<>();
        Product p = rc.getProduct();
        if (p != null) {
            cMap.put("product_id", p.getProductId());
            cMap.put("name", p.getName());
            cMap.put("category", p.getCategory());
            cMap.put("sub_category", p.getSubCategory());
            cMap.put("brand", p.getBrand());
            cMap.put("price", p.getPrice());
        } else {
            cMap.put("product_id", rc.getProductId());
        }
        cMap.put("vector_score", rc.getVectorScore());
        cMap.put("keyword_score", rc.getKeywordScore());
        cMap.put("final_score", rc.getFinalScore());
        cMap.put("matched_sources", rc.getMatchedSources());

        Set<String> chunkTypes = new LinkedHashSet<>();
        List<String> textSnippets = new ArrayList<>();
        for (VectorSearchHit vh : rc.getMatchedChunks()) {
            if (vh.getChunkType() != null) chunkTypes.add(vh.getChunkType());
            if (vh.getText() != null) {
                String snippet = vh.getText().length() > 150
                        ? vh.getText().substring(0, 150) + "..." : vh.getText();
                textSnippets.add(snippet);
            }
        }
        cMap.put("matched_chunk_types", new ArrayList<>(chunkTypes));
        cMap.put("matched_text_snippets", textSnippets);

        if (checkResult != null) {
            cMap.put("constraint_passed", true);
            cMap.put("passed_rules", checkResult.getPassedRules());
        }

        return cMap;
    }

    private Map<String, Object> buildQueryAnalysisMap(QueryAnalysisResult analysis) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("original_query", analysis.getOriginalQuery());
        map.put("normalized_query", analysis.getNormalizedQuery());
        map.put("resolved_query", analysis.getResolvedQuery());
        map.put("inherited_from_context", analysis.getInheritedFromContext());
        map.put("category", analysis.getCategory());
        map.put("sub_category", analysis.getSubCategory());
        map.put("sub_categories", analysis.getSubCategories());
        map.put("min_price", analysis.getMinPrice());
        map.put("max_price", analysis.getMaxPrice());
        map.put("negative_brands", analysis.getNegativeBrands());
        map.put("exclude_brands", analysis.getExcludeBrands());
        map.put("exclude_product_ids", analysis.getExcludeProductIds());
        map.put("positive_keywords", analysis.getPositiveKeywords());
        map.put("negative_keywords", analysis.getNegativeKeywords());
        map.put("avoid_ingredients_or_terms", analysis.getAvoidIngredientsOrTerms());
        map.put("filters", analysis.getFilters());
        map.put("warnings", analysis.getWarnings());
        if (analysis.getIntent() != null) {
            map.put("intent", analysis.getIntent().name());
        }
        return map;
    }

    private Map<String, Object> buildMemoryMap(ConversationState state) {
        if (state == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("session_id", state.getSessionId());
        map.put("turn_count", state.getTurnCount());
        map.put("last_user_query", state.getLastUserQuery());
        map.put("category", state.getCategory());
        map.put("sub_category", state.getSubCategory());
        map.put("min_price", state.getMinPrice());
        map.put("max_price", state.getMaxPrice());
        map.put("recommended_product_ids", state.getRecommendedProductIds());
        return map;
    }

    private Map<String, Object> buildPageContextMap(PageContextResolution pageContext) {
        if (pageContext == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("page_type", pageContext.getPageType().name());
        if (pageContext.isHasValidCurrentProduct()) {
            Product cp = pageContext.getCurrentProduct();
            Map<String, Object> cpMap = new LinkedHashMap<>();
            cpMap.put("product_id", cp.getProductId());
            cpMap.put("name", cp.getName());
            cpMap.put("brand", cp.getBrand());
            cpMap.put("category", cp.getCategory());
            cpMap.put("sub_category", cp.getSubCategory());
            cpMap.put("price", cp.getPrice());
            map.put("current_product", cpMap);
        }
        map.put("visible_product_count", pageContext.getVisibleProducts().size());
        map.put("recently_viewed_count", pageContext.getRecentlyViewedProducts().size());
        map.put("warnings", pageContext.getWarnings());
        return map;
    }

    private Map<String, Object> buildRewriteResultMap(com.ecommerce.rag.rag.rewrite.QueryRewriteResult rewriteResult) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (rewriteResult == null) return map;
        map.put("source", rewriteResult.getSource());
        map.put("expanded_query", rewriteResult.getExpandedQuery());
        map.put("query_variants", rewriteResult.getQueryVariants());
        map.put("soft_keywords", rewriteResult.getSoftKeywords());
        map.put("inferred_scenarios", rewriteResult.getInferredScenarios());
        map.put("confidence", rewriteResult.getConfidence());
        map.put("warnings", rewriteResult.getWarnings());
        return map;
    }

    private Map<String, Object> buildActiveSearchContextMap(com.ecommerce.rag.rag.memory.ActiveSearchContext ctx) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (ctx == null) return map;
        map.put("session_id", ctx.getSessionId());
        map.put("active_task", ctx.getActiveTask());
        map.put("category", ctx.getCategory());
        map.put("sub_category", ctx.getSubCategory());
        map.put("sub_categories", ctx.getSubCategories());
        map.put("min_price", ctx.getMinPrice());
        map.put("max_price", ctx.getMaxPrice());
        map.put("positive_keywords", ctx.getPositiveKeywords());
        map.put("soft_preferences", ctx.getSoftPreferences());
        map.put("negative_keywords", ctx.getNegativeKeywords());
        map.put("negative_brands", ctx.getNegativeBrands());
        map.put("exclude_product_ids", ctx.getExcludeProductIds());
        map.put("audience", ctx.getAudience());
        map.put("scenario", ctx.getScenario());
        map.put("last_recommended_product_ids", ctx.getLastRecommendedProductIds());
        map.put("last_successful_candidate_ids", ctx.getLastSuccessfulCandidateIds());
        map.put("turn_count", ctx.getTurnCount());
        map.put("last_context_action", ctx.getLastContextAction());
        map.put("last_no_match_reason", ctx.getLastNoMatchReason());

        List<Map<String, Object>> constraintMaps = new ArrayList<>();
        for (var c : ctx.getActiveConstraints()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("field", c.getField());
            cm.put("value", c.getValue());
            cm.put("strength", c.getStrength() != null ? c.getStrength().name() : null);
            cm.put("source_query", c.getSourceQuery());
            cm.put("source_turn", c.getSourceTurn());
            cm.put("active", c.isActive());
            constraintMaps.add(cm);
        }
        map.put("active_constraints", constraintMaps);
        return map;
    }
}
