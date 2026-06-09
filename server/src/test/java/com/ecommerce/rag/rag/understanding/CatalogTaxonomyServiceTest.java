package com.ecommerce.rag.rag.understanding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogTaxonomyServiceTest {

    private CatalogTaxonomyService taxonomyService;

    @BeforeEach
    void setUp() {
        FakeProductService fake = new FakeProductService();
        taxonomyService = new CatalogTaxonomyService(fake);
        taxonomyService.init();
    }

    @Test
    void shouldReadCategories() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        assertNotNull(snapshot.getCategories());
        assertFalse(snapshot.getCategories().isEmpty());
        assertTrue(snapshot.getCategories().contains("美妆护肤"));
        assertTrue(snapshot.getCategories().contains("数码电子"));
    }

    @Test
    void shouldReadSubCategories() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        assertNotNull(snapshot.getAllSubCategories());
        assertFalse(snapshot.getAllSubCategories().isEmpty());
    }

    @Test
    void shouldGenerateCategoryToSubCategoriesMapping() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        Map<String, List<String>> subByCat = snapshot.getSubCategoriesByCategory();
        assertNotNull(subByCat);
        assertFalse(subByCat.isEmpty());
        if (subByCat.containsKey("美妆护肤")) {
            assertFalse(subByCat.get("美妆护肤").isEmpty());
        }
    }

    @Test
    void shouldReadBrands() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        assertNotNull(snapshot.getBrands());
        assertFalse(snapshot.getBrands().isEmpty());
    }

    @Test
    void shouldComputeMinMaxPrice() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        assertNotNull(snapshot.getMinPrice());
        assertNotNull(snapshot.getMaxPrice());
        assertTrue(snapshot.getMinPrice().compareTo(snapshot.getMaxPrice()) <= 0);
    }

    @Test
    void shouldNotErrorOnEmptyCatalog() {
        CatalogTaxonomyService emptyService = new CatalogTaxonomyService(new FakeEmptyProductService());
        emptyService.init();
        CatalogTaxonomySnapshot snapshot = emptyService.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.isEmpty());
        assertTrue(snapshot.getCategories().isEmpty());
    }

    @Test
    void shouldHaveFilterableAndTextFields() {
        CatalogTaxonomySnapshot snapshot = taxonomyService.getSnapshot();
        assertNotNull(snapshot.getFilterableFields());
        assertFalse(snapshot.getFilterableFields().isEmpty());
        assertTrue(snapshot.getFilterableFields().contains("category"));
        assertTrue(snapshot.getFilterableFields().contains("price"));
        assertNotNull(snapshot.getTextFields());
        assertTrue(snapshot.getTextFields().contains("name"));
        assertTrue(snapshot.getTextFields().contains("description"));
    }

    static class FakeProductService extends com.ecommerce.rag.services.ProductService {
        private final java.util.List<com.ecommerce.rag.models.entity.Product> fakeProducts;

        FakeProductService() {
            super(null, null);
            fakeProducts = buildFakeProducts();
        }

        @Override
        public java.util.List<com.ecommerce.rag.models.entity.Product> listAll() {
            return fakeProducts;
        }

        private java.util.List<com.ecommerce.rag.models.entity.Product> buildFakeProducts() {
            java.util.List<com.ecommerce.rag.models.entity.Product> list = new java.util.ArrayList<>();

            list.add(createProduct("p_beauty_001", "精华液", "兰蔻", "美妆护肤", "精华", 720));
            list.add(createProduct("p_beauty_002", "洁面乳", "雅诗兰黛", "美妆护肤", "洁面", 89));
            list.add(createProduct("p_beauty_003", "防晒霜", "安热沙", "美妆护肤", "防晒", 159));
            list.add(createProduct("p_digital_001", "真无线耳机", "华为", "数码电子", "真无线耳机", 699));
            list.add(createProduct("p_digital_002", "笔记本电脑", "Apple", "数码电子", "笔记本电脑", 8999));
            list.add(createProduct("p_sport_001", "跑步鞋", "Nike", "服饰运动", "跑步鞋", 599));
            list.add(createProduct("p_sport_002", "双肩包", "Herschel", "服饰运动", "背包", 399));
            list.add(createProduct("p_food_001", "坚果礼盒", "三只松鼠", "食品饮料", "坚果/零食", 129));

            return list;
        }

        private com.ecommerce.rag.models.entity.Product createProduct(
                String id, String name, String brand, String category, String subCategory, int price) {
            com.ecommerce.rag.models.entity.Product p = new com.ecommerce.rag.models.entity.Product();
            p.setProductId(id);
            p.setName(name);
            p.setBrand(brand);
            p.setCategory(category);
            p.setSubCategory(subCategory);
            p.setPrice(new BigDecimal(price));
            p.setCurrency("CNY");
            return p;
        }
    }

    static class FakeEmptyProductService extends com.ecommerce.rag.services.ProductService {
        FakeEmptyProductService() {
            super(null, null);
        }

        @Override
        public java.util.List<com.ecommerce.rag.models.entity.Product> listAll() {
            return java.util.Collections.emptyList();
        }
    }
}
