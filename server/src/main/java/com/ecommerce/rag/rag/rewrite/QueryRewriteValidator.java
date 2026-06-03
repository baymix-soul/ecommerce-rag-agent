package com.ecommerce.rag.rag.rewrite;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriteValidator {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriteValidator.class);

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\d+\\s*(?:元|以内|以下|左右)");
    private static final Pattern FORBIDDEN_PREFIX = Pattern.compile(
            "^(?:不要|排除|除了|不含|别买|禁止)\\s*.*", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PRODUCT_ID_LIKE = Pattern.compile("p_\\w+\\d+",
            Pattern.UNICODE_CHARACTER_CLASS);

    public QueryRewriteResult validate(QueryRewriteResult raw, int maxVariants, int maxSoftKeywords) {
        if (raw == null) {
            return QueryRewriteResult.fallback("raw result is null");
        }

        List<String> warnings = new ArrayList<>(raw.getWarnings());

        List<String> cleanedVariants = cleanVariants(raw.getQueryVariants(), maxVariants, warnings);
        List<String> cleanedKeywords = cleanKeywords(raw.getSoftKeywords(), maxSoftKeywords, warnings);

        raw.setQueryVariants(cleanedVariants);
        raw.setSoftKeywords(cleanedKeywords);
        raw.getWarnings().addAll(warnings);

        if (!warnings.isEmpty()) {
            log.debug("QueryRewriteValidator warnings: {}", warnings);
        }

        return raw;
    }

    private List<String> cleanVariants(List<String> variants, int max, List<String> warnings) {
        if (variants == null || variants.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>();

        for (String v : variants) {
            if (v == null || v.isBlank()) {
                warnings.add("validator: removed empty query variant");
                continue;
            }

            String trimmed = v.trim();

            if (containsPrice(trimmed)) {
                warnings.add("validator: removed variant with price: " + truncated(trimmed));
                continue;
            }

            if (FORBIDDEN_PREFIX.matcher(trimmed).matches()) {
                warnings.add("validator: removed variant with forbidden prefix: " + truncated(trimmed));
                continue;
            }

            if (PRODUCT_ID_LIKE.matcher(trimmed).find()) {
                warnings.add("validator: removed variant with product_id: " + truncated(trimmed));
                continue;
            }

            String lower = trimmed.toLowerCase();
            if (!seen.contains(lower) && cleaned.size() < max) {
                seen.add(lower);
                cleaned.add(trimmed);
            }
        }

        return cleaned;
    }

    private List<String> cleanKeywords(List<String> keywords, int max, List<String> warnings) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> cleaned = new ArrayList<>();

        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) {
                warnings.add("validator: removed empty keyword");
                continue;
            }

            String trimmed = kw.trim();

            if (trimmed.length() > 50) {
                trimmed = trimmed.substring(0, 50);
                warnings.add("validator: truncated long keyword");
            }

            if (containsPrice(trimmed)) {
            warnings.add("validator: removed keyword with price: " + trimmed);
            continue;
        }

        if (FORBIDDEN_PREFIX.matcher(trimmed).matches()) {
            warnings.add("validator: removed keyword with forbidden prefix: " + truncated(trimmed));
            continue;
        }

        if (PRODUCT_ID_LIKE.matcher(trimmed).find()) {
                warnings.add("validator: removed keyword with product_id: " + trimmed);
                continue;
            }

            String lower = trimmed.toLowerCase();
            if (!seen.contains(lower) && cleaned.size() < max) {
                seen.add(lower);
                cleaned.add(trimmed);
            }
        }

        return cleaned;
    }

    private boolean containsPrice(String text) {
        if (text == null) return false;
        return PRICE_PATTERN.matcher(text).find();
    }

    private String truncated(String text) {
        if (text == null) return "null";
        return text.length() > 40 ? text.substring(0, 40) + "..." : text;
    }
}
