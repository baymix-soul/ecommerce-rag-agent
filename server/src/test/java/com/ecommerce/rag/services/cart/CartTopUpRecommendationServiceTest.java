package com.ecommerce.rag.services.cart;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.rag.core.config.AppProperties;
import com.ecommerce.rag.models.dto.CartItem;
import com.ecommerce.rag.models.dto.CartView;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.models.vo.ProductCard;
import com.ecommerce.rag.services.ProductService;

@ExtendWith(MockitoExtension.class)
class CartTopUpRecommendationServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private com.ecommerce.rag.services.recommendation.RecommendationReasonService recommendationReasonService;

    private AppProperties appProperties;

    private CartTopUpRecommendationService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        AppProperties.CartProperties cartProps = new AppProperties.CartProperties();
        cartProps.setTopUpTolerance(100);
        appProperties.setCart(cartProps);

        service = new CartTopUpRecommendationService(productService, appProperties, recommendationReasonService);
    }

    private Product createProduct(String id, String name, BigDecimal price) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setPrice(price);
        p.setCurrency("CNY");
        p.setImageUrl("/images/" + id + ".jpg");
        return p;
    }

    private CartView createCart(String productId, String name, BigDecimal price, BigDecimal totalAmount) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setName(name);
        item.setPrice(price);
        item.setQuantity(1);

        CartView cart = new CartView();
        cart.setItems(List.of(item));
        cart.setTotalQuantity(1);
        cart.setTotalAmount(totalAmount);
        return cart;
    }

    @Test
    @DisplayName("Gap calculation: total=780, target=1000 → gap=220, recommend products with price close to 220")
    void testGapCalculation() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("200"));
        Product p2 = createProduct("p2", "商品B", new BigDecimal("230"));
        Product p3 = createProduct("p3", "商品C", new BigDecimal("300"));
        when(productService.listAll()).thenReturn(List.of(p1, p2, p3));

        CartView cart = createCart("cart_item_1", "已加购商品", new BigDecimal("780"), new BigDecimal("780"));

        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 3);

        assertFalse(result.isEmpty(), "Should have recommendations for gap=220");
        // All recommended products should have price <= gap + tolerance = 320
        for (ProductCard card : result) {
            assertTrue(card.getPrice().compareTo(new BigDecimal("320")) <= 0,
                    "Product price should be within gap + tolerance");
        }
    }

    @Test
    @DisplayName("Recommend closest to gap: prices 200, 230, 300 with gap=220 → first should be 230")
    void testRecommendClosestToGap() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("200"));
        Product p2 = createProduct("p2", "商品B", new BigDecimal("230"));
        Product p3 = createProduct("p3", "商品C", new BigDecimal("300"));
        when(productService.listAll()).thenReturn(List.of(p1, p2, p3));

        CartView cart = createCart("cart_item_1", "已加购商品", new BigDecimal("780"), new BigDecimal("780"));

        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 3);

        assertFalse(result.isEmpty());
        assertEquals(new BigDecimal("230"), result.get(0).getPrice(),
                "First recommendation should be closest to gap=220");
    }

    @Test
    @DisplayName("Exclude products already in cart from recommendations")
    void testExcludeCartProducts() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("200"));
        Product p2 = createProduct("p2", "商品B", new BigDecimal("230"));
        Product p3 = createProduct("p3", "商品C", new BigDecimal("300"));
        when(productService.listAll()).thenReturn(List.of(p1, p2, p3));

        // p1 is already in cart
        CartView cart = createCart("p1", "商品A", new BigDecimal("200"), new BigDecimal("780"));

        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 3);

        for (ProductCard card : result) {
            assertNotEquals("p1", card.getProductId(),
                    "Product already in cart should not be recommended");
        }
    }

    @Test
    @DisplayName("Return at most 3 recommendations even if more products match")
    void testMaxThreeRecommendations() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("200"));
        Product p2 = createProduct("p2", "商品B", new BigDecimal("210"));
        Product p3 = createProduct("p3", "商品C", new BigDecimal("220"));
        Product p4 = createProduct("p4", "商品D", new BigDecimal("230"));
        Product p5 = createProduct("p5", "商品E", new BigDecimal("240"));
        when(productService.listAll()).thenReturn(List.of(p1, p2, p3, p4, p5));

        CartView cart = createCart("cart_item_1", "已加购商品", new BigDecimal("780"), new BigDecimal("780"));

        // Request 10, but should cap at 3
        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 10);

        assertTrue(result.size() <= 3, "Should return at most 3 recommendations");
    }

    @Test
    @DisplayName("No recommendations when gap is zero or negative")
    void testNoRecommendationWhenGapZeroOrNegative() {
        // No need to mock productService.listAll() — the service returns early when gap <= 0

        // Gap = 0: total equals target
        CartView cartAtTarget = createCart("cart_item_1", "已加购商品", new BigDecimal("1000"), new BigDecimal("1000"));
        List<ProductCard> resultZero = service.recommend(cartAtTarget, new BigDecimal("1000"), 3);
        assertTrue(resultZero.isEmpty(), "Should return empty when gap is zero");

        // Gap < 0: total exceeds target
        CartView cartOverTarget = createCart("cart_item_1", "已加购商品", new BigDecimal("1200"), new BigDecimal("1200"));
        List<ProductCard> resultNegative = service.recommend(cartOverTarget, new BigDecimal("1000"), 3);
        assertTrue(resultNegative.isEmpty(), "Should return empty when gap is negative");
    }

    @Test
    @DisplayName("Tolerance allows slightly over gap: gap=220, tolerance=100, product at 300 should be included")
    void testToleranceAllowsSlightlyOverGap() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("300"));
        Product p2 = createProduct("p2", "商品B", new BigDecimal("350"));
        when(productService.listAll()).thenReturn(List.of(p1, p2));

        CartView cart = createCart("cart_item_1", "已加购商品", new BigDecimal("780"), new BigDecimal("780"));

        // gap=220, tolerance=100, maxPrice=320
        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 3);

        // p1 at 300 is within gap + tolerance (320), should be included
        boolean hasP1 = result.stream().anyMatch(c -> "p1".equals(c.getProductId()));
        assertTrue(hasP1, "Product at 300 should be included (within gap+tolerance=320)");

        // p2 at 350 exceeds gap + tolerance (320), should NOT be included
        boolean hasP2 = result.stream().anyMatch(c -> "p2".equals(c.getProductId()));
        assertFalse(hasP2, "Product at 350 should NOT be included (exceeds gap+tolerance=320)");
    }

    @Test
    @DisplayName("Service only depends on ProductService and AppProperties, no LLM/Qdrant dependencies")
    void testDoesNotCallLlmOrQdrant() {
        Product p1 = createProduct("p1", "商品A", new BigDecimal("200"));
        when(productService.listAll()).thenReturn(List.of(p1));

        CartView cart = createCart("cart_item_1", "已加购商品", new BigDecimal("780"), new BigDecimal("780"));

        // The service should work with only ProductService and AppProperties
        // No LLM client, Qdrant client, or Redis client involved
        List<ProductCard> result = service.recommend(cart, new BigDecimal("1000"), 3);

        // Verify only productService.listAll() was called — no other interactions
        verify(productService).listAll();
        verifyNoMoreInteractions(productService);
    }
}
