package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.vector.VectorSearchHit;
import com.ecommerce.rag.services.ProductService;

@Component
public class HybridCandidateRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridCandidateRetriever.class);

    private final VectorRetriever vectorRetriever;
    private final KeywordRetriever keywordRetriever;
    private final CandidateFusionService fusionService;
    private final ProductService productService;
    private final AppProperties appProperties;
    private final QueryAnalyzer queryAnalyzer;
    private final StrictProductConstraintFilter constraintFilter;
    private final com.ecommerce.rag.rag.rewrite.QueryRewriteService rewriteService;

    private QueryAnalysisResult lastAnalysis;

    public HybridCandidateRetriever(VectorRetriever vectorRetriever,
                                     KeywordRetriever keywordRetriever,
                                     CandidateFusionService fusionService,
                                     ProductService productService,
                                     AppProperties appProperties,
                                     QueryAnalyzer queryAnalyzer,
                                     StrictProductConstraintFilter constraintFilter,
                                     com.ecommerce.rag.rag.rewrite.QueryRewriteService rewriteService) {
        this.vectorRetriever = vectorRetriever;
        this.keywordRetriever = keywordRetriever;
        this.fusionService = fusionService;
        this.productService = productService;
        this.appProperties = appProperties;
        this.queryAnalyzer = queryAnalyzer;
        this.constraintFilter = constraintFilter;
        this.rewriteService = rewriteService;
    }

    public QueryAnalysisResult getLastAnalysis() {
        return lastAnalysis;
    }

    public List<ChatCandidate> retrieve(String message, int limit) {
        if (message == null || message.isBlank()) return Collections.emptyList();
        List<RetrievedProductCandidate> candidates = retrieveRaw(message, limit);
        return toChatCandidates(candidates, limit);
    }

    public List<ChatCandidate> retrieveWithAnalysis(String message, int limit, QueryAnalysisResult analysis) {
        if (message == null || message.isBlank()) return Collections.emptyList();
        if (analysis == null) return retrieve(message, limit);

        this.lastAnalysis = analysis;
        List<RetrievedProductCandidate> candidates = retrieveRawWithAnalysis(message, limit, analysis);
        return toChatCandidates(candidates, limit);
    }

    public List<RetrievedProductCandidate> retrieveRawWithAnalysis(String message, int limit,
                                                                    QueryAnalysisResult analysis) {
        if (message == null || message.isBlank()) return Collections.emptyList();

        this.lastAnalysis = analysis;

        analysis = applyQueryRewrite(message, analysis);

        String mode = appProperties.getRetrieval().getMode();
        log.info("Retrieving raw candidates with analysis: mode={}, query={}, limit={}, analysis=[cat={}, sub={}, maxPrice={}]",
                mode, message, limit, analysis.getCategory(), analysis.getSubCategory(), analysis.getMaxPrice());

        List<RetrievedProductCandidate> candidates = switch (mode.toLowerCase()) {
            case "vector" -> vectorOnly(analysis, limit);
            case "keyword" -> keywordOnly(analysis, limit);
            default -> hybrid(analysis, limit);
        };

        for (RetrievedProductCandidate c : candidates) {
            if (c.getProduct() == null) {
                Optional<Product> product = productService.findById(c.getProductId());
                product.ifPresent(c::setProduct);
            }
        }

        int rawCount = candidates.size();

        List<RetrievedProductCandidate> filtered = constraintFilter.filterCandidates(candidates, analysis);

        List<RetrievedProductCandidate> afterBoost = applyBoostedProductIds(filtered, analysis);

        if (afterBoost.isEmpty() && hasPageFilters(analysis)) {
            analysis.getWarnings().add("page filter too strict, fallback to broader retrieval");
            List<RetrievedProductCandidate> fallbackFiltered = constraintFilter.filterCandidates(candidates, null);
            afterBoost = applyBoostedProductIds(fallbackFiltered, analysis);
        }

        logFilteredOut(candidates, afterBoost, analysis);

        log.info("Hybrid retrieval with analysis: {} raw -> {} after filters for query: {}",
                rawCount, afterBoost.size(), message);
        return afterBoost;
    }

    public List<RetrievedProductCandidate> retrieveRaw(String message, int limit) {
        if (message == null || message.isBlank()) return Collections.emptyList();

        QueryAnalysisResult analysis = queryAnalyzer.analyze(message);
        this.lastAnalysis = analysis;

        String mode = appProperties.getRetrieval().getMode();
        log.info("Retrieving raw candidates: mode={}, query={}, limit={}, analysis=[cat={}, sub={}, maxPrice={}]",
                mode, message, limit, analysis.getCategory(), analysis.getSubCategory(), analysis.getMaxPrice());

        List<RetrievedProductCandidate> candidates = switch (mode.toLowerCase()) {
            case "vector" -> vectorOnly(analysis, limit);
            case "keyword" -> keywordOnly(analysis, limit);
            default -> hybrid(analysis, limit);
        };

        for (RetrievedProductCandidate c : candidates) {
            if (c.getProduct() == null) {
                Optional<Product> product = productService.findById(c.getProductId());
                product.ifPresent(c::setProduct);
            }
        }

        List<RetrievedProductCandidate> filtered = constraintFilter.filterCandidates(candidates, analysis);

        log.info("Hybrid retrieval: {} raw -> {} after hard filter for query: {}",
                candidates.size(), filtered.size(), message);
        return filtered;
    }

    private List<RetrievedProductCandidate> vectorOnly(QueryAnalysisResult analysis, int limit) {
        long vectorCount = vectorRetriever.getVectorStoreCount();
        if (vectorCount == 0) {
            if (appProperties.getRetrieval().isAutoFallbackToKeyword()) {
                log.info("Vector index empty, auto-fallback to keyword");
                return keywordOnly(analysis, limit);
            }
            log.warn("Vector index empty and auto-fallback disabled");
            return Collections.emptyList();
        }

        List<VectorSearchHit> hits;
        if (analysis.getQueryVariants() != null && !analysis.getQueryVariants().isEmpty()) {
            hits = vectorRetriever.retrieveMultiQuery(
                    analysis.getOriginalQuery(), analysis.getQueryVariants(), limit * 3, analysis.getFilters());
        } else {
            hits = vectorRetriever.retrieveWithFilters(
                    analysis.getOriginalQuery(), limit * 3, analysis.getFilters());
        }
        return aggregateByProductId(hits);
    }

    private List<RetrievedProductCandidate> keywordOnly(QueryAnalysisResult analysis, int limit) {
        String query = analysis.getNormalizedQuery() != null && !analysis.getNormalizedQuery().isBlank()
                ? analysis.getNormalizedQuery() : analysis.getOriginalQuery();
        List<Product> products = keywordRetriever.retrieveWithSoftKeywords(
                query, limit, analysis, analysis.getSoftKeywords());
        List<RetrievedProductCandidate> candidates = new ArrayList<>();
        int rank = 0;
        for (Product product : products) {
            rank++;
            RetrievedProductCandidate c = new RetrievedProductCandidate();
            c.setProductId(product.getProductId());
            c.setProduct(product);
            c.setKeywordScore(Math.max(0.1, 1.0 - (rank - 1) * 0.1));
            c.setVectorScore(0.0);
            c.setFinalScore(c.getKeywordScore());
            c.getMatchedSources().add("keyword");
            candidates.add(c);
        }
        return candidates;
    }

    private List<RetrievedProductCandidate> hybrid(QueryAnalysisResult analysis, int limit) {
        List<VectorSearchHit> vectorHits = Collections.emptyList();
        List<Product> keywordProducts = Collections.emptyList();

        if (appProperties.getRetrieval().isVectorEnabled()) {
            if (vectorRetriever.getVectorStoreCount() > 0) {
                if (analysis.getQueryVariants() != null && !analysis.getQueryVariants().isEmpty()) {
                    vectorHits = vectorRetriever.retrieveMultiQuery(
                            analysis.getOriginalQuery(), analysis.getQueryVariants(), limit * 3, analysis.getFilters());
                } else {
                    vectorHits = vectorRetriever.retrieveWithFilters(
                            analysis.getOriginalQuery(), limit * 3, analysis.getFilters());
                }
            } else if (!appProperties.getRetrieval().isAutoFallbackToKeyword()) {
                log.warn("Vector index empty and auto-fallback disabled");
            }
        }

        if (appProperties.getRetrieval().isKeywordEnabled() || vectorHits.isEmpty()) {
            String kwQuery = analysis.getNormalizedQuery() != null && !analysis.getNormalizedQuery().isBlank()
                    ? analysis.getNormalizedQuery() : analysis.getOriginalQuery();
            keywordProducts = keywordRetriever.retrieveWithSoftKeywords(
                    kwQuery, limit * 3, analysis, analysis.getSoftKeywords());
        }

        List<RetrievedProductCandidate> merged = fusionService.merge(vectorHits, keywordProducts);

        for (RetrievedProductCandidate c : merged) {
            if (c.getProduct() == null) {
                Optional<Product> product = productService.findById(c.getProductId());
                product.ifPresent(c::setProduct);
            }
        }
        return merged;
    }

    private void logFilteredOut(List<RetrievedProductCandidate> before,
                                 List<RetrievedProductCandidate> after,
                                 QueryAnalysisResult analysis) {
        if (analysis == null || !analysis.hasHardConstraints()) return;

        java.util.Set<String> afterIds = new java.util.HashSet<>();
        for (RetrievedProductCandidate c : after) {
            afterIds.add(c.getProductId());
        }

        for (RetrievedProductCandidate c : before) {
            if (!afterIds.contains(c.getProductId())) {
                Product p = c.getProduct();
                if (p != null) {
                    log.info("Filtered out: productId={}, name={}",
                            p.getProductId(), p.getName());
                }
            }
        }

        log.info("Filter summary: raw={}, afterConstraint={}, final={}",
                before.size(), after.size(), after.size());
    }

    private List<RetrievedProductCandidate> applyBoostedProductIds(
            List<RetrievedProductCandidate> candidates, QueryAnalysisResult analysis) {

        if (analysis == null || analysis.getBoostedProductIds() == null
                || analysis.getBoostedProductIds().isEmpty()) {
            return candidates;
        }

        for (RetrievedProductCandidate c : candidates) {
            if (analysis.getBoostedProductIds().contains(c.getProductId())) {
                c.setFinalScore(c.getFinalScore() + 0.05);
            }
        }

        log.debug("Applied boost to {} candidates", analysis.getBoostedProductIds().size());
        return candidates;
    }

    private boolean hasPageFilters(QueryAnalysisResult analysis) {
        if (analysis == null) return false;
        return analysis.getInheritedFromPageContext() != null
                && analysis.getInheritedFromPageContext();
    }

    private List<RetrievedProductCandidate> aggregateByProductId(List<VectorSearchHit> hits) {
        if (hits == null || hits.isEmpty()) return Collections.emptyList();
        return fusionService.merge(hits, Collections.emptyList());
    }

    private List<ChatCandidate> toChatCandidates(List<RetrievedProductCandidate> candidates, int limit) {
        List<ChatCandidate> result = new ArrayList<>();
        for (RetrievedProductCandidate c : candidates) {
            if (result.size() >= limit) break;
            if (c.getProduct() == null) continue;

            Product p = c.getProduct();
            ChatCandidate chat = new ChatCandidate();
            chat.setProductId(p.getProductId());
            chat.setName(p.getName());
            chat.setBrand(p.getBrand());
            chat.setCategory(p.getCategory());
            chat.setSubCategory(p.getSubCategory());
            chat.setPrice(p.getPrice());
            chat.setCurrency(p.getCurrency());
            chat.setImageUrl(p.getImageUrl());
            chat.setDescription(p.getDescription());
            chat.setScore(c.getFinalScore());
            result.add(chat);
        }

        log.info("Hybrid retrieval returned {} final candidates from {} merged",
                result.size(), candidates.size());
        return result;
    }

    private QueryAnalysisResult applyQueryRewrite(String query, QueryAnalysisResult analysis) {
        try {
            com.ecommerce.rag.rag.rewrite.QueryRewriteResult rewriteResult = rewriteService.rewrite(query, analysis);
            if (rewriteResult != null && rewriteResult.getSource() != null
                    && !com.ecommerce.rag.rag.rewrite.QueryRewriteResult.SOURCE_NONE.equals(rewriteResult.getSource())) {
                analysis.setRewriteResult(rewriteResult);
                if (rewriteResult.getQueryVariants() != null && !rewriteResult.getQueryVariants().isEmpty()) {
                    analysis.setQueryVariants(rewriteResult.getQueryVariants());
                }
                if (rewriteResult.getSoftKeywords() != null && !rewriteResult.getSoftKeywords().isEmpty()) {
                    analysis.setSoftKeywords(rewriteResult.getSoftKeywords());
                }
                log.info("Query rewrite applied: source={}, variants={}, keywords={}",
                        rewriteResult.getSource(),
                        rewriteResult.getQueryVariants().size(),
                        rewriteResult.getSoftKeywords().size());
            }
        } catch (Exception e) {
            log.warn("Query rewrite failed, proceeding with original query: {}", e.getMessage());
        }
        return analysis;
    }
}
