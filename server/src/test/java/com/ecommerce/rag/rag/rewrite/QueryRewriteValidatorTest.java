package com.ecommerce.rag.rag.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class QueryRewriteValidatorTest {

    private final QueryRewriteValidator validator = new QueryRewriteValidator();

    @Test
    void shouldLimitVariantsToMax() {
        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of("v1", "v2", "v3", "v4", "v5"),
                List.of(), List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 3, 8);

        assertEquals(3, validated.getQueryVariants().size());
    }

    @Test
    void shouldLimitKeywordsToMax() {
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < 15; i++) keywords.add("kw" + i);

        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of(), keywords, List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 3, 8);

        assertTrue(validated.getSoftKeywords().size() <= 8);
    }

    @Test
    void shouldDeduplicate() {
        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of("v1", "v1", "V1", "v2"),
                List.of("kw1", "kw1", "KW1", "kw2"), List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 10, 10);

        assertEquals(2, validated.getQueryVariants().size());
        assertEquals(2, validated.getSoftKeywords().size());
    }

    @Test
    void shouldRemoveEmptyStrings() {
        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of("", "valid1", "  "),
                List.of("", "valid2", "  "), List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 10, 10);

        assertEquals(1, validated.getQueryVariants().size());
        assertEquals(1, validated.getSoftKeywords().size());
        assertTrue(validated.getWarnings().size() > 0);
    }

    @Test
    void shouldRemovePriceExpressions() {
        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of("valid1", "100元以内耳机"),
                List.of("valid_kw", "500以下"), List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 10, 10);

        assertFalse(validated.getQueryVariants().contains("100元以内耳机"));
        assertFalse(validated.getSoftKeywords().contains("500以下"));
    }

    @Test
    void shouldRemoveForbiddenPrefix() {
        QueryRewriteResult raw = QueryRewriteResult.fromLlm("test",
                List.of("valid1", "不要苹果"),
                List.of("valid_kw", "除了耐克"), List.of(), 0.9, null);

        QueryRewriteResult validated = validator.validate(raw, 10, 10);

        assertFalse(validated.getQueryVariants().contains("不要苹果"));
        assertFalse(validated.getSoftKeywords().contains("除了耐克"));
    }
}
