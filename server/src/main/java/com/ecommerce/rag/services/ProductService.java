package com.ecommerce.rag.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.utils.JsonLoader;

import jakarta.annotation.PostConstruct;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final JsonLoader jsonLoader;
    private final AppProperties appProperties;

    private List<Product> products;
    private Map<String, Product> productMap;

    public ProductService(JsonLoader jsonLoader, AppProperties appProperties) {
        this.jsonLoader = jsonLoader;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        String dataPath = appProperties.getProduct().getDataPath();
        this.products = jsonLoader.loadProducts(dataPath);
        validateProducts();
        this.productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a, LinkedHashMap::new));
        log.info("ProductService initialized with {} products", products.size());
    }

    private void validateProducts() {
        Set<String> seenIds = new HashSet<>();
        for (Product p : products) {
            if (p.getProductId() == null || p.getProductId().isBlank()) {
                throw new IllegalStateException("Product with null/empty productId found");
            }
            if (!seenIds.add(p.getProductId())) {
                throw new IllegalStateException("Duplicate productId found: " + p.getProductId());
            }
            if (p.getName() == null || p.getName().isBlank()) {
                log.warn("Product {} has empty name", p.getProductId());
            }
            if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Product {} has invalid price: {}", p.getProductId(), p.getPrice());
            }
        }
    }

    public List<Product> listAll() {
        return new ArrayList<>(products);
    }

    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(productMap.get(productId));
    }

    public ProductSearchResponse search(ProductSearchRequest request) {
        List<Product> candidates = new ArrayList<>(products);

        candidates = filterByCategory(candidates, request.getCategory());
        candidates = filterBySubCategory(candidates, request.getSubCategory());
        candidates = filterByBrand(candidates, request.getBrand());
        candidates = filterByPriceRange(candidates, request.getMinPrice(), request.getMaxPrice());

        String query = request.getQuery();
        if (query != null && !query.isBlank()) {
            List<ScoredProduct> scored = candidates.stream()
                    .map(p -> new ScoredProduct(p, computeScore(p, query)))
                    .filter(sp -> sp.score > 0)
                    .sorted(Comparator.comparingInt((ScoredProduct sp) -> sp.score).reversed())
                    .collect(Collectors.toList());

            int limit = request.getEffectiveLimit();
            List<ProductCard> cards = scored.stream()
                    .limit(limit)
                    .map(sp -> toProductCard(sp.product))
                    .collect(Collectors.toList());

            return new ProductSearchResponse(query, cards.size(), cards);
        }

        int limit = request.getEffectiveLimit();
        List<ProductCard> cards = candidates.stream()
                .limit(limit)
                .map(this::toProductCard)
                .collect(Collectors.toList());

        return new ProductSearchResponse(query, cards.size(), cards);
    }

    public List<ProductCard> toProductCards(List<Product> products) {
        return products.stream()
                .map(this::toProductCard)
                .collect(Collectors.toList());
    }

    public ProductCard toProductCard(Product product) {
        return new ProductCard(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getCurrency(),
                product.getImageUrl(),
                ""
        );
    }

    private int computeScore(Product product, String query) {
        String lowerQuery = query.toLowerCase();
        List<String> searchTokens = extractSearchTokens(lowerQuery);
        int score = 0;

        for (String token : searchTokens) {
            if (token.isEmpty() || token.length() < 2) continue;
            if (product.getName() != null && product.getName().toLowerCase().contains(token)) {
                score += 5;
            }
            if (product.getSubCategory() != null && product.getSubCategory().toLowerCase().contains(token)) {
                score += 4;
            }
            if (product.getCategory() != null && product.getCategory().toLowerCase().contains(token)) {
                score += 3;
            }
            if (product.getBrand() != null && product.getBrand().toLowerCase().contains(token)) {
                score += 3;
            }
            if (matchSpecs(product.getSpecs(), token)) {
                score += 2;
            }
            if (product.getDescription() != null && product.getDescription().toLowerCase().contains(token)) {
                score += 1;
            }
        }

        return score;
    }

    private List<String> extractSearchTokens(String query) {
        String[] spaceTokens = query.split("\\s+");
        Set<String> tokenSet = new HashSet<>();
        for (String t : spaceTokens) {
            if (!t.isEmpty()) tokenSet.add(t);
        }
        for (String t : spaceTokens) {
            if (t.length() <= 4) continue;
            boolean hasCjk = t.chars().anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
            if (hasCjk) {
                for (int len = 2; len <= 4 && len <= t.length(); len++) {
                    for (int i = 0; i + len <= t.length(); i++) {
                        tokenSet.add(t.substring(i, i + len));
                    }
                }
            }
        }
        return new ArrayList<>(tokenSet);
    }

    private boolean matchSpecs(Map<String, String> specs, String token) {
        if (specs == null || specs.isEmpty()) return false;
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            if (entry.getKey().toLowerCase().contains(token)
                    || entry.getValue().toLowerCase().contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<Product> filterByCategory(List<Product> candidates, String category) {
        if (category == null || category.isBlank()) return candidates;
        return candidates.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().contains(category))
                .collect(Collectors.toList());
    }

    private List<Product> filterBySubCategory(List<Product> candidates, String subCategory) {
        if (subCategory == null || subCategory.isBlank()) return candidates;
        return candidates.stream()
                .filter(p -> p.getSubCategory() != null && p.getSubCategory().contains(subCategory))
                .collect(Collectors.toList());
    }

    private List<Product> filterByBrand(List<Product> candidates, String brand) {
        if (brand == null || brand.isBlank()) return candidates;
        return candidates.stream()
                .filter(p -> p.getBrand() != null && p.getBrand().contains(brand))
                .collect(Collectors.toList());
    }

    private List<Product> filterByPriceRange(List<Product> candidates, BigDecimal minPrice, BigDecimal maxPrice) {
        return candidates.stream()
                .filter(p -> {
                    if (p.getPrice() == null) return false;
                    if (minPrice != null && p.getPrice().compareTo(minPrice) < 0) return false;
                    if (maxPrice != null && p.getPrice().compareTo(maxPrice) > 0) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private static class ScoredProduct {
        final Product product;
        final int score;

        ScoredProduct(Product product, int score) {
            this.product = product;
            this.score = score;
        }
    }
}
