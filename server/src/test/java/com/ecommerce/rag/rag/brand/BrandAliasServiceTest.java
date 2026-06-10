package com.ecommerce.rag.rag.brand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;

class BrandAliasServiceTest {

    private final BrandAliasService service = new BrandAliasService();

    @Test
    void appleChineseToApple() {
        Set<String> aliases = service.expandBrandAliases("苹果");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Apple")));
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("苹果")));
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("macbook")));
    }

    @Test
    void appleEnglishToApple() {
        Set<String> aliases = service.expandBrandAliases("Apple");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Apple")));
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("苹果")));
    }

    @Test
    void macbookToApple() {
        Set<String> aliases = service.expandBrandAliases("MacBook");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Apple")));
    }

    @Test
    void ipadToApple() {
        Set<String> aliases = service.expandBrandAliases("iPad");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Apple")));
    }

    @Test
    void iphoneToApple() {
        Set<String> aliases = service.expandBrandAliases("iPhone");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Apple")));
    }

    @Test
    void productBrandAppleWithNegativeBrandChineseAppleShouldMatch() {
        Product product = new Product();
        product.setProductId("p_digital_001");
        product.setBrand("Apple");
        product.setName("Apple MacBook Air");
        product.setCategory("数码电子");
        product.setSubCategory("笔记本电脑");
        product.setPrice(new BigDecimal("7999"));

        assertTrue(service.matchesBrandOrAlias(product, List.of("苹果")));
    }

    @Test
    void productNameMacBookWithNegativeBrandChineseAppleShouldMatch() {
        Product product = new Product();
        product.setProductId("p_digital_002");
        product.setBrand(null);
        product.setName("MacBook Pro 14英寸");
        product.setCategory("数码电子");
        product.setSubCategory("笔记本电脑");
        product.setPrice(new BigDecimal("14999"));

        assertTrue(service.matchesBrandOrAlias(product, List.of("苹果")));
    }

    @Test
    void lenovoProductWithNegativeBrandChineseAppleShouldNotMatch() {
        Product product = new Product();
        product.setProductId("p_digital_003");
        product.setBrand("联想");
        product.setName("联想 ThinkPad X1 Carbon");
        product.setCategory("数码电子");
        product.setSubCategory("笔记本电脑");
        product.setPrice(new BigDecimal("8999"));

        assertFalse(service.matchesBrandOrAlias(product, List.of("苹果")));
    }

    @Test
    void nikeChineseToNike() {
        Set<String> aliases = service.expandBrandAliases("耐克");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Nike")));
    }

    @Test
    void adidasChineseToAdidas() {
        Set<String> aliases = service.expandBrandAliases("阿迪");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Adidas")));
    }

    @Test
    void adidasFullChineseToAdidas() {
        Set<String> aliases = service.expandBrandAliases("阿迪达斯");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("Adidas")));
    }

    @Test
    void lenovoAliasThinkPad() {
        Set<String> aliases = service.expandBrandAliases("ThinkPad");
        assertTrue(aliases.stream().anyMatch(a -> a.equalsIgnoreCase("联想")));
    }

    @Test
    void resolveCanonicalAppleChinese() {
        assertEquals("Apple", service.resolveCanonical("苹果"));
    }

    @Test
    void resolveCanonicalAppleEnglish() {
        assertEquals("Apple", service.resolveCanonical("Apple"));
    }

    @Test
    void resolveCanonicalUnknownBrand() {
        assertEquals("UnknownBrand", service.resolveCanonical("UnknownBrand"));
    }

    @Test
    void productDescriptionContainsMacbookWithNegativeBrandAppleShouldMatch() {
        Product product = new Product();
        product.setProductId("p_digital_004");
        product.setBrand("某品牌");
        product.setName("高性能轻薄本");
        product.setDescription("性能媲美 MacBook Pro，适合编程和设计");
        product.setCategory("数码电子");
        product.setSubCategory("笔记本电脑");
        product.setPrice(new BigDecimal("5999"));

        assertTrue(service.matchesBrandOrAlias(product, List.of("苹果")));
    }

    @Test
    void productWithNegativeBrandAppleShouldMatchIpad() {
        Product product = new Product();
        product.setProductId("p_digital_005");
        product.setBrand("Apple");
        product.setName("Apple iPad Pro");
        product.setCategory("数码电子");
        product.setSubCategory("平板电脑");
        product.setPrice(new BigDecimal("6799"));

        assertTrue(service.matchesBrandOrAlias(product, List.of("苹果")));
    }

    @Test
    void productWithNegativeBrandAppleShouldMatchIphone() {
        Product product = new Product();
        product.setProductId("p_digital_006");
        product.setBrand("Apple");
        product.setName("Apple iPhone 16 Pro");
        product.setCategory("数码电子");
        product.setSubCategory("智能手机");
        product.setPrice(new BigDecimal("8999"));

        assertTrue(service.matchesBrandOrAlias(product, List.of("Apple")));
    }
}
