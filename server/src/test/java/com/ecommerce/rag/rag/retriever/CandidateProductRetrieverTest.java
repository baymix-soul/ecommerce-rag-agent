package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ChatCandidate;

@SpringBootTest
class CandidateProductRetrieverTest {

    @Autowired
    private CandidateProductRetriever retriever;

    @Test
    void shouldRetrieveCandidatesForKeywordQuery() {
        List<ChatCandidate> candidates = retriever.retrieve("油皮 洗面奶", 5);
        assertNotNull(candidates);
        assertTrue(candidates.size() <= 5);
    }

    @Test
    void shouldReturnEmptyListForBlankMessage() {
        List<ChatCandidate> candidates = retriever.retrieve("", 5);
        assertNotNull(candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForNullMessage() {
        List<ChatCandidate> candidates = retriever.retrieve(null, 5);
        assertNotNull(candidates);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void shouldRespectLimit() {
        List<ChatCandidate> candidates = retriever.retrieve("洗面奶", 3);
        assertNotNull(candidates);
        assertTrue(candidates.size() <= 3);
    }

    @Test
    void shouldPopulateCandidateFields() {
        List<ChatCandidate> candidates = retriever.retrieve("雅诗兰黛", 1);
        if (!candidates.isEmpty()) {
            ChatCandidate c = candidates.get(0);
            assertNotNull(c.getProductId());
            assertNotNull(c.getName());
            assertNotNull(c.getPrice());
        }
    }
}
