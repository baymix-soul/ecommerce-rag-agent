package com.ecommerce.rag.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.dto.ProductSearchRequest;
import com.ecommerce.rag.models.dto.ProductSearchResponse;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.models.vo.ProductCard;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldLoadAllProducts() {
        List<Product> products = productService.listAll();
        assertNotNull(products);
        assertEquals(100, products.size());
    }

    @Test
    void shouldValidateUniqueProductIds() {
        List<Product> products = productService.listAll();
        long uniqueCount = products.stream()
                .map(Product::getProductId)
                .distinct()
                .count();
        assertEquals(products.size(), uniqueCount);
    }

    @Test
    void shouldFindExistingProductById() {
        Optional<Product> found = productService.findById("p_beauty_001");
        assertTrue(found.isPresent());
        assertEquals("p_beauty_001", found.get().getProductId());
        assertNotNull(found.get().getName());
        assertNotNull(found.get().getPrice());
    }

    @Test
    void shouldReturnEmptyForNonExistentId() {
        Optional<Product> found = productService.findById("non_existent_id");
        assertFalse(found.isPresent());
    }

    @Test
    void shouldSearchByKeyword() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setQuery("油皮 洗面奶");
        ProductSearchResponse response = productService.search(request);
        assertNotNull(response);
        assertNotNull(response.getProducts());
    }

    @Test
    void shouldFilterByCategoryAndMaxPrice() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategory("数码电子");
        request.setMaxPrice(new BigDecimal("200"));
        ProductSearchResponse response = productService.search(request);
        assertNotNull(response);
        for (ProductCard card : response.getProducts()) {
            assertTrue(card.getPrice().compareTo(new BigDecimal("200")) <= 0);
        }
    }

    @Test
    void shouldFilterByBrand() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setBrand("雅诗兰黛");
        ProductSearchResponse response = productService.search(request);
        assertNotNull(response);
        assertFalse(response.getProducts().isEmpty());
    }

    @Test
    void shouldRespectLimit() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setLimit(5);
        ProductSearchResponse response = productService.search(request);
        assertTrue(response.getProducts().size() <= 5);
    }

    @Test
    void shouldCapLimitAt20() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setLimit(50);
        assertEquals(20, request.getEffectiveLimit());
    }

    @Test
    void shouldDefaultLimitTo10() {
        ProductSearchRequest request = new ProductSearchRequest();
        assertEquals(10, request.getEffectiveLimit());
    }

    @Test
    void shouldConvertToProductCards() {
        List<Product> products = productService.listAll();
        List<ProductCard> cards = productService.toProductCards(products.subList(0, 5));
        assertEquals(5, cards.size());
        for (ProductCard card : cards) {
            assertNotNull(card.getProductId());
            assertNotNull(card.getName());
            assertNotNull(card.getPrice());
            assertNotNull(card.getCurrency());
            assertNotNull(card.getImageUrl());
            assertNotNull(card.getReason());
        }
    }

    @Test
    void shouldSearchWithEmptyQueryAndFiltersOnly() {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategory("美妆护肤");
        request.setMinPrice(new BigDecimal("100"));
        request.setMaxPrice(new BigDecimal("500"));
        ProductSearchResponse response = productService.search(request);
        assertNotNull(response);
        for (ProductCard card : response.getProducts()) {
            assertTrue(card.getPrice().compareTo(new BigDecimal("100")) >= 0);
            assertTrue(card.getPrice().compareTo(new BigDecimal("500")) <= 0);
        }
    }

    @Test
    void shouldHaveRequiredFieldsOnAllProducts() {
        List<Product> products = productService.listAll();
        for (Product p : products) {
            assertNotNull(p.getProductId(), "productId must not be null");
            assertFalse(p.getProductId().isBlank(), "productId must not be blank");
            assertNotNull(p.getName(), "name must not be null for " + p.getProductId());
            assertNotNull(p.getCategory(), "category must not be null for " + p.getProductId());
            assertNotNull(p.getSubCategory(), "subCategory must not be null for " + p.getProductId());
            assertNotNull(p.getPrice(), "price must not be null for " + p.getProductId());
            assertTrue(p.getPrice().compareTo(BigDecimal.ZERO) > 0, "price must be > 0 for " + p.getProductId());
            assertNotNull(p.getImageUrl(), "imageUrl must not be null for " + p.getProductId());
            assertNotNull(p.getDescription(), "description must not be null for " + p.getProductId());
            assertNotNull(p.getCurrency(), "currency must not be null for " + p.getProductId());
            assertEquals("CNY", p.getCurrency());
        }
    }
}
