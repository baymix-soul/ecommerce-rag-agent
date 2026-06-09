package com.ecommerce.rag.rag.understanding;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryPlanValidatorCartTest {

    private QueryPlanValidator validator;
    private CatalogTaxonomySnapshot taxonomy;

    @BeforeEach
    void setUp() {
        validator = new QueryPlanValidator();
        taxonomy = new CatalogTaxonomySnapshot();
    }

    private QueryPlanValidationResult validate(QueryPlan plan) {
        return validator.validate(plan, taxonomy);
    }

    @Test
    @DisplayName("CART_SUMMARY with needsCart=true, needsRecommendation=false is valid")
    void testValidCartSummaryPlan() {
        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_SUMMARY);

        CartPlan cart = new CartPlan();
        cart.setAction(CartPlan.ACTION_CART_SUMMARY);
        cart.setNeedsCart(true);
        cart.setNeedsRecommendation(false);
        plan.setCart(cart);

        QueryPlanValidationResult result = validate(plan);

        assertTrue(result.getValid(), "CART_SUMMARY plan should be valid");
        assertTrue(result.getErrors().isEmpty(), "Should have no errors");
    }

    @Test
    @DisplayName("CART_TOP_UP with targetAmount=1000, needsCart=true, needsRecommendation=true is valid")
    void testValidCartTopUpWithTargetAmount() {
        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_TOP_UP);

        CartPlan cart = new CartPlan();
        cart.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cart.setTargetAmount(new BigDecimal("1000"));
        cart.setNeedsCart(true);
        cart.setNeedsRecommendation(true);
        plan.setCart(cart);

        QueryPlanValidationResult result = validate(plan);

        assertTrue(result.getValid(), "CART_TOP_UP with targetAmount should be valid");
        assertTrue(result.getErrors().isEmpty(), "Should have no errors");
    }

    @Test
    @DisplayName("CART_TOP_UP with null targetAmount forces needsClarification=true and adds warning")
    void testCartTopUpNullTargetAmountNeedsClarification() {
        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        plan.setNeedsClarification(false);

        CartPlan cart = new CartPlan();
        cart.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cart.setTargetAmount(null);
        cart.setNeedsCart(true);
        cart.setNeedsRecommendation(true);
        plan.setCart(cart);

        QueryPlanValidationResult result = validate(plan);

        assertTrue(result.getValid(), "Should still be valid (no errors, just warnings)");
        assertTrue(result.getErrors().isEmpty(), "Should have no errors");
        assertFalse(result.getWarnings().isEmpty(), "Should have warnings about null targetAmount");

        QueryPlan validated = result.getValidatedPlan();
        assertTrue(validated.getNeedsClarification(),
                "needsClarification should be forced to true when targetAmount is null");
    }

    @Test
    @DisplayName("CART_TOP_UP with targetAmount=0 or negative adds error")
    void testCartTopUpZeroTargetAmountInvalid() {
        // Test with zero
        QueryPlan planZero = new QueryPlan();
        planZero.setIntent(QueryPlan.INTENT_CART_TOP_UP);

        CartPlan cartZero = new CartPlan();
        cartZero.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cartZero.setTargetAmount(BigDecimal.ZERO);
        cartZero.setNeedsCart(true);
        cartZero.setNeedsRecommendation(true);
        planZero.setCart(cartZero);

        QueryPlanValidationResult resultZero = validate(planZero);
        assertFalse(resultZero.getValid(), "CART_TOP_UP with targetAmount=0 should be invalid");
        assertFalse(resultZero.getErrors().isEmpty(), "Should have error about targetAmount <= 0");

        // Test with negative
        QueryPlan planNeg = new QueryPlan();
        planNeg.setIntent(QueryPlan.INTENT_CART_TOP_UP);

        CartPlan cartNeg = new CartPlan();
        cartNeg.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cartNeg.setTargetAmount(new BigDecimal("-100"));
        cartNeg.setNeedsCart(true);
        cartNeg.setNeedsRecommendation(true);
        planNeg.setCart(cartNeg);

        QueryPlanValidationResult resultNeg = validate(planNeg);
        assertFalse(resultNeg.getValid(), "CART_TOP_UP with negative targetAmount should be invalid");
        assertFalse(resultNeg.getErrors().isEmpty(), "Should have error about targetAmount <= 0");
    }

    @Test
    @DisplayName("referencedProductIds with unknown format produces warning")
    void testUnknownReferencedProductIdWarning() {
        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_TOP_UP);

        CartPlan cart = new CartPlan();
        cart.setAction(CartPlan.ACTION_COMPLETION_RECOMMEND);
        cart.setTargetAmount(new BigDecimal("500"));
        cart.setNeedsCart(true);
        cart.setNeedsRecommendation(true);
        cart.setReferencedProductIds(List.of("fake_id_999"));
        plan.setCart(cart);

        QueryPlanValidationResult result = validate(plan);

        assertTrue(result.getValid(), "Unknown format is a warning, not an error");
        assertTrue(result.getErrors().isEmpty(), "Should have no errors");
        boolean hasFormatWarning = result.getWarnings().stream()
                .anyMatch(w -> w.contains("fake_id_999") && w.contains("unknown format"));
        assertTrue(hasFormatWarning, "Should warn about unknown product ID format");
    }

    @Test
    @DisplayName("validate is a pure function with no external service dependencies")
    void testValidationDoesNotCallCartService() {
        QueryPlanValidator freshValidator = new QueryPlanValidator();

        QueryPlan plan = new QueryPlan();
        plan.setIntent(QueryPlan.INTENT_CART_SUMMARY);

        CartPlan cart = new CartPlan();
        cart.setAction(CartPlan.ACTION_CART_SUMMARY);
        cart.setNeedsCart(true);
        cart.setNeedsRecommendation(false);
        plan.setCart(cart);

        assertDoesNotThrow(() -> freshValidator.validate(plan, taxonomy),
                "validate() should not throw and requires no external services");
    }
}
