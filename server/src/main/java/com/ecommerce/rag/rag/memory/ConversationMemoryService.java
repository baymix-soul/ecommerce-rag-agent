package com.ecommerce.rag.rag.memory;

import java.util.List;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

public interface ConversationMemoryService {

    ConversationState getOrCreate(String sessionId);

    void updateAfterRetrieval(String sessionId, String query,
                               QueryAnalysisResult analysis, List<ChatCandidate> candidates);

    void clearSession(String sessionId);

    boolean hasSession(String sessionId);

    ConversationState getState(String sessionId);
}
