package com.ecommerce.rag.services.cart;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.services.ProductService;
import com.ecommerce.rag.services.recommendation.RecommendationReasonService;

/**
 * Deterministic cart top-up recommendation service.
 * Finds products from the catalog that help reach a target order amount.
 * Does NOT call LLM, Qdrant, or Redis directly.
 */
@Service
public class CartTopUpRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(CartTopUpRecommendationService.class);

    private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("100");

    private final ProductService productService;
    private final AppProperties appProperties;
    private final RecommendationReasonService reasonService;

    public CartTopUpRecommendationService(ProductService productService,
                                          AppProperties appProperties,
                                          RecommendationReasonService reasonService) {
        this.productService = productService;
        this.appProperties = appProperties;
        this.reasonService = reasonService;
    }

    /**
     * Recommend products to help reach the target order amount.
     *
     * @param cart         current cart view
     * @param targetAmount target total amount to reach
     * @param limit        max number of recommendations (capped at 3)
     * @return list of ProductCard sorted by how close the price is to the gap
     */
    public List<ProductCard> recommend(CartView cart, BigDecimal targetAmount, int limit) {
        // Step 1: Calculate gap
        BigDecimal totalAmount = cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal gap = targetAmount.subtract(totalAmount);

        // Step 2: Already reached target
        if (gap.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Cart already reaches target amount, no top-up needed");
            return List.of();
        }

        // Step 3: Collect product IDs already in cart
        Set<String> cartProductIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toSet());

        // Step 4: Get tolerance from config
        BigDecimal tolerance = getTopUpTolerance();
        BigDecimal maxPrice = gap.add(tolerance);

        // Step 5: Filter and sort candidates
        int effectiveLimit = Math.min(limit, 3);

        List<ProductCard> recommendations = productService.listAll().stream()
                .filter(p -> !cartProductIds.contains(p.getProductId()))
                .filter(p -> p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .filter(p -> p.getPrice().compareTo(maxPrice) <= 0)
                .sorted(Comparator.comparing(p -> gap.subtract(p.getPrice()).abs()))
                .limit(effectiveLimit)
                .map(p -> {
                    String reason = reasonService != null
                            ? reasonService.generateTopUpReason(p, gap, targetAmount)
                            : "适合凑单";
                    return new ProductCard(
                            p.getProductId(),
                            p.getName(),
                            p.getPrice(),
                            p.getCurrency(),
                            p.getImageUrl(),
                            reason
                    );
                })
                .collect(Collectors.toList());

        log.debug("Top-up recommendations: {} products for gap={}, tolerance={}",
                recommendations.size(), gap, tolerance);

        return recommendations;
    }

    private BigDecimal getTopUpTolerance() {
        if (appProperties.getCart() != null) {
            return new BigDecimal(String.valueOf(appProperties.getCart().getTopUpTolerance()));
        }
        return DEFAULT_TOLERANCE;
    }
}
