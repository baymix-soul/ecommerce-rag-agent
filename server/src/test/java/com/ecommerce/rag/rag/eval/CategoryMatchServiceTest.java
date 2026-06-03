package com.ecommerce.rag.rag.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CategoryMatchServiceTest {

    private final CategoryMatchService svc = new CategoryMatchService();

    @Test
    void foodLifeShouldMatchFoodBeverage() {
        assertTrue(svc.categoryMatches("食品生活", "食品饮料"));
        assertTrue(svc.categoryMatches("食品饮料", "食品生活"));
        assertTrue(svc.categoryMatches("食品", "食品饮料"));
    }

    @Test
    void cleansingShouldMatchFacialCleanser() {
        assertTrue(svc.subCategoryMatches("洁面", "洗面奶"));
        assertTrue(svc.subCategoryMatches("洗面奶", "洁面"));
    }

    @Test
    void headphoneShouldMatchTrueWireless() {
        assertTrue(svc.subCategoryMatches("耳机", "真无线耳机"));
        assertTrue(svc.subCategoryMatches("真无线耳机", "耳机"));
    }

    @Test
    void sportswearShouldMatchTShirt() {
        assertTrue(svc.subCategoryMatches("运动服", "短袖T恤"));
        assertTrue(svc.subCategoryMatches("运动服", "速干T恤"));
        assertTrue(svc.subCategoryMatches("运动服", "卫衣"));
    }

    @Test
    void drinksShouldMatchTeaCoffee() {
        assertTrue(svc.subCategoryMatches("饮料", "茶饮"));
        assertTrue(svc.subCategoryMatches("饮料", "咖啡"));
        assertTrue(svc.subCategoryMatches("饮料", "牛奶"));
        assertTrue(svc.subCategoryMatches("饮料", "酸奶"));
    }

    @Test
    void beautyShouldMatchSkinCare() {
        assertTrue(svc.categoryMatches("美妆", "美妆护肤"));
        assertTrue(svc.categoryMatches("美妆护肤", "美妆"));
    }

    @Test
    void subCategoryMatchesAnyShouldWork() {
        assertTrue(svc.subCategoryMatchesAny(
                java.util.List.of("短袖T恤", "速干T恤", "卫衣"), "短袖T恤"));
        assertTrue(svc.subCategoryMatchesAny(
                java.util.List.of("短袖T恤", "速干T恤", "卫衣"), "速干T恤"));
        assertFalse(svc.subCategoryMatchesAny(
                java.util.List.of("短袖T恤", "速干T恤", "卫衣"), "羽绒服"));
    }
}
