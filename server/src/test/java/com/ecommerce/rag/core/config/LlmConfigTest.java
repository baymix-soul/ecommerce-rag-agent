package com.ecommerce.rag.core.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.rag.llm.DoubaoLlmClient;
import com.ecommerce.rag.rag.llm.LlmClient;
import com.ecommerce.rag.rag.llm.MockLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;

class LlmConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnMockLlmClientWhenMockEnabled() {
        AppProperties props = new AppProperties();
        props.getChat().setMockLlmEnabled(true);

        LlmConfig config = new LlmConfig();
        LlmClient client = config.llmClient(props, objectMapper);

        assertTrue(client instanceof MockLlmClient);
    }

    @Test
    void shouldReturnDoubaoLlmClientWhenMockDisabledAndConfigured() {
        AppProperties props = new AppProperties();
        props.getChat().setMockLlmEnabled(false);
        props.getLlm().setApiKey("valid-api-key");
        props.getLlm().setModel("doubao-seed-2.0-lite");

        LlmConfig config = new LlmConfig();
        LlmClient client = config.llmClient(props, objectMapper);

        assertTrue(client instanceof DoubaoLlmClient);
    }

    @Test
    void shouldThrowWhenMockDisabledAndApiKeyMissing() {
        AppProperties props = new AppProperties();
        props.getChat().setMockLlmEnabled(false);
        props.getLlm().setApiKey("");
        props.getLlm().setModel("doubao-seed-2.0-lite");

        LlmConfig config = new LlmConfig();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.llmClient(props, objectMapper)
        );
        assertTrue(exception.getMessage().contains("LLM_API_KEY"));
    }

    @Test
    void shouldThrowWhenMockDisabledAndModelMissing() {
        AppProperties props = new AppProperties();
        props.getChat().setMockLlmEnabled(false);
        props.getLlm().setApiKey("valid-api-key");
        props.getLlm().setModel("");

        LlmConfig config = new LlmConfig();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> config.llmClient(props, objectMapper)
        );
        assertTrue(exception.getMessage().contains("LLM_MODEL"));
    }

    @Test
    void shouldThrowWhenMockDisabledAndApiKeyNull() {
        AppProperties props = new AppProperties();
        props.getChat().setMockLlmEnabled(false);
        props.getLlm().setApiKey(null);

        LlmConfig config = new LlmConfig();

        assertNotNull(assertThrows(
                IllegalStateException.class,
                () -> config.llmClient(props, objectMapper)
        ));
    }
}
