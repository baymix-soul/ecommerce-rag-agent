package com.ecommerce.rag.rag.retriever;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.rag.models.entity.Product;

@SpringBootTest
class KeywordRetrieverTest {

    @Autowired
    private KeywordRetriever keywordRetriever;

    @Test
    void shouldRetrieveProductsByKeyword() {
        List<Product> products = keywordRetriever.retrieve("油皮 洗面奶", 5);

        assertNotNull(products);
        assertTrue(products.size() > 0, "Should find at least one product");
    }

    @Test
    void shouldReturnEmptyForBlankQuery() {
        List<Product> products = keywordRetriever.retrieve("", 5);

        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullQuery() {
        List<Product> products = keywordRetriever.retrieve(null, 5);

        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    void shouldRespectLimit() {
        List<Product> products = keywordRetriever.retrieve("洗面奶", 3);

        assertNotNull(products);
        assertTrue(products.size() <= 3);
    }

    @Test
    void shouldPopulateProductFields() {
        List<Product> products = keywordRetriever.retrieve("雅诗兰黛", 1);

        if (!products.isEmpty()) {
            Product p = products.get(0);
            assertNotNull(p.getProductId());
            assertNotNull(p.getName());
            assertNotNull(p.getPrice());
        }
    }
}
