package com.ecommerce.rag.rag.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetrievalRouter {

    private static final Logger log = LoggerFactory.getLogger(RetrievalRouter.class);

    public RetrievalRouteResult route(String query) {
        if (query == null || query.isBlank()) {
            return new RetrievalRouteResult(RetrievalIntent.UNKNOWN, false, "empty query");
        }

        String trimmed = query.trim().toLowerCase();

        if (isSmalltalk(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.SMALLTALK, false, "smalltalk greeting");
        }

        if (isHelp(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.HELP, false, "help request");
        }

        if (isThanks(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.THANKS, false, "thanks/acknowledgment");
        }

        if (isNegativeConstraint(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.NEGATIVE_CONSTRAINT, true,
                    "negative constraint detected");
        }

        if (isChangeOrMore(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.CHANGE_OR_MORE, true,
                    "change request or ask for more");
        }

        if (isRefinePreviousQuery(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.REFINE_PREVIOUS_QUERY, true,
                    "refine previous query");
        }

        if (isCompare(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.COMPARE_PRODUCTS, true,
                    "compare products request");
        }

        if (isProductSearch(trimmed)) {
            return new RetrievalRouteResult(RetrievalIntent.PRODUCT_SEARCH, true,
                    "product search intent detected");
        }

        return new RetrievalRouteResult(RetrievalIntent.UNKNOWN, true, "default to retrieval");
    }

    private boolean isSmalltalk(String query) {
        String[] patterns = {"你好", "在吗", "在不在", "你是谁", "你叫什么", "hello", "hi", "嗨",
                "早上好", "中午好", "下午好", "晚上好", "晚安", "再见", "拜拜", "bye"};
        for (String p : patterns) {
            if (query.equals(p) || query.startsWith(p + " ") || query.startsWith(p + "!") || query.startsWith(p + "，")) {
                return true;
            }
        }
        return query.equals("你好") || query.equals("在吗") || query.equals("你是谁");
    }

    private boolean isHelp(String query) {
        return query.contains("怎么用") || query.contains("你能做什么") || query.contains("有什么功能")
                || query.contains("怎么玩") || query.equals("帮助") || query.equals("help")
                || query.contains("使用说明") || query.contains("功能");
    }

    private boolean isThanks(String query) {
        return query.equals("谢谢") || query.equals("多谢") || query.equals("感谢")
                || query.equals("ok") || query.equals("好的") || query.equals("好") || query.equals("知道了")
                || query.equals("thanks") || query.equals("thank you");
    }

    private boolean isNegativeConstraint(String query) {
        return query.contains("不要") || query.contains("不要含")
                || query.contains("排除") || query.contains("除了")
                || query.contains("不想要") || query.contains("不想买")
                || query.contains("别推荐");
    }

    private boolean isChangeOrMore(String query) {
        return query.contains("换一个") || query.contains("还有吗")
                || query.contains("还有什么") || query.contains("其他的")
                || query.contains("别的") || query.contains("其它")
                || query.equals("再来") || query.equals("下一个");
    }

    private boolean isRefinePreviousQuery(String query) {
        return query.contains("再便宜点") || query.contains("便宜一点")
                || query.contains("便宜点") || query.contains("贵一点")
                || query.contains("轻一点") || query.contains("大一点")
                || query.contains("要轻量") || query.contains("轻量的")
                || query.contains("预算");
    }

    private boolean isCompare(String query) {
        return (query.contains("和") && query.contains("哪个好"))
                || query.contains("对比") || query.contains("比较")
                || query.contains("vs") || query.contains("区别");
    }

    private boolean isProductSearch(String query) {
        return query.contains("推荐") || query.contains("有没有")
                || query.contains("想要") || query.contains("帮我找")
                || query.contains("买") || query.contains("元以内")
                || query.contains("以下") || query.contains("以内")
                || query.contains("适合") || query.contains("好用")
                || query.contains("哪个好");
    }
}
