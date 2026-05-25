package com.ecommerce.rag.rag.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.ChatCandidate;

@Component
public class RagPromptBuilder {

    private static final String SYSTEM_TEMPLATE = """
            你是电商导购助手。你只能基于候选商品列表回答用户问题。

            规则：
            1. 只能基于下方「候选商品列表」中的商品回答问题
            2. 不得编造商品、价格、库存、优惠或功效
            3. 如果候选商品中没有合适的，明确说明"商品库中暂无合适的商品"
            4. 推荐商品时必须引用 product_id
            5. 推荐理由必须来自候选商品字段（name、brand、category、sub_category、price、description、specs 等）
            6. 不得推荐候选商品列表之外的任何商品
            7. 回复语气自然、专业，像一位有经验的导购
            """;

    private static final String CANDIDATE_ITEM_TEMPLATE = """
            %d. product_id: %s
               name: %s
               brand: %s
               category: %s
               sub_category: %s
               price: %s %s
               description: %s
            """;

    private static final String NO_CANDIDATE_PROMPT = """
            用户问题：%s

            候选商品列表：（空）

            当前商品库中没有找到与用户需求匹配的商品，请如实告知用户。
            """;

    public String build(String message, List<ChatCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return SYSTEM_TEMPLATE + "\n" + String.format(NO_CANDIDATE_PROMPT, message);
        }

        StringBuilder candidateSection = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            ChatCandidate c = candidates.get(i);
            candidateSection.append(String.format(CANDIDATE_ITEM_TEMPLATE,
                    i + 1,
                    c.getProductId(),
                    c.getName(),
                    c.getBrand() != null ? c.getBrand() : "未知",
                    c.getCategory() != null ? c.getCategory() : "未知",
                    c.getSubCategory() != null ? c.getSubCategory() : "未知",
                    c.getPrice() != null ? c.getPrice().toPlainString() : "未知",
                    c.getCurrency() != null ? c.getCurrency() : "",
                    c.getDescription() != null ? c.getDescription() : "暂无描述"
            ));
        }

        return SYSTEM_TEMPLATE + "\n\n" +
                "用户问题：" + message + "\n\n" +
                "候选商品列表：\n" + candidateSection + "\n" +
                "要求输出：\n" +
                "- 自然语言推荐说明\n" +
                "- 推荐商品 product_id\n" +
                "- 推荐理由\n";
    }
}
