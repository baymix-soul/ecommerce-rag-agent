package com.ecommerce.rag.rag.document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

@Service
public class RagDocumentService {

    private final ProductService productService;
    private final RagDocumentBuilder documentBuilder;

    public RagDocumentService(ProductService productService, RagDocumentBuilder documentBuilder) {
        this.productService = productService;
        this.documentBuilder = documentBuilder;
    }

    public List<RagChunkDocument> buildAllChunks() {
        List<Product> products = productService.listAll();
        return documentBuilder.buildChunks(products);
    }

    public List<RagChunkDocument> buildChunksByProductId(String productId) {
        Product product = productService.findById(productId)
                .orElseThrow(() -> new com.ecommerce.rag.core.exception.ProductNotFoundException(productId));
        return documentBuilder.buildChunks(product);
    }

    public Map<String, Long> countByChunkType() {
        List<RagChunkDocument> allChunks = buildAllChunks();
        return allChunks.stream()
                .collect(Collectors.groupingBy(RagChunkDocument::getChunkType, Collectors.counting()));
    }
}
