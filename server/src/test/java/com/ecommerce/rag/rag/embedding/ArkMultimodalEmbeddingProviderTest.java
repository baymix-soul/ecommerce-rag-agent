package com.ecommerce.rag.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class ArkMultimodalEmbeddingProviderTest {

    @Test
    void shouldEmbedSingleTextSuccessfully() throws Exception {
        HttpServer server = createServer(200, responseBody(64));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            List<Double> vec = provider.embed("测试文本");

            assertNotNull(vec);
            assertEquals(64, vec.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldEmbedBatchSuccessfully() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embeddings/multimodal", exchange -> {
            callCount.incrementAndGet();
            byte[] bytes = responseBody(64).getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            List<List<Double>> vectors = provider.embedBatch(List.of("A", "B", "C"));

            assertEquals(3, vectors.size());
            assertEquals(3, callCount.get());
            for (List<Double> v : vectors) {
                assertEquals(64, v.size());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldUseMultimodalPath() throws Exception {
        AtomicReference<String> capturedPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embeddings/multimodal", exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            byte[] bytes = responseBody(64).getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            provider.embed("test");

            assertEquals("/embeddings/multimodal", capturedPath.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void inputShouldBeObjectArrayNotStringArray() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embeddings/multimodal", exchange -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = exchange.getRequestBody()) {
                is.transferTo(baos);
            }
            capturedBody.set(baos.toString(StandardCharsets.UTF_8));
            byte[] bytes = responseBody(64).getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            provider.embed("测试文本");

            String body = capturedBody.get();
            assertTrue(body.contains("\"type\""));
            assertTrue(body.contains("\"text\""));
            assertTrue(body.contains("测试文本"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnCorrectDimension() throws Exception {
        HttpServer server = createServer(200, responseBody(128));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 128, 16, 30, "/embeddings/multimodal");

            assertEquals(128, provider.dimension());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowWhenDimensionMismatch() throws Exception {
        HttpServer server = createServer(200, responseBody(128));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("128"));
            assertTrue(ex.getMessage().contains("64"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOn401() throws Exception {
        HttpServer server = createServer(401, "{\"error\":\"Unauthorized\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "bad-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("401"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOn429() throws Exception {
        HttpServer server = createServer(429, "{\"error\":\"Rate limited\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("429"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOn5xx() throws Exception {
        HttpServer server = createServer(503, "{\"error\":\"Service Unavailable\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("503"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldNotLeakApiKeyInException() throws Exception {
        HttpServer server = createServer(401, "{\"error\":\"Unauthorized\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "secret-key-abc123", "test-model", 64, 16, 30, "/embeddings/multimodal");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(!ex.getMessage().contains("secret-key-abc123"),
                    "Exception should not contain API key: " + ex.getMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnModelName() throws Exception {
        HttpServer server = createServer(200, responseBody(64));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "doubao-embedding-vision-251215", 64, 16, 30, "/embeddings/multimodal");

            assertEquals("doubao-embedding-vision-251215", provider.modelName());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOnNullText() throws Exception {
        HttpServer server = createServer(200, responseBody(64));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            assertThrows(IllegalArgumentException.class, () -> provider.embed(null));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOnBlankText() throws Exception {
        HttpServer server = createServer(200, responseBody(64));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            ArkMultimodalEmbeddingProvider provider = new ArkMultimodalEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30, "/embeddings/multimodal");

            assertThrows(IllegalArgumentException.class, () -> provider.embed("  "));
        } finally {
            server.stop(0);
        }
    }

    // --- helpers ---

    private HttpServer createServer(int statusCode, String responseBody) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/embeddings/multimodal", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseBody.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        return server;
    }

    private String responseBody(int dim) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":{\"embedding\":[");
        for (int j = 0; j < dim; j++) {
            if (j > 0) sb.append(",");
            sb.append(String.format("%.6f", (j + 1) * 0.01));
        }
        sb.append("]}}");
        return sb.toString();
    }
}
