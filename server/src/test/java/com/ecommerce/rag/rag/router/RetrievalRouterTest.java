package com.ecommerce.rag.rag.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RetrievalRouterTest {

    private final RetrievalRouter router = new RetrievalRouter();

    @Test
    void shouldRouteSmalltalk() {
        RetrievalRouteResult r = router.route("你好");
        assertEquals(RetrievalIntent.SMALLTALK, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteSmalltalkInma() {
        RetrievalRouteResult r = router.route("在吗");
        assertEquals(RetrievalIntent.SMALLTALK, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteSmalltalkWho() {
        RetrievalRouteResult r = router.route("你是谁");
        assertEquals(RetrievalIntent.SMALLTALK, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteHelp() {
        RetrievalRouteResult r = router.route("你能做什么");
        assertEquals(RetrievalIntent.HELP, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteHelpZenmeYong() {
        RetrievalRouteResult r = router.route("怎么用");
        assertEquals(RetrievalIntent.HELP, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteThanks() {
        RetrievalRouteResult r = router.route("谢谢");
        assertEquals(RetrievalIntent.THANKS, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteThanksHaoDe() {
        RetrievalRouteResult r = router.route("好的");
        assertEquals(RetrievalIntent.THANKS, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteProductSearch() {
        RetrievalRouteResult r = router.route("推荐洗面奶");
        assertEquals(RetrievalIntent.PRODUCT_SEARCH, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteRefinePreviousQuery() {
        RetrievalRouteResult r = router.route("再便宜点");
        assertEquals(RetrievalIntent.REFINE_PREVIOUS_QUERY, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteRefineLightweight() {
        RetrievalRouteResult r = router.route("要轻量的");
        assertEquals(RetrievalIntent.REFINE_PREVIOUS_QUERY, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteNegativeConstraintBuYao() {
        RetrievalRouteResult r = router.route("不要日系");
        assertEquals(RetrievalIntent.NEGATIVE_CONSTRAINT, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteNegativeConstraintChuLe() {
        RetrievalRouteResult r = router.route("除了耐克还有什么");
        assertEquals(RetrievalIntent.NEGATIVE_CONSTRAINT, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteChangeOrMore() {
        RetrievalRouteResult r = router.route("换一个");
        assertEquals(RetrievalIntent.CHANGE_OR_MORE, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteChangeOrMoreHaiYouMa() {
        RetrievalRouteResult r = router.route("还有吗");
        assertEquals(RetrievalIntent.CHANGE_OR_MORE, r.getIntent());
        assertTrue(r.isNeedsRetrieval());
    }

    @Test
    void shouldRouteCompareProducts() {
        RetrievalRouteResult r = router.route("A和B哪个好");
        assertEquals(RetrievalIntent.COMPARE_PRODUCTS, r.getIntent());
    }

    @Test
    void shouldRouteUnknown() {
        RetrievalRouteResult r = router.route("随便看看");
        assertEquals(RetrievalIntent.UNKNOWN, r.getIntent());
    }

    @Test
    void shouldRouteEmptyToUnknown() {
        RetrievalRouteResult r = router.route(null);
        assertEquals(RetrievalIntent.UNKNOWN, r.getIntent());
        assertFalse(r.isNeedsRetrieval());
    }
}
