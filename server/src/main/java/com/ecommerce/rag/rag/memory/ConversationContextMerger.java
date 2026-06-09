package com.ecommerce.rag.rag.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.understanding.QueryPlan;

@Component
public class ConversationContextMerger {

    private static final Logger log = LoggerFactory.getLogger(ConversationContextMerger.class);

    public static final String ACTION_NEW_SEARCH = "NEW_SEARCH";
    public static final String ACTION_REFINE_ADD_HARD_CONSTRAINT = "REFINE_ADD_HARD_CONSTRAINT";
    public static final String ACTION_REFINE_ADD_SOFT_PREFERENCE = "REFINE_ADD_SOFT_PREFERENCE";
    public static final String ACTION_REPLACE_HARD_CONSTRAINT = "REPLACE_HARD_CONSTRAINT";
    public static final String ACTION_REPLACE_SOFT_PREFERENCE = "REPLACE_SOFT_PREFERENCE";
    public static final String ACTION_ADD_EXCLUSION = "ADD_EXCLUSION";
    public static final String ACTION_RELAX_CONSTRAINT = "RELAX_CONSTRAINT";
    public static final String ACTION_SWITCH_CATEGORY = "SWITCH_CATEGORY";
    public static final String ACTION_SWITCH_SUBCATEGORY = "SWITCH_SUBCATEGORY";
    public static final String ACTION_SWITCH_AUDIENCE = "SWITCH_AUDIENCE";
    public static final String ACTION_REFER_CURRENT_PRODUCT = "REFER_CURRENT_PRODUCT";
    public static final String ACTION_REFER_RECOMMENDED_PRODUCT = "REFER_RECOMMENDED_PRODUCT";
    public static final String ACTION_CART_ACTION = "CART_ACTION";
    public static final String ACTION_UNKNOWN = "UNKNOWN";

    public MergeResult merge(ActiveSearchContext activeContext, QueryAnalysisResult currentAnalysis,
                              QueryPlan queryPlan, String query) {
        if (activeContext == null) {
            activeContext = new ActiveSearchContext();
        }

        String contextAction = determineContextAction(activeContext, currentAnalysis, queryPlan, query);
        activeContext.setLastContextAction(contextAction);

        int nextTurn = activeContext.getTurnCount() + 1;

        switch (contextAction) {
            case ACTION_NEW_SEARCH -> handleNewSearch(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REFINE_ADD_HARD_CONSTRAINT -> handleRefineHard(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REFINE_ADD_SOFT_PREFERENCE -> handleRefineSoft(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REPLACE_HARD_CONSTRAINT -> handleReplaceHard(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REPLACE_SOFT_PREFERENCE -> handleReplaceSoft(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_ADD_EXCLUSION -> handleAddExclusion(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_RELAX_CONSTRAINT -> handleRelaxConstraint(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_SWITCH_CATEGORY -> handleSwitchCategory(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_SWITCH_SUBCATEGORY -> handleSwitchSubCategory(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_SWITCH_AUDIENCE -> handleSwitchAudience(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REFER_CURRENT_PRODUCT -> handleReferCurrentProduct(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_REFER_RECOMMENDED_PRODUCT -> handleReferRecommendedProduct(activeContext, currentAnalysis, query, nextTurn);
            case ACTION_CART_ACTION -> {}
            default -> handleDefault(activeContext, currentAnalysis, query, nextTurn);
        }

        activeContext.touch();

        QueryAnalysisResult effectiveAnalysis = activeContext.toQueryAnalysisResult();
        effectiveAnalysis.setOriginalQuery(currentAnalysis.getOriginalQuery());
        effectiveAnalysis.setNormalizedQuery(currentAnalysis.getNormalizedQuery());
        effectiveAnalysis.setResolvedQuery(currentAnalysis.getResolvedQuery());
        effectiveAnalysis.setIntent(currentAnalysis.getIntent());
        effectiveAnalysis.setInheritedFromContext(currentAnalysis.getInheritedFromContext());
        effectiveAnalysis.setInheritedFromPageContext(currentAnalysis.getInheritedFromPageContext());
        effectiveAnalysis.setCurrentProductId(currentAnalysis.getCurrentProductId());
        effectiveAnalysis.setBoostedProductIds(new ArrayList<>(currentAnalysis.getBoostedProductIds()));
        effectiveAnalysis.setScopeProductIds(new ArrayList<>(currentAnalysis.getScopeProductIds()));
        effectiveAnalysis.setPageWarnings(new ArrayList<>(currentAnalysis.getPageWarnings()));
        effectiveAnalysis.setResponseStyle(currentAnalysis.getResponseStyle());
        effectiveAnalysis.setRequestedProductCount(currentAnalysis.getRequestedProductCount());

        MergeResult result = new MergeResult();
        result.setActiveContext(activeContext);
        result.setEffectiveAnalysis(effectiveAnalysis);
        result.setContextAction(contextAction);
        return result;
    }

    private String determineContextAction(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                          QueryPlan plan, String query) {
        if (plan != null && plan.getCart() != null && Boolean.TRUE.equals(plan.getCart().getNeedsCart())) {
            return ACTION_CART_ACTION;
        }

        if (query == null || query.isBlank()) {
            return ACTION_UNKNOWN;
        }

        boolean hasActiveSearch = ctx.getCategory() != null && !ctx.getCategory().isBlank();

        if (plan != null && plan.getContextAction() != null && !plan.getContextAction().isBlank()) {
            String pa = plan.getContextAction();
            return switch (pa) {
                case QueryPlan.CONTEXT_ACTION_NEW_SEARCH -> ACTION_NEW_SEARCH;
                case QueryPlan.CONTEXT_ACTION_REFINE_PREVIOUS_SEARCH -> {
                    if (analysis.getMaxPrice() != null || analysis.getMinPrice() != null) {
                        yield ACTION_REFINE_ADD_HARD_CONSTRAINT;
                    }
                    yield ACTION_REFINE_ADD_SOFT_PREFERENCE;
                }
                case QueryPlan.CONTEXT_ACTION_REPLACE_PREVIOUS_SEARCH -> ACTION_REPLACE_HARD_CONSTRAINT;
                case QueryPlan.CONTEXT_ACTION_EXCLUDE_FROM_PREVIOUS -> ACTION_ADD_EXCLUSION;
                case QueryPlan.CONTEXT_ACTION_CURRENT_PRODUCT_REFERENCE -> ACTION_REFER_CURRENT_PRODUCT;
                case QueryPlan.CONTEXT_ACTION_READ_CART, QueryPlan.CONTEXT_ACTION_READ_CART_AND_RECOMMEND -> ACTION_CART_ACTION;
                default -> hasActiveSearch ? ACTION_REFINE_ADD_SOFT_PREFERENCE : ACTION_NEW_SEARCH;
            };
        }

        if (analysis.getCurrentProductId() != null && !analysis.getCurrentProductId().isBlank()
                && (query.contains("这个") || query.contains("这款"))) {
            return ACTION_REFER_CURRENT_PRODUCT;
        }

        if (!hasActiveSearch) {
            return ACTION_NEW_SEARCH;
        }

        if (query.contains("不要") || query.contains("排除") || query.contains("除了") || query.contains("不含")) {
            return ACTION_ADD_EXCLUSION;
        }

        if (query.contains("换一个") || query.contains("还有吗") || query.contains("其他的") || query.contains("别的")) {
            return ACTION_REFER_RECOMMENDED_PRODUCT;
        }

        if (analysis.getMaxPrice() != null || analysis.getMinPrice() != null) {
            return ACTION_REFINE_ADD_HARD_CONSTRAINT;
        }

        if (analysis.getCategory() != null && !analysis.getCategory().isBlank()
                && !analysis.getCategory().equals(ctx.getCategory())) {
            return ACTION_SWITCH_CATEGORY;
        }

        if (analysis.getSubCategory() != null && !analysis.getSubCategory().isBlank()
                && !analysis.getSubCategory().equals(ctx.getSubCategory())) {
            return ACTION_SWITCH_SUBCATEGORY;
        }

        return ACTION_REFINE_ADD_SOFT_PREFERENCE;
    }

    private void handleNewSearch(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                 String query, int turn) {
        ctx.setCategory(analysis.getCategory());
        ctx.setSubCategory(analysis.getSubCategory());
        ctx.setSubCategories(new ArrayList<>(analysis.getSubCategories()));
        ctx.setMinPrice(analysis.getMinPrice());
        ctx.setMaxPrice(analysis.getMaxPrice());
        ctx.setPositiveKeywords(new ArrayList<>(analysis.getPositiveKeywords()));
        ctx.setSoftPreferences(new ArrayList<>(analysis.getSoftKeywords()));
        ctx.setNegativeKeywords(new ArrayList<>(analysis.getNegativeKeywords()));
        ctx.setNegativeBrands(new ArrayList<>(analysis.getNegativeBrands()));
        ctx.setExcludeProductIds(new ArrayList<>(analysis.getExcludeProductIds()));
        ctx.setAudience(null);
        ctx.setScenario(null);
        ctx.setLastRecommendedProductIds(new ArrayList<>());
        ctx.setLastCandidateProductIds(new ArrayList<>());
        ctx.setConstraints(new ArrayList<>());

        if (analysis.getCategory() != null) {
            ctx.addConstraint("category", analysis.getCategory(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getSubCategory() != null) {
            ctx.addConstraint("subCategory", analysis.getSubCategory(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getMaxPrice() != null) {
            ctx.addConstraint("maxPrice", analysis.getMaxPrice().toString(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getMinPrice() != null) {
            ctx.addConstraint("minPrice", analysis.getMinPrice().toString(), ConstraintStrength.HARD, query, turn);
        }
        for (String kw : analysis.getSoftKeywords()) {
            ctx.addConstraint("softPreferences", kw, ConstraintStrength.SOFT, query, turn);
        }
        for (String brand : analysis.getNegativeBrands()) {
            ctx.addConstraint("negativeBrands", brand, ConstraintStrength.EXCLUSION, query, turn);
        }

        log.debug("ContextMerger: NEW_SEARCH category={}, subCategory={}", ctx.getCategory(), ctx.getSubCategory());
    }

    private void handleRefineHard(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                  String query, int turn) {
        if (analysis.getMaxPrice() != null) {
            ctx.setMaxPrice(analysis.getMaxPrice());
            ctx.addConstraint("maxPrice", analysis.getMaxPrice().toString(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getMinPrice() != null) {
            ctx.setMinPrice(analysis.getMinPrice());
            ctx.addConstraint("minPrice", analysis.getMinPrice().toString(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getCategory() != null) {
            ctx.setCategory(analysis.getCategory());
        }
        if (analysis.getSubCategory() != null) {
            ctx.setSubCategory(analysis.getSubCategory());
        }
        for (String kw : analysis.getSoftKeywords()) {
            if (!ctx.getSoftPreferences().contains(kw)) {
                ctx.getSoftPreferences().add(kw);
                ctx.addConstraint("softPreferences", kw, ConstraintStrength.SOFT, query, turn);
            }
        }
        log.debug("ContextMerger: REFINE_ADD_HARD_CONSTRAINT maxPrice={}", ctx.getMaxPrice());
    }

    private void handleRefineSoft(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                  String query, int turn) {
        for (String kw : analysis.getSoftKeywords()) {
            if (!ctx.getSoftPreferences().contains(kw)) {
                ctx.getSoftPreferences().add(kw);
                ctx.addConstraint("softPreferences", kw, ConstraintStrength.SOFT, query, turn);
            }
        }
        for (String kw : analysis.getPositiveKeywords()) {
            if (!ctx.getPositiveKeywords().contains(kw)) {
                ctx.getPositiveKeywords().add(kw);
            }
        }
        if (analysis.getCategory() != null) {
            ctx.setCategory(analysis.getCategory());
        }
        if (analysis.getSubCategory() != null) {
            ctx.setSubCategory(analysis.getSubCategory());
        }
        log.debug("ContextMerger: REFINE_ADD_SOFT_PREFERENCE softPreferences={}", ctx.getSoftPreferences());
    }

    private void handleReplaceHard(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                   String query, int turn) {
        if (analysis.getMaxPrice() != null) {
            ctx.setMaxPrice(analysis.getMaxPrice());
        }
        if (analysis.getMinPrice() != null) {
            ctx.setMinPrice(analysis.getMinPrice());
        }
        log.debug("ContextMerger: REPLACE_HARD_CONSTRAINT");
    }

    private void handleReplaceSoft(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                   String query, int turn) {
        List<String> newSoft = new ArrayList<>(analysis.getSoftKeywords());
        ctx.setSoftPreferences(newSoft);
        ctx.setAudience(analysis.getPositiveKeywords().isEmpty() ? null
                : analysis.getPositiveKeywords().get(0));
        log.debug("ContextMerger: REPLACE_SOFT_PREFERENCE softPreferences={}", ctx.getSoftPreferences());
    }

    private void handleAddExclusion(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                    String query, int turn) {
        for (String brand : analysis.getNegativeBrands()) {
            if (!ctx.getNegativeBrands().contains(brand)) {
                ctx.getNegativeBrands().add(brand);
                ctx.addConstraint("negativeBrands", brand, ConstraintStrength.EXCLUSION, query, turn);
            }
        }
        for (String kw : analysis.getNegativeKeywords()) {
            if (!ctx.getNegativeKeywords().contains(kw)) {
                ctx.getNegativeKeywords().add(kw);
                ctx.addConstraint("negativeKeywords", kw, ConstraintStrength.EXCLUSION, query, turn);
            }
        }
        for (String id : analysis.getExcludeProductIds()) {
            if (!ctx.getExcludeProductIds().contains(id)) {
                ctx.getExcludeProductIds().add(id);
                ctx.addConstraint("excludeProductIds", id, ConstraintStrength.EXCLUSION, query, turn);
            }
        }
        log.debug("ContextMerger: ADD_EXCLUSION negativeBrands={}", ctx.getNegativeBrands());
    }

    private void handleRelaxConstraint(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                       String query, int turn) {
        ctx.deactivateSoftPreferences();
        log.debug("ContextMerger: RELAX_CONSTRAINT deactivated soft preferences");
    }

    private void handleSwitchCategory(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                      String query, int turn) {
        ctx.setCategory(analysis.getCategory());
        ctx.setSubCategory(analysis.getSubCategory());
        ctx.setSubCategories(new ArrayList<>(analysis.getSubCategories()));
        ctx.setSoftPreferences(new ArrayList<>());
        ctx.setConstraints(new ArrayList<>());
        if (analysis.getCategory() != null) {
            ctx.addConstraint("category", analysis.getCategory(), ConstraintStrength.HARD, query, turn);
        }
        if (analysis.getSubCategory() != null) {
            ctx.addConstraint("subCategory", analysis.getSubCategory(), ConstraintStrength.HARD, query, turn);
        }
        log.debug("ContextMerger: SWITCH_CATEGORY category={}", ctx.getCategory());
    }

    private void handleSwitchSubCategory(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                         String query, int turn) {
        ctx.setSubCategory(analysis.getSubCategory());
        ctx.setSubCategories(new ArrayList<>(analysis.getSubCategories()));
        if (analysis.getSubCategory() != null) {
            ctx.addConstraint("subCategory", analysis.getSubCategory(), ConstraintStrength.HARD, query, turn);
        }
        log.debug("ContextMerger: SWITCH_SUBCATEGORY subCategory={}", ctx.getSubCategory());
    }

    private void handleSwitchAudience(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                      String query, int turn) {
        if (!analysis.getSoftKeywords().isEmpty()) {
            ctx.setAudience(analysis.getSoftKeywords().get(0));
        } else if (!analysis.getPositiveKeywords().isEmpty()) {
            ctx.setAudience(analysis.getPositiveKeywords().get(0));
        }
        log.debug("ContextMerger: SWITCH_AUDIENCE audience={}", ctx.getAudience());
    }

    private void handleReferCurrentProduct(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                           String query, int turn) {
        log.debug("ContextMerger: REFER_CURRENT_PRODUCT");
    }

    private void handleReferRecommendedProduct(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                                               String query, int turn) {
        ctx.getExcludeProductIds().addAll(ctx.getLastRecommendedProductIds());
        for (String id : ctx.getLastRecommendedProductIds()) {
            ctx.addConstraint("excludeProductIds", id, ConstraintStrength.EXCLUSION, query, turn);
        }
        log.debug("ContextMerger: REFER_RECOMMENDED_PRODUCT excluded={}", ctx.getExcludeProductIds());
    }

    private void handleDefault(ActiveSearchContext ctx, QueryAnalysisResult analysis,
                               String query, int turn) {
        if (analysis.getCategory() != null) {
            ctx.setCategory(analysis.getCategory());
        }
        if (analysis.getSubCategory() != null) {
            ctx.setSubCategory(analysis.getSubCategory());
        }
        for (String kw : analysis.getSoftKeywords()) {
            if (!ctx.getSoftPreferences().contains(kw)) {
                ctx.getSoftPreferences().add(kw);
                ctx.addConstraint("softPreferences", kw, ConstraintStrength.SOFT, query, turn);
            }
        }
    }

    public static class MergeResult {
        private ActiveSearchContext activeContext;
        private QueryAnalysisResult effectiveAnalysis;
        private String contextAction;

        public ActiveSearchContext getActiveContext() { return activeContext; }
        public void setActiveContext(ActiveSearchContext activeContext) { this.activeContext = activeContext; }

        public QueryAnalysisResult getEffectiveAnalysis() { return effectiveAnalysis; }
        public void setEffectiveAnalysis(QueryAnalysisResult effectiveAnalysis) { this.effectiveAnalysis = effectiveAnalysis; }

        public String getContextAction() { return contextAction; }
        public void setContextAction(String contextAction) { this.contextAction = contextAction; }
    }
}
