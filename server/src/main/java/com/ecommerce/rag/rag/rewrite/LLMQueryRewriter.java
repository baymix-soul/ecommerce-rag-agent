package com.ecommerce.rag.rag.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LLMQueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(LLMQueryRewriter.class);

    private static final String REWRITE_PROMPT_TEMPLATE = """
            你是电商导购的查询扩展助手。你只做软语义扩展，不改写硬约束。
            
            商品原始查询：%s
            
            请将查询扩展为合适的搜索变体和关键词，输出纯 JSON：
            
            {
              "expanded_query": "原始查询 + 扩展的相关关键词",
              "query_variants": ["变体1", "变体2", "变体3"],
              "soft_keywords": ["关键词1", "关键词2", ...],
              "inferred_scenarios": ["场景1", "场景2", ...],
              "confidence": 0.0
            }
            
            规则：
            1. 只扩展软语义（场景、人群、使用方式、功能需求）。
            2. 不改写硬约束（价格、品牌排除、品类）。
            3. 不输出 price filter。
            4. 不输出 brand exclusion。
            5. 不输出 product_id。
            6. 不发明具体商品。
            7. query_variants 不超过 3 个。
            8. soft_keywords 不超过 8 个。
            9. 输出必须是合法 JSON。
            10. 如果无法扩展，返回空列表和 confidence=0。
            
            只输出 JSON，不要其他文字。""";

    private final LlmClient llmClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMQueryRewriter(LlmClient llmClient, AppProperties appProperties) {
        this.llmClient = llmClient;
        this.appProperties = appProperties;
    }

    public QueryRewriteResult rewrite(String query) {
        if (query == null || query.isBlank()) {
            return QueryRewriteResult.none();
        }

        String prompt = String.format(REWRITE_PROMPT_TEMPLATE, query);

        AtomicReference<String> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        int timeout = appProperties.getRewrite() != null
                ? appProperties.getRewrite().getTimeoutSeconds() : 10;

        llmClient.streamGenerate(prompt,
                text -> {
                    responseRef.updateAndGet(current ->
                            (current == null ? "" : current) + text);
                },
                latch::countDown,
                err -> {
                    errorRef.set(err);
                    latch.countDown();
                });

        try {
            boolean completed = latch.await(timeout, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("LLMQueryRewriter timed out after {} seconds", timeout);
                return QueryRewriteResult.fallback("LLM rewrite timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return QueryRewriteResult.fallback("LLM rewrite interrupted");
        }

        if (errorRef.get() != null) {
            log.warn("LLMQueryRewriter error: {}", errorRef.get().getMessage());
            return QueryRewriteResult.fallback("LLM error: " + errorRef.get().getMessage());
        }

        String response = responseRef.get();
        if (response == null || response.isBlank()) {
            return QueryRewriteResult.fallback("LLM returned empty response");
        }

        return parseResponse(response, query);
    }

    private QueryRewriteResult parseResponse(String response, String query) {
        try {
            String json = extractJson(response);
            if (json == null) {
                log.warn("LLMQueryRewriter: no JSON found in response: {}", truncated(response));
                return QueryRewriteResult.fallback("No JSON in LLM response");
            }

            JsonNode root = objectMapper.readTree(json);

            List<String> variants = new ArrayList<>();
            if (root.has("query_variants") && root.get("query_variants").isArray()) {
                for (JsonNode v : root.get("query_variants")) {
                    if (v.isTextual()) variants.add(v.asText());
                }
            }

            List<String> keywords = new ArrayList<>();
            if (root.has("soft_keywords") && root.get("soft_keywords").isArray()) {
                for (JsonNode k : root.get("soft_keywords")) {
                    if (k.isTextual()) keywords.add(k.asText());
                }
            }

            List<String> scenarios = new ArrayList<>();
            if (root.has("inferred_scenarios") && root.get("inferred_scenarios").isArray()) {
                for (JsonNode s : root.get("inferred_scenarios")) {
                    if (s.isTextual()) scenarios.add(s.asText());
                }
            }

            double confidence = 0.0;
            if (root.has("confidence") && root.get("confidence").isNumber()) {
                confidence = root.get("confidence").asDouble();
            }

            String expandedQuery = null;
            if (root.has("expanded_query") && root.get("expanded_query").isTextual()) {
                expandedQuery = root.get("expanded_query").asText();
            }

            return QueryRewriteResult.fromLlm(query, variants, keywords, scenarios, confidence, expandedQuery);

        } catch (Exception e) {
            log.warn("LLMQueryRewriter: failed to parse JSON response: {}", e.getMessage());
            return QueryRewriteResult.fallback("Failed to parse LLM JSON: " + e.getMessage());
        }
    }

    private String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    private String truncated(String text) {
        if (text == null) return "null";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }
}
