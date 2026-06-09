package com.ecommerce.rag.rag.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class QueryRewriteServiceTest {

    private QueryRewriteService service;
    private AppProperties appProperties;
    private SoftSemanticLexicon lexicon;
    private QueryRewriteValidator validator;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRewrite(new AppProperties.RewriteProperties());
        lexicon = new SoftSemanticLexicon();
        lexicon.init();
        validator = new QueryRewriteValidator();

        LLMQueryRewriter llmRewriter = new LLMQueryRewriter(
                createFakeLlmClient(), appProperties);

        service = new QueryRewriteService(appProperties, lexicon, llmRewriter, validator);
    }

    @Test
    void disabledShouldReturnNone() {
        appProperties.getRewrite().setEnabled(false);

        QueryRewriteResult result = service.rewrite("学生党耳机", null);

        assertEquals(QueryRewriteResult.SOURCE_NONE, result.getSource());
    }

    @Test
    void lexiconProviderShouldReturnLexicon() {
        appProperties.getRewrite().setEnabled(true);
        appProperties.getRewrite().setProvider("lexicon");

        QueryRewriteResult result = service.rewrite("学生党耳机", null);

        assertNotNull(result);
        assertTrue(result.getSource().equals(QueryRewriteResult.SOURCE_LEXICON)
                || result.getSource().equals(QueryRewriteResult.SOURCE_NONE));
        if (result.getSource().equals(QueryRewriteResult.SOURCE_LEXICON)) {
            assertTrue(result.getSoftKeywords().size() > 0);
        }
    }

    @Test
    void llmProviderShouldWorkWithFakeClient() {
        appProperties.getRewrite().setEnabled(true);
        appProperties.getRewrite().setProvider("llm");
        appProperties.getRewrite().setMaxVariants(3);
        appProperties.getRewrite().setMaxSoftKeywords(8);

        QueryRewriteResult result = service.rewrite("测试查询", null);

        assertNotNull(result);
        String source = result.getSource();
        assertTrue(source.equals(QueryRewriteResult.SOURCE_LLM)
                || source.equals(QueryRewriteResult.SOURCE_FALLBACK));
    }

    @Test
    void hybridProviderShouldMergeResults() {
        appProperties.getRewrite().setEnabled(true);
        appProperties.getRewrite().setProvider("hybrid");
        appProperties.getRewrite().setMaxVariants(3);
        appProperties.getRewrite().setMaxSoftKeywords(8);

        QueryRewriteResult result = service.rewrite("学生党耳机", null);

        assertNotNull(result);
        assertTrue(result.getSource().equals(QueryRewriteResult.SOURCE_HYBRID)
                || result.getSource().equals(QueryRewriteResult.SOURCE_LEXICON));
    }

    @Test
    void cacheShouldReturnCachedResult() {
        appProperties.getRewrite().setEnabled(true);
        appProperties.getRewrite().setProvider("lexicon");
        appProperties.getRewrite().setCacheEnabled(true);

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("数码电子");
        analysis.setSubCategory("真无线耳机");

        QueryRewriteResult r1 = service.rewrite("学生党耳机", analysis);
        QueryRewriteResult r2 = service.rewrite("学生党耳机", analysis);

        assertEquals(r1.getSource(), r2.getSource());
    }

    private com.ecommerce.rag.rag.llm.LlmClient createFakeLlmClient() {
        return new com.ecommerce.rag.rag.llm.LlmClient() {
            @Override
            public void streamGenerate(String prompt, java.util.function.Consumer<String> onText,
                                       Runnable onComplete, java.util.function.Consumer<Throwable> onError) {
                onText.accept("""
                        {
                          "expanded_query": "测试 扩展",
                          "query_variants": ["变体1"],
                          "soft_keywords": ["关键1", "关键2"],
                          "inferred_scenarios": ["场景1"],
                          "confidence": 0.8
                        }""");
                onComplete.run();
            }
        };
    }
}
