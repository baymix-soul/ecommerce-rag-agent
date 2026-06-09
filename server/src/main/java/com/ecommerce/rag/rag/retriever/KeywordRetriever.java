package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.ProductService;

@Component
public class KeywordRetriever {

    private static final Logger log = LoggerFactory.getLogger(KeywordRetriever.class);

    private final ProductService productService;

    public KeywordRetriever(ProductService productService) {
        this.productService = productService;
    }

    public List<Product> retrieveWithSoftKeywords(String query, int limit,
                                                   QueryAnalysisResult analysis,
                                                   List<String> softKeywords) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        PerfTraceContext.startSpan("keyword.total");

        Set<String> queryTokens = tokenize(query);
        List<String> posKeywords = analysis != null && analysis.getPositiveKeywords() != null
                ? analysis.getPositiveKeywords()
                : Collections.emptyList();
        List<String> negKeywords = analysis != null && analysis.getNegativeKeywords() != null
                ? analysis.getNegativeKeywords()
                : Collections.emptyList();
        List<String> negBrands = analysis != null && analysis.getNegativeBrands() != null
                ? analysis.getNegativeBrands()
                : Collections.emptyList();
        String category = analysis != null ? analysis.getCategory() : null;
        String subCategory = analysis != null ? analysis.getSubCategory() : null;

        PerfTraceContext.startSpan("keyword.search");
        List<Product> all = productService.listAll();
        PerfTraceContext.endSpan("keyword.search");

        PerfTraceContext.startSpan("keyword.score");
        Map<Product, Double> scored = new LinkedHashMap<>();

        for (Product p : all) {
            double score = scoreProduct(p, queryTokens, posKeywords, negKeywords, negBrands,
                    category, subCategory, softKeywords);
            if (score > 0) {
                scored.put(p, score);
            }
        }

        List<Map.Entry<Product, Double>> sorted = scored.entrySet().stream()
                .sorted(Map.Entry.<Product, Double>comparingByValue().reversed())
                .toList();

        List<Product> result = new ArrayList<>();
        for (Map.Entry<Product, Double> entry : sorted) {
            result.add(entry.getKey());
            if (result.size() >= limit) break;
        }
        PerfTraceContext.endSpan("keyword.score");

        PerfTraceContext.addAttribute("query_token_count", queryTokens.size());
        PerfTraceContext.addAttribute("keyword_candidate_count", all.size());
        PerfTraceContext.addAttribute("keyword_final_count", result.size());

        PerfTraceContext.endSpan("keyword.total");

        log.debug("Keyword retrieval: {} candidates for query: {}", result.size(), query);
        return result;
    }

    private Set<String> tokenize(String query) {
        Set<String> tokens = new java.util.HashSet<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }
        String[] parts = query.toLowerCase().split("\\s+");
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private double scoreProduct(Product p, Set<String> queryTokens, List<String> posKeywords,
                                 List<String> negKeywords, List<String> negBrands,
                                 String category, String subCategory, List<String> softKeywords) {
        double score = 0.0;
        String name = p.getName() != null ? p.getName().toLowerCase() : "";
        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
        String brand = p.getBrand() != null ? p.getBrand().toLowerCase() : "";
        String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
        String subCat = p.getSubCategory() != null ? p.getSubCategory().toLowerCase() : "";

        for (String token : queryTokens) {
            if (name.contains(token)) score += 3.0;
            if (desc.contains(token)) score += 1.0;
            if (brand.contains(token)) score += 2.0;
        }

        for (String kw : posKeywords) {
            String k = kw.toLowerCase();
            if (name.contains(k)) score += 2.0;
            if (desc.contains(k)) score += 0.5;
        }

        if (softKeywords != null) {
            for (String kw : softKeywords) {
                String k = kw.toLowerCase();
                if (name.contains(k)) score += 1.0;
                if (desc.contains(k)) score += 0.3;
            }
        }

        for (String kw : negKeywords) {
            String k = kw.toLowerCase();
            if (name.contains(k)) score -= 2.0;
            if (desc.contains(k)) score -= 0.5;
        }

        for (String b : negBrands) {
            String nb = b.toLowerCase();
            if (brand.contains(nb)) score -= 5.0;
        }

        if (category != null && !category.isBlank()) {
            if (cat.contains(category.toLowerCase())) score += 2.0;
        }
        if (subCategory != null && !subCategory.isBlank()) {
            if (subCat.contains(subCategory.toLowerCase())) score += 1.5;
        }

        return Math.max(0.0, score);
    }
}
