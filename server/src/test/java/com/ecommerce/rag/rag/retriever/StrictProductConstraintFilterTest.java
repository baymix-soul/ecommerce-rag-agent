package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.brand.BrandAliasService;
import com.ecommerce.rag.rag.eval.CategoryMatchService;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class StrictProductConstraintFilterTest {

    private final CategoryMatchService categoryMatchService = new CategoryMatchService();
    private final BrandAliasService brandAliasService = new BrandAliasService();
    private final StrictProductConstraintFilter filter = new StrictProductConstraintFilter(
            categoryMatchService, brandAliasService);

    private Product makeProduct(String id, String category, String subCategory,
                                 BigDecimal price, String brand, String name, String description) {
        Product p = new Product();
        p.setProductId(id);
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(price);
        p.setBrand(brand);
        p.setName(name);
        p.setDescription(description);
        return p;
    }

    @Test
    void backpackShouldFailRunningShoeSubCategoryConstraint() {
        Product backpack = makeProduct("p_backpack_001", "服饰运动", "背包",
                new BigDecimal("299"), "Nike", "运动背包", "大容量运动背包");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        assertFalse(filter.passes(backpack, analysis),
                "Backpack should fail running shoe subCategory constraint");
    }

    @Test
    void tshirtShouldFailRunningShoeSubCategoryConstraint() {
        Product tshirt = makeProduct("p_clothes_010", "服饰运动", "短袖T恤",
                new BigDecimal("199"), "Adidas", "运动T恤", "透气速干");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        assertFalse(filter.passes(tshirt, analysis),
                "T-shirt should fail running shoe subCategory constraint");
    }

    @Test
    void productOverMaxPriceShouldFail() {
        Product expensiveShoe = makeProduct("p_shoe_001", "服饰运动", "跑步鞋",
                new BigDecimal("1099"), "Nike", "高端跑鞋", "顶级缓震");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));

        assertFalse(filter.passes(expensiveShoe, analysis),
                "1099 product should fail maxPrice=1000 constraint");
    }

    @Test
    void productWithinMaxPriceShouldPass() {
        Product affordableShoe = makeProduct("p_shoe_002", "服饰运动", "跑步鞋",
                new BigDecimal("999"), "Adidas", "实惠跑鞋", "日常训练");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setMaxPrice(new BigDecimal("1000"));

        assertTrue(filter.passes(affordableShoe, analysis),
                "999 product should pass maxPrice=1000 constraint");
    }

    @Test
    void nikeProductShouldFailNegativeBrandNike() {
        Product nikeShoe = makeProduct("p_shoe_003", "服饰运动", "跑步鞋",
                new BigDecimal("599"), "Nike", "Nike跑鞋", "舒适缓震");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("耐克");
        analysis.getNegativeBrands().add("Nike");

        assertFalse(filter.passes(nikeShoe, analysis),
                "Nike product should fail when Nike is in negativeBrands");
    }

    @Test
    void naikeChineseProductShouldFailNegativeBrandNike() {
        Product naikeShoe = makeProduct("p_shoe_004", "服饰运动", "跑步鞋",
                new BigDecimal("599"), "耐克", "耐克跑鞋", "舒适缓震");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("Nike");

        assertFalse(filter.passes(naikeShoe, analysis),
                "耐克 product should fail when Nike is in negativeBrands");
    }

    @Test
    void excludedProductIdShouldFail() {
        Product excluded = makeProduct("p_excluded_001", "服饰运动", "跑步鞋",
                new BigDecimal("499"), "Li-Ning", "李宁跑鞋", "国产优质");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getExcludeProductIds().add("p_excluded_001");

        assertFalse(filter.passes(excluded, analysis),
                "Product in excludeProductIds should fail");
    }

    @Test
    void productWithAlcoholDescriptionShouldFailNegativeKeyword() {
        Product alcoholProduct = makeProduct("p_beauty_099", "美妆护肤", "精华",
                new BigDecimal("299"), "某品牌", "含酒精精华", "本产品含有酒精成分，适合油皮使用");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("美妆护肤");
        analysis.getNegativeKeywords().add("酒精");

        assertFalse(filter.passes(alcoholProduct, analysis),
                "Product with alcohol description should fail negative keyword filter");
    }

    @Test
    void productWithNoAlcoholShouldPassNegativeKeyword() {
        Product noAlcohol = makeProduct("p_beauty_100", "美妆护肤", "精华",
                new BigDecimal("399"), "某品牌", "无酒精精华", "不含酒精，温和配方");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("美妆护肤");
        analysis.getNegativeKeywords().add("酒精");

        assertFalse(filter.passes(noAlcohol, analysis),
                "Product with '不含酒精' should fail because description contains 酒精 text");
    }

    @Test
    void nonRunningShoeInDifferentCategoryShouldFailCategoryConstraint() {
        Product digitalProduct = makeProduct("p_digital_001", "数码电子", "智能手机",
                new BigDecimal("4999"), "Apple", "iPhone", "旗舰手机");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");

        assertFalse(filter.passes(digitalProduct, analysis),
                "Digital product should fail sportswear category constraint");
    }
}
