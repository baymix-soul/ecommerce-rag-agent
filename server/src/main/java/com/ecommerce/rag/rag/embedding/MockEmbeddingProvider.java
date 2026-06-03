package com.ecommerce.rag.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public MockEmbeddingProvider(int dimension) {
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
        this.dimension = dimension;
    }

    public MockEmbeddingProvider() {
        this(64);
    }

    @Override
    public List<Double> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or blank");
        }
        long seed = hashText(text);
        Random rng = new Random(seed);
        return generateNormalizedVector(rng, dimension);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be null or empty");
        }
        List<List<Double>> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public String modelName() {
        return "mock-hash-embedding";
    }

    private long hashText(String text) {
        long h = 1125899906842597L;
        for (int i = 0; i < text.length(); i++) {
            h = 31 * h + text.charAt(i);
        }
        return h;
    }

    private List<Double> generateNormalizedVector(Random rng, int dim) {
        List<Double> vector = new ArrayList<>(dim);
        double sumSq = 0.0;
        for (int i = 0; i < dim; i++) {
            double val = rng.nextGaussian();
            vector.add(val);
            sumSq += val * val;
        }
        double norm = Math.sqrt(sumSq);
        if (norm > 0.0) {
            for (int i = 0; i < dim; i++) {
                vector.set(i, vector.get(i) / norm);
            }
        }
        return vector;
    }
}
