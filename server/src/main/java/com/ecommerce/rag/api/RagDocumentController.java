package com.ecommerce.rag.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.core.exception.ProductNotFoundException;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.document.RagChunkDocument;
import com.ecommerce.rag.rag.document.RagDocumentService;
import com.ecommerce.rag.services.ProductService;

@RestController
@RequestMapping("/api/rag/chunks")
public class RagDocumentController {

    private final RagDocumentService ragDocumentService;
    private final ProductService productService;

    public RagDocumentController(RagDocumentService ragDocumentService, ProductService productService) {
        this.ragDocumentService = ragDocumentService;
        this.productService = productService;
    }

    @GetMapping("/preview")
    public ResponseEntity<List<RagChunkDocument>> preview(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        List<RagChunkDocument> allChunks = ragDocumentService.buildAllChunks();
        List<RagChunkDocument> preview = allChunks.stream()
                .limit(effectiveLimit)
                .toList();
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<RagChunkDocument>> chunksByProduct(
            @PathVariable("productId") String productId) {
        if (productService.findById(productId).isEmpty()) {
            throw new ProductNotFoundException(productId);
        }
        List<RagChunkDocument> chunks = ragDocumentService.buildChunksByProductId(productId);
        return ResponseEntity.ok(chunks);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        List<Product> allProducts = productService.listAll();
        Map<String, Long> byChunkType = ragDocumentService.countByChunkType();

        long totalChunks = byChunkType.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("total_products", allProducts.size());
        result.put("total_chunks", totalChunks);
        result.put("by_chunk_type", byChunkType);

        return ResponseEntity.ok(result);
    }
}
