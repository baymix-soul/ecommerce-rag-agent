package com.ecommerce.rag.rag.memory;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.query.QueryAnalysisResult;

class ActiveSearchContextTest {

    @Test
    void testAddHardConstraint() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.addConstraint("category", "服饰运动", ConstraintStrength.HARD, "推荐跑鞋", 1);

        assertEquals(1, ctx.getActiveHardConstraints().size());
        ContextConstraint c = ctx.getActiveHardConstraints().get(0);
        assertEquals("category", c.getField());
        assertEquals("服饰运动", c.getValue());
        assertEquals(ConstraintStrength.HARD, c.getStrength());
        assertEquals("推荐跑鞋", c.getSourceQuery());
        assertEquals(1, c.getSourceTurn());
    }

    @Test
    void testAddSoftPreference() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.addConstraint("softPreferences", "轻量", ConstraintStrength.SOFT, "要轻量的", 2);

        assertEquals(1, ctx.getActiveSoftPreferences().size());
        assertEquals(0, ctx.getActiveHardConstraints().size());
        assertEquals(0, ctx.getActiveExclusions().size());
    }

    @Test
    void testAddExclusionConstraint() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.addConstraint("negativeBrands", "耐克", ConstraintStrength.EXCLUSION, "不要耐克", 3);

        assertEquals(1, ctx.getActiveExclusions().size());
        assertEquals("negativeBrands", ctx.getActiveExclusions().get(0).getField());
    }

    @Test
    void testDeactivateSoftPreferences() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.addConstraint("softPreferences", "轻量", ConstraintStrength.SOFT, "要轻量的", 2);
        ctx.addConstraint("softPreferences", "透气", ConstraintStrength.SOFT, "要透气的", 3);
        ctx.addConstraint("category", "服饰运动", ConstraintStrength.HARD, "推荐跑鞋", 1);

        ctx.deactivateSoftPreferences();

        assertEquals(0, ctx.getActiveSoftPreferences().size());
        assertEquals(1, ctx.getActiveHardConstraints().size());
        assertEquals(3, ctx.getConstraints().size());
    }

    @Test
    void testToQueryAnalysisResult() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        ctx.setCategory("服饰运动");
        ctx.setSubCategory("跑步鞋");
        ctx.setMaxPrice(new BigDecimal("1000"));
        ctx.setMinPrice(new BigDecimal("200"));
        ctx.setPositiveKeywords(List.of("跑鞋", "运动"));
        ctx.setSoftPreferences(List.of("轻量"));
        ctx.setNegativeBrands(List.of("耐克"));
        ctx.setNegativeKeywords(List.of("重"));
        ctx.setExcludeProductIds(List.of("P001"));

        QueryAnalysisResult result = ctx.toQueryAnalysisResult();

        assertEquals("服饰运动", result.getCategory());
        assertEquals("跑步鞋", result.getSubCategory());
        assertEquals(new BigDecimal("1000"), result.getMaxPrice());
        assertEquals(new BigDecimal("200"), result.getMinPrice());
        assertTrue(result.getPositiveKeywords().contains("跑鞋"));
        assertTrue(result.getSoftKeywords().contains("轻量"));
        assertTrue(result.getNegativeBrands().contains("耐克"));
        assertTrue(result.getNegativeKeywords().contains("重"));
        assertTrue(result.getExcludeProductIds().contains("P001"));
    }

    @Test
    void testTurnCountAndTouch() {
        ActiveSearchContext ctx = new ActiveSearchContext("s1");
        assertEquals(0, ctx.getTurnCount());
        ctx.touch();
        assertEquals(1, ctx.getTurnCount());
        assertNotNull(ctx.getUpdatedAt());
    }
}
