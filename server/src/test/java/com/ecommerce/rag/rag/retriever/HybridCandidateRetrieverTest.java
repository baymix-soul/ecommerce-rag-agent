package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ChatCandidate;

@SpringBootTest
class HybridCandidateRetrieverTest {

    @Autowired
    private HybridCandidateRetriever hybridRetriever;

    @Test
    void shouldReturnChatCandidatesInHybridMode() {
        List<ChatCandidate> candidates = hybridRetriever.retrieve("推荐一款适合油皮的洗面奶", 5);

        assertNotNull(candidates);
        assertTrue(candidates.size() > 0, "Should return at least keyword candidates when index not built");
    }

    @Test
    void shouldReturnEmptyForBlankMessage() {
        List<ChatCandidate> candidates = hybridRetriever.retrieve("", 5);

        assertNotNull(candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullMessage() {
        List<ChatCandidate> candidates = hybridRetriever.retrieve(null, 5);

        assertNotNull(candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldRespectLimit() {
        List<ChatCandidate> candidates = hybridRetriever.retrieve("油皮 洗面奶", 3);

        assertNotNull(candidates);
        assertTrue(candidates.size() <= 3);
    }

    @Test
    void chatCandidatesShouldHaveRequiredFields() {
        List<ChatCandidate> candidates = hybridRetriever.retrieve("推荐一款适合油皮的洗面奶", 5);

        assertTrue(candidates.size() > 0, "Should return candidates for known category query");
        for (ChatCandidate c : candidates) {
            assertNotNull(c.getProductId());
            assertNotNull(c.getName());
            assertNotNull(c.getPrice());
        }
    }
}
