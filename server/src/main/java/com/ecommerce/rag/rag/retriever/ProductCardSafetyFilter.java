package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.ProductService;

@Component
public class ProductCardSafetyFilter {

    private static final Logger log = LoggerFactory.getLogger(ProductCardSafetyFilter.class);

    private final StrictProductConstraintFilter constraintFilter;
    private final ProductService productService;

    public ProductCardSafetyFilter(StrictProductConstraintFilter constraintFilter,
                                    ProductService productService) {
        this.constraintFilter = constraintFilter;
        this.productService = productService;
    }

    public SafetyFilterResult filter(List<ChatCandidate> displayCandidates,
                                      QueryAnalysisResult effectiveAnalysis,
                                      ActiveSearchContext activeContext) {
        if (displayCandidates == null || displayCandidates.isEmpty()) {
            return new SafetyFilterResult(List.of(), List.of());
        }

        QueryAnalysisResult filterAnalysis = buildFilterAnalysis(effectiveAnalysis, activeContext);

        List<ChatCandidate> safe = new ArrayList<>();
        List<String> removedDescriptions = new ArrayList<>();

        for (ChatCandidate candidate : displayCandidates) {
            if (candidate.getProductId() == null) {
                safe.add(candidate);
                continue;
            }

            var productOpt = productService.findById(candidate.getProductId());
            if (productOpt.isEmpty()) {
                removedDescriptions.add(candidate.getProductId() + ":PRODUCT_NOT_FOUND");
                log.warn("card_safety_filter: product not found: productId={}", candidate.getProductId());
                continue;
            }

            Product product = productOpt.get();
            ConstraintCheckResult checkResult = constraintFilter.check(product, filterAnalysis);

            if (checkResult.isPassed()) {
                safe.add(candidate);
            } else {
                for (String failedRule : checkResult.getFailedRules()) {
                    removedDescriptions.add(candidate.getProductId() + ":" + failedRule);
                }
                log.warn("card_safety_filter: removed candidate productId={}, name={}, failedRules={}",
                        candidate.getProductId(), candidate.getName(), checkResult.getFailedRules());
            }
        }

        if (displayCandidates.size() != safe.size()) {
            log.info("card_safety_filter: before={}, after={}, removed={}",
                    displayCandidates.size(), safe.size(), removedDescriptions);
        }

        return new SafetyFilterResult(safe, removedDescriptions);
    }

    private QueryAnalysisResult buildFilterAnalysis(QueryAnalysisResult effectiveAnalysis,
                                                     ActiveSearchContext activeContext) {
        if (activeContext != null) {
            QueryAnalysisResult ctxAnalysis = activeContext.toQueryAnalysisResult();
            if (effectiveAnalysis != null) {
                if (ctxAnalysis.getNegativeBrands().isEmpty()
                        && effectiveAnalysis.getNegativeBrands() != null) {
                    ctxAnalysis.setNegativeBrands(
                            new ArrayList<>(effectiveAnalysis.getNegativeBrands()));
                }
                if (ctxAnalysis.getNegativeKeywords().isEmpty()
                        && effectiveAnalysis.getNegativeKeywords() != null) {
                    ctxAnalysis.setNegativeKeywords(
                            new ArrayList<>(effectiveAnalysis.getNegativeKeywords()));
                }
                if (ctxAnalysis.getExcludeProductIds().isEmpty()
                        && effectiveAnalysis.getExcludeProductIds() != null) {
                    ctxAnalysis.setExcludeProductIds(
                            new ArrayList<>(effectiveAnalysis.getExcludeProductIds()));
                }
                if (ctxAnalysis.getMaxPrice() == null && effectiveAnalysis.getMaxPrice() != null) {
                    ctxAnalysis.setMaxPrice(effectiveAnalysis.getMaxPrice());
                }
                if (ctxAnalysis.getMinPrice() == null && effectiveAnalysis.getMinPrice() != null) {
                    ctxAnalysis.setMinPrice(effectiveAnalysis.getMinPrice());
                }
            }
            return ctxAnalysis;
        }

        return effectiveAnalysis != null ? effectiveAnalysis : new QueryAnalysisResult();
    }

    public static class SafetyFilterResult {
        private final List<ChatCandidate> safeCandidates;
        private final List<String> removedDescriptions;

        public SafetyFilterResult(List<ChatCandidate> safeCandidates,
                                   List<String> removedDescriptions) {
            this.safeCandidates = safeCandidates;
            this.removedDescriptions = removedDescriptions;
        }

        public List<ChatCandidate> getSafeCandidates() {
            return safeCandidates;
        }

        public List<String> getRemovedDescriptions() {
            return removedDescriptions;
        }

        public boolean anyRemoved() {
            return !removedDescriptions.isEmpty();
        }
    }
}
