package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车语义 hint 输出。
 * 不作为最终唯一判断来源，只提供给 LLM prompt 或作为 fallback。
 */
public class CartSemanticHint {

    private boolean cartRelated;
    private List<String> possibleIntents = new ArrayList<>();
    private BigDecimal ruleParsedTargetAmount;
    private List<String> keywords = new ArrayList<>();

    public boolean isCartRelated() { return cartRelated; }
    public void setCartRelated(boolean cartRelated) { this.cartRelated = cartRelated; }

    public List<String> getPossibleIntents() { return possibleIntents; }
    public void setPossibleIntents(List<String> possibleIntents) {
        this.possibleIntents = possibleIntents != null ? possibleIntents : new ArrayList<>();
    }

    public BigDecimal getRuleParsedTargetAmount() { return ruleParsedTargetAmount; }
    public void setRuleParsedTargetAmount(BigDecimal ruleParsedTargetAmount) {
        this.ruleParsedTargetAmount = ruleParsedTargetAmount;
    }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? keywords : new ArrayList<>();
    }

    public static CartSemanticHint notCartRelated() {
        CartSemanticHint hint = new CartSemanticHint();
        hint.setCartRelated(false);
        return hint;
    }
}
