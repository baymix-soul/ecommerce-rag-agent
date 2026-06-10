package com.ecommerce.rag.rag.retriever;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.brand.BrandAliasService;
import com.ecommerce.rag.rag.eval.CategoryMatchService;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

@Component
public class StrictProductConstraintFilter {

    private static final Logger log = LoggerFactory.getLogger(StrictProductConstraintFilter.class);

    private final CategoryMatchService categoryMatchService;
    private final BrandAliasService brandAliasService;

    public StrictProductConstraintFilter(CategoryMatchService categoryMatchService,
                                          BrandAliasService brandAliasService) {
        this.categoryMatchService = categoryMatchService;
        this.brandAliasService = brandAliasService;
    }

    public ConstraintCheckResult check(Product product, QueryAnalysisResult analysis) {
        if (analysis == null) {
            return ConstraintCheckResult.passed("no_constraints");
        }

        ConstraintCheckResult result = ConstraintCheckResult.passed("start");

        result = ConstraintCheckResult.merge(result, checkCategory(product, analysis));
        result = ConstraintCheckResult.merge(result, checkSubCategory(product, analysis));
        result = ConstraintCheckResult.merge(result, checkPrice(product, analysis));
        result = ConstraintCheckResult.merge(result, checkNegativeBrands(product, analysis));
        result = ConstraintCheckResult.merge(result, checkExcludeProductIds(product, analysis));
        result = ConstraintCheckResult.merge(result, checkNegativeKeywords(product, analysis));

        return result;
    }

    public boolean passes(Product product, QueryAnalysisResult analysis) {
        return check(product, analysis).isPassed();
    }

    public List<RetrievedProductCandidate> filterCandidates(
            List<RetrievedProductCandidate> candidates,
            QueryAnalysisResult analysis) {

        if (analysis == null) {
            return candidates;
        }

        if (!analysis.hasHardConstraints()
                && (analysis.getNegativeKeywords() == null || analysis.getNegativeKeywords().isEmpty())
                && (analysis.getAvoidIngredientsOrTerms() == null || analysis.getAvoidIngredientsOrTerms().isEmpty())
                && (analysis.getNegativeBrands() == null || analysis.getNegativeBrands().isEmpty())) {
            return candidates;
        }

        List<RetrievedProductCandidate> filtered = new ArrayList<>();
        for (RetrievedProductCandidate c : candidates) {
            Product p = c.getProduct();
            if (p == null) continue;

            ConstraintCheckResult checkResult = check(p, analysis);
            if (checkResult.isPassed()) {
                filtered.add(c);
            } else {
                c.setFailedRules(checkResult.getFailedRules());
                c.setFailures(checkResult.getFailures());
                log.debug("Filtered out {} ({}): failedRules={}",
                        p.getProductId(), p.getName(), checkResult.getFailedRules());
            }
        }

        log.info("StrictProductConstraintFilter: {} candidates -> {} after filter",
                candidates.size(), filtered.size());
        return filtered;
    }

    private ConstraintCheckResult checkCategory(Product product, QueryAnalysisResult analysis) {
        String expectedCategory = analysis.getCategory();
        if (expectedCategory == null || expectedCategory.isBlank()) {
            return ConstraintCheckResult.passed("category:no_constraint");
        }

        String actualCategory = product.getCategory();
        if (actualCategory == null || actualCategory.isBlank()) {
            return ConstraintCheckResult.failed("category_mismatch(expected=" + expectedCategory + ", actual=null)",
                    ConstraintFailure.of(ConstraintFailure.CATEGORY_MISMATCH, "category", expectedCategory, "null"));
        }

        if (categoryMatchService.categoryMatches(expectedCategory, actualCategory)) {
            return ConstraintCheckResult.passed("category_match(" + expectedCategory + ")");
        }

        if (actualCategory.equals(expectedCategory)) {
            return ConstraintCheckResult.passed("category_match(" + expectedCategory + ")");
        }

        return ConstraintCheckResult.failed("category_mismatch(expected=" + expectedCategory
                        + ", actual=" + actualCategory + ")",
                ConstraintFailure.of(ConstraintFailure.CATEGORY_MISMATCH, "category", expectedCategory, actualCategory));
    }

    private ConstraintCheckResult checkSubCategory(Product product, QueryAnalysisResult analysis) {
        String expectedSub = analysis.getSubCategory();
        List<String> expectedSubs = analysis.getSubCategories();

        boolean hasSub = expectedSub != null && !expectedSub.isBlank();
        boolean hasSubs = expectedSubs != null && !expectedSubs.isEmpty();

        if (!hasSub && !hasSubs) {
            return ConstraintCheckResult.passed("subCategory:no_constraint");
        }

        String actualSub = product.getSubCategory();
        if (actualSub == null || actualSub.isBlank()) {
            String expected = hasSub ? expectedSub : expectedSubs.toString();
            return ConstraintCheckResult.failed("sub_category_mismatch(expected="
                            + expected + ", actual=null)",
                    ConstraintFailure.of(ConstraintFailure.SUB_CATEGORY_MISMATCH, "subCategory", expected, "null"));
        }

        if (hasSub && categoryMatchService.subCategoryMatches(expectedSub, actualSub)) {
            return ConstraintCheckResult.passed("sub_category_match(" + expectedSub + ")");
        }

        if (hasSubs && categoryMatchService.subCategoryMatchesAny(expectedSubs, actualSub)) {
            return ConstraintCheckResult.passed("sub_category_match(one of " + expectedSubs + ")");
        }

        if (hasSub && actualSub.equals(expectedSub)) {
            return ConstraintCheckResult.passed("sub_category_match(" + expectedSub + ")");
        }

        String expected = hasSub ? expectedSub : expectedSubs.toString();
        return ConstraintCheckResult.failed("sub_category_mismatch(expected="
                        + expected + ", actual=" + actualSub + ")",
                ConstraintFailure.of(ConstraintFailure.SUB_CATEGORY_MISMATCH, "subCategory", expected, actualSub));
    }

    private ConstraintCheckResult checkPrice(Product product, QueryAnalysisResult analysis) {
        BigDecimal minPrice = analysis.getMinPrice();
        BigDecimal maxPrice = analysis.getMaxPrice();
        BigDecimal productPrice = product.getPrice();

        if (minPrice == null && maxPrice == null) {
            return ConstraintCheckResult.passed("price:no_constraint");
        }

        if (productPrice == null) {
            return ConstraintCheckResult.passed("price:no_product_price");
        }

        if (minPrice != null && productPrice.compareTo(minPrice) < 0) {
            return ConstraintCheckResult.failed("price_lt_min(min=" + minPrice
                            + ", actual=" + productPrice + ")",
                    ConstraintFailure.of(ConstraintFailure.PRICE_LT_MIN, "price", minPrice.toString(), productPrice.toString()));
        }

        if (maxPrice != null && productPrice.compareTo(maxPrice) > 0) {
            return ConstraintCheckResult.failed("price_gt_max(max=" + maxPrice
                            + ", actual=" + productPrice + ")",
                    ConstraintFailure.of(ConstraintFailure.PRICE_GT_MAX, "price", "<=" + maxPrice, productPrice.toString()));
        }

        return ConstraintCheckResult.passed("price_ok(range=[" + minPrice + ", " + maxPrice + "])");
    }

    private ConstraintCheckResult checkNegativeBrands(Product product, QueryAnalysisResult analysis) {
        List<String> negativeBrands = analysis.getNegativeBrands();
        if (negativeBrands == null || negativeBrands.isEmpty()) {
            return ConstraintCheckResult.passed("negativeBrands:no_constraint");
        }

        if (brandAliasService.matchesBrandOrAlias(product, negativeBrands)) {
            String brand = product.getBrand();
            String displayBrand = (brand != null && !brand.isBlank()) ? brand : product.getName();
            return ConstraintCheckResult.failed("NEGATIVE_BRAND: brand=" + displayBrand
                            + ", negative=" + negativeBrands,
                    ConstraintFailure.of(ConstraintFailure.NEGATIVE_BRAND, "brand/product",
                            "not " + String.join(",", negativeBrands), displayBrand));
        }

        return ConstraintCheckResult.passed("negativeBrands_ok");
    }

    private ConstraintCheckResult checkExcludeProductIds(Product product, QueryAnalysisResult analysis) {
        List<String> excludeIds = analysis.getExcludeProductIds();
        if (excludeIds == null || excludeIds.isEmpty()) {
            return ConstraintCheckResult.passed("excludeProductIds:no_constraint");
        }

        String productId = product.getProductId();
        if (productId != null && excludeIds.contains(productId)) {
            return ConstraintCheckResult.failed("excluded_product_id(productId=" + productId + ")",
                    ConstraintFailure.of(ConstraintFailure.EXCLUDED_PRODUCT, "productId", "not in " + excludeIds, productId));
        }

        return ConstraintCheckResult.passed("excludeProductIds_ok");
    }

    private ConstraintCheckResult checkNegativeKeywords(Product product, QueryAnalysisResult analysis) {
        List<String> negativeTerms = new ArrayList<>();
        if (analysis.getAvoidIngredientsOrTerms() != null && !analysis.getAvoidIngredientsOrTerms().isEmpty()) {
            negativeTerms.addAll(analysis.getAvoidIngredientsOrTerms());
        }
        if (analysis.getNegativeKeywords() != null && !analysis.getNegativeKeywords().isEmpty()) {
            for (String kw : analysis.getNegativeKeywords()) {
                if (!negativeTerms.contains(kw)) {
                    negativeTerms.add(kw);
                }
            }
        }

        if (negativeTerms.isEmpty()) {
            return ConstraintCheckResult.passed("negativeKeywords:no_constraint");
        }

        String fullText = buildProductText(product).toLowerCase();
        for (String term : negativeTerms) {
            if (fullText.contains(term.toLowerCase())) {
                return ConstraintCheckResult.failed("negative_keyword_hit(keyword=" + term + ")",
                        ConstraintFailure.of(ConstraintFailure.NEGATIVE_KEYWORD, "text", "not contain " + term, "contains " + term));
            }
        }

        return ConstraintCheckResult.passed("negativeKeywords_ok");
    }

    private String buildProductText(Product product) {
        StringBuilder sb = new StringBuilder();
        if (product.getBrand() != null) sb.append(product.getBrand()).append(" ");
        if (product.getName() != null) sb.append(product.getName()).append(" ");
        if (product.getDescription() != null) sb.append(product.getDescription()).append(" ");
        if (product.getSpecs() != null) {
            for (Map.Entry<String, String> e : product.getSpecs().entrySet()) {
                sb.append(e.getKey()).append(" ").append(e.getValue()).append(" ");
            }
        }
        return sb.toString();
    }
}
