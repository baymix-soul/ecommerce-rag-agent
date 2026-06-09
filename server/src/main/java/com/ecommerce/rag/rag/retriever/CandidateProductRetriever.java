package com.ecommerce.rag.rag.retriever;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.services.ProductService;

@Component
public class CandidateProductRetriever {

    private static final Logger log = LoggerFactory.getLogger(CandidateProductRetriever.class);

    private final ProductService productService;

    public CandidateProductRetriever(ProductService productService) {
        this.productService = productService;
    }

    public List<ChatCandidate> retrieve(String message, int limit) {
        if (message == null || message.isBlank()) {
            return Collections.emptyList();
        }

        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery(message);
        request.setLimit(limit);

        ProductSearchResponse response = productService.search(request);
        List<ProductCard> cards = response.getProducts();

        log.info("Retrieved {} candidate products for query: {}", cards.size(), message);

        return cards.stream()
                .map(this::toChatCandidate)
                .toList();
    }

    private ChatCandidate toChatCandidate(ProductCard card) {
        ChatCandidate candidate = new ChatCandidate();
        candidate.setProductId(card.getProductId());
        candidate.setName(card.getName());
        candidate.setPrice(card.getPrice());
        candidate.setCurrency(card.getCurrency());
        candidate.setImageUrl(card.getImageUrl());
        return candidate;
    }
}
