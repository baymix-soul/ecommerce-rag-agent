package com.ecommerce.rag.rag.retriever;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.vector.VectorSearchHit;

@Component
public class CandidateFusionService {

    private static final Logger log = LoggerFactory.getLogger(CandidateFusionService.class);

    private static final double VECTOR_WEIGHT = 0.65;
    private static final double KEYWORD_WEIGHT = 0.35;
    private static final double CHUNK_BONUS = 0.1;

    public List<RetrievedProductCandidate> merge(List<VectorSearchHit> vectorHits, List<Product> keywordProducts) {
        Map<String, RetrievedProductCandidate> candidateMap = new LinkedHashMap<>();

        if (vectorHits != null) {
            for (VectorSearchHit hit : vectorHits) {
                String pid = hit.getProductId();
                if (pid == null) continue;

                RetrievedProductCandidate candidate = candidateMap.computeIfAbsent(pid, k -> {
                    RetrievedProductCandidate c = new RetrievedProductCandidate();
                    c.setProductId(pid);
                    c.setVectorScore(0.0);
                    c.setKeywordScore(0.0);
                    return c;
                });

                candidate.getMatchedChunks().add(hit);
                double hitScore = hit.getScore() != null ? hit.getScore() : 0.0;
                if (candidate.getVectorScore() == null || hitScore > candidate.getVectorScore()) {
                    candidate.setVectorScore(hitScore);
                } else if (candidate.getMatchedChunks().size() >= 2) {
                    candidate.setVectorScore(Math.min(1.0, candidate.getVectorScore() + CHUNK_BONUS));
                }
            }
        }

        if (keywordProducts != null) {
            int rank = 0;
            for (Product product : keywordProducts) {
                String pid = product.getProductId();
                if (pid == null) continue;
                rank++;

                RetrievedProductCandidate candidate = candidateMap.computeIfAbsent(pid, k -> {
                    RetrievedProductCandidate c = new RetrievedProductCandidate();
                    c.setProductId(pid);
                    c.setVectorScore(0.0);
                    c.setKeywordScore(0.0);
                    return c;
                });

                candidate.setProduct(product);
                double kwScore = Math.max(0.1, 1.0 - (rank - 1) * 0.1);
                if (candidate.getKeywordScore() == null || kwScore > candidate.getKeywordScore()) {
                    candidate.setKeywordScore(kwScore);
                }
            }
        }

        for (Map.Entry<String, RetrievedProductCandidate> entry : candidateMap.entrySet()) {
            RetrievedProductCandidate c = entry.getValue();
            double vScore = c.getVectorScore() != null ? c.getVectorScore() : 0.0;
            double kScore = c.getKeywordScore() != null ? c.getKeywordScore() : 0.0;

            boolean hasVector = c.getMatchedChunks() != null && !c.getMatchedChunks().isEmpty();
            boolean hasKeyword = c.getKeywordScore() > 0;

            c.setFinalScore(VECTOR_WEIGHT * vScore + KEYWORD_WEIGHT * kScore);

            List<String> sources = new ArrayList<>();
            if (hasVector) sources.add("vector");
            if (hasKeyword) sources.add("keyword");
            c.setMatchedSources(sources);
        }

        List<RetrievedProductCandidate> results = new ArrayList<>(candidateMap.values());
        results.sort(Comparator.comparingDouble(
                (RetrievedProductCandidate c) -> c.getFinalScore() != null ? c.getFinalScore() : 0.0).reversed());

        log.debug("Fusion merged {} candidates from {} vector hits and {} keyword products",
                results.size(),
                vectorHits != null ? vectorHits.size() : 0,
                keywordProducts != null ? keywordProducts.size() : 0);

        return results;
    }
}
