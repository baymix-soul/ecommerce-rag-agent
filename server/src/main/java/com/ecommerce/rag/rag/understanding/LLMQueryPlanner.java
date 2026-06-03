package com.ecommerce.rag.rag.understanding;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationState;

@Component
public class LLMQueryPlanner {

    private static final Logger log = LoggerFactory.getLogger(LLMQueryPlanner.class);

    private final LlmClient llmClient;
    private final AppProperties appProperties;
    private final QueryPlannerPromptBuilder promptBuilder;
    private final QueryPlanJsonParser jsonParser;
    private final QueryPlanValidator validator;
    private final CatalogTaxonomyService taxonomyService;

    public LLMQueryPlanner(LlmClient llmClient,
                            AppProperties appProperties,
                            QueryPlannerPromptBuilder promptBuilder,
                            QueryPlanJsonParser jsonParser,
                            QueryPlanValidator validator,
                            CatalogTaxonomyService taxonomyService) {
        this.llmClient = llmClient;
        this.appProperties = appProperties;
        this.promptBuilder = promptBuilder;
        this.jsonParser = jsonParser;
        this.validator = validator;
        this.taxonomyService = taxonomyService;
    }

    public QueryPlanningResult plan(String query, ConversationState conversationState,
                                     PageContextResolution pageContext) {
        long startTime = System.currentTimeMillis();

        var plannerProps = appProperties.getUnderstanding() != null
                ? appProperties.getUnderstanding().getPlanner() : null;

        if (plannerProps == null || !plannerProps.isEnabled()) {
            return QueryPlanningResult.disabled();
        }

        String mode = plannerProps.getMode();
        if (mode == null || mode.isBlank()) mode = "shadow";

        CatalogTaxonomySnapshot taxonomy = taxonomyService.getSnapshot();

        String prompt = promptBuilder.build(query, taxonomy, conversationState, pageContext);

        String rawOutput = callLlm(prompt, query, plannerProps.getTimeoutSeconds());
        long latencyMs = System.currentTimeMillis() - startTime;

        QueryPlanningResult result = new QueryPlanningResult();
        result.setOriginalQuery(query);
        result.setPlannerEnabled(true);
        result.setMode(mode);
        result.setLatencyMs(latencyMs);

        if (rawOutput == null) {
            result.setSource(QueryPlanningResult.SOURCE_ERROR);
            result.setParseSuccess(false);
            result.setValid(false);
            result.getErrors().add("LLM returned no output");
            return result;
        }

        result.setRawLlmOutput(rawOutput);

        var parsed = jsonParser.parse(rawOutput);
        if (parsed.isEmpty()) {
            result.setSource(QueryPlanningResult.SOURCE_ERROR);
            result.setParseSuccess(false);
            result.setValid(false);
            result.getErrors().add("Failed to parse LLM JSON output");
            log.warn("LLMQueryPlanner: failed to parse JSON, raw output length={}", rawOutput.length());
            return result;
        }

        QueryPlan rawPlan = parsed.get();
        result.setRawPlan(rawPlan);
        result.setParseSuccess(true);
        result.setSource(QueryPlanningResult.SOURCE_LLM);

        QueryPlanValidationResult validationResult = validator.validate(rawPlan, taxonomy);
        result.setValidationResult(validationResult);
        result.setValidatedPlan(validationResult.getValidatedPlan());
        result.setValid(validationResult.getValid() != null && validationResult.getValid());
        result.setWarnings(validationResult.getWarnings());
        result.setErrors(validationResult.getErrors());

        log.info("LLMQueryPlanner: query='{}', latency={}ms, parseSuccess={}, valid={}, "
                        + "category={}, subCategory={}, intent={}, confidence={}",
                query, latencyMs, true, result.getValid(),
                result.getValidatedPlan() != null && result.getValidatedPlan().getTarget() != null
                        ? result.getValidatedPlan().getTarget().getCategory() : null,
                result.getValidatedPlan() != null && result.getValidatedPlan().getTarget() != null
                        ? result.getValidatedPlan().getTarget().getSubCategory() : null,
                result.getValidatedPlan() != null ? result.getValidatedPlan().getIntent() : null,
                result.getValidatedPlan() != null ? result.getValidatedPlan().getConfidence() : null);

        return result;
    }

    private String callLlm(String prompt, String query, int timeoutSeconds) {
        AtomicReference<String> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        try {
            llmClient.streamGenerate(prompt,
                    text -> responseRef.updateAndGet(current -> (current == null ? "" : current) + text),
                    latch::countDown,
                    err -> {
                        errorRef.set(err);
                        latch.countDown();
                    });
        } catch (Exception e) {
            log.warn("LLMQueryPlanner: LLM call failed for query '{}': {}", query, e.getMessage());
            return null;
        }

        try {
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("LLMQueryPlanner: timed out after {} seconds for query '{}'", timeoutSeconds, query);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("LLMQueryPlanner: interrupted for query '{}'", query);
            return null;
        }

        if (errorRef.get() != null) {
            log.warn("LLMQueryPlanner: error for query '{}': {}", query, errorRef.get().getMessage());
            return null;
        }

        return responseRef.get();
    }
}
