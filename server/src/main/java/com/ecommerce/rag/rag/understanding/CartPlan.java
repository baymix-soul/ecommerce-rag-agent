package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 购物车计划子结构，表达 LLM 对购物车相关意图的结构化输出。
 * LLM 只负责填字段，不执行购物车业务。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartPlan {

    public static final String ACTION_CART_SUMMARY = "CART_SUMMARY";
    public static final String ACTION_AMOUNT_GAP_QUERY = "AMOUNT_GAP_QUERY";
    public static final String ACTION_COMPLETION_RECOMMEND = "COMPLETION_RECOMMEND";
    public static final String ACTION_ADD_TO_CART = "ADD_TO_CART";
    public static final String ACTION_REMOVE_FROM_CART = "REMOVE_FROM_CART";

    private String action;
    private BigDecimal targetAmount;
    private String currency;
    private Boolean needsCart;
    private Boolean needsRecommendation;
    private List<String> referencedProductIds = new ArrayList<>();
    private String productReference;

    public CartPlan() {
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Boolean getNeedsCart() { return needsCart; }
    public void setNeedsCart(Boolean needsCart) { this.needsCart = needsCart; }

    public Boolean getNeedsRecommendation() { return needsRecommendation; }
    public void setNeedsRecommendation(Boolean needsRecommendation) { this.needsRecommendation = needsRecommendation; }

    public List<String> getReferencedProductIds() { return referencedProductIds; }
    public void setReferencedProductIds(List<String> referencedProductIds) {
        this.referencedProductIds = referencedProductIds != null ? referencedProductIds : new ArrayList<>();
    }

    public String getProductReference() { return productReference; }
    public void setProductReference(String productReference) { this.productReference = productReference; }

    public CartPlan deepCopy() {
        CartPlan copy = new CartPlan();
        copy.setAction(this.action);
        copy.setTargetAmount(this.targetAmount);
        copy.setCurrency(this.currency);
        copy.setNeedsCart(this.needsCart);
        copy.setNeedsRecommendation(this.needsRecommendation);
        copy.setReferencedProductIds(new ArrayList<>(this.referencedProductIds));
        copy.setProductReference(this.productReference);
        return copy;
    }
}
