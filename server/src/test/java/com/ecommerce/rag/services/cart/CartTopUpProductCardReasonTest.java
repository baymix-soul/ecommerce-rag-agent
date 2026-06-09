package com.ecommerce.rag.services.cart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.services.recommendation.RecommendationReasonService;

@SpringBootTest
class CartTopUpProductCardReasonTest {

    @Autowired
    private CartTopUpRecommendationService cartTopUpRecommendationService;

    @Autowired
    private RecommendationReasonService reasonService;

    @Test
    void topUpProductCardReasonShouldNotBePlaceholder() {
        CartView cart = createCartWithItem("p1", "商品A", new BigDecimal("500"), 1);
        BigDecimal targetAmount = new BigDecimal("1000");

        List<ProductCard> recommendations = cartTopUpRecommendationService.recommend(cart, targetAmount, 3);

        for (ProductCard card : recommendations) {
            assertNotNull(card.getReason(), "Top-up card reason should not be null");
            assertFalse(reasonService.isPlaceholder(card.getReason()),
                    "Top-up card reason should not be placeholder: " + card.getReason());
        }
    }

    @Test
    void topUpReasonShouldContainTopUpHint() {
        CartView cart = createCartWithItem("p1", "商品A", new BigDecimal("500"), 1);
        BigDecimal targetAmount = new BigDecimal("1000");

        List<ProductCard> recommendations = cartTopUpRecommendationService.recommend(cart, targetAmount, 3);

        for (ProductCard card : recommendations) {
            assertTrue(card.getReason().contains("凑单") || card.getReason().contains("接近"),
                    "Top-up reason should mention 凑单 or 接近: " + card.getReason());
        }
    }

    @Test
    void topUpReasonShouldNotCallLlm() {
        CartView cart = createCartWithItem("p1", "商品A", new BigDecimal("500"), 1);
        BigDecimal targetAmount = new BigDecimal("1000");

        List<ProductCard> recommendations = cartTopUpRecommendationService.recommend(cart, targetAmount, 3);

        assertNotNull(recommendations);
    }

    private CartView createCartWithItem(String productId, String name, BigDecimal price, int quantity) {
        CartView cart = new CartView();
        cart.setUserId("demo-user");
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setName(name);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setSubtotal(price.multiply(BigDecimal.valueOf(quantity)));
        cart.setItems(List.of(item));
        cart.setTotalQuantity(quantity);
        cart.setTotalAmount(item.getSubtotal());
        cart.setCurrency("CNY");
        return cart;
    }
}
