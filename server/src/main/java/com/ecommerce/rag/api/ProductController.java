package com.ecommerce.rag.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.rag.core.exception.ProductNotFoundException;
import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.services.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductCard>> listProducts(
            @RequestParam(required = false) Integer limit) {
        int effectiveLimit = 20;
        if (limit != null && limit > 0) {
            effectiveLimit = Math.min(limit, 100);
        }
        List<Product> all = productService.listAll();
        List<ProductCard> cards = all.stream()
                .limit(effectiveLimit)
                .map(productService::toProductCard)
                .toList();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        log.info("Getting product: {}", productId);
        Product product = productService.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return ResponseEntity.ok(product);
    }

    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestBody ProductSearchRequest request) {
        log.info("Searching products: query={}, category={}, brand={}",
                request.getQuery(), request.getCategory(), request.getBrand());
        ProductSearchResponse response = productService.search(request);
        return ResponseEntity.ok(response);
    }
}
