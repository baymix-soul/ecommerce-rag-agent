package com.ecommerce.rag.rag.llm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DoubaoLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(DoubaoLlmClient.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DoubaoLlmClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(appProperties.getLlm().getTimeoutSeconds()))
                .build();
    }

    @Override
    public void streamGenerate(String prompt, Consumer<String> onText, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            String requestBody = buildRequestBody(prompt);
            AppProperties.LlmProperties llmConfig = appProperties.getLlm();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(llmConfig.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmConfig.getApiKey())
                    .timeout(Duration.ofSeconds(llmConfig.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Calling LLM API: model={}, timeout={}s", llmConfig.getModel(), llmConfig.getTimeoutSeconds());

            PerfTraceContext.startSpan("llm.http_connect_or_request");
            java.util.concurrent.atomic.AtomicBoolean firstTokenRecorded = new java.util.concurrent.atomic.AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger outputCharCount = new java.util.concurrent.atomic.AtomicInteger(0);

            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            PerfTraceContext.endSpan("llm.http_connect_or_request");

            int statusCode = response.statusCode();
            if (statusCode == 401) {
                PerfTraceContext.addAttribute("llm_http_status", 401);
                onError.accept(new RuntimeException("LLM API 认证失败 (401)，请检查 LLM_API_KEY 配置"));
                return;
            }
            if (statusCode == 429) {
                PerfTraceContext.addAttribute("llm_http_status", 429);
                onError.accept(new RuntimeException("LLM API 请求频率超限 (429)，请稍后重试"));
                return;
            }
            if (statusCode >= 500) {
                PerfTraceContext.addAttribute("llm_http_status", statusCode);
                onError.accept(new RuntimeException("LLM API 服务异常 (" + statusCode + ")，请稍后重试"));
                return;
            }
            if (statusCode != 200) {
                String body = response.body().reduce("", (a, b) -> a + b);
                PerfTraceContext.addAttribute("llm_http_status", statusCode);
                onError.accept(new RuntimeException("LLM API 调用失败 (" + statusCode + ")"));
                return;
            }
            PerfTraceContext.addAttribute("llm_http_status", 200);

            parseSseStream(response.body(), onText, firstTokenRecorded, chunkCount, outputCharCount);
            PerfTraceContext.addAttribute("llm_model", llmConfig.getModel());
            PerfTraceContext.addAttribute("llm_chunk_count", chunkCount.get());
            PerfTraceContext.addAttribute("llm_output_chars", outputCharCount.get());
            onComplete.run();

        } catch (IOException e) {
            PerfTraceContext.addAttribute("llm_error", e.getClass().getSimpleName());
            if (e.getCause() instanceof java.net.ConnectException) {
                onError.accept(new RuntimeException("LLM API 连接失败，请检查 LLM_BASE_URL 配置"));
            } else {
                log.error("DoubaoLlmClient IO error: {}", e.getMessage());
                onError.accept(new RuntimeException("LLM API 通信异常: " + e.getMessage()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PerfTraceContext.addAttribute("llm_error", "InterruptedException");
            onError.accept(new RuntimeException("LLM API 调用被中断"));
        } catch (Exception e) {
            PerfTraceContext.addAttribute("llm_error", e.getClass().getSimpleName());
            log.error("DoubaoLlmClient error: {}", e.getMessage());
            onError.accept(new RuntimeException("LLM 生成异常: " + e.getMessage()));
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", appProperties.getLlm().getModel());
        body.put("stream", true);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        body.put("messages", List.of(userMessage));

        Map<String, Object> params = new HashMap<>();
        params.put("max_tokens", appProperties.getLlm().getMaxTokens());
        params.put("temperature", appProperties.getLlm().getTemperature());
        body.put("parameters", params);

        return objectMapper.writeValueAsString(body);
    }

    private void parseSseStream(Stream<String> lines, Consumer<String> onText,
                                 java.util.concurrent.atomic.AtomicBoolean firstTokenRecorded,
                                 java.util.concurrent.atomic.AtomicInteger chunkCount,
                                 java.util.concurrent.atomic.AtomicInteger outputCharCount) {
        lines.forEach(line -> {
            if (!line.startsWith("data: ")) {
                return;
            }
            String data = line.substring(6).trim();
            if ("[DONE]".equals(data)) {
                return;
            }
            try {
                JsonNode node = objectMapper.readTree(data);
                JsonNode deltaContent = node.at("/choices/0/delta/content");
                if (!deltaContent.isMissingNode() && !deltaContent.isNull() && !deltaContent.asText().isEmpty()) {
                    String content = deltaContent.asText();
                    if (firstTokenRecorded.compareAndSet(false, true)) {
                        PerfTraceContext.mark("llm.first_token");
                    }
                    chunkCount.incrementAndGet();
                    outputCharCount.addAndGet(content.length());
                    onText.accept(content);
                }
            } catch (Exception e) {
                log.warn("Failed to parse SSE chunk: {}", e.getMessage());
            }
        });
    }
}
