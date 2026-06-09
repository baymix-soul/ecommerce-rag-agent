package com.ecommerce.rag.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ecommerce.rag.core.auth.AuthContextSnapshot;
import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTrace;
import com.ecommerce.rag.core.perf.PerfTraceContext;
import com.ecommerce.rag.core.perf.PerformanceTraceService;
import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.dto.ChatRequest;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.context.PageContextResolver;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.memory.ConversationMemoryService;
import com.ecommerce.rag.rag.memory.ConversationState;
import com.ecommerce.rag.rag.prompt.RagPromptBuilder;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;
import com.ecommerce.rag.rag.response.RecommendationCountResolver;
import com.ecommerce.rag.rag.retriever.HybridCandidateRetriever;
import com.ecommerce.rag.rag.retriever.NoMatchRecoveryResult;
import com.ecommerce.rag.rag.retriever.NoMatchRecoveryService;
import com.ecommerce.rag.rag.retriever.StrictProductConstraintFilter;
import com.ecommerce.rag.rag.router.RetrievalIntent;
import com.ecommerce.rag.rag.router.RetrievalRouteResult;
import com.ecommerce.rag.rag.router.RetrievalRouter;
import com.ecommerce.rag.rag.understanding.CartPlan;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingResult;
import com.ecommerce.rag.rag.understanding.QueryUnderstandingService;
import com.ecommerce.rag.services.ProductService;
import com.ecommerce.rag.services.cart.CartService;
import com.ecommerce.rag.services.cart.CartTopUpRecommendationService;
import com.ecommerce.rag.services.recommendation.RecommendationReasonService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final RecommendationReasonService reasonService;

    private static final Pattern ORDINAL_PATTERN = Pattern.compile("第([一二三四五六七八九十\\d]+)[个款]");

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
    private final CartService cartService;
    private final CartTopUpRecommendationService cartTopUpRecommendationService;
    private final PerformanceTraceService perfService;
    private final NoMatchRecoveryService noMatchRecoveryService;
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
                       QueryUnderstandingService queryUnderstandingService,
                       CartService cartService,
                       CartTopUpRecommendationService cartTopUpRecommendationService,
                       PerformanceTraceService perfService,
                       NoMatchRecoveryService noMatchRecoveryService,
                       RecommendationReasonService reasonService) {
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
        this.cartService = cartService;
        this.cartTopUpRecommendationService = cartTopUpRecommendationService;
        this.perfService = perfService;
        this.noMatchRecoveryService = noMatchRecoveryService;
        this.reasonService = reasonService;
    }

    public SseEmitter chat(ChatRequest request) {
        return chat(request, AuthContextSnapshot.unauthenticated());
    }

    public SseEmitter chat(ChatRequest request, AuthContextSnapshot authContext) {
        PerfTrace trace = perfService.beginTrace("/api/chat/stream",
                request.getSessionId(), request.getMessage());

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            SseEmitter emitter = new SseEmitter();
            sendErrorAndComplete(emitter, "INVALID_REQUEST", "message 不能为空");
            perfService.finishTraceWithError(trace, "INVALID_REQUEST");
            return emitter;
        }

        String message = request.getMessage().trim();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        int limit = request.getEffectiveLimit();

        PerfTraceContext.mark("chat.route_intent");
        RetrievalRouteResult routeResult = retrievalRouter.route(message);
        RetrievalIntent intent = routeResult.getIntent();

        log.info("Chat request: message={}, sessionId={}, intent={}, needsRetrieval={}, authenticated={}",
                message, sessionId, intent, routeResult.isNeedsRetrieval(), authContext.isAuthenticated());

        if (intent == RetrievalIntent.ADD_TO_CART) {
            PerfTraceContext.mark("chat.cart_handler");
            SseEmitter emitter = handleAddToCart(message, sessionId, request, authContext);
            perfService.finishTrace(trace);
            return emitter;
        }

        if (intent == RetrievalIntent.VIEW_CART) {
            PerfTraceContext.mark("chat.cart_handler");
            SseEmitter emitter = handleViewCart(authContext);
            perfService.finishTrace(trace);
            return emitter;
        }

        if (intent == RetrievalIntent.REMOVE_FROM_CART) {
            PerfTraceContext.mark("chat.cart_handler");
            SseEmitter emitter = handleRemoveFromCart(authContext);
            perfService.finishTrace(trace);
            return emitter;
        }

        // Cart-aware query understanding: check planner result for cart intents
        if (intent == RetrievalIntent.CART_SUMMARY || intent == RetrievalIntent.CART_TOP_UP
                || intent == RetrievalIntent.CART_COMPLETION_RECOMMEND) {
            PerfTraceContext.mark("chat.cart_handler");
            SseEmitter emitter = handleCartIntent(message, sessionId, request, authContext);
            perfService.finishTrace(trace);
            return emitter;
        }

        if (!routeResult.isNeedsRetrieval()) {
            PerfTraceContext.mark("chat.non_retrieval");
            SseEmitter emitter = handleNonRetrievalIntent(intent, message);
            perfService.finishTrace(trace);
            return emitter;
        }

        PerfTraceContext.startSpan("understanding.total");
        QueryUnderstandingResult understanding = queryUnderstandingService.understandForRetrieval(
                message, sessionId, request.getPageContext());
        PerfTraceContext.endSpan("understanding.total");

        // Check planner result for cart intents even when legacy router didn't detect them
        if (understanding.getValidatedPlan() != null
                && understanding.getValidatedPlan().getCart() != null) {
            CartPlan cartPlan = understanding.getValidatedPlan().getCart();
            String planIntent = understanding.getValidatedPlan().getIntent();
            if (("CART_SUMMARY".equals(planIntent) || "CART_TOP_UP".equals(planIntent)
                    || "CART_COMPLETION_RECOMMEND".equals(planIntent))
                    && Boolean.TRUE.equals(cartPlan.getNeedsCart())) {
                PerfTraceContext.mark("chat.cart_handler");
                SseEmitter emitter = handleCartIntentFromPlan(message, sessionId, request, authContext,
                        understanding.getValidatedPlan());
                perfService.finishTrace(trace);
                return emitter;
            }
        }

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

        PerfTraceContext.addAttribute("selected_source", selectedSource);
        PerfTraceContext.addAttribute("planner_enabled", plannerEnabled);
        PerfTraceContext.addAttribute("planner_used_for_retrieval", plannerUsed);
        PerfTraceContext.addAttribute("intent", intent != null ? intent.name() : null);

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

        PerfTraceContext.startSpan("retrieval.total");
        List<ChatCandidate> retrievedCandidates = retriever.retrieveWithAnalysis(message, limit, analysis);
        PerfTraceContext.endSpan("retrieval.total");

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

        List<ChatCandidate> displayCandidates;
        String recoveryMessage = null;
        boolean recoveryApplied = false;

        if (finalCandidates.isEmpty()) {
            // NoMatch Recovery v2
            PerfTraceContext.startSpan("retrieval.no_match_recovery");
            var understandingResult = queryUnderstandingService.understandForRetrieval(
                    message, sessionId, request.getPageContext());
            var activeContext = understandingResult.getActiveSearchContext();

            if (activeContext != null && noMatchRecoveryService != null) {
                // Build raw candidates from retriever internal result if possible; fallback to retrievedCandidates
                List<com.ecommerce.rag.rag.retriever.RetrievedProductCandidate> rawCandidates =
                        retriever.getLastRetrievedCandidates();

                NoMatchRecoveryResult recovery = noMatchRecoveryService.tryRecover(
                        analysis, activeContext, rawCandidates, displayLimit);

                if (recovery.isRecovered() && !recovery.getRelaxedCandidates().isEmpty()) {
                    displayCandidates = recovery.getRelaxedCandidates();
                    recoveryMessage = recovery.getUserMessage();
                    recoveryApplied = true;
                    analysis.setResponseStyle(QueryAnalysisResult.MULTI_RECOMMENDATION);
                    log.info("NoMatchRecovery: recovered with {} candidates, type={}",
                            displayCandidates.size(), recovery.getRecoveryType());
                } else {
                    displayCandidates = List.of();
                    recoveryMessage = recovery.getUserMessage();
                    analysis.setResponseStyle(QueryAnalysisResult.NO_MATCH);
                    log.info("NoMatchRecovery: not recovered, message={}", recoveryMessage);
                }

                // Update memory: NO_MATCH does NOT clear active context
                memoryService.updateAfterNoMatch(sessionId, analysis, recovery.isRecovered());
            } else {
                displayCandidates = List.of();
                analysis.setResponseStyle(QueryAnalysisResult.NO_MATCH);
                memoryService.updateAfterNoMatch(sessionId, analysis, false);
            }
            PerfTraceContext.endSpan("retrieval.no_match_recovery");
        } else {
            displayCandidates = finalCandidates.stream()
                    .limit(displayLimit)
                    .toList();
            memoryService.updateAfterRetrieval(sessionId, message, analysis, displayCandidates);
        }

        boolean skipProductCards = pageContext.isProductDetail()
                && pageContext.isHasValidCurrentProduct()
                && analysis.getInheritedFromPageContext() != null
                && analysis.getInheritedFromPageContext()
                && analysis.getBoostedProductIds().contains(pageContext.getCurrentProduct().getProductId());

        String responseStyle = analysis.getResponseStyle();

        PerfTraceContext.startSpan("prompt.build");
        String prompt = promptBuilder.build(message, displayCandidates, pageContext, responseStyle);
        PerfTraceContext.endSpan("prompt.build");

        log.info("Chat retrieval: displayLimit={}, finalCandidates={}, displayCandidates={}, style={}, skipCards={}, recoveryApplied={}",
                displayLimit, finalCandidates.size(), displayCandidates.size(), responseStyle, skipProductCards, recoveryApplied);

        SseEmitter emitter = new SseEmitter(60_000L);

        emitter.onCompletion(() -> log.debug("SSE emitter completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out");
            emitter.complete();
        });

        final String recoveryText = recoveryMessage;
        final boolean isRecovery = recoveryApplied;
        PerfTrace traceRef = trace;
        executor.submit(() -> {
            PerfTraceContext.set(traceRef);
            try {
                PerfTraceContext.startSpan("llm.total");
                if (recoveryText != null && !recoveryText.isBlank()) {
                    sendSseEvent(emitter, "text", Map.of("content", recoveryText));
                }
                llmClient.streamGenerate(
                        prompt,
                        textChunk -> sendSseEvent(emitter, "text", Map.of("content", textChunk)),
                        () -> {
                            PerfTraceContext.mark("sse.send_done");
                            if (!skipProductCards) {
                                sendProductCards(emitter, displayCandidates, analysis);
                            }
                            sendSseEvent(emitter, "done", Collections.emptyMap());
                            emitter.complete();
                            PerfTraceContext.endSpan("llm.total");
                            perfService.finishTrace(traceRef);
                        },
                        error -> {
                            PerfTraceContext.endSpan("llm.total");
                            perfService.finishTraceWithError(traceRef, "LLM_ERROR");
                            sendErrorAndComplete(emitter, "LLM_ERROR",
                                    error.getMessage() != null ? error.getMessage() : "LLM 生成失败");
                        }
                );
            } catch (Exception e) {
                log.error("Error during LLM generation", e);
                PerfTraceContext.endSpan("llm.total");
                perfService.finishTraceWithError(traceRef, "LLM_ERROR");
                sendErrorAndComplete(emitter, "LLM_ERROR",
                        e.getMessage() != null ? e.getMessage() : "LLM 生成异常");
            } finally {
                PerfTraceContext.clear();
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

    private SseEmitter handleAddToCart(String message, String sessionId,
                                         ChatRequest request, AuthContextSnapshot authContext) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后再加入购物车。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        String targetProductId = resolveTargetProductId(message, sessionId, request);
        if (targetProductId == null) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text",
                        Map.of("content", "请告诉我你想加购哪款商品？例如可以说\"把第一款加入购物车\"，或者在商品详情页点击\"加入购物车\"按钮。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        String userId = authContext.getUserId();
        try {
            CartItem added = cartService.addItemAndReturn(userId, targetProductId, 1);
            String responseText;
            if (added != null) {
                responseText = "已加入购物车：" + added.getName()
                        + "，¥" + String.format("%.0f", added.getPrice())
                        + "，数量 ×" + added.getQuantity();
            } else {
                responseText = "已加入购物车，商品 " + targetProductId;
            }
            log.info("Add to cart via chat: userId={}, productId={}, name={}",
                    userId, targetProductId, added != null ? added.getName() : "unknown");
            final String finalText = responseText;
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", finalText));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        } catch (Exception e) {
            log.error("Failed to add to cart via chat: {}", e.getMessage());
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "加入购物车失败：" + e.getMessage()));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        }

        return emitter;
    }

    private SseEmitter handleViewCart(AuthContextSnapshot authContext) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后查看购物车。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        try {
            String userId = authContext.getUserId();
            var cartView = cartService.getCart(userId);
            StringBuilder sb = new StringBuilder();
            sb.append("你的购物车：\n");
            if (cartView.getItems().isEmpty()) {
                sb.append("购物车是空的，试试搜索并添加你喜欢的商品吧。");
            } else {
                sb.append("共 ").append(cartView.getTotalQuantity()).append(" 件商品，合计 ¥")
                        .append(String.format("%.0f", cartView.getTotalAmount())).append("\n");
                int i = 1;
                for (var item : cartView.getItems()) {
                    sb.append(i++).append(". ").append(item.getName())
                            .append(" — ¥").append(String.format("%.0f", item.getPrice()))
                            .append(" × ").append(item.getQuantity()).append("\n");
                }
            }
            String responseText = sb.toString().trim();
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", responseText));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        } catch (Exception e) {
            log.error("Failed to view cart: {}", e.getMessage());
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "获取购物车失败，请稍后再试。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        }

        return emitter;
    }

    private SseEmitter handleRemoveFromCart(AuthContextSnapshot authContext) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后再管理购物车。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        executor.submit(() -> {
            sendSseEvent(emitter, "text",
                    Map.of("content", "购物车管理功能已就绪，你可以通过购物车页面管理已添加的商品。"));
            sendSseEvent(emitter, "done", Collections.emptyMap());
            emitter.complete();
        });

        return emitter;
    }

    private SseEmitter handleCartIntent(String message, String sessionId,
                                          ChatRequest request, AuthContextSnapshot authContext) {
        // Use legacy router intent to determine cart action
        RetrievalRouteResult routeResult = retrievalRouter.route(message);
        RetrievalIntent intent = routeResult.getIntent();

        if (intent == RetrievalIntent.CART_SUMMARY) {
            return handleCartSummary(authContext);
        }

        if (intent == RetrievalIntent.CART_TOP_UP || intent == RetrievalIntent.CART_COMPLETION_RECOMMEND) {
            // Try to parse targetAmount from the message using simple rules
            BigDecimal targetAmount = parseTargetAmount(message);
            return handleCartTopUp(authContext, targetAmount);
        }

        // Fallback: treat as cart summary
        return handleCartSummary(authContext);
    }

    private SseEmitter handleCartIntentFromPlan(String message, String sessionId,
                                                  ChatRequest request, AuthContextSnapshot authContext,
                                                  com.ecommerce.rag.rag.understanding.QueryPlan plan) {
        CartPlan cartPlan = plan.getCart();
        String action = cartPlan != null ? cartPlan.getAction() : null;

        if (CartPlan.ACTION_CART_SUMMARY.equals(action)) {
            return handleCartSummary(authContext);
        }

        if (CartPlan.ACTION_AMOUNT_GAP_QUERY.equals(action)) {
            BigDecimal targetAmount = cartPlan != null ? cartPlan.getTargetAmount() : null;
            return handleAmountGapQuery(authContext, targetAmount);
        }

        if (CartPlan.ACTION_COMPLETION_RECOMMEND.equals(action)) {
            BigDecimal targetAmount = cartPlan != null ? cartPlan.getTargetAmount() : null;
            if (Boolean.TRUE.equals(plan.getNeedsClarification()) || targetAmount == null) {
                return handleCartTopUp(authContext, null);
            }
            return handleCartTopUp(authContext, targetAmount);
        }

        if (CartPlan.ACTION_ADD_TO_CART.equals(action)) {
            return handleAddToCart(message, sessionId, request, authContext);
        }

        return handleCartSummary(authContext);
    }

    private SseEmitter handleCartSummary(AuthContextSnapshot authContext) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后查看购物车。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        try {
            String userId = authContext.getUserId();
            var cartView = cartService.getCart(userId);
            String responseText;
            if (cartView.getItems().isEmpty()) {
                responseText = "当前购物车还是空的。";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("当前购物车共有 ").append(cartView.getTotalQuantity())
                        .append(" 件商品，合计 ¥")
                        .append(String.format("%.0f", cartView.getTotalAmount()))
                        .append("。已加入：");
                for (int i = 0; i < cartView.getItems().size(); i++) {
                    var item = cartView.getItems().get(i);
                    if (i > 0) sb.append("、");
                    sb.append(item.getName()).append(" ×").append(item.getQuantity());
                }
                sb.append("。");
                responseText = sb.toString();
            }
            final String text = responseText;
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", text));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        } catch (Exception e) {
            log.error("Failed to handle cart summary: {}", e.getMessage());
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "获取购物车信息失败，请稍后再试。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        }

        return emitter;
    }

    private SseEmitter handleAmountGapQuery(AuthContextSnapshot authContext, BigDecimal targetAmount) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后查看购物车差额。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        if (targetAmount == null) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text",
                        Map.of("content", "你想知道距离多少元还差多少？例如可以说\"离2000还差多少\"。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        try {
            String userId = authContext.getUserId();
            var cartView = cartService.getCart(userId);
            BigDecimal currentAmount = cartView.getTotalAmount();
            BigDecimal gap = targetAmount.subtract(currentAmount);

            String text;
            if (gap.compareTo(BigDecimal.ZERO) > 0) {
                text = "当前购物车合计 ¥" + String.format("%.0f", currentAmount)
                        + "，距离 ¥" + String.format("%.0f", targetAmount)
                        + " 还差 ¥" + String.format("%.0f", gap) + "。";
            } else {
                text = "当前购物车合计 ¥" + String.format("%.0f", currentAmount)
                        + "，已经达到 ¥" + String.format("%.0f", targetAmount) + "。";
            }
            final String finalText = text;
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", finalText));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        } catch (Exception e) {
            log.error("Failed to handle amount gap query: {}", e.getMessage());
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "获取购物车信息失败，请稍后再试。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        }

        return emitter;
    }

    private SseEmitter handleCartTopUp(AuthContextSnapshot authContext, BigDecimal targetAmount) {
        SseEmitter emitter = new SseEmitter(15_000L);

        if (!authContext.isAuthenticated()) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "请先登录后使用凑单推荐。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        if (targetAmount == null) {
            executor.submit(() -> {
                sendSseEvent(emitter, "text",
                        Map.of("content", "你想凑到多少元？例如可以说\"帮我凑到1000元\"。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
            return emitter;
        }

        try {
            String userId = authContext.getUserId();
            var cartView = cartService.getCart(userId);
            BigDecimal currentAmount = cartView.getTotalAmount();
            BigDecimal gap = targetAmount.subtract(currentAmount);

            if (gap.compareTo(BigDecimal.ZERO) <= 0) {
                String text = "当前购物车合计 ¥" + String.format("%.0f", currentAmount)
                        + "，已经达到 ¥" + String.format("%.0f", targetAmount) + "，不需要再凑单。";
                executor.submit(() -> {
                    sendSseEvent(emitter, "text", Map.of("content", text));
                    sendSseEvent(emitter, "done", Collections.emptyMap());
                    emitter.complete();
                });
                return emitter;
            }

            List<ProductCard> recommendations = cartTopUpRecommendationService.recommend(
                    cartView, targetAmount, 3);

            String text = "当前购物车合计 ¥" + String.format("%.0f", currentAmount)
                    + "，距离 ¥" + String.format("%.0f", targetAmount)
                    + " 还差 ¥" + String.format("%.0f", gap) + "。";
            if (recommendations.isEmpty()) {
                text += "暂时没有找到适合凑单的商品。";
            } else {
                text += "我给你找了几款适合凑单的商品。";
            }

            final String finalText = text;
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", finalText));
                for (ProductCard card : recommendations) {
                    sendSseEvent(emitter, "product_card", card);
                }
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        } catch (Exception e) {
            log.error("Failed to handle cart top-up: {}", e.getMessage());
            executor.submit(() -> {
                sendSseEvent(emitter, "text", Map.of("content", "凑单推荐失败，请稍后再试。"));
                sendSseEvent(emitter, "done", Collections.emptyMap());
                emitter.complete();
            });
        }

        return emitter;
    }

    private BigDecimal parseTargetAmount(String message) {
        // Try to parse obvious number patterns from the message
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?:凑到|到|满|还差|达到)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?").matcher(message);
        if (m.find()) {
            try { return new BigDecimal(m.group(1)); } catch (NumberFormatException ignored) {}
        }

        // Chinese number patterns
        if (message.contains("一千")) return new BigDecimal("1000");
        if (message.contains("两千")) return new BigDecimal("2000");
        if (message.contains("三千")) return new BigDecimal("3000");
        if (message.contains("五千")) return new BigDecimal("5000");
        if (message.contains("一万")) return new BigDecimal("10000");
        if (message.contains("两万")) return new BigDecimal("20000");

        // Standalone number with unit
        m = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:元|块)").matcher(message);
        if (m.find()) {
            try { return new BigDecimal(m.group(1)); } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    private String resolveTargetProductId(String message, String sessionId, ChatRequest request) {
        if (request.getPageContext() != null
                && request.getPageContext().getCurrentProductId() != null
                && !request.getPageContext().getCurrentProductId().isBlank()) {
            return request.getPageContext().getCurrentProductId();
        }

        Matcher ordinalMatcher = ORDINAL_PATTERN.matcher(message);
        if (ordinalMatcher.find()) {
            int index = parseOrdinal(ordinalMatcher.group(1)) - 1;
            ConversationState state = memoryService.getOrCreate(sessionId);
            List<String> recommendedIds = state.getRecommendedProductIds();
            if (index >= 0 && recommendedIds != null && index < recommendedIds.size()) {
                return recommendedIds.get(index);
            }
        }

        if (message.contains("这个") || message.contains("这款") || message.contains("它")) {
            ConversationState state = memoryService.getOrCreate(sessionId);
            List<String> recommendedIds = state.getRecommendedProductIds();
            if (recommendedIds != null && !recommendedIds.isEmpty()) {
                return recommendedIds.get(0);
            }
        }

        ConversationState state = memoryService.getOrCreate(sessionId);
        List<String> recommendedIds = state.getRecommendedProductIds();
        if (recommendedIds != null && !recommendedIds.isEmpty()) {
            for (String id : recommendedIds) {
                var product = productService.findById(id);
                if (product.isPresent() && message.contains(product.get().getName().substring(0,
                        Math.min(4, product.get().getName().length())))) {
                    return id;
                }
            }
        }

        return null;
    }

    private int parseOrdinal(String chineseNumber) {
        try {
            return Integer.parseInt(chineseNumber);
        } catch (NumberFormatException e) {
            return switch (chineseNumber) {
                case "一" -> 1;
                case "二" -> 2;
                case "三" -> 3;
                case "四" -> 4;
                case "五" -> 5;
                case "六" -> 6;
                case "七" -> 7;
                case "八" -> 8;
                case "九" -> 9;
                case "十" -> 10;
                default -> -1;
            };
        }
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

            String reason = generateReasonForCandidate(candidate, analysis);
            ProductCard card = new ProductCard(
                    candidate.getProductId(),
                    candidate.getName(),
                    candidate.getPrice(),
                    candidate.getCurrency(),
                    candidate.getImageUrl(),
                    reason
            );
            sendSseEvent(emitter, "product_card", card);
        }
    }

    private String generateReasonForCandidate(ChatCandidate candidate, QueryAnalysisResult analysis) {
        if (reasonService == null) {
            return "符合当前检索条件，可优先查看。";
        }
        var productOpt = productService.findById(candidate.getProductId());
        if (productOpt.isPresent()) {
            return reasonService.generateReason(candidate, productOpt.get(), analysis);
        }
        return "符合当前检索条件，可优先查看。";
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
