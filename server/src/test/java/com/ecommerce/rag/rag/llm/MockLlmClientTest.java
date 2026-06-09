package com.ecommerce.rag.rag.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;

class MockLlmClientTest {

    private final MockLlmClient mockLlmClient = new MockLlmClient(new AppProperties());

    @Test
    void shouldStreamTextChunksForCandidatesPrompt() throws InterruptedException {
        String prompt = "候选商品列表：\n1. product_id: p_001\n用户问题：洗面奶";
        List<String> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        mockLlmClient.streamGenerate(
                prompt,
                chunks::add,
                () -> { completed.set(true); latch.countDown(); },
                e -> latch.countDown()
        );

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed.get());
        assertFalse(chunks.isEmpty());

        String fullText = String.join("", chunks);
        assertTrue(fullText.contains("匹配商品"));
    }

    @Test
    void shouldStreamNoCandidatesResponse() throws InterruptedException {
        String prompt = "候选商品列表：（空）\n用户问题：找不到的商品";
        List<String> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        mockLlmClient.streamGenerate(
                prompt,
                chunks::add,
                () -> { completed.set(true); latch.countDown(); },
                e -> latch.countDown()
        );

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed.get());
        assertFalse(chunks.isEmpty());

        String fullText = String.join("", chunks);
        assertTrue(fullText.contains("暂未找到"));
    }

    @Test
    void shouldCallOnErrorWhenExceptionOccurs() throws InterruptedException {
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        LlmClient failingClient = new LlmClient() {
            @Override
            public void streamGenerate(String prompt, java.util.function.Consumer<String> onText,
                                       Runnable onComplete, java.util.function.Consumer<Throwable> onError) {
                onError.accept(new RuntimeException("test error"));
                latch.countDown();
            }
        };

        failingClient.streamGenerate(
                "test",
                s -> {},
                () -> {},
                e -> { errorRef.set(e); }
        );

        latch.await(5, TimeUnit.SECONDS);
        assertTrue(errorRef.get() instanceof RuntimeException);
        assertEquals("test error", errorRef.get().getMessage());
    }

    private void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
}
