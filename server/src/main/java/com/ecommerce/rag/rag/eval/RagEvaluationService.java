package com.ecommerce.rag.rag.eval;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.embedding.EmbeddingProvider;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.RetrievedProductCandidate;
import com.ecommerce.rag.rag.vector.VectorSearchHit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RagEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationService.class);

    private static final String EVAL_QUERIES_PATH = "eval/rag_eval_queries.json";

    private final HybridCandidateRetriever hybridRetriever;
    private final EmbeddingProvider embeddingProvider;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final CategoryMatchService categoryMatchService;

    private List<RagEvalQuery> cachedQueries;

    public RagEvaluationService(HybridCandidateRetriever hybridRetriever,
                                 EmbeddingProvider embeddingProvider,
                                 AppProperties appProperties,
                                 ObjectMapper objectMapper,
                                 CategoryMatchService categoryMatchService) {
        this.hybridRetriever = hybridRetriever;
        this.embeddingProvider = embeddingProvider;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.categoryMatchService = categoryMatchService;
    }

    public List<RagEvalQuery> loadQueries() {
        if (cachedQueries != null) return cachedQueries;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(EVAL_QUERIES_PATH)) {
            if (is == null) throw new IllegalStateException("Eval queries file not found: " + EVAL_QUERIES_PATH);
            cachedQueries = objectMapper.readValue(is, new TypeReference<List<RagEvalQuery>>() {});
            log.info("Loaded {} eval queries from {}", cachedQueries.size(), EVAL_QUERIES_PATH);
            return cachedQueries;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load eval queries: " + e.getMessage(), e);
        }
    }

    public RagEvalResult evaluateOne(RagEvalQuery evalQuery, int topK) {
        String query = evalQuery.getQuery();
        log.info("Evaluating query: query={}, topK={}", query, topK);

        RagEvalResult result = new RagEvalResult();
        result.setQuery(query);
        result.setExpectedCategory(evalQuery.getExpectedCategory());
        result.setExpectedSubCategory(evalQuery.getExpectedSubCategory());
        result.setNotes(evalQuery.getNotes());
        result.setMode(appProperties.getRetrieval().getMode());
        result.setTopK(topK);

        if (!evalQuery.isSupported()) {
            result.setPass(false);
            result.getReasons().add("unsupported_query");
            result.getReasons().add("reason: " + evalQuery.getUnsupportedReason());
            result.setTotalHits(0);
            result.setHits(List.of());
            return result;
        }

        List<RetrievedProductCandidate> candidates = hybridRetriever.retrieveRaw(query, topK);
        result.setTotalHits(candidates.size());

        List<RagEvalHit> hits = new ArrayList<>();
        int rank = 0;
        for (RetrievedProductCandidate c : candidates) {
            rank++;
            RagEvalHit hit = toEvalHit(c, rank);
            judgeHitRelevance(hit, evalQuery);
            hits.add(hit);
        }
        result.setHits(hits);

        int relevantCount = 0;
        List<String> reasons = new ArrayList<>();
        for (RagEvalHit hit : hits) {
            if (hit.isJudgedRelevant()) {
                relevantCount++;
                reasons.add("relevant_hit_found: " + hit.getProductId() + " rank=" + hit.getRank());
            }
        }

        boolean pass = relevantCount >= evalQuery.getMinRelevantCount();
        if (pass) {
            reasons.add(0, "pass");
        } else {
            reasons.add(0, "fail");
            if (relevantCount == 0) reasons.add("no_relevant_hit_in_top_k");
            else reasons.add("insufficient_relevant_hits: got " + relevantCount
                    + ", need " + evalQuery.getMinRelevantCount());

            boolean anyCategoryMatch = hits.stream().anyMatch(h -> h.getMatchedRules().contains("category"));
            if (!anyCategoryMatch) reasons.add("category_mismatch");

            boolean anySubMatch = hits.stream().anyMatch(h ->
                    h.getMatchedRules().contains("sub_category"));
            if (!anySubMatch && hasExpectedSubConstraint(evalQuery)) reasons.add("subcategory_mismatch");

            if (evalQuery.getMaxPrice() != null) {
                boolean anyPriceMatch = hits.stream().anyMatch(h ->
                        h.getMatchedRules().contains("max_price"));
                if (!anyPriceMatch) reasons.add("price_constraint_fail");
            }
        }
        result.setPass(pass);
        result.setReasons(reasons);
        return result;
    }

    public RagEvalSummary evaluateAll(int topK) {
        List<RagEvalQuery> queries = loadQueries();
        RagEvalSummary summary = new RagEvalSummary();
        summary.setEmbeddingModel(embeddingProvider.modelName());
        summary.setVectorStore(appProperties.getVector().getStore());
        summary.setRetrievalMode(appProperties.getRetrieval().getMode());
        summary.setTotalQueries(queries.size());

        List<RagEvalResult> results = new ArrayList<>();
        int evaluatedCount = 0, passCount = 0, failCount = 0, unsupportedCount = 0;
        int totalHits = 0;
        List<String> unsupportedList = new ArrayList<>();
        List<String> failedList = new ArrayList<>();
        Set<String> failedReasonSet = new LinkedHashSet<>();

        for (RagEvalQuery evalQuery : queries) {
            RagEvalResult result = evaluateOne(evalQuery, topK);
            results.add(result);

            if (!evalQuery.isSupported()) {
                unsupportedCount++;
                unsupportedList.add(evalQuery.getQuery());
                continue;
            }

            evaluatedCount++;
            totalHits += result.getTotalHits();
            if (result.isPass()) {
                passCount++;
            } else {
                failCount++;
                failedList.add(evalQuery.getQuery());
                for (String reason : result.getReasons()) {
                    if (!reason.equals("pass") && !reason.equals("fail")
                            && !reason.startsWith("relevant_hit_found")) {
                        failedReasonSet.add(reason);
                    }
                }
            }
        }

        summary.setResults(results);
        summary.setEvaluatedQueries(evaluatedCount);
        summary.setUnsupportedQueries(unsupportedCount);
        summary.setUnsupportedQueryList(unsupportedList);
        summary.setPassedQueries(passCount);
        summary.setFailedQueries(failCount);
        summary.setPassRate(evaluatedCount > 0 ? (double) passCount / evaluatedCount : 0.0);
        summary.setAvgHitCount(evaluatedCount > 0 ? (double) totalHits / evaluatedCount : 0.0);
        summary.setFailedQueryList(failedList);
        summary.setFailedReasons(new ArrayList<>(failedReasonSet));

        log.info("Eval complete: {}/{} passed ({:.0f}%), {} unsupported",
                passCount, evaluatedCount, summary.getPassRate() * 100, unsupportedCount);
        return summary;
    }

    private RagEvalHit toEvalHit(RetrievedProductCandidate c, int rank) {
        RagEvalHit hit = new RagEvalHit();
        hit.setRank(rank);
        Product p = c.getProduct();
        if (p != null) {
            hit.setProductId(p.getProductId());
            hit.setName(p.getName());
            hit.setCategory(p.getCategory());
            hit.setSubCategory(p.getSubCategory());
            hit.setBrand(p.getBrand());
            hit.setPrice(p.getPrice());
        } else {
            hit.setProductId(c.getProductId());
        }
        hit.setScore(c.getFinalScore());
        hit.setVectorScore(c.getVectorScore());
        hit.setKeywordScore(c.getKeywordScore());
        hit.setFinalScore(c.getFinalScore());
        hit.setMatchedSources(c.getMatchedSources());

        Set<String> chunkTypes = new LinkedHashSet<>();
        String textSnippet = null;
        for (VectorSearchHit vh : c.getMatchedChunks()) {
            if (vh.getChunkType() != null) chunkTypes.add(vh.getChunkType());
            if (textSnippet == null && vh.getText() != null) {
                textSnippet = vh.getText().length() > 200
                        ? vh.getText().substring(0, 200) + "..." : vh.getText();
            }
        }
        hit.setMatchedChunkTypes(new ArrayList<>(chunkTypes));
        hit.setTextSnippet(textSnippet);
        return hit;
    }

    private void judgeHitRelevance(RagEvalHit hit, RagEvalQuery evalQuery) {
        Product p = new Product();
        p.setProductId(hit.getProductId());
        p.setCategory(hit.getCategory());
        p.setSubCategory(hit.getSubCategory());
        p.setBrand(hit.getBrand());
        p.setPrice(hit.getPrice());

        List<String> matchedRules = new ArrayList<>();
        List<String> failedRules = new ArrayList<>();

        String expectedCategory = evalQuery.getExpectedCategory();
        if (expectedCategory != null && !expectedCategory.isBlank()) {
            if (categoryMatchService.categoryMatches(expectedCategory, p.getCategory())) {
                matchedRules.add("category");
            } else {
                failedRules.add("category: expected " + expectedCategory + ", got " + p.getCategory());
            }
        }

        List<String> expectedSubs = evalQuery.getExpectedSubCategories();
        String expectedSingleSub = evalQuery.getExpectedSubCategory();
        boolean hasSubConstraint = (expectedSubs != null && !expectedSubs.isEmpty())
                || (expectedSingleSub != null && !expectedSingleSub.isBlank());

        if (hasSubConstraint) {
            boolean subOk = false;
            if (expectedSubs != null && !expectedSubs.isEmpty()) {
                subOk = categoryMatchService.subCategoryMatchesAny(expectedSubs, p.getSubCategory());
            } else if (expectedSingleSub != null && !expectedSingleSub.isBlank()) {
                subOk = categoryMatchService.subCategoryMatches(expectedSingleSub, p.getSubCategory());
            }
            if (subOk) {
                matchedRules.add("sub_category");
            } else {
                failedRules.add("sub_category: expected " + (expectedSubs != null ? expectedSubs : expectedSingleSub)
                        + ", got " + p.getSubCategory());
            }
        }

        BigDecimal maxPrice = evalQuery.getMaxPrice();
        if (maxPrice != null && p.getPrice() != null) {
            if (p.getPrice().compareTo(maxPrice) <= 0) {
                matchedRules.add("max_price");
            } else {
                failedRules.add("max_price: expected <=" + maxPrice + ", got " + p.getPrice());
            }
        }

        hit.setMatchedRules(matchedRules);
        hit.setFailedRules(failedRules);
        hit.setJudgedRelevant(failedRules.isEmpty());
    }

    private boolean hasExpectedSubConstraint(RagEvalQuery evalQuery) {
        return (evalQuery.getExpectedSubCategory() != null && !evalQuery.getExpectedSubCategory().isBlank())
                || (evalQuery.getExpectedSubCategories() != null && !evalQuery.getExpectedSubCategories().isEmpty());
    }
}
