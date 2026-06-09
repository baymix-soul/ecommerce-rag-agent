package com.ecommerce.rag.rag.retriever;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.memory.ActiveSearchContext;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.services.ProductService;

@Service
public class NoMatchRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(NoMatchRecoveryService.class);

    public static final String RECOVERY_TYPE_RELAX_SOFT = "RELAX_SOFT_PREFERENCES";
    public static final String RECOVERY_TYPE_SUGGESTION = "SUGGESTION_ONLY";

    private final ProductService productService;
    private final StrictProductConstraintFilter constraintFilter;

    public NoMatchRecoveryService(ProductService productService,
                                   StrictProductConstraintFilter constraintFilter) {
        this.productService = productService;
        this.constraintFilter = constraintFilter;
    }

    public NoMatchRecoveryResult tryRecover(QueryAnalysisResult exactAnalysis,
                                             ActiveSearchContext activeContext,
                                             List<RetrievedProductCandidate> rawCandidates,
                                             int limit) {
        if (exactAnalysis == null) {
            return NoMatchRecoveryResult.notRecovered("无法恢复：缺少查询分析结果");
        }

        if (activeContext == null) {
            return NoMatchRecoveryResult.notRecovered("无法恢复：缺少会话上下文");
        }

        log.info("NoMatchRecovery: attempting recovery for category={}, subCategory={}, maxPrice={}",
                exactAnalysis.getCategory(), exactAnalysis.getSubCategory(), exactAnalysis.getMaxPrice());

        // Level 1: 放宽 soft preferences，保留 HARD + EXCLUSION
        QueryAnalysisResult relaxedAnalysis = buildHardOnlyAnalysis(activeContext);
        relaxedAnalysis.setOriginalQuery(exactAnalysis.getOriginalQuery());
        relaxedAnalysis.setNormalizedQuery(exactAnalysis.getNormalizedQuery());
        relaxedAnalysis.setResolvedQuery(exactAnalysis.getResolvedQuery());
        relaxedAnalysis.setIntent(exactAnalysis.getIntent());
        relaxedAnalysis.setResponseStyle(exactAnalysis.getResponseStyle());
        relaxedAnalysis.setRequestedProductCount(exactAnalysis.getRequestedProductCount());

        List<RetrievedProductCandidate> hardOnlyCandidates = filterWithAnalysis(rawCandidates, relaxedAnalysis);
        if (!hardOnlyCandidates.isEmpty()) {
            List<ChatCandidate> chatCandidates = toChatCandidates(hardOnlyCandidates, limit);
            List<String> relaxed = activeContext.getSoftPreferences();
            String userMessage = buildRelaxMessage(activeContext, relaxed);
            log.info("NoMatchRecovery: recovered with RELAX_SOFT_PREFERENCES, candidates={}", chatCandidates.size());
            return NoMatchRecoveryResult.recovered(RECOVERY_TYPE_RELAX_SOFT, chatCandidates, userMessage, relaxed);
        }

        // Level 2: 只给建议，不发卡片（查找最接近的价格）
        String suggestion = buildPriceSuggestion(rawCandidates, exactAnalysis);
        log.info("NoMatchRecovery: no hard-only candidates, returning suggestion");
        return NoMatchRecoveryResult.notRecovered(suggestion);
    }

    private QueryAnalysisResult buildHardOnlyAnalysis(ActiveSearchContext ctx) {
        QueryAnalysisResult result = new QueryAnalysisResult();
        result.setCategory(ctx.getCategory());
        result.setSubCategory(ctx.getSubCategory());
        result.setSubCategories(new ArrayList<>(ctx.getSubCategories()));
        result.setMinPrice(ctx.getMinPrice());
        result.setMaxPrice(ctx.getMaxPrice());
        result.setNegativeKeywords(new ArrayList<>(ctx.getNegativeKeywords()));
        result.setNegativeBrands(new ArrayList<>(ctx.getNegativeBrands()));
        result.setExcludeProductIds(new ArrayList<>(ctx.getExcludeProductIds()));
        result.setPositiveKeywords(new ArrayList<>(ctx.getPositiveKeywords()));
        result.setSoftKeywords(new ArrayList<>());
        return result;
    }

    private List<RetrievedProductCandidate> filterWithAnalysis(
            List<RetrievedProductCandidate> candidates, QueryAnalysisResult analysis) {
        List<RetrievedProductCandidate> filtered = new ArrayList<>();
        for (RetrievedProductCandidate c : candidates) {
            if (c.getProduct() == null) continue;
            if (constraintFilter.passes(c.getProduct(), analysis)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    private List<ChatCandidate> toChatCandidates(List<RetrievedProductCandidate> candidates, int limit) {
        List<ChatCandidate> result = new ArrayList<>();
        int count = Math.min(candidates.size(), limit);
        for (int i = 0; i < count; i++) {
            RetrievedProductCandidate c = candidates.get(i);
            Product p = c.getProduct();
            ChatCandidate cc = new ChatCandidate();
            cc.setProductId(p.getProductId());
            cc.setName(p.getName());
            cc.setPrice(p.getPrice());
            cc.setImageUrl(p.getImageUrl());
            cc.setBrand(p.getBrand());
            cc.setCategory(p.getCategory());
            cc.setSubCategory(p.getSubCategory());
            cc.setScore(c.getFinalScore());
            result.add(cc);
        }
        return result;
    }

    private String buildRelaxMessage(ActiveSearchContext ctx, List<String> relaxed) {
        StringBuilder sb = new StringBuilder();
        sb.append("同时满足");
        if (!relaxed.isEmpty()) {
            sb.append("'").append(String.join("'、'", relaxed)).append("'");
        }
        sb.append("和您的其他条件的商品暂时没有。");
        sb.append("我先给您筛选了几款符合核心条件的商品，");
        if (!relaxed.isEmpty()) {
            sb.append("'").append(relaxed.get(0)).append("'");
            sb.append("属性可能没有上一轮那么强。");
        } else {
            sb.append("部分偏好条件已放宽。");
        }
        return sb.toString();
    }

    private String buildPriceSuggestion(List<RetrievedProductCandidate> rawCandidates,
                                        QueryAnalysisResult exactAnalysis) {
        BigDecimal maxPrice = exactAnalysis.getMaxPrice();
        BigDecimal minPrice = exactAnalysis.getMinPrice();
        String category = exactAnalysis.getSubCategory() != null ? exactAnalysis.getSubCategory() : exactAnalysis.getCategory();

        if (category == null) {
            category = "商品";
        }

        // 查找最接近的价格边界
        BigDecimal closestAbove = null;
        BigDecimal closestBelow = null;

        for (RetrievedProductCandidate c : rawCandidates) {
            Product p = c.getProduct();
            if (p == null || p.getPrice() == null) continue;
            BigDecimal price = p.getPrice();

            if (maxPrice != null && price.compareTo(maxPrice) > 0) {
                if (closestAbove == null || price.compareTo(closestAbove) < 0) {
                    closestAbove = price;
                }
            }
            if (minPrice != null && price.compareTo(minPrice) < 0) {
                if (closestBelow == null || price.compareTo(closestBelow) > 0) {
                    closestBelow = price;
                }
            }
        }

        if (maxPrice != null && closestAbove != null) {
            return String.format("当前没有%d元以下的%s，最接近的是¥%s。您可以考虑把预算放宽到%s元左右，或者调整其他筛选条件。",
                    maxPrice.intValue(), category, closestAbove.toPlainString(), closestAbove.toPlainString());
        }

        if (minPrice != null && closestBelow != null) {
            return String.format("当前没有%d元以上的%s，最接近的是¥%s。您可以考虑降低预算要求。",
                    minPrice.intValue(), category, closestBelow.toPlainString());
        }

        return String.format("抱歉，当前商品库中暂无符合条件的%s。您可以尝试放宽预算、更换品类或调整筛选条件。", category);
    }
}
