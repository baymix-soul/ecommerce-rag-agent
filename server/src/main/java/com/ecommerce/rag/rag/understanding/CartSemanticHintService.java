package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 购物车语义 hint 服务。
 * 不作为主意图判断器，仅提供 hint 给 LLM planner 或作为 planner 禁用时的 fallback。
 */
@Component
public class CartSemanticHintService {

    private static final String[] CART_SUMMARY_KEYWORDS = {
            "购物车", "已经买了", "合计", "总价", "多少钱",
            "已经加购", "购物车里有什么", "花了多少钱", "买了多少"
    };

    private static final String[] CART_TOP_UP_KEYWORDS = {
            "凑", "凑单", "满", "还差", "达到", "凑单推荐", "帮我凑", "凑到"
    };

    // Matches "到" followed by digits (e.g. "到1000")
    private static final Pattern TOP_UP_TARGET_PATTERN = Pattern.compile("到(\\d+)");

    // Matches digits optionally followed by 元/块 (e.g. "1000", "1000元", "1000块")
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+)(?:元|块)?");

    // Chinese number mapping for common amounts
    private static final Pattern CHINESE_AMOUNT_PATTERN = Pattern.compile(
            "(一两)(千|万)(?:元|块)?"
    );

    private static final Pattern CHINESE_SINGLE_PATTERN = Pattern.compile(
            "([一两三四五六七八九])(千|万)(?:元|块)?"
    );

    public CartSemanticHint analyze(String query) {
        if (query == null || query.isBlank()) {
            return CartSemanticHint.notCartRelated();
        }

        List<String> possibleIntents = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        // Check cart summary keywords
        for (String keyword : CART_SUMMARY_KEYWORDS) {
            if (query.contains(keyword)) {
                possibleIntents.add("CART_SUMMARY");
                matchedKeywords.add(keyword);
                break;
            }
        }

        // Check cart top-up keywords
        for (String keyword : CART_TOP_UP_KEYWORDS) {
            if (query.contains(keyword)) {
                if (!possibleIntents.contains("CART_TOP_UP")) {
                    possibleIntents.add("CART_TOP_UP");
                }
                matchedKeywords.add(keyword);
            }
        }

        // Also detect "到\d+" as a top-up indicator
        if (TOP_UP_TARGET_PATTERN.matcher(query).find() && !possibleIntents.contains("CART_TOP_UP")) {
            possibleIntents.add("CART_TOP_UP");
        }

        if (matchedKeywords.isEmpty() && !TOP_UP_TARGET_PATTERN.matcher(query).find()) {
            return CartSemanticHint.notCartRelated();
        }

        // Parse target amount
        BigDecimal targetAmount = parseTargetAmount(query);

        CartSemanticHint hint = new CartSemanticHint();
        hint.setCartRelated(true);
        hint.setPossibleIntents(possibleIntents);
        hint.setRuleParsedTargetAmount(targetAmount);
        hint.setKeywords(matchedKeywords);
        return hint;
    }

    private BigDecimal parseTargetAmount(String query) {
        // Try Chinese number patterns first (higher priority)
        BigDecimal chineseAmount = parseChineseAmount(query);
        if (chineseAmount != null) {
            return chineseAmount;
        }

        // Try patterns like "凑到1000", "到1000块", "满1000", "还差1000"
        Matcher topUpMatcher = TOP_UP_TARGET_PATTERN.matcher(query);
        if (topUpMatcher.find()) {
            return new BigDecimal(topUpMatcher.group(1));
        }

        // Try "满" followed by digits
        Pattern manPattern = Pattern.compile("满(\\d+)");
        Matcher manMatcher = manPattern.matcher(query);
        if (manMatcher.find()) {
            return new BigDecimal(manMatcher.group(1));
        }

        // Try "还差" followed by digits
        Pattern diffPattern = Pattern.compile("还差(\\d+)");
        Matcher diffMatcher = diffPattern.matcher(query);
        if (diffMatcher.find()) {
            return new BigDecimal(diffMatcher.group(1));
        }

        // Fallback: look for standalone number patterns with optional 元/块
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(query);
        BigDecimal lastAmount = null;
        while (amountMatcher.find()) {
            lastAmount = new BigDecimal(amountMatcher.group(1));
        }
        return lastAmount;
    }

    private BigDecimal parseChineseAmount(String query) {
        // Handle "一千" → 1000, "一万元" → 10000, "两万" → 20000, etc.
        Matcher matcher = CHINESE_SINGLE_PATTERN.matcher(query);
        BigDecimal lastResult = null;
        while (matcher.find()) {
            String numStr = matcher.group(1);
            String unit = matcher.group(2);
            int base = chineseDigitToInt(numStr);
            int multiplier = "万".equals(unit) ? 10000 : 1000;
            lastResult = BigDecimal.valueOf((long) base * multiplier);
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
