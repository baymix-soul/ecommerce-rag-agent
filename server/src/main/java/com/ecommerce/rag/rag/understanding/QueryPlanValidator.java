package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QueryPlanValidator {

    private static final Logger log = LoggerFactory.getLogger(QueryPlanValidator.class);

    private static final int MAX_QUERY_VARIANTS = 3;
    private static final int MAX_SOFT_KEYWORDS = 8;
    private static final int MAX_REQUESTED_PRODUCT_COUNT = 3;
    private static final int DEFAULT_REQUESTED_PRODUCT_COUNT = 3;

    private static final Set<String> NON_RETRIEVAL_INTENTS = Set.of(
            QueryPlan.INTENT_SMALLTALK, QueryPlan.INTENT_HELP, QueryPlan.INTENT_THANKS
    );

    private static final Set<String> RETRIEVAL_INTENTS = Set.of(
            QueryPlan.INTENT_PRODUCT_SEARCH, QueryPlan.INTENT_REFINE_PREVIOUS_QUERY,
            QueryPlan.INTENT_NEGATIVE_CONSTRAINT, QueryPlan.INTENT_CHANGE_OR_MORE,
            QueryPlan.INTENT_CURRENT_PRODUCT_QA
    );

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\d+\\s*(?:元|以内|以下|左右)");
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("p_[a-z]+_\\d+");
    private static final Set<String> FORBIDDEN_PREFIX = Set.of(
            "除了", "不要", "不含", "不想要", "排除", "别推荐"
    );

    public QueryPlanValidator() {
    }

    public QueryPlanValidationResult validate(QueryPlan plan, CatalogTaxonomySnapshot taxonomy) {
        if (plan == null) {
            QueryPlanValidationResult r = new QueryPlanValidationResult();
            r.setValid(false);
            r.getErrors().add("plan is null");
            return r;
        }

        if (taxonomy == null) {
            QueryPlanValidationResult r = new QueryPlanValidationResult();
            r.setOriginalPlan(plan);
            r.setValidatedPlan(plan);
            r.setValid(true);
            r.getWarnings().add("taxonomy is null, skipping category/brand/price validation");
            return r;
        }

        QueryPlan validated = plan.deepCopy();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> fixedFields = new ArrayList<>();
        ValidationState state = new ValidationState();

        validateCategory(validated, taxonomy, state, warnings);
        validateSubCategory(validated, taxonomy, state, warnings);
        validatePrice(validated, taxonomy, warnings, errors, fixedFields, state);
        validateBrands(validated, taxonomy, state, warnings);
        validateVariantsAndKeywords(validated, warnings, fixedFields);
        validateNeedsRetrieval(validated, warnings, fixedFields);
        validateRequestedProductCount(validated, warnings, fixedFields);
        validateAttributes(validated, warnings);

        QueryPlanValidationResult result = new QueryPlanValidationResult();
        result.setOriginalPlan(plan);
        result.setValidatedPlan(validated);
        result.setValid(errors.isEmpty());
        result.setWarnings(warnings);
        result.setErrors(errors);
        result.setFixedFields(fixedFields);
        result.setCategoryMatched(state.categoryMatched);
        result.setSubCategoryMatched(state.subCategoryMatched);
        result.setPriceValid(state.priceValid);
        result.setBrandValid(state.brandValid);

        return result;
    }

    private void validateCategory(QueryPlan plan, CatalogTaxonomySnapshot taxonomy,
                                   ValidationState state, List<String> warnings) {
        if (plan.getTarget() == null) return;

        String category = plan.getTarget().getCategory();
        if (category == null || category.isBlank()) return;

        if (taxonomy.getCategories().contains(category)) {
            state.categoryMatched = true;
            return;
        }

        warnings.add("unknown_category: '" + category + "' not in taxonomy categories");
        state.categoryMatched = false;
        plan.getTarget().setCategory(null);
    }

    private void validateSubCategory(QueryPlan plan, CatalogTaxonomySnapshot taxonomy,
                                      ValidationState state, List<String> warnings) {
        if (plan.getTarget() == null) return;

        String subCategory = plan.getTarget().getSubCategory();
        if (subCategory == null || subCategory.isBlank()) return;

        if (taxonomy.getAllSubCategories().contains(subCategory)) {
            state.subCategoryMatched = true;

            String category = plan.getTarget().getCategory();
            if (category != null && !category.isBlank()) {
                List<String> subForCat = taxonomy.getSubCategoriesByCategory().get(category);
                if (subForCat != null && !subForCat.contains(subCategory)) {
                    warnings.add("sub_category_not_under_category: '" + subCategory
                            + "' not under '" + category + "'");
                }
            }
            return;
        }

        warnings.add("unknown_sub_category: '" + subCategory + "' not in taxonomy");
        state.subCategoryMatched = false;
        plan.getTarget().setSubCategory(null);
    }

    private void validatePrice(QueryPlan plan, CatalogTaxonomySnapshot taxonomy,
                                List<String> warnings, List<String> errors,
                                List<String> fixedFields, ValidationState state) {
        if (plan.getPrice() == null) return;

        QueryPlanPrice price = plan.getPrice();
        boolean hasError = false;

        if (price.getCurrency() == null || price.getCurrency().isBlank()) {
            price.setCurrency("CNY");
            fixedFields.add("price.currency=CNY");
        }

        if (price.getStrict() == null) {
            price.setStrict(true);
            fixedFields.add("price.strict=true");
        }

        if (price.getMin() != null && price.getMin().compareTo(BigDecimal.ZERO) < 0) {
            price.setMin(BigDecimal.ZERO);
            warnings.add("price.min negative, corrected to 0");
            fixedFields.add("price.min=0");
        }

        if (price.getMax() != null && price.getMax().compareTo(BigDecimal.ZERO) < 0) {
            price.setMax(null);
            warnings.add("price.max negative, removed");
            fixedFields.add("price.max=removed");
        }

        if (price.getMin() != null && price.getMax() != null && price.getMin().compareTo(price.getMax()) > 0) {
            BigDecimal tmp = price.getMin();
            price.setMin(price.getMax());
            price.setMax(tmp);
            warnings.add("price.min > price.max, swapped");
            fixedFields.add("price.min_max_swapped");
        }

        if (taxonomy.getMaxPrice() != null && price.getMax() != null
                && price.getMax().compareTo(taxonomy.getMaxPrice().multiply(new BigDecimal("3"))) > 0) {
            warnings.add("price.max (" + price.getMax() + ") significantly above catalog max ("
                    + taxonomy.getMaxPrice() + ")");
        }

        state.priceValid = !hasError;
    }

    private void validateBrands(QueryPlan plan, CatalogTaxonomySnapshot taxonomy,
                                 ValidationState state, List<String> warnings) {
        if (plan.getBrands() == null) return;

        List<String> allBrands = taxonomy.getBrands();
        if (allBrands.isEmpty()) return;

        boolean hasUnknown = false;
        hasUnknown |= validateBrandList(plan.getBrands().getInclude(), allBrands, "brands.include", warnings);
        hasUnknown |= validateBrandList(plan.getBrands().getExclude(), allBrands, "brands.exclude", warnings);

        state.brandValid = !hasUnknown;
    }

    private boolean validateBrandList(List<String> brandList, List<String> knownBrands,
                                       String fieldName, List<String> warnings) {
        if (brandList == null || brandList.isEmpty()) return false;
        boolean hasUnknown = false;
        for (String brand : brandList) {
            if (!knownBrands.contains(brand)) {
                warnings.add("unknown_brand in " + fieldName + ": '" + brand + "'");
                hasUnknown = true;
            }
        }
        return hasUnknown;
    }

    private void validateVariantsAndKeywords(QueryPlan plan, List<String> warnings, List<String> fixedFields) {
        plan.setQueryVariants(cleanVariantList(plan.getQueryVariants(), MAX_QUERY_VARIANTS, warnings, fixedFields));
        plan.setSoftKeywords(cleanKeywordList(plan.getSoftKeywords(), MAX_SOFT_KEYWORDS, warnings, fixedFields));
    }

    private List<String> cleanVariantList(List<String> list, int maxCount,
                                           List<String> warnings, List<String> fixedFields) {
        if (list == null || list.isEmpty()) return new ArrayList<>();

        Set<String> deduped = new LinkedHashSet<>();
        int originalSize = list.size();
        for (String s : list) {
            if (s == null || s.isBlank()) continue;
            String cleaned = s.trim();
            if (PRODUCT_ID_PATTERN.matcher(cleaned).find()) {
                warnings.add("removed product_id pattern from variant: '" + cleaned + "'");
                continue;
            }
            if (PRICE_PATTERN.matcher(cleaned).find()) {
                warnings.add("removed price pattern from variant: '" + cleaned + "'");
                continue;
            }
            boolean forbidden = false;
            for (String prefix : FORBIDDEN_PREFIX) {
                if (cleaned.startsWith(prefix)) {
                    warnings.add("removed forbidden prefix variant: '" + cleaned + "'");
                    forbidden = true;
                    break;
                }
            }
            if (forbidden) continue;
            if (deduped.size() >= maxCount) break;
            deduped.add(cleaned);
        }

        if (deduped.size() < originalSize) {
            fixedFields.add("queryVariants deduped (" + originalSize + " -> " + deduped.size() + ")");
        }

        return new ArrayList<>(deduped);
    }

    private List<String> cleanKeywordList(List<String> list, int maxCount,
                                           List<String> warnings, List<String> fixedFields) {
        if (list == null || list.isEmpty()) return new ArrayList<>();

        Set<String> deduped = new LinkedHashSet<>();
        int originalSize = list.size();
        for (String s : list) {
            if (s == null || s.isBlank()) continue;
            String cleaned = s.trim();
            if (PRODUCT_ID_PATTERN.matcher(cleaned).find()) {
                warnings.add("removed product_id pattern from keyword: '" + cleaned + "'");
                continue;
            }
            if (PRICE_PATTERN.matcher(cleaned).find()) {
                warnings.add("removed price pattern from keyword: '" + cleaned + "'");
                continue;
            }
            boolean forbidden = false;
            for (String prefix : FORBIDDEN_PREFIX) {
                if (cleaned.startsWith(prefix)) {
                    warnings.add("removed forbidden prefix keyword: '" + cleaned + "'");
                    forbidden = true;
                    break;
                }
            }
            if (forbidden) continue;
            if (deduped.size() >= maxCount) break;
            deduped.add(cleaned);
        }

        if (deduped.size() < originalSize) {
            fixedFields.add("softKeywords deduped (" + originalSize + " -> " + deduped.size() + ")");
        }

        return new ArrayList<>(deduped);
    }

    private void validateNeedsRetrieval(QueryPlan plan, List<String> warnings, List<String> fixedFields) {
        if (plan.getNeedsRetrieval() == null && plan.getIntent() != null) {
            if (NON_RETRIEVAL_INTENTS.contains(plan.getIntent())) {
                plan.setNeedsRetrieval(false);
                fixedFields.add("needsRetrieval=false (inferred from " + plan.getIntent() + ")");
            } else if (RETRIEVAL_INTENTS.contains(plan.getIntent())) {
                plan.setNeedsRetrieval(true);
                fixedFields.add("needsRetrieval=true (inferred from " + plan.getIntent() + ")");
            }
        }
    }

    private void validateRequestedProductCount(QueryPlan plan, List<String> warnings, List<String> fixedFields) {
        if (plan.getRequestedProductCount() == null) {
            plan.setRequestedProductCount(DEFAULT_REQUESTED_PRODUCT_COUNT);
            fixedFields.add("requestedProductCount=" + DEFAULT_REQUESTED_PRODUCT_COUNT);
            return;
        }

        Integer count = plan.getRequestedProductCount();
        if (count < 1) {
            plan.setRequestedProductCount(1);
            warnings.add("requestedProductCount < 1, corrected to 1");
            fixedFields.add("requestedProductCount=1 (was " + count + ")");
        } else if (count > MAX_REQUESTED_PRODUCT_COUNT) {
            plan.setRequestedProductCount(MAX_REQUESTED_PRODUCT_COUNT);
            warnings.add("requestedProductCount > " + MAX_REQUESTED_PRODUCT_COUNT
                    + ", corrected to " + MAX_REQUESTED_PRODUCT_COUNT);
            fixedFields.add("requestedProductCount=" + MAX_REQUESTED_PRODUCT_COUNT + " (was " + count + ")");
        }

        if ("SINGLE_RECOMMENDATION".equals(plan.getAnswerMode())) {
            plan.setRequestedProductCount(1);
            fixedFields.add("requestedProductCount=1 (answerMode=SINGLE_RECOMMENDATION)");
        }
    }

    private void validateAttributes(QueryPlan plan, List<String> warnings) {
        if (plan.getAttributes() == null) return;

        if (plan.getAttributes().getInclude() != null && plan.getAttributes().getInclude().size() > 20) {
            warnings.add("attributes.include has > 20 entries, may be too broad");
        }
    }

    private static class ValidationState {
        boolean categoryMatched;
        boolean subCategoryMatched;
        boolean priceValid;
        boolean brandValid;
    }
}
