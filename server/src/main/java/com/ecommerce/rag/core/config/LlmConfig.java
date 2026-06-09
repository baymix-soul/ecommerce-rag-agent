package com.ecommerce.rag.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.rag.rag.llm.DoubaoLlmClient;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.llm.MockLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class LlmConfig {

    @Bean
    public LlmClient llmClient(AppProperties appProperties, ObjectMapper objectMapper) {
        if (appProperties.getChat().isMockLlmEnabled()) {
            return new MockLlmClient(appProperties);
        }

        AppProperties.LlmProperties llm = appProperties.getLlm();
        if (llm.getApiKey() == null || llm.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "LLM API Key 未配置。请设置环境变量 LLM_API_KEY 或配置 app.llm.api-key");
        }
        if (llm.getModel() == null || llm.getModel().isBlank()) {
            throw new IllegalStateException(
                    "LLM Model 未配置。请设置环境变量 LLM_MODEL 或配置 app.llm.model");
        }

        return new DoubaoLlmClient(appProperties, objectMapper);
    }
}
