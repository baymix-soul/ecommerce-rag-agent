package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.ProductService;

@Component
public class KeywordRetriever {

    private static final Logger log = LoggerFactory.getLogger(KeywordRetriever.class);

    private final ProductService productService;
    private final StrictProductConstraintFilter constraintFilter;

    public KeywordRetriever(ProductService productService,
                             StrictProductConstraintFilter constraintFilter) {
        this.productService = productService;
        this.constraintFilter = constraintFilter;
    }

    public List<Product> retrieve(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery(query);
        request.setLimit(limit);

        ProductSearchResponse response = productService.search(request);

        List<Product> products = new ArrayList<>();
        for (var card : response.getProducts()) {
            productService.findById(card.getProductId()).ifPresent(products::add);
        }

        log.debug("Keyword retrieval returned {} products for query: {}", products.size(), query);
        return products;
    }

    public List<Product> retrieveWithFilter(String query, int limit, QueryAnalysisResult analysis) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String normalizedQuery = analysis != null && analysis.getNormalizedQuery() != null
                ? analysis.getNormalizedQuery() : query;

        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery(normalizedQuery);
        request.setLimit(limit * 2);

        if (analysis != null) {
            if (analysis.getCategory() != null) {
                request.setCategory(analysis.getCategory());
            }
            if (analysis.getMinPrice() != null) {
                request.setMinPrice(analysis.getMinPrice());
            }
            if (analysis.getMaxPrice() != null) {
                request.setMaxPrice(analysis.getMaxPrice());
            }
        }

        ProductSearchResponse response = productService.search(request);
        List<Product> allProducts = new ArrayList<>();
        for (var card : response.getProducts()) {
            productService.findById(card.getProductId()).ifPresent(allProducts::add);
        }

        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (analysis == null || constraintFilter.passes(p, analysis)) {
                filtered.add(p);
            }
        }

        List<Product> result = filtered.size() > limit ? filtered.subList(0, limit) : filtered;

        log.debug("Keyword retrieval (filtered): {} -> {} products for query: {}",
                allProducts.size(), result.size(), query);
        return result;
    }

    public List<Product> retrieveWithSoftKeywords(String query, int limit, QueryAnalysisResult analysis,
                                                   java.util.List<String> softKeywords) {
        String enhancedQuery = query;
        if (softKeywords != null && !softKeywords.isEmpty()) {
            StringBuilder sb = new StringBuilder(query != null ? query : "");
            for (String kw : softKeywords) {
                if (kw != null && !kw.isBlank() && !sb.toString().contains(kw)) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(kw);
                }
            }
            enhancedQuery = sb.toString();
        }

        return retrieveWithFilter(enhancedQuery, limit, analysis);
    }
}
