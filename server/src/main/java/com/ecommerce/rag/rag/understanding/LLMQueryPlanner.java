package com.ecommerce.rag.rag.understanding;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTraceContext;
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

        PerfTraceContext.startSpan("planner.prompt_build");
        String prompt = promptBuilder.build(query, taxonomy, conversationState, pageContext);
        PerfTraceContext.endSpan("planner.prompt_build");

        PerfTraceContext.startSpan("planner.llm_call");
        String rawOutput = callLlm(prompt, query, plannerProps.getTimeoutSeconds());
        PerfTraceContext.endSpan("planner.llm_call");

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

        PerfTraceContext.startSpan("planner.parse");
        var parsed = jsonParser.parse(rawOutput);
        PerfTraceContext.endSpan("planner.parse");

        if (parsed.isEmpty()) {
            result.setSource(QueryPlanningResult.SOURCE_ERROR);
            result.setParseSuccess(false);
            result.setValid(false);
            result.getErrors().add("Failed to parse LLM JSON output");
            log.warn("LLMQueryPlanner: failed to parse JSON, raw output length={}", rawOutput.length());
            PerfTraceContext.addAttribute("parse_success", false);
            return result;
        }

        QueryPlan rawPlan = parsed.get();
        result.setRawPlan(rawPlan);
        result.setParseSuccess(true);
        result.setSource(QueryPlanningResult.SOURCE_LLM);

        PerfTraceContext.startSpan("planner.validate");
        QueryPlanValidationResult validationResult = validator.validate(rawPlan, taxonomy);
        PerfTraceContext.endSpan("planner.validate");

        result.setValidationResult(validationResult);
        result.setValidatedPlan(validationResult.getValidatedPlan());
        result.setValid(validationResult.getValid() != null && validationResult.getValid());
        result.setWarnings(validationResult.getWarnings());
        result.setErrors(validationResult.getErrors());

        PerfTraceContext.addAttribute("planner_latency_ms", latencyMs);
        PerfTraceContext.addAttribute("parse_success", true);
        PerfTraceContext.addAttribute("valid", result.getValid());
        PerfTraceContext.addAttribute("warnings_count",
                validationResult.getWarnings() != null ? validationResult.getWarnings().size() : 0);
        PerfTraceContext.addAttribute("errors_count",
                validationResult.getErrors() != null ? validationResult.getErrors().size() : 0);
        PerfTraceContext.addAttribute("raw_output_length", rawOutput.length());
        if (result.getValidatedPlan() != null) {
            PerfTraceContext.addAttribute("confidence", result.getValidatedPlan().getConfidence());
        }

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
