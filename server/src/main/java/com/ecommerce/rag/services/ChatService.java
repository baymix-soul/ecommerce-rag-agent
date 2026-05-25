package com.ecommerce.rag.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.retriever.CandidateProductRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String REASON_PLACEHOLDER =
            "该商品来自当前商品库候选结果，具体推荐理由将在后续 LLM 阶段生成。";

    private final CandidateProductRetriever retriever;
    private final RagPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatService(CandidateProductRetriever retriever,
                       RagPromptBuilder promptBuilder,
                       LlmClient llmClient,
                       AppProperties appProperties,
                       ObjectMapper objectMapper) {
        this.retriever = retriever;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public SseEmitter chat(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            sendErrorAndComplete(emitter, "INVALID_REQUEST", "message 不能为空");
            return emitter;
        }

        int limit = request.getEffectiveLimit();
        SseEmitter emitter = new SseEmitter(60_000L);

        List<ChatCandidate> candidates = retriever.retrieve(request.getMessage(), limit);
        String prompt = promptBuilder.build(request.getMessage(), candidates);

        log.info("Chat request: message={}, candidates={}", request.getMessage(), candidates.size());

        emitter.onCompletion(() -> log.debug("SSE emitter completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out");
            emitter.complete();
        });

        executor.submit(() -> {
            try {
                llmClient.streamGenerate(
                        prompt,
                        textChunk -> sendSseEvent(emitter, "text", Map.of("content", textChunk)),
                        () -> {
                            sendProductCards(emitter, candidates);
                            sendSseEvent(emitter, "done", Collections.emptyMap());
                            emitter.complete();
                        },
                        error -> sendErrorAndComplete(emitter, "LLM_ERROR", error.getMessage() != null ? error.getMessage() : "LLM 生成失败")
                );
            } catch (Exception e) {
                log.error("Error during LLM generation", e);
                sendErrorAndComplete(emitter, "LLM_ERROR", e.getMessage() != null ? e.getMessage() : "LLM 生成异常");
            }
        });

        return emitter;
    }

    private void sendProductCards(SseEmitter emitter, List<ChatCandidate> candidates) {
        for (ChatCandidate candidate : candidates) {
            ProductCard card = new ProductCard(
                    candidate.getProductId(),
                    candidate.getName(),
                    candidate.getPrice(),
                    candidate.getCurrency(),
                    candidate.getImageUrl(),
                    REASON_PLACEHOLDER
            );
            sendSseEvent(emitter, "product_card", card);
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(jsonData));
        } catch (IOException e) {
            log.warn("Failed to send SSE event: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String code, String message) {
        try {
            String jsonData = objectMapper.writeValueAsString(
                    Map.of("code", code, "message", message));
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(jsonData));
            emitter.complete();
        } catch (IOException e) {
            log.warn("Failed to send error SSE event: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }
}
