package com.ecommerce.rag.rag.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.llm.LlmClient;

class LLMQueryRewriterTest {

    private LLMQueryRewriter rewriter;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setRewrite(new AppProperties.RewriteProperties());
    }

    @Test
    void shouldParseValidJsonResponse() {
        String jsonResponse = """
                {
                  "expanded_query": "学生党耳机 性价比 低价 实用 入门款",
                  "query_variants": ["性价比蓝牙耳机", "适合学生党的入门耳机"],
                  "soft_keywords": ["性价比", "低价", "实用", "入门款", "续航"],
                  "inferred_scenarios": ["学生党", "日常使用"],
                  "confidence": 0.85
                }""";

        LlmClient fakeClient = createFakeLlmClient(jsonResponse);
        rewriter = new LLMQueryRewriter(fakeClient, appProperties);

        QueryRewriteResult result = rewriter.rewrite("学生党耳机");

        assertNotNull(result);
        assertEquals("LLM", result.getSource());
        assertTrue(result.getQueryVariants().size() > 0);
        assertTrue(result.getSoftKeywords().size() > 0);
        assertTrue(result.getSoftKeywords().contains("性价比"));
        assertTrue(result.getConfidence() > 0.5);
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        String badResponse = "This is not JSON at all, just some random text";

        LlmClient fakeClient = createFakeLlmClient(badResponse);
        rewriter = new LLMQueryRewriter(fakeClient, appProperties);

        QueryRewriteResult result = rewriter.rewrite("学生党耳机");

        assertNotNull(result);
        assertTrue(result.getSource().equals("FALLBACK"));
    }

    @Test
    void shouldHandleEmptyResponse() {
        LlmClient fakeClient = createFakeLlmClient("");
        rewriter = new LLMQueryRewriter(fakeClient, appProperties);

        QueryRewriteResult result = rewriter.rewrite("测试");

        assertNotNull(result);
        assertTrue(result.getSource().equals("FALLBACK"));
    }

    @Test
    void shouldHandleLlmError() {
        LlmClient errorClient = new LlmClient() {
            @Override
            public void streamGenerate(String prompt, Consumer<String> onText,
                                       Runnable onComplete, Consumer<Throwable> onError) {
                onError.accept(new RuntimeException("Simulated LLM error"));
            }
        };
        rewriter = new LLMQueryRewriter(errorClient, appProperties);

        QueryRewriteResult result = rewriter.rewrite("测试");

        assertNotNull(result);
        assertTrue(result.getSource().equals("FALLBACK"));
    }

    private LlmClient createFakeLlmClient(String response) {
        return new LlmClient() {
            @Override
            public void streamGenerate(String prompt, Consumer<String> onText,
                                       Runnable onComplete, Consumer<Throwable> onError) {
                onText.accept(response);
                onComplete.run();
            }
        };
    }
}
