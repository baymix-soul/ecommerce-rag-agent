package com.ecommerce.rag.models.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ProductModelExtendedFieldsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeReviewSummaryJsonField() throws Exception {
        String json = "{\"product_id\":\"p_001\",\"name\":\"测试商品\",\"review_summary\":\"大多数用户反馈清洁力好，温和不刺激\"}";

        Product product = objectMapper.readValue(json, Product.class);

        assertEquals("p_001", product.getProductId());
        assertEquals("大多数用户反馈清洁力好，温和不刺激", product.getReviewSummary());
    }

    @Test
    void shouldDeserializeFaqSummaryJsonField() throws Exception {
        String json = "{\"product_id\":\"p_002\",\"name\":\"测试商品\",\"faq_summary\":\"Q:适合敏感肌吗？A:是的，配方温和\"}";

        Product product = objectMapper.readValue(json, Product.class);

        assertEquals("p_002", product.getProductId());
        assertEquals("Q:适合敏感肌吗？A:是的，配方温和", product.getFaqSummary());
    }

    @Test
    void shouldDeserializeMarketingCopyJsonField() throws Exception {
        String json = "{\"product_id\":\"p_003\",\"name\":\"测试商品\",\"marketing_copy\":\"年度畅销TOP1，百万用户口碑之选\"}";

        Product product = objectMapper.readValue(json, Product.class);

        assertEquals("p_003", product.getProductId());
        assertEquals("年度畅销TOP1，百万用户口碑之选", product.getMarketingCopy());
    }

    @Test
    void shouldNotErrorOnMissingExtendedFields() throws Exception {
        String json = "{\"product_id\":\"p_004\",\"name\":\"基础商品\",\"price\":99.0,\"currency\":\"CNY\"}";

        Product product = objectMapper.readValue(json, Product.class);

        assertNotNull(product);
        assertEquals("p_004", product.getProductId());
        assertNull(product.getReviewSummary());
        assertNull(product.getFaqSummary());
        assertNull(product.getMarketingCopy());
    }

    @Test
    void shouldHandleProductWithAllThreeNewFields() throws Exception {
        String json = "{"
                + "\"product_id\":\"p_005\","
                + "\"name\":\"全能测试商品\","
                + "\"brand\":\"测试品牌\","
                + "\"category\":\"美妆护肤\","
                + "\"sub_category\":\"精华\","
                + "\"price\":350.0,"
                + "\"currency\":\"CNY\","
                + "\"review_summary\":\"4.8星好评，用户反馈质地清爽吸收快\","
                + "\"faq_summary\":\"Q:保质期多久？A:开封后12个月内用完\","
                + "\"marketing_copy\":\"抗初老明星单品，28天见证肌肤焕变\""
                + "}";

        Product product = objectMapper.readValue(json, Product.class);

        assertEquals("p_005", product.getProductId());
        assertEquals("全能测试商品", product.getName());
        assertEquals("测试品牌", product.getBrand());
        assertEquals("美妆护肤", product.getCategory());
        assertEquals("精华", product.getSubCategory());

        assertEquals("4.8星好评，用户反馈质地清爽吸收快", product.getReviewSummary());
        assertEquals("Q:保质期多久？A:开封后12个月内用完", product.getFaqSummary());
        assertEquals("抗初老明星单品，28天见证肌肤焕变", product.getMarketingCopy());
    }
}
