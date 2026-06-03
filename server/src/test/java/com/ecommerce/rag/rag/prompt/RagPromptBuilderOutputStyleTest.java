package com.ecommerce.rag.rag.prompt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.models.dto.ChatCandidate;

class RagPromptBuilderOutputStyleTest {

    private final RagPromptBuilder promptBuilder = new RagPromptBuilder();

    @Test
    void promptShouldTellModelNotToOutputProductId() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐一款跑鞋", List.of(candidate), null, "SINGLE_RECOMMENDATION");
        assertTrue(prompt.contains("不要输出 product_id"));
    }

    @Test
    void promptShouldRequireShortReply() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐跑鞋", List.of(candidate), null, "SINGLE_RECOMMENDATION");
        assertTrue(prompt.contains("简短"));
    }

    @Test
    void promptShouldProhibitMarketingTalk() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐跑鞋", List.of(candidate), null, "SINGLE_RECOMMENDATION");
        assertTrue(prompt.contains("不要长篇营销话术"));
    }

    @Test
    void singleRecommendationShouldLimitTo3Sentences() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐一款跑鞋", List.of(candidate), null, "SINGLE_RECOMMENDATION");
        assertTrue(prompt.contains("不超过 120"));
    }

    @Test
    void multiRecommendationShouldLimitTo3Bullets() {
        ChatCandidate c1 = createCandidate("p_001", "商品1");
        ChatCandidate c2 = createCandidate("p_002", "商品2");
        ChatCandidate c3 = createCandidate("p_003", "商品3");
        String prompt = promptBuilder.build("推荐几款跑鞋", List.of(c1, c2, c3), null, "MULTI_RECOMMENDATION");
        assertTrue(prompt.contains("3 个 bullet"));
    }

    @Test
    void noMatchShouldIndicateNoMatch() {
        String prompt = promptBuilder.build("推荐不存在的商品", Collections.emptyList(), null, "NO_MATCH");
        assertTrue(prompt.contains("没有找到满足这些条件"));
    }

    @Test
    void singleRecommendationShouldNotRequireProductId() {
        ChatCandidate candidate = createCandidate("p_001", "测试商品");
        String prompt = promptBuilder.build("推荐一款跑鞋", List.of(candidate));
        assertTrue(prompt.contains("不要输出 product_id"));
    }

    @Test
    void multiRecommendationShouldMentionCardDetail() {
        ChatCandidate c1 = createCandidate("p_001", "商品1");
        ChatCandidate c2 = createCandidate("p_002", "商品2");
        String prompt = promptBuilder.build("推荐几款跑鞋", List.of(c1, c2));
        assertTrue(prompt.contains("点卡片看详情") || prompt.contains("商品卡片"));
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
