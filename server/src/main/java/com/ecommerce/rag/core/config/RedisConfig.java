package com.ecommerce.rag.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    private final AppProperties appProperties;

    public RedisConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory() {
        AppProperties.RedisProperties redis = appProperties.getRedis();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis.getHost());
        config.setPort(redis.getPort());
        config.setDatabase(redis.getDatabase());
        String password = redis.getPassword();
        if (password != null && !password.isEmpty()) {
            config.setPassword(org.springframework.data.redis.connection.RedisPassword.of(password));
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
