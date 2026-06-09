package com.ecommerce.rag.rag.memory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

@Service
public class InMemoryConversationMemoryService implements ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryConversationMemoryService.class);

    private final ConcurrentHashMap<String, ConversationState> sessions = new ConcurrentHashMap<>();

    @Override
    public ConversationState getOrCreate(String sessionId) {
        String sid = resolveSessionId(sessionId);
        return sessions.computeIfAbsent(sid, k -> {
            ConversationState state = new ConversationState(k);
            log.debug("Created new session: {}", k);
            return state;
        });
    }

    @Override
    public void updateAfterRetrieval(String sessionId, String query,
                                      QueryAnalysisResult analysis, List<ChatCandidate> candidates) {
        String sid = resolveSessionId(sessionId);
        ConversationState state = sessions.computeIfAbsent(sid, k -> new ConversationState(k));
        state.touch();
        state.setLastUserQuery(query);

        if (analysis != null) {
            String resolved = analysis.getResolvedQuery() != null && !analysis.getResolvedQuery().isBlank()
                    ? analysis.getResolvedQuery() : analysis.getOriginalQuery();
            state.setLastResolvedQuery(resolved);

            if (analysis.getCategory() != null && !analysis.getCategory().isBlank()) {
                state.setCategory(analysis.getCategory());
            }
            if (analysis.getSubCategory() != null && !analysis.getSubCategory().isBlank()) {
                state.setSubCategory(analysis.getSubCategory());
            }
            if (analysis.getSubCategories() != null && !analysis.getSubCategories().isEmpty()) {
                state.setSubCategories(analysis.getSubCategories());
            }
            if (analysis.getMinPrice() != null) {
                state.setMinPrice(analysis.getMinPrice());
            }
            if (analysis.getMaxPrice() != null) {
                state.setMaxPrice(analysis.getMaxPrice());
            }
            if (analysis.getNegativeBrands() != null && !analysis.getNegativeBrands().isEmpty()) {
                state.setNegativeBrands(analysis.getNegativeBrands());
            }
            if (analysis.getNegativeKeywords() != null && !analysis.getNegativeKeywords().isEmpty()) {
                state.setNegativeKeywords(analysis.getNegativeKeywords());
            }
            if (analysis.getPositiveKeywords() != null && !analysis.getPositiveKeywords().isEmpty()) {
                state.setPositiveKeywords(analysis.getPositiveKeywords());
            }
        }

        if (candidates != null && !candidates.isEmpty()) {
            List<String> candidateIds = candidates.stream()
                    .map(ChatCandidate::getProductId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            state.setCandidateProductIds(candidateIds);
            state.setRecommendedProductIds(candidateIds);

            // Update active context v2
            if (state.getActiveSearchContext() != null) {
                state.getActiveSearchContext().setLastSuccessfulCandidateIds(candidateIds);
                state.getActiveSearchContext().setLastRecommendedProductIds(candidateIds);
                state.getActiveSearchContext().setLastCandidateProductIds(candidateIds);
            }
        }

        log.debug("Updated session {}: turn={}, category={}, sub={}, recommended={}",
                sid, state.getTurnCount(), state.getCategory(), state.getSubCategory(),
                state.getRecommendedProductIds().size());
    }

    @Override
    public void updateAfterNoMatch(String sessionId, QueryAnalysisResult analysis, boolean recovered) {
        String sid = resolveSessionId(sessionId);
        ConversationState state = sessions.get(sid);
        if (state == null) {
            return;
        }

        // DO NOT clear active context on NO_MATCH
        state.setLastNoMatchReason("NO_MATCH");
        state.setLastNoMatchRecovered(recovered);
        state.touch();

        if (state.getActiveSearchContext() != null) {
            state.getActiveSearchContext().setLastNoMatchReason("NO_MATCH");
            if (recovered) {
                state.getActiveSearchContext().setLastContextAction(ConversationContextMerger.ACTION_RELAX_CONSTRAINT);
            }
        }

        log.debug("Updated session {} after NO_MATCH: recovered={}", sid, recovered);
    }

    @Override
    public void save(String sessionId, ConversationState state) {
        String sid = resolveSessionId(sessionId);
        if (state != null) {
            sessions.put(sid, state);
        }
    }

    @Override
    public void clearSession(String sessionId) {
        String sid = resolveSessionId(sessionId);
        sessions.remove(sid);
        log.debug("Cleared session: {}", sid);
    }

    @Override
    public boolean hasSession(String sessionId) {
        String sid = resolveSessionId(sessionId);
        return sessions.containsKey(sid);
    }

    @Override
    public ConversationState getState(String sessionId) {
        String sid = resolveSessionId(sessionId);
        return sessions.get(sid);
    }

    @Override
    public String buildMemoryKey(String sessionId, String authenticatedUserId) {
        String sid = sessionId != null && !sessionId.isBlank() ? sessionId : "default";
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            return authenticatedUserId + ":" + sid;
        }
        return "anon:" + sid;
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "default";
        }
        return sessionId.trim();
    }
}
