package com.ecommerce.rag.services.recommendation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

/**
 * 确定性推荐理由生成服务。
 * 不调用 LLM，基于 softKeywords / positiveKeywords / 商品真实字段 / 价格约束生成一句话推荐理由。
 */
@Service
public class RecommendationReasonService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationReasonService.class);

    private static final int MAX_REASON_LENGTH = 60;
    private static final int MAX_KEYWORDS_IN_REASON = 2;

    private static final String PLACEHOLDER_LEGACY = "该商品来自当前商品库候选结果，具体推荐理由将在后续 LLM 阶段生成。";
    private static final String PLACEHOLDER_LLM = "由LLM推荐";

    /**
     * 为候选商品生成推荐理由。
     *
     * @param candidate 候选商品（含 productId、name 等）
     * @param product   完整商品实体（用于匹配真实字段）
     * @param analysis  查询分析结果（含 softKeywords、maxPrice 等）
     * @return 一句话推荐理由，长度不超过 60 字
     */
    public String generateReason(ChatCandidate candidate, Product product, QueryAnalysisResult analysis) {
        if (candidate == null || product == null) {
            return "符合当前检索条件，可优先查看。";
        }

        String reason = doGenerateReason(product, analysis);
        log.debug("Generated reason for {}: {}", product.getProductId(), reason);
        return reason;
    }

    /**
     * 为凑单推荐生成推荐理由。
     *
     * @param product     商品实体
     * @param gap         距离目标金额的差额
     * @param targetAmount 目标金额
     * @return 凑单推荐理由
     */
    public String generateTopUpReason(Product product, BigDecimal gap, BigDecimal targetAmount) {
        if (product == null || gap == null) {
            return "适合凑单";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("价格接近还差金额，适合凑单。");
        return sb.toString();
    }

    private String doGenerateReason(Product product, QueryAnalysisResult analysis) {
        // 1. 收集关键词来源
        List<String> keywords = collectKeywords(analysis);

        // 2. 构建商品可搜索文本
        String searchableText = buildSearchableText(product);

        // 3. 匹配关键词
        List<String> matched = keywords.stream()
                .filter(kw -> searchableText.contains(kw))
                .limit(MAX_KEYWORDS_IN_REASON)
                .collect(Collectors.toList());

        // 4. 价格约束信息
        boolean priceSatisfied = isPriceSatisfied(product, analysis);

        // 5. 按规则组装 reason
        if (!matched.isEmpty()) {
            String keywordPart = String.join("、", matched);
            if (priceSatisfied && analysis != null && analysis.getMaxPrice() != null) {
                return String.format("匹配「%s」需求，同时满足 ¥%s 以下预算。",
                        keywordPart, analysis.getMaxPrice().toPlainString());
            }
            return String.format("匹配「%s」需求，适合%s。",
                    keywordPart, inferScenario(analysis));
        }

        // 无 softKeywords 命中时的 fallback
        if (analysis != null && analysis.getSubCategory() != null && !analysis.getSubCategory().isBlank()) {
            if (priceSatisfied && analysis.getMaxPrice() != null) {
                return String.format("符合%s筛选，同时满足 ¥%s 以下预算。",
                        analysis.getSubCategory(), analysis.getMaxPrice().toPlainString());
            }
            return String.format("符合你当前筛选的「%s」需求。", analysis.getSubCategory());
        }

        if (analysis != null && analysis.getCategory() != null && !analysis.getCategory().isBlank()
                && analysis.getMaxPrice() != null) {
            return String.format("符合「%s」类目，并满足预算条件。", analysis.getCategory());
        }

        if (product.getBrand() != null && !product.getBrand().isBlank()) {
            return String.format("来自「%s」，符合当前筛选条件。", product.getBrand());
        }

        return "符合当前检索条件，可优先查看。";
    }

    private List<String> collectKeywords(QueryAnalysisResult analysis) {
        Set<String> keywords = new LinkedHashSet<>();
        if (analysis == null) {
            return new ArrayList<>();
        }

        if (analysis.getSoftKeywords() != null) {
            for (String kw : analysis.getSoftKeywords()) {
                if (kw != null && !kw.isBlank() && kw.length() <= 10) {
                    keywords.add(kw.trim());
                }
            }
        }

        if (analysis.getPositiveKeywords() != null) {
            for (String kw : analysis.getPositiveKeywords()) {
                if (kw != null && !kw.isBlank() && kw.length() <= 10) {
                    keywords.add(kw.trim());
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private String buildSearchableText(Product product) {
        StringBuilder sb = new StringBuilder();
        appendIfNotBlank(sb, product.getName());
        appendIfNotBlank(sb, product.getBrand());
        appendIfNotBlank(sb, product.getCategory());
        appendIfNotBlank(sb, product.getSubCategory());
        appendIfNotBlank(sb, product.getDescription());
        appendIfNotBlank(sb, product.getReviewSummary());
        appendIfNotBlank(sb, product.getMarketingCopy());
        if (product.getSpecs() != null) {
            for (String value : product.getSpecs().values()) {
                appendIfNotBlank(sb, value);
            }
        }
        return sb.toString();
    }

    private void appendIfNotBlank(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(" ");
        }
    }

    private boolean isPriceSatisfied(Product product, QueryAnalysisResult analysis) {
        if (analysis == null || product == null || product.getPrice() == null) {
            return false;
        }
        if (analysis.getMaxPrice() != null
                && product.getPrice().compareTo(analysis.getMaxPrice()) > 0) {
            return false;
        }
        if (analysis.getMinPrice() != null
                && product.getPrice().compareTo(analysis.getMinPrice()) < 0) {
            return false;
        }
        return true;
    }

    private String inferScenario(QueryAnalysisResult analysis) {
        if (analysis == null) {
            return "日常使用";
        }
        String sub = analysis.getSubCategory();
        if (sub != null) {
            if (sub.contains("跑") || sub.contains("鞋")) {
                return "跑步训练";
            }
            if (sub.contains("电脑") || sub.contains("笔记本")) {
                return "编程开发";
            }
            if (sub.contains("洁面") || sub.contains("洗面") || sub.contains("精华")) {
                return "日常护肤";
            }
            if (sub.contains("耳机")) {
                return "日常聆听";
            }
            return sub.replace(" ", "");
        }
        String cat = analysis.getCategory();
        if (cat != null) {
            return cat.replace(" ", "");
        }
        return "日常使用";
    }

    /**
     * 检查 reason 是否为占位文本。
     */
    public boolean isPlaceholder(String reason) {
        if (reason == null || reason.isBlank()) {
            return true;
        }
        String trimmed = reason.trim();
        return trimmed.equals(PLACEHOLDER_LEGACY)
                || trimmed.equals(PLACEHOLDER_LLM)
                || trimmed.contains("由LLM推荐");
    }
}
