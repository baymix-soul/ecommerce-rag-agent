package com.ecommerce.rag.rag.embedding;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.core.perf.PerfTraceContext;

@Component
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public MockEmbeddingProvider(AppProperties appProperties) {
        this.dimension = appProperties.getEmbedding().getMockDimension();
    }

    @Override
    public List<Double> embed(String text) {
        List<List<Double>> batch = embedBatch(List.of(text));
        return batch.get(0);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        PerfTraceContext.startSpan("embedding.batch");
        List<List<Double>> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embedOne(text));
        }
        PerfTraceContext.endSpan("embedding.batch");
        PerfTraceContext.addAttribute("embedding_provider", "mock");
        PerfTraceContext.addAttribute("embedding_dimension", dimension);
        PerfTraceContext.addAttribute("embedding_text_count", texts.size());
        int totalLen = texts.stream().mapToInt(t -> t != null ? t.length() : 0).sum();
        PerfTraceContext.addAttribute("embedding_text_length_sum", totalLen);
        return result;
    }

    private List<Double> embedOne(String text) {
        List<Double> vec = new ArrayList<>(dimension);
        int hash = text.hashCode();
        for (int i = 0; i < dimension; i++) {
            vec.add(((hash + i * 31) % 1000) / 1000.0);
        }
        return vec;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return "mock-hash-embedding";
    }
}
