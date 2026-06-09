package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.List;

public class CartSemanticFrame {

    private String frameId;
    private String description;
    private String intent;
    private String cartAction;
    private List<String> requiredSlots = new ArrayList<>();
    private List<String> optionalSlots = new ArrayList<>();
    private List<String> positiveExamples = new ArrayList<>();
    private List<String> negativeExamples = new ArrayList<>();

    public CartSemanticFrame() {
    }

    public String getFrameId() {
        return frameId;
    }

    public void setFrameId(String frameId) {
        this.frameId = frameId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getCartAction() {
        return cartAction;
    }

    public void setCartAction(String cartAction) {
        this.cartAction = cartAction;
    }

    public List<String> getRequiredSlots() {
        return requiredSlots;
    }

    public void setRequiredSlots(List<String> requiredSlots) {
        this.requiredSlots = requiredSlots != null ? requiredSlots : new ArrayList<>();
    }

    public List<String> getOptionalSlots() {
        return optionalSlots;
    }

    public void setOptionalSlots(List<String> optionalSlots) {
        this.optionalSlots = optionalSlots != null ? optionalSlots : new ArrayList<>();
    }

    public List<String> getPositiveExamples() {
        return positiveExamples;
    }

    public void setPositiveExamples(List<String> positiveExamples) {
        this.positiveExamples = positiveExamples != null ? positiveExamples : new ArrayList<>();
    }

    public List<String> getNegativeExamples() {
        return negativeExamples;
    }

    public void setNegativeExamples(List<String> negativeExamples) {
        this.negativeExamples = negativeExamples != null ? negativeExamples : new ArrayList<>();
    }

    public boolean hasRequiredSlot(String slotName) {
        return requiredSlots != null && requiredSlots.contains(slotName);
    }
}
