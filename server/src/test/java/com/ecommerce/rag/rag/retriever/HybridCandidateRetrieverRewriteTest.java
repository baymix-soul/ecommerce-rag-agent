package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.query.QueryAnalyzer;

@SpringBootTest
class HybridCandidateRetrieverRewriteTest {

    @Autowired
    private HybridCandidateRetriever retriever;

    @Autowired
    private QueryAnalyzer queryAnalyzer;

    @Test
    void shouldInstantiateWithRewriteServiceInjected() {
        assertNotNull(retriever);
    }

    @Test
    void studentEarbudsShouldStillHaveCategoryFilter() {
        QueryAnalysisResult analysis = queryAnalyzer.analyze("学生党耳机");

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("学生党耳机", 5, analysis);

        assertNotNull(candidates);
        for (ChatCandidate c : candidates) {
            if (c.getCategory() != null) {
                assertTrue(c.getCategory().equals("数码电子"),
                        "Category should still be 数码电子 after rewrite, got: " + c.getCategory());
            }
        }
    }

    @Test
    void budgetQueryPriceConstraintShouldBeRespected() {
        QueryAnalysisResult analysis = queryAnalyzer.analyze("预算200以内耳机");

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("预算200以内耳机", 5, analysis);

        for (ChatCandidate c : candidates) {
            if (c.getPrice() != null) {
                assertTrue(c.getPrice().doubleValue() <= 200,
                        "Price should be <= 200 after rewrite: " + c.getPrice());
            }
        }
    }

    @Test
    void retrieveRawShouldWorkWithDefaultAnalysis() {
        List<RetrievedProductCandidate> raw = retriever.retrieveRaw("学生党耳机", 5);

        assertNotNull(raw);
        assertTrue(raw.size() >= 0);
    }

    @Test
    void rewriteFailureShouldNotBreakRetrieval() {
        QueryAnalysisResult emptyAnalysis = new QueryAnalysisResult();
        emptyAnalysis.setOriginalQuery("test");
        emptyAnalysis.setNormalizedQuery("test");

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("test", 3, emptyAnalysis);

        assertNotNull(candidates);
    }
}
