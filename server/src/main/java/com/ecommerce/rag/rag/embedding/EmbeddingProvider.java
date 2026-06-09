package com.ecommerce.rag.rag.embedding;

import java.util.List;

public interface EmbeddingProvider {

    List<Double> embed(String text);

    List<List<Double>> embedBatch(List<String> texts);

    int dimension();

    String modelName();
}
