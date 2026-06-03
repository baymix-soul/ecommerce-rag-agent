package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

@SpringBootTest
class HybridCandidateRetrieverPageContextTest {

    @Autowired
    private HybridCandidateRetriever hybridRetriever;

    @Test
    void excludeCurrentProductIdShouldWork() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getExcludeProductIds().add("p_sports_003");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("推荐跑鞋", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            assertNotEquals("p_sports_003", c.getProductId(),
                    "Should not return excluded product p_sports_003");
        }
    }

    @Test
    void boostedProductIdsShouldNotCrash() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.getBoostedProductIds().add("p_sports_001");
        analysis.getScopeProductIds().add("p_sports_001");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("推荐跑鞋", 10, analysis);
        assertNotNull(candidates);
    }

    @Test
    void selectedFiltersCategoryShouldFilter() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("推荐跑鞋", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            assertEquals("服饰运动", c.getCategory(),
                    "All candidates should be in 服饰运动 category");
        }
    }

    @Test
    void emptyPageContextShouldNotAffectRetrieval() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("推荐跑鞋", 5, analysis);
        assertNotNull(candidates);
    }
}
