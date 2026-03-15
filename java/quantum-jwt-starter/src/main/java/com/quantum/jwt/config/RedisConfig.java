package com.quantum.jwt.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "quantum.jwt", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        // StringRedisTemplate is optimized for String keys/values
        return new StringRedisTemplate(connectionFactory);
    }
}