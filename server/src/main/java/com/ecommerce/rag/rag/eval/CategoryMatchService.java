package com.ecommerce.rag.rag.eval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class CategoryMatchService {

    private static final Map<String, Set<String>> CATEGORY_ALIASES = new LinkedHashMap<>();
    private static final Map<String, Set<String>> SUBCATEGORY_ALIASES = new LinkedHashMap<>();

    static {
        CATEGORY_ALIASES.put("食品生活", Set.of("食品饮料", "食品生活"));
        CATEGORY_ALIASES.put("食品", Set.of("食品饮料", "食品生活"));
        CATEGORY_ALIASES.put("食品饮料", Set.of("食品饮料", "食品生活"));
        CATEGORY_ALIASES.put("美妆", Set.of("美妆护肤", "美妆"));
        CATEGORY_ALIASES.put("美妆护肤", Set.of("美妆护肤", "美妆"));
        CATEGORY_ALIASES.put("数码", Set.of("数码电子", "数码"));
        CATEGORY_ALIASES.put("数码电子", Set.of("数码电子", "数码"));
        CATEGORY_ALIASES.put("服饰", Set.of("服饰运动", "服饰"));
        CATEGORY_ALIASES.put("服饰运动", Set.of("服饰运动", "服饰"));
        CATEGORY_ALIASES.put("运动", Set.of("服饰运动", "运动"));

        SUBCATEGORY_ALIASES.put("洁面", Set.of("洁面", "洗面奶"));
        SUBCATEGORY_ALIASES.put("洗面奶", Set.of("洁面", "洗面奶"));
        SUBCATEGORY_ALIASES.put("耳机", Set.of("真无线耳机", "耳机", "头戴式耳机"));
        SUBCATEGORY_ALIASES.put("真无线耳机", Set.of("真无线耳机", "耳机"));
        SUBCATEGORY_ALIASES.put("手机", Set.of("智能手机", "手机"));
        SUBCATEGORY_ALIASES.put("智能手机", Set.of("智能手机", "手机"));
        SUBCATEGORY_ALIASES.put("跑鞋", Set.of("跑步鞋", "跑鞋"));
        SUBCATEGORY_ALIASES.put("跑步鞋", Set.of("跑步鞋", "跑鞋"));
        SUBCATEGORY_ALIASES.put("运动服", Set.of("短袖T恤", "速干T恤", "卫衣", "运动服"));
        SUBCATEGORY_ALIASES.put("背包", Set.of("背包", "双肩包"));
        SUBCATEGORY_ALIASES.put("双肩包", Set.of("背包", "双肩包"));
        SUBCATEGORY_ALIASES.put("零食", Set.of("坚果/零食", "零食"));
        SUBCATEGORY_ALIASES.put("坚果/零食", Set.of("坚果/零食", "零食"));
        SUBCATEGORY_ALIASES.put("饮料", Set.of("碳酸饮料", "功能饮料", "茶饮", "咖啡", "牛奶", "酸奶", "饮料"));
        SUBCATEGORY_ALIASES.put("茶饮", Set.of("茶饮", "饮料"));
        SUBCATEGORY_ALIASES.put("咖啡", Set.of("咖啡", "饮料"));
        SUBCATEGORY_ALIASES.put("牛奶", Set.of("牛奶", "饮料"));
        SUBCATEGORY_ALIASES.put("酸奶", Set.of("酸奶", "饮料"));
        SUBCATEGORY_ALIASES.put("食品礼盒", Set.of("坚果/零食", "食品礼盒"));
    }

    public boolean categoryMatches(String expectedCategory, String actualCategory) {
        if (expectedCategory == null || actualCategory == null) return false;
        if (expectedCategory.equals(actualCategory)) return true;

        Set<String> aliases = CATEGORY_ALIASES.get(expectedCategory);
        if (aliases != null && aliases.contains(actualCategory)) return true;

        aliases = CATEGORY_ALIASES.get(actualCategory);
        return aliases != null && aliases.contains(expectedCategory);
    }

    public boolean subCategoryMatches(String expectedSub, String actualSubCategory) {
        if (expectedSub == null || actualSubCategory == null) return false;
        if (actualSubCategory.contains(expectedSub) || expectedSub.contains(actualSubCategory)) return true;

        Set<String> aliases = SUBCATEGORY_ALIASES.get(expectedSub);
        if (aliases != null && aliases.contains(actualSubCategory)) return true;

        aliases = SUBCATEGORY_ALIASES.get(actualSubCategory);
        return aliases != null && aliases.contains(expectedSub);
    }

    public boolean subCategoryMatchesAny(List<String> expectedSubs, String actualSubCategory) {
        if (expectedSubs == null || actualSubCategory == null) return false;
        for (String expected : expectedSubs) {
            if (subCategoryMatches(expected, actualSubCategory)) return true;
        }
        return false;
    }
}
