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
class HybridCandidateRetrieverContextTest {

    @Autowired
    private HybridCandidateRetriever hybridRetriever;

    @Test
    void excludeNikeShouldNotReturnNike() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("Nike");
        analysis.getNegativeBrands().add("耐克");
        analysis.getExcludeBrands().add("Nike");
        analysis.getExcludeBrands().add("耐克");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("推荐跑鞋", 10, analysis);
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
    void runningShoesWithinBudgetShouldNotExceedMaxPrice() {
        BigDecimal maxPrice = new BigDecimal("500");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("500元以内跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(maxPrice);

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("500元以内跑鞋", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            assertTrue(c.getPrice().compareTo(maxPrice) <= 0,
                    "Price should be <= 500: " + c.getPrice() + " for " + c.getName());
        }
    }

    @Test
    void emptyResultShouldNotThrow500() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("zzzz_not_exist");
        analysis.setCategory("zzz_category");
        analysis.setSubCategory("zzz_sub");
        analysis.setMaxPrice(new BigDecimal("1"));

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("zzzz_not_exist", 5, analysis);
        assertNotNull(candidates);
        assertTrue(candidates.isEmpty(), "Should return empty for impossible query, not 500");
    }

    @Test
    void textBasedFilterShouldExcludeAlcoholTerms() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("护肤品推荐");
        analysis.setCategory("美妆护肤");
        analysis.getAvoidIngredientsOrTerms().add("酒精");
        analysis.getAvoidIngredientsOrTerms().add("alcohol");

        List<ChatCandidate> candidates = hybridRetriever.retrieveWithAnalysis("护肤品推荐", 10, analysis);
        assertNotNull(candidates);

        for (ChatCandidate c : candidates) {
            String desc = c.getDescription();
            if (desc != null) {
                assertTrue(!desc.toLowerCase().contains("酒精") && !desc.toLowerCase().contains("alcohol"),
                        "Should not contain alcohol terms: " + c.getName());
            }
        }
    }
}
