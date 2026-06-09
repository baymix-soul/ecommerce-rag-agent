package com.ecommerce.rag.rag.memory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;
import com.ecommerce.rag.rag.understanding.QueryPlan;

class ConversationContextMergerTest {

    private final ConversationContextMerger merger = new ConversationContextMerger();

    @Test
    void testNewSearch() {
        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("服饰运动");
        analysis.setSubCategory("跑步鞋");
        analysis.setSoftKeywords(List.of());

        var result = merger.merge(null, analysis, null, "推荐几款跑鞋");

        assertEquals(ConversationContextMerger.ACTION_NEW_SEARCH, result.getContextAction());
        assertEquals("服饰运动", result.getActiveContext().getCategory());
        assertEquals("跑步鞋", result.getActiveContext().getSubCategory());
        assertEquals(2, result.getActiveContext().getActiveHardConstraints().size());
    }

    @Test
    void testRefineAddSoftPreference() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setSoftPreferences(new java.util.ArrayList<>());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("轻量"));

        var result = merger.merge(ctx, analysis, null, "要轻量的");

        assertEquals(ConversationContextMerger.ACTION_REFINE_ADD_SOFT_PREFERENCE, result.getContextAction());
        assertEquals("服饰运动", result.getActiveContext().getCategory());
        assertEquals("跑步鞋", result.getActiveContext().getSubCategory());
        assertTrue(result.getActiveContext().getSoftPreferences().contains("轻量"));
        assertEquals(1, result.getActiveContext().getActiveSoftPreferences().size());
    }

    @Test
    void testRefineAddHardConstraint() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setSoftPreferences(new java.util.ArrayList<>(List.of("轻量")));

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setMaxPrice(new BigDecimal("1000"));

        var result = merger.merge(ctx, analysis, null, "一千块以下的");

        assertEquals(ConversationContextMerger.ACTION_REFINE_ADD_HARD_CONSTRAINT, result.getContextAction());
        assertEquals(new BigDecimal("1000"), result.getActiveContext().getMaxPrice());
        assertTrue(result.getActiveContext().getSoftPreferences().contains("轻量"));
    }

    @Test
    void testAddExclusion() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setNegativeBrands(new java.util.ArrayList<>());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setNegativeBrands(List.of("耐克"));

        var result = merger.merge(ctx, analysis, null, "不要耐克");

        assertEquals(ConversationContextMerger.ACTION_ADD_EXCLUSION, result.getContextAction());
        assertTrue(result.getActiveContext().getNegativeBrands().contains("耐克"));
        assertEquals(1, result.getActiveContext().getActiveExclusions().size());
    }

    @Test
    void testReplaceSoftPreferenceAudience() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("数码电器");
        ctx.setSubCategory("笔记本电脑");
        ctx.setSoftPreferences(new java.util.ArrayList<>(List.of("程序员")));
        ctx.setAudience("程序员");

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setSoftKeywords(List.of("上班族"));

        var result = merger.merge(ctx, analysis, null, "上班族呢");

        assertEquals(ConversationContextMerger.ACTION_REFINE_ADD_SOFT_PREFERENCE, result.getContextAction());
        assertEquals("数码电器", result.getActiveContext().getCategory());
        assertEquals("笔记本电脑", result.getActiveContext().getSubCategory());
    }

    @Test
    void testSwitchCategoryClearsContext() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setSoftPreferences(new java.util.ArrayList<>(List.of("轻量")));
        ctx.setConstraints(new java.util.ArrayList<>());

        QueryAnalysisResult analysis = new QueryAnalysisResult();
        analysis.setCategory("数码电器");
        analysis.setSubCategory("手机");
        analysis.setSoftKeywords(List.of());

        var result = merger.merge(ctx, analysis, null, "推荐手机");

        assertEquals(ConversationContextMerger.ACTION_SWITCH_CATEGORY, result.getContextAction());
        assertEquals("数码电器", result.getActiveContext().getCategory());
        assertEquals("手机", result.getActiveContext().getSubCategory());
        assertTrue(result.getActiveContext().getSoftPreferences().isEmpty());
    }

    @Test
    void testExcludeRecommendedProductIds() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setLastRecommendedProductIds(List.of("P001", "P002"));
        ctx.setExcludeProductIds(new java.util.ArrayList<>());

        QueryAnalysisResult analysis = new QueryAnalysisResult();

        var result = merger.merge(ctx, analysis, null, "换一个");

        assertEquals(ConversationContextMerger.ACTION_REFER_RECOMMENDED_PRODUCT, result.getContextAction());
        assertTrue(result.getActiveContext().getExcludeProductIds().contains("P001"));
        assertTrue(result.getActiveContext().getExcludeProductIds().contains("P002"));
    }
}
