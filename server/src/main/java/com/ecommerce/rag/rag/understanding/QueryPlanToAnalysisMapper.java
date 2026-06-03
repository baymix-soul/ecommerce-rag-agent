package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.router.RetrievalIntent;

@Component
public class QueryPlanToAnalysisMapper {

    public QueryAnalysisResult map(QueryPlan plan, QueryPlanValidationResult validationResult) {
        QueryAnalysisResult analysis = new QueryAnalysisResult();

        if (plan == null) {
            return analysis;
        }

        analysis.setOriginalQuery(plan.getOriginalQuery());

        String normalized = buildNormalizedQuery(plan);
        analysis.setNormalizedQuery(normalized);

        if (plan.getIntent() != null) {
            try {
                analysis.setIntent(RetrievalIntent.valueOf(plan.getIntent()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (plan.getTarget() != null) {
            QueryPlanTarget target = plan.getTarget();
            analysis.setCategory(target.getCategory());
            analysis.setSubCategory(target.getSubCategory());
            if (target.getSubCategories() != null) {
                analysis.setSubCategories(new ArrayList<>(target.getSubCategories()));
            }
            analysis.setCurrentProductId(target.getCurrentProductId());
            if (target.getScopeProductIds() != null) {
                analysis.setScopeProductIds(new ArrayList<>(target.getScopeProductIds()));
            }
            if (target.getExcludeProductIds() != null) {
                analysis.setExcludeProductIds(new ArrayList<>(target.getExcludeProductIds()));
            }
        }

        if (plan.getPrice() != null) {
            analysis.setMinPrice(plan.getPrice().getMin());
            analysis.setMaxPrice(plan.getPrice().getMax());
        }

        if (plan.getBrands() != null) {
            if (plan.getBrands().getInclude() != null && !plan.getBrands().getInclude().isEmpty()) {
                analysis.setBrand(plan.getBrands().getInclude().get(0));
            }
            if (plan.getBrands().getExclude() != null) {
                for (String b : plan.getBrands().getExclude()) {
                    if (!analysis.getNegativeBrands().contains(b)) {
                        analysis.getNegativeBrands().add(b);
                    }
                    if (!analysis.getExcludeBrands().contains(b)) {
                        analysis.getExcludeBrands().add(b);
                    }
                }
            }
        }

        if (plan.getAttributes() != null) {
            if (plan.getAttributes().getInclude() != null) {
                for (String a : plan.getAttributes().getInclude()) {
                    if (!analysis.getPositiveKeywords().contains(a)) {
                        analysis.getPositiveKeywords().add(a);
                    }
                }
            }
            if (plan.getAttributes().getExclude() != null) {
                for (String a : plan.getAttributes().getExclude()) {
                    if (!analysis.getNegativeKeywords().contains(a)) {
                        analysis.getNegativeKeywords().add(a);
                    }
                }
            }
        }

        if (plan.getSoftKeywords() != null) {
            for (String kw : plan.getSoftKeywords()) {
                if (!analysis.getSoftKeywords().contains(kw)) {
                    analysis.getSoftKeywords().add(kw);
                }
                if (!analysis.getPositiveKeywords().contains(kw)) {
                    analysis.getPositiveKeywords().add(kw);
                }
            }
        }

        if (plan.getQueryVariants() != null && !plan.getQueryVariants().isEmpty()) {
            analysis.setQueryVariants(new ArrayList<>(plan.getQueryVariants()));
        }

        if (plan.getRequestedProductCount() != null) {
            analysis.setRequestedProductCount(plan.getRequestedProductCount());
        }

        if (plan.getAnswerMode() != null) {
            analysis.setResponseStyle(plan.getAnswerMode());
        }

        if (plan.getWarnings() != null) {
            for (String w : plan.getWarnings()) {
                if (!analysis.getWarnings().contains(w)) {
                    analysis.getWarnings().add(w);
                }
            }
        }

        if (validationResult != null && validationResult.getWarnings() != null) {
            for (String w : validationResult.getWarnings()) {
                if (!analysis.getWarnings().contains(w)) {
                    analysis.getWarnings().add(w);
                }
            }
        }

        analysis.getWarnings().add("selectedSource=PLANNER");

        buildFilters(analysis);

        return analysis;
    }

    private String buildNormalizedQuery(QueryPlan plan) {
        if (plan.getNormalizedQuery() != null && !plan.getNormalizedQuery().isBlank()) {
            return plan.getNormalizedQuery();
        }

        StringBuilder sb = new StringBuilder();

        if (plan.getTarget() != null && plan.getTarget().getSubCategory() != null
                && !plan.getTarget().getSubCategory().isBlank()) {
            sb.append(plan.getTarget().getSubCategory());
        }

        if (plan.getSoftKeywords() != null && !plan.getSoftKeywords().isEmpty()) {
            int limit = Math.min(plan.getSoftKeywords().size(), 5);
            for (int i = 0; i < limit; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(plan.getSoftKeywords().get(i));
            }
        }

        if (sb.length() == 0 && plan.getOriginalQuery() != null) {
            return plan.getOriginalQuery();
        }

        return sb.toString();
    }

    private void buildFilters(QueryAnalysisResult analysis) {
        var filters = new LinkedHashMap<String, Object>();

        if (analysis.getCategory() != null && !analysis.getCategory().isBlank()) {
            filters.put("category", analysis.getCategory());
        }
        if (analysis.getSubCategory() != null && !analysis.getSubCategory().isBlank()) {
            filters.put("sub_category", analysis.getSubCategory());
        }
        if (analysis.getMinPrice() != null) {
            filters.put("min_price", analysis.getMinPrice());
        }
        if (analysis.getMaxPrice() != null) {
            filters.put("max_price", analysis.getMaxPrice());
        }

        analysis.setFilters(filters);
    }
}
