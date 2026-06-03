package com.ecommerce.rag.models.dto;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class ChatRequestPageContextTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void oldRequestWithoutPageContextShouldDeserialize() throws Exception {
        String json = """
                {"message":"推荐一款洗面奶","session_id":"user-001","limit":3}""";

        ChatRequest request = mapper.readValue(json, ChatRequest.class);

        assertEquals("推荐一款洗面奶", request.getMessage());
        assertEquals("user-001", request.getSessionId());
        assertEquals(3, request.getLimit());
        assertNull(request.getPageContext());
    }

    @Test
    void productDetailPageContextShouldDeserialize() throws Exception {
        String json = """
                {"message":"这个适合敏感肌吗？","limit":10,"page_context":{"page_type":"PRODUCT_DETAIL","current_product_id":"p_beauty_001","visible_product_ids":[],"search_query":null,"selected_filters":{},"recently_viewed_product_ids":["p_beauty_001","p_digital_003"]}}""";

        ChatRequest request = mapper.readValue(json, ChatRequest.class);

        assertEquals("这个适合敏感肌吗？", request.getMessage());
        assertEquals(10, request.getLimit());
        assertNotNull(request.getPageContext());
        assertEquals(PageType.PRODUCT_DETAIL, request.getPageContext().getPageType());
        assertEquals("p_beauty_001", request.getPageContext().getCurrentProductId());
        assertEquals(2, request.getPageContext().getRecentlyViewedProductIds().size());
    }

    @Test
    void productListPageContextShouldDeserialize() throws Exception {
        String json = """
                {"message":"有没有更便宜的耳机？","limit":10,"page_context":{"page_type":"PRODUCT_LIST","current_product_id":null,"visible_product_ids":["p_digital_001","p_digital_002","p_digital_003"],"search_query":"耳机","selected_filters":{"category":"数码电子"},"recently_viewed_product_ids":[]}}""";

        ChatRequest request = mapper.readValue(json, ChatRequest.class);

        assertEquals("有没有更便宜的耳机？", request.getMessage());
        assertNotNull(request.getPageContext());
        assertEquals(PageType.PRODUCT_LIST, request.getPageContext().getPageType());
        assertNull(request.getPageContext().getCurrentProductId());
        assertEquals(3, request.getPageContext().getVisibleProductIds().size());
        assertEquals("耳机", request.getPageContext().getSearchQuery());
        assertEquals("数码电子", request.getPageContext().getSelectedFilters().get("category"));
    }

    @Test
    void unknownPageTypeShouldNotThrow() throws Exception {
        String json = """
                {"message":"test","page_context":{"page_type":"SOME_UNKNOWN_TYPE","current_product_id":null}}""";

        ChatRequest request = mapper.readValue(json, ChatRequest.class);

        assertNotNull(request.getPageContext());
        assertEquals(PageType.UNKNOWN, request.getPageContext().getPageType());
    }

    @Test
    void nullPageTypeShouldDefaultToUnknown() throws Exception {
        PageContext ctx = new PageContext();
        ctx.setPageType(null);

        assertEquals(PageType.UNKNOWN, ctx.getPageType());
    }

    @Test
    void emptyPageContextUsesDefaults() {
        PageContext ctx = new PageContext();

        assertEquals(PageType.UNKNOWN, ctx.getPageType());
        assertNull(ctx.getCurrentProductId());
        assertTrue(ctx.getVisibleProductIds().isEmpty());
        assertNull(ctx.getSearchQuery());
        assertTrue(ctx.getSelectedFilters().isEmpty());
        assertTrue(ctx.getRecentlyViewedProductIds().isEmpty());
    }
}
