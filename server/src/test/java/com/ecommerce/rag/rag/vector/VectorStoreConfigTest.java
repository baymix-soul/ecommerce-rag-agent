package com.ecommerce.rag.rag.vector;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
class VectorStoreConfigTest {

    @Autowired
    private VectorStoreService vectorStoreService;

    @Test
    void shouldDefaultToInMemoryVectorStore() {
        assertInstanceOf(InMemoryVectorStoreService.class, vectorStoreService,
                "Default should be InMemoryVectorStoreService");
    }
}
