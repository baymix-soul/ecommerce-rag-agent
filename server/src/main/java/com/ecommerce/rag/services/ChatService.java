package com.ecommerce.rag.services;

import java.io.IOException;
import java.util.ArrayList;
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
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.response.RecommendationCountResolver;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String REASON_PLACEHOLDER =
            "该商品来自当前商品库候选结果，具体推荐理由将在后续 LLM 阶段生成。";

    private final HybridCandidateRetriever retriever;
    private final RagPromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RetrievalRouter retrievalRouter;
    private final ConversationMemoryService memoryService;
    private final QueryAnalyzer queryAnalyzer;
    private final PageContextResolver pageContextResolver;
    private final StrictProductConstraintFilter constraintFilter;
    private final ProductService productService;
    private final RecommendationCountResolver countResolver;
    private final QueryUnderstandingService queryUnderstandingService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatService(HybridCandidateRetriever retriever,
                       RagPromptBuilder promptBuilder,
                       LlmClient llmClient,
                       AppProperties appProperties,
                       ObjectMapper objectMapper,
                       RetrievalRouter retrievalRouter,
                       ConversationMemoryService memoryService,
                       QueryAnalyzer queryAnalyzer,
                       PageContextResolver pageContextResolver,
                       StrictProductConstraintFilter constraintFilter,
                       ProductService productService,
                       RecommendationCountResolver countResolver,
                       QueryUnderstandingService queryUnderstandingService) {
        this.retriever = retriever;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.retrievalRouter = retrievalRouter;
        this.memoryService = memoryService;
        this.queryAnalyzer = queryAnalyzer;
        this.pageContextResolver = pageContextResolver;
        this.constraintFilter = constraintFilter;
        this.productService = productService;
        this.countResolver = countResolver;
        this.queryUnderstandingService = queryUnderstandingService;
    }

    public SseEmitter chat(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            sendErrorAndComplete(emitter, "INVALID_REQUEST", "message 不能为空");
            return emitter;
        }

        String message = request.getMessage().trim();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        int limit = request.getEffectiveLimit();

        RetrievalRouteResult routeResult = retrievalRouter.route(message);
        RetrievalIntent intent = routeResult.getIntent();

        log.info("Chat request: message={}, sessionId={}, intent={}, needsRetrieval={}",
                message, sessionId, intent, routeResult.isNeedsRetrieval());

        if (!routeResult.isNeedsRetrieval()) {
            return handleNonRetrievalIntent(intent, message);
        }

        QueryUnderstandingResult understanding = queryUnderstandingService.understandForRetrieval(
                message, sessionId, request.getPageContext());
        QueryAnalysisResult analysis = understanding.getEffectiveAnalysis();

        analysis.setSessionId(sessionId);
        analysis.setIntent(intent);

        boolean plannerEnabled = appProperties.getUnderstanding() != null
                && appProperties.getUnderstanding().getPlanner() != null
                && appProperties.getUnderstanding().getPlanner().isEnabled();
        String plannerMode = appProperties.getUnderstanding() != null
                && appProperties.getUnderstanding().getPlanner() != null
                ? appProperties.getUnderstanding().getPlanner().getMode() : "disabled";
        String selectedSource = understanding.getSelectedSource();
        Boolean plannerUsed = understanding.getPlannerUsedForRetrieval();
        String fallbackReason = understanding.getFallbackReason();

        QueryAnalysisResult legacyAnalysis = null;
        if (understanding.getLegacyAnalysis() instanceof Map) {
            legacyAnalysis = new QueryAnalysisResult();
            @SuppressWarnings("unchecked")
            Map<String, Object> legacyMap = (Map<String, Object>) understanding.getLegacyAnalysis();
            legacyAnalysis.setOriginalQuery((String) legacyMap.get("original_query"));
            legacyAnalysis.setCategory((String) legacyMap.get("category"));
            legacyAnalysis.setSubCategory((String) legacyMap.get("sub_category"));
            legacyAnalysis.setMaxPrice(legacyMap.get("max_price") instanceof java.math.BigDecimal
                    ? (java.math.BigDecimal) legacyMap.get("max_price") : null);
        }

        log.info("QueryUnderstanding: query='{}', sessionId={}, plannerEnabled={}, plannerMode={}, selectedSource={}, plannerUsedForRetrieval={}, fallbackReason={}, legacy=[cat={}, sub={}, maxPrice={}], effective=[cat={}, sub={}, maxPrice={}]",
                message, sessionId, plannerEnabled, plannerMode, selectedSource,
                plannerUsed != null ? plannerUsed : false,
                fallbackReason != null ? fallbackReason : "N/A",
                legacyAnalysis != null ? legacyAnalysis.getCategory() : null,
                legacyAnalysis != null ? legacyAnalysis.getSubCategory() : null,
                legacyAnalysis != null ? legacyAnalysis.getMaxPrice() : null,
                analysis.getCategory(), analysis.getSubCategory(), analysis.getMaxPrice());

        PageContextResolution pageContext = pageContextResolver.resolve(request.getPageContext());

        int requestedCount = countResolver.resolve(message);
        int maxCardLimit = appProperties.getChat().getMaxProductCardLimit();
        int displayLimit = Math.min(requestedCount, maxCardLimit);
        displayLimit = Math.min(displayLimit, limit);

        analysis.setRequestedProductCount(requestedCount);

        if (pageContext.isProductDetail()
                && pageContext.isHasValidCurrentProduct()
                && analysis.getInheritedFromPageContext() != null
                && analysis.getInheritedFromPageContext()) {
            analysis.setResponseStyle(QueryAnalysisResult.CURRENT_PRODUCT_QA);
        } else if (requestedCount == 1) {
            analysis.setResponseStyle(QueryAnalysisResult.SINGLE_RECOMMENDATION);
        } else {
            analysis.setResponseStyle(QueryAnalysisResult.MULTI_RECOMMENDATION);
        }

        List<ChatCandidate> retrievedCandidates = retriever.retrieveWithAnalysis(message, limit, analysis);

        List<ChatCandidate> finalCandidates = new ArrayList<>();
        for (ChatCandidate c : retrievedCandidates) {
            if (c.getProductId() == null) {
                finalCandidates.add(c);
                continue;
            }

            var fullProduct = productService.findById(c.getProductId());
            if (fullProduct.isPresent() && constraintFilter.passes(fullProduct.get(), analysis)) {
                finalCandidates.add(c);
            } else {
                log.warn("product_card secondary check failed: productId={}, name={}, price={}",
                        c.getProductId(), c.getName(), c.getPrice());
            }
        }

        if (retrievedCandidates.size() != finalCandidates.size()) {
            log.warn("Secondary constraint filter removed {} candidates: {} -> {}",
                    retrievedCandidates.size() - finalCandidates.size(),
                    retrievedCandidates.size(), finalCandidates.size());
        }

        if (finalCandidates.isEmpty()) {
            analysis.setResponseStyle(QueryAnalysisResult.NO_MATCH);
        }

        List<ChatCandidate> displayCandidates = finalCandidates.stream()
                .limit(displayLimit)
                .toList();

        memoryService.updateAfterRetrieval(sessionId, message, analysis, displayCandidates);

        boolean skipProductCards = pageContext.isProductDetail()
                && pageContext.isHasValidCurrentProduct()
                && analysis.getInheritedFromPageContext() != null
                && analysis.getInheritedFromPageContext()
                && analysis.getBoostedProductIds().contains(pageContext.getCurrentProduct().getProductId());

        String responseStyle = analysis.getResponseStyle();
        String prompt = promptBuilder.build(message, displayCandidates, pageContext, responseStyle);

        log.info("Chat retrieval: displayLimit={}, finalCandidates={}, displayCandidates={}, style={}, skipCards={}",
                displayLimit, finalCandidates.size(), displayCandidates.size(), responseStyle, skipProductCards);

        SseEmitter emitter = new SseEmitter(60_000L);

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
                            if (!skipProductCards) {
                                sendProductCards(emitter, displayCandidates, analysis);
                            }
                            sendSseEvent(emitter, "done", Collections.emptyMap());
                            emitter.complete();
                        },
                        error -> {
                            sendErrorAndComplete(emitter, "LLM_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "LLM 生成失败");
                        }
                );
            } catch (Exception e) {
                log.error("Error during LLM generation", e);
                sendErrorAndComplete(emitter, "LLM_ERROR",
                        e.getMessage() != null ? e.getMessage() : "LLM 生成异常");
            }
        });

        return emitter;
    }

    private SseEmitter handleNonRetrievalIntent(RetrievalIntent intent, String message) {
        SseEmitter emitter = new SseEmitter(30_000L);

        String response = buildNonRetrievalResponse(intent, message);

        executor.submit(() -> {
            try {
                sendSseEvent(emitter, "text", Map.of("content", response));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            } catch (Exception e) {
                log.error("Error sending non-retrieval response", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String buildNonRetrievalResponse(RetrievalIntent intent, String message) {
        return switch (intent) {
            case SMALLTALK -> "你好！我是电商导购助手，可以帮你推荐护肤品、数码产品、运动装备和食品饮料等商品。有什么需要帮忙的吗？";
            case HELP -> "我可以帮你：\n1. 按类目推荐商品，如\"推荐一款洗面奶\"\n2. 按价格筛选，如\"500元以内的跑鞋\"\n3. 按品牌推荐，如\"有没有耐克的运动鞋\"\n4. 排除特定条件，如\"不要日系护肤品\"\n你想要什么类型的商品呢？";
            case THANKS -> "不客气！有问题随时问我，祝你购物愉快！";
            default -> "你好，有什么可以帮你的吗？";
        };
    }

    private void sendProductCards(SseEmitter emitter, List<ChatCandidate> displayCandidates,
                                   QueryAnalysisResult analysis) {
        if (displayCandidates.isEmpty()) {
            log.info("No displayCandidates to send as product_card");
            return;
        }

        for (ChatCandidate candidate : displayCandidates) {
            if (analysis != null && analysis.hasHardConstraints()) {
                var fullProduct = productService.findById(candidate.getProductId());
                if (fullProduct.isPresent() && !constraintFilter.passes(fullProduct.get(), analysis)) {
                    log.warn("Skipping product_card for {} ({}): failed secondary constraint check",
                            candidate.getProductId(), candidate.getName());
                    continue;
                }
            }

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
