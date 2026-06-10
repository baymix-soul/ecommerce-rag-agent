package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.brand.BrandAliasService;
import com.ecommerce.rag.rag.eval.CategoryMatchService;
import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class StrictProductConstraintFilterNegativeBrandTest {

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
    void appleMacBookShouldFailWhenNegativeBrandsChineseApple() {
        Product macbook = makeProduct("p_digital_001", "数码电子", "笔记本电脑",
                new BigDecimal("7999"), "Apple", "Apple MacBook Air", "轻薄笔记本电脑");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.getNegativeBrands().add("苹果");

        assertFalse(filter.passes(macbook, analysis),
                "Apple MacBook should fail when negativeBrands contains 苹果");
    }

    @Test
    void appleMacBookShouldFailWhenNegativeBrandsEnglishApple() {
        Product macbook = makeProduct("p_digital_002", "数码电子", "笔记本电脑",
                new BigDecimal("9999"), "Apple", "MacBook Pro", "专业性能笔记本");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.getNegativeBrands().add("Apple");

        assertFalse(filter.passes(macbook, analysis),
                "Apple MacBook should fail when negativeBrands contains Apple");
    }

    @Test
    void appleIpadShouldFailWhenNegativeBrandsChineseApple() {
        Product ipad = makeProduct("p_digital_003", "数码电子", "平板电脑",
                new BigDecimal("6799"), "Apple", "Apple iPad Pro", "平板电脑");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.setSubCategory("平板电脑");
        analysis.getNegativeBrands().add("苹果");

        assertFalse(filter.passes(ipad, analysis),
                "Apple iPad should fail when negativeBrands contains 苹果");
    }

    @Test
    void lenovoThinkPadShouldPassWhenNegativeBrandsChineseApple() {
        Product thinkpad = makeProduct("p_digital_004", "数码电子", "笔记本电脑",
                new BigDecimal("8999"), "联想", "联想 ThinkPad X1 Carbon", "商务笔记本电脑");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.getNegativeBrands().add("苹果");

        assertTrue(filter.passes(thinkpad, analysis),
                "联想 ThinkPad should pass when negativeBrands only contains 苹果");
    }

    @Test
    void filteredOutShouldContainNegativeBrandFailure() {
        Product macbook = makeProduct("p_digital_005", "数码电子", "笔记本电脑",
                new BigDecimal("7999"), "Apple", "Apple MacBook Air", "轻薄笔记本电脑");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.getNegativeBrands().add("苹果");

        ConstraintCheckResult result = filter.check(macbook, analysis);
        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("NEGATIVE_BRAND")),
                "Failed rules should contain NEGATIVE_BRAND");
    }

    @Test
    void categoryAndSubCategoryShouldStillWorkWithNegativeBrands() {
        Product wrongCategory = makeProduct("p_digital_006", "服饰运动", "背包",
                new BigDecimal("299"), "某品牌", "运动背包", "大容量背包");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.getNegativeBrands().add("苹果");

        ConstraintCheckResult result = filter.check(wrongCategory, analysis);
        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("category_mismatch")),
                "Category mismatch should still be detected");
    }

    @Test
    void nikeProductShouldFailWhenNegativeBrandsChineseNike() {
        Product nikeShoe = makeProduct("p_shoe_001", "服饰运动", "跑步鞋",
                new BigDecimal("599"), "Nike", "Nike跑鞋", "舒适缓震");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("耐克");

        assertFalse(filter.passes(nikeShoe, analysis),
                "Nike product should fail when negativeBrands contains 耐克");
    }

    @Test
    void adidasProductShouldPassWhenNegativeBrandsChineseNike() {
        Product adidasShoe = makeProduct("p_shoe_002", "服饰运动", "跑步鞋",
                new BigDecimal("499"), "Adidas", "Adidas跑鞋", "轻量透气");
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.getNegativeBrands().add("耐克");

        assertTrue(filter.passes(adidasShoe, analysis),
                "Adidas product should pass when negativeBrands only contains 耐克");
    }

    @Test
    void priceConstraintShouldStillWorkWithNegativeBrands() {
        Product expensiveLaptop = makeProduct("p_digital_007", "数码电子", "笔记本电脑",
                new BigDecimal("15000"), "联想", "联想高端本", "旗舰笔记本");
        QueryAnalysisResult analysis = buildLaptopAnalysis();
        analysis.setMaxPrice(new BigDecimal("10000"));
        analysis.getNegativeBrands().add("苹果");

        ConstraintCheckResult result = filter.check(expensiveLaptop, analysis);
        assertFalse(result.isPassed());
        assertTrue(result.getFailedRules().stream().anyMatch(r -> r.contains("price_gt_max")),
                "Price constraint should still be detected");
    }

    private QueryAnalysisResult buildLaptopAnalysis() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("数码电子");
        analysis.setSubCategory("笔记本电脑");
        return analysis;
    }
}
