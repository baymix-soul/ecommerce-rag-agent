package com.ecommerce.rag.rag.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;

class RagPromptBuilderTest {

    private final RagPromptBuilder promptBuilder = new RagPromptBuilder();

    @Test
    void shouldIncludeUserQuestionInPrompt() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐一款洗面奶", List.of(candidate));
        assertTrue(prompt.contains("推荐一款洗面奶"));
    }

    @Test
    void shouldIncludeCandidateProductIdInPrompt() {
        ChatCandidate candidate = createCandidate("p_beauty_001", "测试洗面奶");
        String prompt = promptBuilder.build("洗面奶", List.of(candidate));
        assertTrue(prompt.contains("p_beauty_001"));
    }

    @Test
    void shouldIncludeConstraintAgainstFabrication() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("测试", List.of(candidate));
        assertTrue(prompt.contains("不得编造商品"));
        assertTrue(prompt.contains("价格") || prompt.contains("库存") || prompt.contains("功效"));
    }

    @Test
    void shouldIncludeConstraintAgainstRecommendingOutsideList() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("测试", List.of(candidate));
        assertTrue(prompt.contains("不得推荐候选商品列表之外的任何商品"));
    }

    @Test
    void shouldBuildNoCandidatePromptWhenEmpty() {
        String prompt = promptBuilder.build("找不到的商品", Collections.emptyList());
        assertNotNull(prompt);
        assertTrue(prompt.contains("找不到的商品"));
        assertTrue(prompt.contains("空"));
    }

    @Test
    void shouldBuildNoCandidatePromptWhenNull() {
        String prompt = promptBuilder.build("测试问题", null);
        assertNotNull(prompt);
        assertFalse(prompt.contains("product_id:"));
    }

    @Test
    void shouldIncludeCandidateDetailsInPrompt() {
        ChatCandidate candidate = createCandidate("p_001", "雅诗兰黛精华");
        candidate.setBrand("雅诗兰黛");
        candidate.setCategory("美妆护肤");
        candidate.setSubCategory("精华");
        candidate.setPrice(new BigDecimal("720"));
        candidate.setDescription("测试描述");

        String prompt = promptBuilder.build("精华", List.of(candidate));
        assertTrue(prompt.contains("雅诗兰黛精华"));
        assertTrue(prompt.contains("雅诗兰黛"));
        assertTrue(prompt.contains("美妆护肤"));
        assertTrue(prompt.contains("720"));
    }

    private ChatCandidate createCandidate(String productId, String name) {
        ChatCandidate candidate = new ChatCandidate();
        candidate.setProductId(productId);
        candidate.setName(name);
        candidate.setPrice(new BigDecimal("100"));
        candidate.setCurrency("CNY");
        return candidate;
    }
}
