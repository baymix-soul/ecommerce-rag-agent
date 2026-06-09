package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

class CandidateFusionServiceTest {

    private final CandidateFusionService fusionService = new CandidateFusionService();

    @Test
    void shouldMergeVectorHitsAndKeywordProducts() {
        List<VectorSearchHit> hits = new ArrayList<>();
        VectorSearchHit hit = new VectorSearchHit();
        hit.setProductId("p_001");
        hit.setScore(0.85);
        hits.add(hit);

        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setProductId("p_002");
        p.setName("测试商品");
        p.setPrice(new BigDecimal("100"));
        products.add(p);

        List<RetrievedProductCandidate> merged = fusionService.merge(hits, products);

        assertEquals(2, merged.size());
    }

    @Test
    void shouldAggregateSameProductIdFromMultipleChunks() {
        List<VectorSearchHit> hits = new ArrayList<>();
        VectorSearchHit h1 = new VectorSearchHit();
        h1.setProductId("p_001");
        h1.setScore(0.85);
        hits.add(h1);

        VectorSearchHit h2 = new VectorSearchHit();
        h2.setProductId("p_001");
        h2.setScore(0.75);
        hits.add(h2);

        List<RetrievedProductCandidate> merged = fusionService.merge(hits, new ArrayList<>());

        assertEquals(1, merged.size());
        assertEquals("p_001", merged.get(0).getProductId());
        assertEquals(2, merged.get(0).getMatchedChunks().size());
        assertTrue(merged.get(0).getMatchedSources().contains("vector"));
    }

    @Test
    void finalScoreShouldSortDescending() {
        List<VectorSearchHit> hits = new ArrayList<>();
        VectorSearchHit h1 = new VectorSearchHit();
        h1.setProductId("p_001");
        h1.setScore(0.90);
        hits.add(h1);

        List<Product> products = new ArrayList<>();
        Product p1 = new Product();
        p1.setProductId("p_001");
        p1.setName("商品1");
        p1.setPrice(new BigDecimal("100"));
        products.add(p1);

        Product p2 = new Product();
        p2.setProductId("p_002");
        p2.setName("商品2");
        p2.setPrice(new BigDecimal("200"));
        products.add(p2);

        List<RetrievedProductCandidate> merged = fusionService.merge(hits, products);

        assertTrue(merged.size() >= 2);
        assertTrue(merged.get(0).getFinalScore() >= merged.get(1).getFinalScore(),
                "First candidate should have higher finalScore");
    }

    @Test
    void keywordOnlyShouldWork() {
        List<Product> products = new ArrayList<>();
        Product p = new Product();
        p.setProductId("p_001");
        p.setName("测试");
        p.setPrice(new BigDecimal("100"));
        products.add(p);

        List<RetrievedProductCandidate> merged = fusionService.merge(new ArrayList<>(), products);

        assertEquals(1, merged.size());
        assertEquals(0.0, merged.get(0).getVectorScore());
        assertTrue(merged.get(0).getKeywordScore() > 0);
    }

    @Test
    void vectorOnlyShouldWork() {
        List<VectorSearchHit> hits = new ArrayList<>();
        VectorSearchHit hit = new VectorSearchHit();
        hit.setProductId("p_001");
        hit.setScore(0.85);
        hits.add(hit);

        List<RetrievedProductCandidate> merged = fusionService.merge(hits, new ArrayList<>());

        assertEquals(1, merged.size());
        assertEquals(0.85, merged.get(0).getVectorScore());
        assertEquals(0.0, merged.get(0).getKeywordScore());
    }

    @Test
    void emptyInputsShouldReturnEmptyList() {
        List<RetrievedProductCandidate> merged = fusionService.merge(new ArrayList<>(), new ArrayList<>());

        assertNotNull(merged);
        assertTrue(merged.isEmpty());
    }

    @Test
    void nullInputsShouldReturnEmptyList() {
        List<RetrievedProductCandidate> merged = fusionService.merge(null, null);

        assertNotNull(merged);
        assertTrue(merged.isEmpty());
    }
}
