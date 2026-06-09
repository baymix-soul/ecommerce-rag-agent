package com.ecommerce.rag.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.ecommerce.rag.models.entity.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonLoader {

    private static final Logger log = LoggerFactory.getLogger(JsonLoader.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public JsonLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    public List<Product> loadProducts(String location) {
        log.info("Loading product data from: {}", location);

        Resource resource = resourceLoader.getResource(location);

        if (!resource.exists()) {
            throw new IllegalStateException("Product data file not found: " + location);
        }

        try (InputStream is = resource.getInputStream()) {
            List<Product> products = objectMapper.readValue(is, new TypeReference<List<Product>>() {});
            log.info("Loaded {} products from {}", products.size(), location);
            return products;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse product data from: " + location, e);
        }
    }
}
