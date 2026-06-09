package com.ecommerce.rag.rag.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;

class MockLlmClientConciseOutputTest {

    private final MockLlmClient mockLlmClient = new MockLlmClient(new AppProperties());

    @Test
    void hasCandidatesShouldOutputShortText() throws InterruptedException {
        String prompt = "候选商品列表：\n1. product_id: p_001 name: 测试\n用户问题：跑鞋";
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
        String fullText = String.join("", chunks);
        assertTrue(fullText.contains("匹配商品"));
        assertTrue(fullText.length() <= 30);
    }

    @Test
    void noCandidatesShouldOutputShortText() throws InterruptedException {
        String prompt = "候选商品列表：（空）\n用户问题：不存在";
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
        String fullText = String.join("", chunks);
        assertTrue(fullText.contains("暂未找到"));
    }

    @Test
    void responseShouldNotContainProductId() throws InterruptedException {
        String prompt = "候选商品列表：\n1. product_id: p_001\n用户问题：洗面奶";
        List<String> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        mockLlmClient.streamGenerate(
                prompt,
                chunks::add,
                () -> latch.countDown(),
                e -> latch.countDown()
        );

        latch.await(5, TimeUnit.SECONDS);
        String fullText = String.join("", chunks);
        assertFalse(fullText.contains("product_id"), "Mock output should not contain product_id");
    }
}
