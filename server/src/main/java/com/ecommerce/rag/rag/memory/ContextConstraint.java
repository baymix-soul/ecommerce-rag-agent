package com.ecommerce.rag.rag.memory;

import java.time.Instant;

public class ContextConstraint {

    private String field;
    private String value;
    private ConstraintStrength strength;
    private String sourceQuery;
    private int sourceTurn;
    private Instant createdAt;
    private boolean active = true;

    public ContextConstraint() {
        this.createdAt = Instant.now();
    }

    public ContextConstraint(String field, String value, ConstraintStrength strength,
                             String sourceQuery, int sourceTurn) {
        this.field = field;
        this.value = value;
        this.strength = strength;
        this.sourceQuery = sourceQuery;
        this.sourceTurn = sourceTurn;
        this.createdAt = Instant.now();
        this.active = true;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ConstraintStrength getStrength() {
        return strength;
    }

    public void setStrength(ConstraintStrength strength) {
        this.strength = strength;
    }

    public String getSourceQuery() {
        return sourceQuery;
    }

    public void setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
    }

    public int getSourceTurn() {
        return sourceTurn;
    }

    public void setSourceTurn(int sourceTurn) {
        this.sourceTurn = sourceTurn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
