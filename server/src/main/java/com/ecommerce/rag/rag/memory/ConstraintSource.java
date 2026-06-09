package com.ecommerce.rag.rag.memory;

import java.time.Instant;

public class ConstraintSource {

    private String sourceQuery;
    private int sourceTurn;
    private Instant createdAt;

    public ConstraintSource() {
        this.createdAt = Instant.now();
    }

    public ConstraintSource(String sourceQuery, int sourceTurn) {
        this.sourceQuery = sourceQuery;
        this.sourceTurn = sourceTurn;
        this.createdAt = Instant.now();
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
}
