package com.quantum.jwt.config;

import com.quantum.jwt.core.*;
import com.quantum.jwt.security.HoneyTokenEvaluator;
import com.quantum.jwt.security.IpBindingValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(QuantumJwtProperties.class)
@Import(RedisConfig.class)
public class QuantumJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EntropyPool.class)
    @ConditionalOnProperty(prefix = "quantum.jwt", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public EntropyPool redisEntropyPool(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        return new RedisEntropyPool(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(KeyManager.class)
    @ConditionalOnProperty(prefix = "quantum.jwt", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public KeyManager redisKeyManager(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        return new RedisKeyManager(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(EntropyPool.class)
    @ConditionalOnProperty(prefix = "quantum.jwt", name = "redis-enabled", havingValue = "false")
    public EntropyPool localEntropyPool(QuantumJwtProperties properties) {
        return new LocalEntropyPool(properties);
    }

    @Bean
    @ConditionalOnMissingBean(KeyManager.class)
    @ConditionalOnProperty(prefix = "quantum.jwt", name = "redis-enabled", havingValue = "false")
    public KeyManager localKeyManager() {
        return new LocalKeyManager();
    }


    @Bean
    @ConditionalOnMissingBean
    public HoneyTokenEvaluator honeyTokenEvaluator(
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        return new HoneyTokenEvaluator(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public QuantumJwtService quantumJwtService(KeyManager keyManager,
            EntropyPool entropyPool,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            TokenSigner tokenSigner,
            TokenValidator tokenValidator,
            HoneyTokenEvaluator honeyTokenEvaluator,
            IpBindingValidator ipBindingValidator,
            QuantumJwtProperties properties) {
        return new QuantumJwtService(
                keyManager,
                entropyPool,
                redisTemplate,
                tokenSigner,
                tokenValidator,
                honeyTokenEvaluator,
                ipBindingValidator,
                properties);
    }
}