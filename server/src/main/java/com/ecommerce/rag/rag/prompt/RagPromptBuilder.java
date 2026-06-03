package com.ecommerce.rag.rag.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.dto.ChatCandidate;
import com.ecommerce.rag.rag.context.PageContextResolution;
import com.ecommerce.rag.models.entity.Product;

@Component
public class RagPromptBuilder {

    private static final String SYSTEM_TEMPLATE = """
            你是电商导购助手，回复显示在手机悬浮面板中。你只能基于候选商品列表回答用户问题。

            规则：
            1. 只能基于下方「候选商品列表」中的商品回答问题
            2. 不得编造商品、价格、库存、优惠或功效
            3. 如果候选商品中没有合适的，明确说明"商品库中暂无合适的商品"
            4. 不得推荐候选商品列表之外的任何商品
            5. 回复必须简短，适合手机悬浮导购面板
            6. 不要输出 product_id
            7. 不要重复商品卡片已经展示的信息（名称、价格已在卡片上）
            8. 不要长篇营销话术，不要用"我这边给您""亲""宝子"等直播/客服腔
            9. 语气自然、简洁、专业
            """;

    private static final String SINGLE_TEMPLATE = """
            输出规则（只推荐 1 款商品）：
            - 最多 4 行，不超过 120 个中文字符
            - 格式：推荐你优先看这款：{商品名}
            - 适合：{一句话说明适合谁}
            - 理由：{一句话说明核心卖点}
            - 不要输出 product_id，不要展开完整参数
            - 推荐理由必须来自候选商品字段
            - 下面会展示商品卡片，不要在文本中重复完整详情
            """;

    private static final String MULTI_TEMPLATE = """
            输出规则（推荐多款商品）：
            - 最多 3 个 bullet，每个 bullet 不超过 30 个汉字
            - 格式：
              给你筛了 N 款，优先看这几款：
              1. {商品名}：{一句话理由}
              2. {商品名}：{一句话理由}
              3. {商品名}：{一句话理由}
            - 可以点卡片看详情
            - 不要输出 product_id，不要展开完整参数
            - 总字数不超过 180 个中文字符
            """;

    private static final String NO_MATCH_TEMPLATE = """
            输出规则（无匹配商品）：
            - 当前商品库里没有找到满足这些条件的商品
            - 你可以放宽预算或换个关键词，我再帮你筛
            - 不推荐候选列表之外的商品
            - 不编造商品
            """;

    private static final String QA_TEMPLATE = """
            输出规则（回答当前商品问题）：
            - 从当前商品信息看，{结论}
            - 依据：{1-2 个字段证据}
            - 如果你对成分/尺码/适用人群很敏感，建议再核对详情页说明
            - 只回答当前商品，不推荐其他商品
            - 当前商品信息没提到的内容，要说"当前信息未提及"
            - 不输出 product_id
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

    private static final String PRODUCT_DETAIL_CONTEXT_TEMPLATE = """
            当前用户正在查看商品：
            product_id: %s
            name: %s
            brand: %s
            category: %s
            sub_category: %s
            price: %s
            description: %s
            specs: %s

            规则：
            - 如果用户说"这个/这款/它"，默认指当前正在查看的商品。
            - 回答当前商品问题时，不要编造商品没有的信息。
            - 如果当前商品信息不足，应明确说明"当前商品信息中没有提到"。
            """;

    private static final String PRODUCT_LIST_CONTEXT_TEMPLATE = """
            当前用户正在浏览商品列表：
            search_query: %s
            selected_filters: %s
            visible_product_ids: %s

            规则：
            - 用户说"更便宜的/类似的/还有吗"时，优先结合当前列表筛选条件理解。
            - 不要推荐不在候选商品列表中的商品。
            """;

    public String build(String message, List<ChatCandidate> candidates) {
        return build(message, candidates, null, null);
    }

    public String build(String message, List<ChatCandidate> candidates,
                         PageContextResolution pageContext) {
        return build(message, candidates, pageContext, null);
    }

    public String build(String message, List<ChatCandidate> candidates,
                         PageContextResolution pageContext,
                         String responseStyle) {
        String pageContextSection = buildPageContextSection(pageContext);

        if (candidates == null || candidates.isEmpty()) {
            return SYSTEM_TEMPLATE + "\n" + NO_MATCH_TEMPLATE + "\n"
                    + pageContextSection
                    + String.format(NO_CANDIDATE_PROMPT, message);
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

        String outputRule = selectOutputTemplate(responseStyle, candidates.size());

        return SYSTEM_TEMPLATE + "\n" + outputRule + "\n" +
                pageContextSection +
                "用户问题：" + message + "\n\n" +
                "候选商品列表：\n" + candidateSection;
    }

    private String selectOutputTemplate(String responseStyle, int candidateCount) {
        if (productDetailQA(responseStyle)) {
            return QA_TEMPLATE;
        }
        if (candidateCount <= 1) {
            return SINGLE_TEMPLATE;
        }
        return MULTI_TEMPLATE;
    }

    private boolean productDetailQA(String responseStyle) {
        return com.ecommerce.rag.rag.query.QueryAnalysisResult.CURRENT_PRODUCT_QA.equals(responseStyle);
    }

    private String buildPageContextSection(PageContextResolution pageContext) {
        if (pageContext == null || !pageContext.hasPageContext()) {
            return "";
        }

        if (pageContext.isProductDetail() && pageContext.isHasValidCurrentProduct()) {
            Product p = pageContext.getCurrentProduct();
            return String.format(PRODUCT_DETAIL_CONTEXT_TEMPLATE,
                    p.getProductId(),
                    p.getName(),
                    p.getBrand() != null ? p.getBrand() : "无",
                    p.getCategory() != null ? p.getCategory() : "无",
                    p.getSubCategory() != null ? p.getSubCategory() : "无",
                    p.getPrice() != null ? p.getPrice().toPlainString() : "未知",
                    p.getDescription() != null ? p.getDescription() : "无",
                    p.getSpecs() != null ? p.getSpecs().toString() : "无"
            ) + "\n\n";
        }

        if (pageContext.isProductList()) {
            List<String> visibleIds = pageContext.getVisibleProducts().stream()
                    .map(Product::getProductId)
                    .toList();
            return String.format(PRODUCT_LIST_CONTEXT_TEMPLATE,
                    pageContext.getPageSearchQuery() != null ? pageContext.getPageSearchQuery() : "无",
                    pageContext.getSelectedFilters(),
                    visibleIds
            ) + "\n\n";
        }

        return "";
    }
}
