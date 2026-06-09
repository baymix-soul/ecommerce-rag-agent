package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartSemanticMatchResult {

    public static final String LEVEL_EXACT = "EXACT";
    public static final String LEVEL_PARTIAL = "PARTIAL";
    public static final String LEVEL_NONE = "NONE";

    private boolean cartRelated;
    private String matchLevel = LEVEL_NONE;
    private String matchedFrameId;
    private List<String> candidateFrameIds = new ArrayList<>();
    private Map<String, Object> extractedSlots = new HashMap<>();
    private List<String> missingSlots = new ArrayList<>();
    private BigDecimal ruleParsedTargetAmount;
    private String normalizedAmountText;
    private double ruleConfidence;
    private List<String> matchedPatterns = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public CartSemanticMatchResult() {
    }

    public boolean isCartRelated() {
        return cartRelated;
    }

    public void setCartRelated(boolean cartRelated) {
        this.cartRelated = cartRelated;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public String getMatchedFrameId() {
        return matchedFrameId;
    }

    public void setMatchedFrameId(String matchedFrameId) {
        this.matchedFrameId = matchedFrameId;
    }

    public List<String> getCandidateFrameIds() {
        return candidateFrameIds;
    }

    public void setCandidateFrameIds(List<String> candidateFrameIds) {
        this.candidateFrameIds = candidateFrameIds != null ? candidateFrameIds : new ArrayList<>();
    }

    public Map<String, Object> getExtractedSlots() {
        return extractedSlots;
    }

    public void setExtractedSlots(Map<String, Object> extractedSlots) {
        this.extractedSlots = extractedSlots != null ? extractedSlots : new HashMap<>();
    }

    public List<String> getMissingSlots() {
        return missingSlots;
    }

    public void setMissingSlots(List<String> missingSlots) {
        this.missingSlots = missingSlots != null ? missingSlots : new ArrayList<>();
    }

    public BigDecimal getRuleParsedTargetAmount() {
        return ruleParsedTargetAmount;
    }

    public void setRuleParsedTargetAmount(BigDecimal ruleParsedTargetAmount) {
        this.ruleParsedTargetAmount = ruleParsedTargetAmount;
    }

    public String getNormalizedAmountText() {
        return normalizedAmountText;
    }

    public void setNormalizedAmountText(String normalizedAmountText) {
        this.normalizedAmountText = normalizedAmountText;
    }

    public double getRuleConfidence() {
        return ruleConfidence;
    }

    public void setRuleConfidence(double ruleConfidence) {
        this.ruleConfidence = ruleConfidence;
    }

    public List<String> getMatchedPatterns() {
        return matchedPatterns;
    }

    public void setMatchedPatterns(List<String> matchedPatterns) {
        this.matchedPatterns = matchedPatterns != null ? matchedPatterns : new ArrayList<>();
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    public static CartSemanticMatchResult notCartRelated() {
        CartSemanticMatchResult r = new CartSemanticMatchResult();
        r.setCartRelated(false);
        r.setMatchLevel(LEVEL_NONE);
        return r;
    }
}
