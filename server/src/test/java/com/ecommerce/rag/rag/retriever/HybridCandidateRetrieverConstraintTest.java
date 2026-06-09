package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

@SpringBootTest
class HybridCandidateRetrieverConstraintTest {

    @Autowired
    private HybridCandidateRetriever hybridRetriever;

    @Test
    void lightweightWithRunningShoeContextShouldNotReturnBackpack() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("跑步鞋 轻量");
        analysis.setNormalizedQuery("跑步鞋 轻量");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getPositiveKeywords().add("轻量");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("跑步鞋 轻量", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            assertTrue(c.getSubCategory() != null && !c.getSubCategory().equals("背包"),
                    "Should not return backpack: " + c.getName() + " subCategory=" + c.getSubCategory());
            assertTrue(c.getSubCategory() != null && !c.getSubCategory().equals("短袖T恤"),
                    "Should not return t-shirt: " + c.getName());
        }
    }

    @Test
    void budget1000WithRunningShoeContextShouldNotReturnOver1000() {
        BigDecimal maxPrice = new BigDecimal("1000");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("跑步鞋");
        analysis.setNormalizedQuery("跑步鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(maxPrice);

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("跑步鞋", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            assertTrue(c.getPrice().compareTo(maxPrice) <= 0,
                    "Price should be <= 1000: " + c.getPrice() + " for " + c.getName());
        }
    }

    @Test
    void excludeNikeShouldNotReturnNikeProducts() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("跑步鞋");
        analysis.setNormalizedQuery("跑步鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("Nike");
        analysis.getNegativeBrands().add("耐克");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("跑步鞋", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            if (c.getBrand() != null) {
                assertTrue(!c.getBrand().equalsIgnoreCase("Nike")
                                && !c.getBrand().contains("耐克"),
                        "Should not return Nike: " + c.getBrand());
            }
        }
    }

    @Test
    void allCandidatesFailConstraintsShouldReturnEmpty() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("zzzz_not_exist");
        analysis.setCategory("zzz_category");
        analysis.setSubCategory("zzz_sub");
        analysis.setMaxPrice(new BigDecimal("1"));

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("zzzz_not_exist", 5, analysis);
        assertNotNull(candidates);
        assertTrue(candidates.isEmpty(), "Should return empty when no candidates pass constraints");
    }
}
