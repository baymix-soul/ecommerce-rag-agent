package com.ecommerce.rag.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatRequest {

    @JsonProperty("message")
    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("limit")
    private Integer limit;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public int getEffectiveLimit() {
        if (limit == null || limit <= 0) {
            return 5;
        }
        return Math.min(limit, 10);
    }
}
