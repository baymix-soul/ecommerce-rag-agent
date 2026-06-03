package com.ecommerce.rag.rag.response;

import org.springframework.stereotype.Component;

@Component
public class RecommendationCountResolver {

    private static final int DEFAULT_COUNT = 3;

    private static final String[] SINGLE_PATTERNS = {
            "推荐一款", "推荐一个", "推荐一双", "推荐1款", "推荐1个",
            "来一款", "来一个", "来一双",
            "选一款", "选一个", "挑一款", "挑一个",
            "哪款最适合", "哪个最适合", "哪款最好", "哪个最好",
            "优先推荐", "最推荐", "首推", "第一款",
            "给我推荐一款", "推荐哪一款"
    };

    private static final String[] MULTI_PATTERNS = {
            "推荐几款", "有几个", "有哪些", "有什么",
            "多推荐几个", "给我几个选择", "有什么选择",
            "帮我筛几款", "来几个", "选几个", "挑几个",
            "都有哪些", "还有哪些"
    };

    public int resolve(String query) {
        if (query == null || query.isBlank()) {
            return DEFAULT_COUNT;
        }

        for (String pattern : SINGLE_PATTERNS) {
            if (query.contains(pattern)) {
                return 1;
            }
        }

        for (String pattern : MULTI_PATTERNS) {
            if (query.contains(pattern)) {
                return DEFAULT_COUNT;
            }
        }

        if (query.contains("推荐") || query.contains("适合")
                || query.contains("求推荐") || query.contains("想要")) {
            return DEFAULT_COUNT;
        }

        return DEFAULT_COUNT;
    }
}
