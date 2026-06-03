package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class HybridCandidateRetrieverExplicitAnalysisTest {

    @Autowired
    private HybridCandidateRetriever retriever;

    @Test
    void shouldReturnLaptopCandidatesWhenCategoryDigitalAndSubCategoryLaptop() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐笔记本电脑");
        analysis.setCategory("数码电子");
        analysis.setSubCategory("笔记本电脑");
        analysis.getFilters().put("category", "数码电子");
        analysis.getFilters().put("sub_category", "笔记本电脑");

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("推荐笔记本电脑", 10, analysis);

        assertNotNull(candidates);
        assertFalse(candidates.isEmpty());
        for (ChatCandidate c : candidates) {
            assertEquals("数码电子", c.getCategory());
            assertEquals("笔记本电脑", c.getSubCategory());
        }
    }

    @Test
    void shouldNotReturnExpensiveProductsWhenMaxPriceSet() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐笔记本电脑");
        analysis.setCategory("数码电子");
        analysis.setSubCategory("笔记本电脑");
        analysis.setMaxPrice(new BigDecimal("10000"));
        analysis.getFilters().put("category", "数码电子");
        analysis.getFilters().put("sub_category", "笔记本电脑");
        analysis.getFilters().put("max_price", new BigDecimal("10000"));

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("推荐笔记本电脑", 10, analysis);

        assertNotNull(candidates);
        assertFalse(candidates.isEmpty());
        for (ChatCandidate c : candidates) {
            assertNotNull(c.getPrice());
            assertTrue(c.getPrice().compareTo(new BigDecimal("10000")) <= 0,
                    "Product " + c.getName() + " price " + c.getPrice() + " exceeds max 10000");
        }
    }

    @Test
    void strictProductConstraintFilterShouldStillExecute() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setOriginalQuery("推荐跑鞋");
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getFilters().put("category", "服饰运动");
        analysis.getFilters().put("sub_category", "跑步鞋");

        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("推荐跑鞋", 10, analysis);

        assertNotNull(candidates);
    }

    @Test
    void nullAnalysisShouldStillRetrieve() {
        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("推荐跑鞋", 5, null);

        assertNotNull(candidates);
    }

    @Test
    void emptyQueryShouldReturnEmpty() {
        List<ChatCandidate> candidates = retriever.retrieveWithAnalysis("", 5, null);
        assertNotNull(candidates);
        assertTrue(candidates.isEmpty());
    }
}
