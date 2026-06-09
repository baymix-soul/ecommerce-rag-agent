package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.rag.memory.ConversationState;
import org.springframework.stereotype.Component;

@Component
public class CartSemanticFrameMatcher {

    private final CartSemanticFrameCatalog catalog;
    private final CartSemanticHintService hintService;

    private static final Pattern AMOUNT_GAP_PATTERN = Pattern.compile(
            "(离|距离|到|满|差)(\\d+)\\s*(?:元|块)?\\s*(还差)?");

    private static final Pattern AMOUNT_GAP_REVERSE_PATTERN = Pattern.compile(
            "(还差|差)\\s*多少\\s*(到|离|距离)\\s*(\\d+)"
    );

    private static final Pattern AMOUNT_GAP_REVERSE_CN_PATTERN = Pattern.compile(
            "(还差|差)\\s*多少\\s*(到|离|距离)\\s*([一两三四五六七八九])(千|万)"
    );

    private static final Pattern COMPLETION_REC_PATTERN = Pattern.compile(
            "(凑|满)\\s*(到\\s*)?(\\d+)\\s*(?:元|块)?\\s*(推荐|商品|什么|可以|东西|加)"
    );

    private static final Pattern COMPLETION_REC_ALT_PATTERN = Pattern.compile(
            "(帮我凑|凑到|要凑|想凑)\\D*(\\d+)");

    private static final Pattern COMPLETION_REC_CN_PATTERN = Pattern.compile(
            "(凑到|凑|要凑|想凑)\\s*([一两三四五六七八九千千万]+)(?:元|块)?\\s*(推荐|商品|东西|可以|有什么)"
    );

    private static final Pattern TARGET_AMOUNT_PATTERN = Pattern.compile(
            "(离|距离|到|满|凑到|凑|还差|差)\\s*(\\d+)\\s*(?:元|块)?"
    );

    private static final Pattern STANDALONE_AMOUNT_PATTERN = Pattern.compile(
            "(\\d+)\\s*(?:元|块)"
    );

    private static final Pattern CHINESE_AMOUNT_PATTERN = Pattern.compile(
            "([一两三四五六七八九])(千|万)(?:元|块)?"
    );

    private static final Pattern ANTI_PATTERN_BUDGET = Pattern.compile(
            "(推荐|预算|找|选)\\s*(\\d+)\\s*(?:元|块)?\\s*(?:以内的|以下|左右|价位的)?\\s*(的)?\\s*(电脑|手机|笔记本|耳机|相机|电视|冰箱|洗衣机|空调)"
    );

    private static final Pattern ANTI_PATTERN_PRICE_FILTER = Pattern.compile(
            "推荐\\s*(.+?)\\s*(\\d+)\\s*(?:元|块)?\\s*(?:以内|以下)"
    );

    private static final Pattern ANTI_PATTERN_BUDGET_BUY = Pattern.compile(
            "预算\\s*(\\d+)\\s*(?:元|块)?\\s*(买|购|找|选)"
    );

    public CartSemanticFrameMatcher(CartSemanticFrameCatalog catalog, CartSemanticHintService hintService) {
        this.catalog = catalog;
        this.hintService = hintService;
    }

    public CartSemanticMatchResult match(String query, PageContext pageContext, ConversationState state) {
        if (query == null || query.isBlank()) {
            return CartSemanticMatchResult.notCartRelated();
        }

        if (isAntiPattern(query)) {
            return CartSemanticMatchResult.notCartRelated();
        }

        CartSemanticHint hint = hintService != null ? hintService.analyze(query) : CartSemanticHint.notCartRelated();
        boolean structurallyCart = isStructurallyCartRelated(query);

        if (!structurallyCart && (hintService == null || !hint.isCartRelated())) {
            return CartSemanticMatchResult.notCartRelated();
        }

        List<CartSemanticFrame> matchedFrames = new ArrayList<>();
        List<String> matchedPatterns = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (CartSemanticFrame frame : catalog.getAllFrames()) {
            if (matchesNegativeExamples(query, frame)) {
                continue;
            }

            boolean matched = matchesPositiveExamples(query, frame) || matchesStructuralPattern(query, frame);
            if (matched) {
                matchedFrames.add(frame);
                matchedPatterns.add(frame.getFrameId());
            }
        }

        if (matchedFrames.isEmpty()) {
            return CartSemanticMatchResult.notCartRelated();
        }

        CartSemanticMatchResult result = new CartSemanticMatchResult();
        result.setCartRelated(true);
        result.setMatchedPatterns(matchedPatterns);

        List<String> candidateIds = matchedFrames.stream()
                .map(CartSemanticFrame::getFrameId)
                .toList();
        result.setCandidateFrameIds(new ArrayList<>(candidateIds));

        if (matchedFrames.size() == 1) {
            CartSemanticFrame frame = matchedFrames.get(0);
            result.setMatchedFrameId(frame.getFrameId());

            boolean hasTargetSlot = frame.hasRequiredSlot("target_amount");
            BigDecimal targetAmount = null;
            List<String> missingSlots = new ArrayList<>();

            if (hasTargetSlot) {
                targetAmount = parseTargetAmount(query);
                if (targetAmount != null) {
                    result.setRuleParsedTargetAmount(targetAmount);
                    result.setNormalizedAmountText(String.valueOf(targetAmount.longValue()));
                } else {
                    missingSlots.add("target_amount");
                }
            }

            result.setMissingSlots(missingSlots);

            if (missingSlots.isEmpty()) {
                result.setMatchLevel(CartSemanticMatchResult.LEVEL_EXACT);
                result.setRuleConfidence(0.95);
            } else {
                result.setMatchLevel(CartSemanticMatchResult.LEVEL_PARTIAL);
                result.setRuleConfidence(targetAmount != null ? 0.80 : 0.60);
            }
        } else {
            if (matchedFrames.size() == 2) {
                boolean hasClarify = matchedFrames.stream()
                        .anyMatch(f -> "cart.completion_clarify".equals(f.getFrameId()));
                boolean hasRec = matchedFrames.stream()
                        .anyMatch(f -> "cart.completion_recommend".equals(f.getFrameId()));

                if (hasClarify && hasRec) {
                    BigDecimal targetAmount = parseTargetAmount(query);
                    if (targetAmount != null) {
                        CartSemanticFrame recFrame = matchedFrames.stream()
                                .filter(f -> "cart.completion_recommend".equals(f.getFrameId()))
                                .findFirst()
                                .orElse(null);
                        result.setMatchedFrameId("cart.completion_recommend");
                        result.setRuleParsedTargetAmount(targetAmount);
                        result.setNormalizedAmountText(String.valueOf(targetAmount.longValue()));
                        result.setMatchLevel(CartSemanticMatchResult.LEVEL_EXACT);
                        result.setRuleConfidence(0.95);
                    } else {
                        CartSemanticFrame clarifyFrame = matchedFrames.stream()
                                .filter(f -> "cart.completion_clarify".equals(f.getFrameId()))
                                .findFirst()
                                .orElse(null);
                        result.setMatchedFrameId("cart.completion_clarify");
                        result.setMissingSlots(List.of("target_amount"));
                        result.setMatchLevel(CartSemanticMatchResult.LEVEL_PARTIAL);
                        result.setRuleConfidence(0.60);
                    }
                } else {
                    BigDecimal targetAmount = parseTargetAmount(query);
                    if (targetAmount != null) {
                        result.setRuleParsedTargetAmount(targetAmount);
                        result.setNormalizedAmountText(String.valueOf(targetAmount.longValue()));
                    }
                    result.setMatchLevel(CartSemanticMatchResult.LEVEL_PARTIAL);
                    result.setRuleConfidence(targetAmount != null ? 0.80 : 0.60);
                    warnings.add("Multiple candidate frames: " + candidateIds);
                }
            } else {
                BigDecimal targetAmount = parseTargetAmount(query);
                if (targetAmount != null) {
                    result.setRuleParsedTargetAmount(targetAmount);
                    result.setNormalizedAmountText(String.valueOf(targetAmount.longValue()));
                }
                result.setMatchLevel(CartSemanticMatchResult.LEVEL_PARTIAL);
                result.setRuleConfidence(targetAmount != null ? 0.80 : 0.60);
                warnings.add("Multiple candidate frames: " + candidateIds);
            }
        }

        result.setWarnings(warnings);
        return result;
    }

    private boolean isAntiPattern(String query) {
        if (ANTI_PATTERN_BUDGET.matcher(query).find()) {
            return true;
        }
        if (ANTI_PATTERN_PRICE_FILTER.matcher(query).find()) {
            return true;
        }
        if (ANTI_PATTERN_BUDGET_BUY.matcher(query).find()) {
            return true;
        }
        return false;
    }

    private boolean isStructurallyCartRelated(String query) {
        if (query.contains("购物车")) {
            return true;
        }
        if (query.contains("已经买了") || query.contains("已经加购")) {
            return true;
        }
        if ((query.contains("合计") || query.contains("总价") || query.contains("花了"))
                && (query.contains("多少") || query.contains("钱"))) {
            return true;
        }
        if (query.contains("凑单") || query.contains("帮我凑") || query.contains("凑到")
                || query.contains("要凑") || query.contains("想凑")) {
            return true;
        }
        if (AMOUNT_GAP_PATTERN.matcher(query).find()) {
            return true;
        }
        if (AMOUNT_GAP_REVERSE_PATTERN.matcher(query).find()) {
            return true;
        }
        if (AMOUNT_GAP_REVERSE_CN_PATTERN.matcher(query).find()) {
            return true;
        }
        if (query.contains("还差多少") || query.contains("差多少")) {
            return true;
        }
        if (query.contains("加入购物车") || query.contains("加购") || query.contains("买这个")) {
            return true;
        }
        return false;
    }

    private boolean matchesNegativeExamples(String query, CartSemanticFrame frame) {
        for (String negative : frame.getNegativeExamples()) {
            if (containsMatch(query, negative)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPositiveExamples(String query, CartSemanticFrame frame) {
        for (String positive : frame.getPositiveExamples()) {
            if (containsMatch(query, positive)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMatch(String query, String example) {
        return query.contains(example) || example.contains(query);
    }

    private boolean matchesStructuralPattern(String query, CartSemanticFrame frame) {
        String frameId = frame.getFrameId();

        return switch (frameId) {
            case "cart.summary" -> matchesCartSummaryPattern(query);
            case "cart.amount_gap_query" -> matchesAmountGapQueryPattern(query);
            case "cart.completion_recommend" -> matchesCompletionRecommendPattern(query);
            case "cart.completion_clarify" -> matchesCompletionClarifyPattern(query);
            case "cart.add_item" -> matchesAddItemPattern(query);
            default -> false;
        };
    }

    private boolean matchesCartSummaryPattern(String query) {
        if (query.contains("购物车") && (query.contains("钱") || query.contains("价格")
                || query.contains("合计") || query.contains("总") || query.contains("多少"))) {
            return true;
        }
        if (query.contains("已经买了") && (query.contains("多少") || query.contains("钱"))) {
            return true;
        }
        if ((query.contains("合计") || query.contains("总价") || query.contains("花了"))
                && (query.contains("多少") || query.contains("钱"))) {
            return true;
        }
        if (query.contains("已经加购")) {
            return true;
        }
        if (query.contains("购物车里有什么") || query.contains("购物车里有")) {
            return true;
        }
        return false;
    }

    private boolean matchesAmountGapQueryPattern(String query) {
        if (AMOUNT_GAP_PATTERN.matcher(query).find()) {
            return true;
        }
        if (AMOUNT_GAP_REVERSE_PATTERN.matcher(query).find()) {
            return true;
        }
        if (AMOUNT_GAP_REVERSE_CN_PATTERN.matcher(query).find()) {
            return true;
        }
        if (query.contains("还差多少到") || query.contains("差多少到")) {
            return true;
        }
        return false;
    }

    private boolean matchesCompletionRecommendPattern(String query) {
        if (COMPLETION_REC_PATTERN.matcher(query).find()) {
            return true;
        }
        if (COMPLETION_REC_ALT_PATTERN.matcher(query).find()) {
            return true;
        }
        if (COMPLETION_REC_CN_PATTERN.matcher(query).find()) {
            return true;
        }
        boolean hasNumber = Pattern.compile("\\d+").matcher(query).find();
        boolean hasCnAmount = CHINESE_AMOUNT_PATTERN.matcher(query).find();
        boolean hasCompletion = query.contains("凑") || query.contains("凑单") || query.contains("满");
        boolean hasRecommend = query.contains("推荐") || query.contains("商品") || query.contains("什么")
                || query.contains("可以") || query.contains("东西") || query.contains("加");
        if (hasCompletion && hasRecommend && (hasNumber || hasCnAmount)) {
            return true;
        }
        return false;
    }

    private boolean matchesCompletionClarifyPattern(String query) {
        boolean hasNumber = Pattern.compile("\\d+").matcher(query).find();
        boolean hasCnAmount = CHINESE_AMOUNT_PATTERN.matcher(query).find();
        if (hasNumber || hasCnAmount) {
            return false;
        }
        if (query.contains("凑单推荐") || query.contains("帮我凑单") || query.contains("适合凑单")) {
            return true;
        }
        if (query.contains("凑单") && (query.contains("推荐") || query.contains("什么") || query.contains("可以"))) {
            return true;
        }
        return false;
    }

    private boolean matchesAddItemPattern(String query) {
        if (query.contains("加入购物车") || query.contains("加到购物车")) {
            return true;
        }
        if (query.contains("加购") && !query.contains("凑单")) {
            return true;
        }
        if (query.contains("买这个") || query.contains("买这个商品")) {
            return true;
        }
        return false;
    }

    private BigDecimal parseTargetAmount(String query) {
        BigDecimal chineseAmount = parseChineseAmount(query);
        if (chineseAmount != null) {
            return chineseAmount;
        }

        Matcher tmatcher = TARGET_AMOUNT_PATTERN.matcher(query);
        if (tmatcher.find()) {
            return new BigDecimal(tmatcher.group(2));
        }

        Matcher reverseMatcher = AMOUNT_GAP_REVERSE_PATTERN.matcher(query);
        if (reverseMatcher.find()) {
            return new BigDecimal(reverseMatcher.group(3));
        }

        Matcher compMatcher = COMPLETION_REC_PATTERN.matcher(query);
        if (compMatcher.find()) {
            return new BigDecimal(compMatcher.group(3));
        }

        Matcher compAltMatcher = COMPLETION_REC_ALT_PATTERN.matcher(query);
        if (compAltMatcher.find()) {
            return new BigDecimal(compAltMatcher.group(2));
        }

        Matcher amountMatcher = STANDALONE_AMOUNT_PATTERN.matcher(query);
        BigDecimal lastAmount = null;
        while (amountMatcher.find()) {
            lastAmount = new BigDecimal(amountMatcher.group(1));
        }
        return lastAmount;
    }

    private BigDecimal parseChineseAmount(String query) {
        String normalized = normalizeChineseNumbers(query);
        if (normalized != null) {
            return new BigDecimal(normalized);
        }
        return null;
    }

    private String normalizeChineseNumbers(String query) {
        Matcher matcher = CHINESE_AMOUNT_PATTERN.matcher(query);
        String lastResult = null;
        while (matcher.find()) {
            String numStr = matcher.group(1);
            String unit = matcher.group(2);
            int base = chineseDigitToInt(numStr);
            int multiplier = "万".equals(unit) ? 10000 : 1000;
            lastResult = String.valueOf((long) base * multiplier);
        }
        return lastResult;
    }

    private int chineseDigitToInt(String digit) {
        return switch (digit) {
            case "一" -> 1;
            case "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            default -> 0;
        };
    }
}
