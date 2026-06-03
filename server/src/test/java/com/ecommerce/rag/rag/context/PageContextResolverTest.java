package com.ecommerce.rag.rag.context;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.PageContext;
import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.services.ProductService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PageContextResolverTest {

    private ProductService productService;
    private PageContextResolver resolver;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        resolver = new PageContextResolver(productService);
    }

    @Test
    void nullPageContextShouldReturnUnknown() {
        PageContextResolution result = resolver.resolve(null);

        assertEquals(PageType.UNKNOWN, result.getPageType());
        assertFalse(result.isHasValidCurrentProduct());
        assertTrue(result.getWarnings().contains("page_context is null"));
    }

    @Test
    void productDetailCurrentProductIdShouldResolveProduct() {
        Product product = createProduct("p_beauty_001", "测试精华", "美妆护肤", "精华");
        when(productService.findById("p_beauty_001")).thenReturn(java.util.Optional.of(product));

        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_DETAIL);
        ctx.setCurrentProductId("p_beauty_001");

        PageContextResolution result = resolver.resolve(ctx);

        assertEquals(PageType.PRODUCT_DETAIL, result.getPageType());
        assertTrue(result.isHasValidCurrentProduct());
        assertEquals("p_beauty_001", result.getCurrentProduct().getProductId());
        assertEquals("测试精华", result.getCurrentProduct().getName());
    }

    @Test
    void productDetailMissingCurrentProductIdShouldWarn() {
        when(productService.findById("non_existent")).thenReturn(java.util.Optional.empty());

        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_DETAIL);
        ctx.setCurrentProductId("non_existent");

        PageContextResolution result = resolver.resolve(ctx);

        assertFalse(result.isHasValidCurrentProduct());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("not found")));
    }

    @Test
    void productDetailNullProductIdShouldWarn() {
        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_DETAIL);
        ctx.setCurrentProductId(null);

        PageContextResolution result = resolver.resolve(ctx);

        assertFalse(result.isHasValidCurrentProduct());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("current_product_id is null")));
    }

    @Test
    void productListVisibleProductIdsShouldResolveProducts() {
        Product p1 = createProduct("p_digital_001", "耳机A", "数码电子", "真无线耳机");
        Product p2 = createProduct("p_digital_002", "耳机B", "数码电子", "真无线耳机");
        when(productService.findById("p_digital_001")).thenReturn(java.util.Optional.of(p1));
        when(productService.findById("p_digital_002")).thenReturn(java.util.Optional.of(p2));

        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_LIST);
        ctx.setVisibleProductIds(List.of("p_digital_001", "p_digital_002"));
        ctx.setSearchQuery("耳机");

        PageContextResolution result = resolver.resolve(ctx);

        assertEquals(PageType.PRODUCT_LIST, result.getPageType());
        assertEquals(2, result.getVisibleProducts().size());
        assertEquals("耳机", result.getPageSearchQuery());
    }

    @Test
    void productListMissingVisibleIdsShouldWarn() {
        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_LIST);
        ctx.setVisibleProductIds(List.of());
        ctx.setSearchQuery("耳机");

        PageContextResolution result = resolver.resolve(ctx);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("visible_product_ids is empty")));
    }

    @Test
    void recentlyViewedShouldResolveProducts() {
        Product p1 = createProduct("p_beauty_001", "精华A", "美妆护肤", "精华");
        when(productService.findById("p_beauty_001")).thenReturn(java.util.Optional.of(p1));
        when(productService.findById("p_digital_003")).thenReturn(java.util.Optional.empty());

        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.PRODUCT_DETAIL);
        ctx.setCurrentProductId("p_beauty_001");
        ctx.setRecentlyViewedProductIds(List.of("p_beauty_001", "p_digital_003"));

        PageContextResolution result = resolver.resolve(ctx);

        assertEquals(1, result.getRecentlyViewedProducts().size());
        assertEquals("p_beauty_001", result.getRecentlyViewedProducts().get(0).getProductId());
    }

    @Test
    void chatPageTypeShouldUseDefaultRetrieval() {
        PageContext ctx = new PageContext();
        ctx.setPageType(PageType.CHAT);

        PageContextResolution result = resolver.resolve(ctx);

        assertEquals(PageType.CHAT, result.getPageType());
    }

    private Product createProduct(String id, String name, String category, String subCategory) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(new java.math.BigDecimal("100"));
        p.setBrand("测试品牌");
        p.setDescription("测试描述");
        p.setCurrency("CNY");
        return p;
    }
}
