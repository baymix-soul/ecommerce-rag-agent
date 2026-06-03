package com.ecommerce.rag.rag.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.PageType;
import com.ecommerce.rag.models.entity.Product;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.rag.memory.ConversationState;

import static org.junit.jupiter.api.Assertions.*;

class QueryAnalyzerPageContextTest {

    private QueryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new QueryAnalyzer();
    }

    @Test
    void productDetailRefersToCurrentShouldSetCurrentProductId() {
        Product current = createProduct("p_beauty_001", "雅诗兰黛精华", "美妆护肤", "精华", 720);
        PageContextResolution pageContext = createDetailContext(current);

        QueryAnalysisResult result = analyzer.analyze("这个适合敏感肌吗", null, pageContext);

        assertEquals("p_beauty_001", result.getCurrentProductId());
        assertTrue(result.getInheritedFromPageContext());
        assertTrue(result.getBoostedProductIds().contains("p_beauty_001"));
        assertTrue(result.getPageWarnings().stream().anyMatch(w -> w.contains("current product QA")));
    }

    @Test
    void productDetailCheaperShouldInheritCategoryAndExcludeCurrent() {
        Product current = createProduct("p_beauty_001", "雅诗兰黛精华", "美妆护肤", "精华", 720);
        PageContextResolution pageContext = createDetailContext(current);

        QueryAnalysisResult result = analyzer.analyze("有没有更便宜的", null, pageContext);

        assertEquals("美妆护肤", result.getCategory());
        assertEquals("精华", result.getSubCategory());
        assertTrue(result.getExcludeProductIds().contains("p_beauty_001"));
        assertNotNull(result.getMaxPrice());
        assertTrue(result.getMaxPrice().compareTo(new BigDecimal("720")) < 0);
    }

    @Test
    void productDetailSimilarShouldInheritAndExclude() {
        Product current = createProduct("p_digital_003", "索尼耳机", "数码电子", "头戴式耳机", 1500);
        PageContextResolution pageContext = createDetailContext(current);

        QueryAnalysisResult result = analyzer.analyze("有没有类似的", null, pageContext);

        assertEquals("数码电子", result.getCategory());
        assertTrue(result.getExcludeProductIds().contains("p_digital_003"));
    }

    @Test
    void productDetailChangeOneShouldExcludeCurrent() {
        Product current = createProduct("p_beauty_001", "雅诗兰黛精华", "美妆护肤", "精华", 720);
        PageContextResolution pageContext = createDetailContext(current);

        QueryAnalysisResult result = analyzer.analyze("换一个", null, pageContext);

        assertTrue(result.getExcludeProductIds().contains("p_beauty_001"));
        assertEquals("美妆护肤", result.getCategory());
    }

    @Test
    void productListSelectedFiltersCategoryShouldEnterAnalysis() {
        PageContextResolution pageContext = new PageContextResolution();
        pageContext.setPageType(PageType.PRODUCT_LIST);
        pageContext.setPageSearchQuery("耳机");
        pageContext.setSelectedFilters(Map.of("category", "数码电子"));

        QueryAnalysisResult result = analyzer.analyze("有没有更便宜的耳机", null, pageContext);

        assertEquals("数码电子", result.getCategory());
        assertTrue(result.getInheritedFromPageContext());
        assertEquals("数码电子", result.getPageFilters().get("category"));
    }

    @Test
    void productListSearchQueryShouldMergeWithNormalizedQuery() {
        PageContextResolution pageContext = new PageContextResolution();
        pageContext.setPageType(PageType.PRODUCT_LIST);
        pageContext.setPageSearchQuery("耳机");

        QueryAnalysisResult result = analyzer.analyze("便宜点的", null, pageContext);

        assertNotNull(result.getNormalizedQuery());
        assertTrue(result.getNormalizedQuery().contains("耳机"));
        assertEquals("耳机", result.getPageSearchQuery());
    }

    @Test
    void productListVisibleProductIdsShouldEnterBoostedIds() {
        Product p1 = createProduct("p_digital_001", "耳机A", "数码电子", "真无线耳机", 299);
        Product p2 = createProduct("p_digital_002", "耳机B", "数码电子", "真无线耳机", 399);
        PageContextResolution pageContext = new PageContextResolution();
        pageContext.setPageType(PageType.PRODUCT_LIST);
        pageContext.setPageSearchQuery("耳机");
        pageContext.setVisibleProducts(List.of(p1, p2));

        QueryAnalysisResult result = analyzer.analyze("还有吗", null, pageContext);

        assertTrue(result.getBoostedProductIds().contains("p_digital_001"));
        assertTrue(result.getBoostedProductIds().contains("p_digital_002"));
        assertEquals(2, result.getScopeProductIds().size());
    }

    @Test
    void nullPageContextShouldWorkAsBefore() {
        QueryAnalysisResult result = analyzer.analyze("推荐跑鞋", null, (PageContextResolution) null);

        assertNotNull(result);
        assertEquals("服饰运动", result.getCategory());
        assertEquals("跑步鞋", result.getSubCategory());
    }

    @Test
    void unknownPageTypeShouldNotModifyAnalysis() {
        PageContextResolution pageContext = new PageContextResolution();
        pageContext.setPageType(PageType.UNKNOWN);

        QueryAnalysisResult result = analyzer.analyze("推荐跑鞋", null, pageContext);

        assertEquals("服饰运动", result.getCategory());
        assertEquals("跑步鞋", result.getSubCategory());
        assertNull(result.getInheritedFromPageContext());
    }

    @Test
    void productDetailBrandFilterShouldWork() {
        Product current = createProduct("p_sports_001", "Nike跑鞋", "服饰运动", "跑步鞋", 899);
        PageContextResolution pageContext = createDetailContext(current);

        QueryAnalysisResult result = analyzer.analyze("除了这个还有别的牌子吗", null, pageContext);

        assertTrue(result.getExcludeProductIds().contains("p_sports_001"));
        assertEquals("服饰运动", result.getCategory());
    }

    private Product createProduct(String id, String name, String category, String subCategory, int price) {
        Product p = new Product();
        p.setProductId(id);
        p.setName(name);
        p.setCategory(category);
        p.setSubCategory(subCategory);
        p.setPrice(new BigDecimal(price));
        p.setBrand("测试品牌");
        p.setDescription("测试描述");
        p.setCurrency("CNY");
        return p;
    }

    private PageContextResolution createDetailContext(Product currentProduct) {
        PageContextResolution ctx = new PageContextResolution();
        ctx.setPageType(PageType.PRODUCT_DETAIL);
        ctx.setCurrentProduct(currentProduct);
        return ctx;
    }
}
