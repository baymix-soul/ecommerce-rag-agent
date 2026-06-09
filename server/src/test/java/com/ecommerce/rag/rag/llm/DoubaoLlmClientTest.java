package com.ecommerce.rag.rag.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ecommerce.rag.core.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpServer;

class DoubaoLlmClientTest {

    private HttpServer mockServer;
    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private DoubaoLlmClient doubaoLlmClient;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.setExecutor(null);
        mockServer.start();

        int port = mockServer.getAddress().getPort();

        appProperties = new AppProperties();
        appProperties.getLlm().setBaseUrl("http://localhost:" + port);
        appProperties.getLlm().setApiKey("test-api-key-for-unit-test");
        appProperties.getLlm().setModel("test-model");
        appProperties.getLlm().setTimeoutSeconds(10);

        objectMapper = new ObjectMapper();
        doubaoLlmClient = new DoubaoLlmClient(appProperties, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void shouldStreamTextChunksFromSseResponse() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(("data: {\"id\":\"1\",\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n").getBytes(StandardCharsets.UTF_8));
            os.write(("data: {\"id\":\"1\",\"choices\":[{\"delta\":{\"content\":\"，世界\"}}]}\n\n").getBytes(StandardCharsets.UTF_8));
            os.write(("data: {\"id\":\"1\",\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n").getBytes(StandardCharsets.UTF_8));
            os.write(("data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        List<String> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        doubaoLlmClient.streamGenerate(
                "测试提示词",
                chunks::add,
                () -> { completed.set(true); latch.countDown(); },
                e -> latch.countDown()
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(completed.get());
        assertEquals(2, chunks.size());
        assertEquals("你好", chunks.get(0));
        assertEquals("，世界", chunks.get(1));
    }

    @Test
    void shouldHandleEmptyDeltaContent() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(("data: {\"id\":\"1\",\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}\n\n").getBytes(StandardCharsets.UTF_8));
            os.write(("data: {\"id\":\"1\",\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n").getBytes(StandardCharsets.UTF_8));
            os.write(("data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        List<String> chunks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean completed = new AtomicBoolean(false);

        doubaoLlmClient.streamGenerate(
                "测试",
                chunks::add,
                () -> { completed.set(true); latch.countDown(); },
                e -> latch.countDown()
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(completed.get());
        assertEquals(1, chunks.size());
        assertEquals("Hello", chunks.get(0));
    }

    @Test
    void shouldHandle401Error() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doubaoLlmClient.streamGenerate(
                "测试",
                s -> {},
                () -> latch.countDown(),
                e -> { errorRef.set(e); latch.countDown(); }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof RuntimeException);
        assertTrue(errorRef.get().getMessage().contains("401"));
    }

    @Test
    void shouldHandle429Error() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(429, 0);
            exchange.getResponseBody().close();
        });

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doubaoLlmClient.streamGenerate(
                "测试",
                s -> {},
                () -> latch.countDown(),
                e -> { errorRef.set(e); latch.countDown(); }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof RuntimeException);
        assertTrue(errorRef.get().getMessage().contains("429"));
    }

    @Test
    void shouldHandle5xxError() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doubaoLlmClient.streamGenerate(
                "测试",
                s -> {},
                () -> latch.countDown(),
                e -> { errorRef.set(e); latch.countDown(); }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof RuntimeException);
        assertTrue(errorRef.get().getMessage().contains("500"));
    }

    @Test
    void shouldNotExposeApiKeyInErrorMessages() throws Exception {
        mockServer.createContext("/chat/completions", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.getResponseBody().close();
        });

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doubaoLlmClient.streamGenerate(
                "测试",
                s -> {},
                () -> latch.countDown(),
                e -> { errorRef.set(e); latch.countDown(); }
        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        String errorMsg = errorRef.get().getMessage();
        assertFalse(errorMsg.contains("test-api-key-for-unit-test"));
    }

    @Test
    void shouldHandleConnectionFailure() throws Exception {
        mockServer.stop(0);
        mockServer = null;

        appProperties.getLlm().setBaseUrl("http://localhost:1");

        DoubaoLlmClient client = new DoubaoLlmClient(appProperties, objectMapper);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        client.streamGenerate(
                "测试",
                s -> {},
                () -> latch.countDown(),
                e -> { errorRef.set(e); latch.countDown(); }
        );

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(errorRef.get() instanceof RuntimeException);
    }
}
