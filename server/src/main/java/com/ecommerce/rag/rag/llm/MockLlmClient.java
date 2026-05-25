package com.ecommerce.rag.rag.llm;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecommerce.rag.core.config.AppProperties;

public class MockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    private static final String HAS_CANDIDATES_RESPONSE =
            "我根据你的需求，从商品库中找到了几款相关商品。下面这些商品与需求匹配度较高，你可以优先看看。";

    private static final String NO_CANDIDATES_RESPONSE =
            "当前商品库中暂未找到完全匹配的商品，你可以换个关键词或放宽条件再试试。";

    private final AppProperties appProperties;

    public MockLlmClient(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void streamGenerate(String prompt, Consumer<String> onText, Runnable onComplete, Consumer<Throwable> onError) {
        try {
            String response = determineResponse(prompt);
            streamText(response, onText);
            onComplete.run();
        } catch (Exception e) {
            log.error("MockLlmClient error", e);
            onError.accept(e);
        }
    }

    private String determineResponse(String prompt) {
        if (prompt.contains("候选商品列表：（空）") || prompt.contains("没有找到")) {
            return NO_CANDIDATES_RESPONSE;
        }
        return HAS_CANDIDATES_RESPONSE;
    }

    private void streamText(String text, Consumer<String> onText) {
        int chunkSize = 4;
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            onText.accept(text.substring(i, end));
        }
    }
}
