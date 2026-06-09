package com.ecommerce.rag.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

class OpenAIStyleEmbeddingProviderTest {

    @Test
    void shouldEmbedSingleTextSuccessfully() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(64, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            List<Double> vec = provider.embed("测试文本");

            assertNotNull(vec);
            assertEquals(64, vec.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldEmbedBatchSuccessfully() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(64, 3));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            List<List<Double>> vectors = provider.embedBatch(List.of("A", "B", "C"));

            assertEquals(3, vectors.size());
            for (List<Double> v : vectors) {
                assertEquals(64, v.size());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnCorrectDimension() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(128, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 128, 16, 30);

            assertEquals(128, provider.dimension());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowWhenDimensionMismatch() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(128, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("dimension"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOn401() throws Exception {
        HttpServer server = createEmbeddingServer(401, "{\"error\":\"Unauthorized\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "bad-key", "test-model", 64, 16, 30);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("401"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOn429() throws Exception {
        HttpServer server = createEmbeddingServer(429, "{\"error\":\"Rate limited\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(ex.getMessage().contains("429"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldNotLeakApiKeyInException() throws Exception {
        HttpServer server = createEmbeddingServer(401, "{\"error\":\"Unauthorized\"}");
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "secret-key-abc123", "test-model", 64, 16, 30);

            RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.embed("test"));
            assertTrue(!ex.getMessage().contains("secret-key-abc123"),
                    "Exception should not contain API key: " + ex.getMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnModelName() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(64, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "my-embedding-model", 64, 16, 30);

            assertEquals("my-embedding-model", provider.modelName());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOnNullText() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(64, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            assertThrows(IllegalArgumentException.class, () -> provider.embed(null));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldThrowOnBlankText() throws Exception {
        HttpServer server = createEmbeddingServer(200, responseBody(64, 1));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort();
            OpenAIStyleEmbeddingProvider provider = new OpenAIStyleEmbeddingProvider(
                    url, "test-key", "test-model", 64, 16, 30);

            assertThrows(IllegalArgumentException.class, () -> provider.embed("  "));
        } finally {
            server.stop(0);
        }
    }

    // --- helpers ---

    private HttpServer createEmbeddingServer(int statusCode, String responseBody) throws Exception {
        AtomicReference<HttpServer> serverRef = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        serverRef.set(server);
        server.createContext("/embeddings", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseBody.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        return server;
    }

    private String responseBody(int dim, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"data\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"embedding\":[");
            for (int j = 0; j < dim; j++) {
                if (j > 0) sb.append(",");
                sb.append(String.format("%.6f", (j + 1) * 0.01));
            }
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
