package com.ecommerce.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ecommerce.rag.core.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
