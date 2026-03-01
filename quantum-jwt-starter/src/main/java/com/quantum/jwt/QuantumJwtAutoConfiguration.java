// package com.quantum.jwt;

// import org.springframework.boot.autoconfigure.AutoConfiguration;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
// import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
// import org.springframework.boot.context.properties.EnableConfigurationProperties;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Import;
// import org.springframework.data.redis.core.StringRedisTemplate;

// @AutoConfiguration(after = RedisAutoConfiguration.class)
// @EnableConfigurationProperties(QuantumJwtProperties.class)
// @ConditionalOnClass({StringRedisTemplate.class, Jwts.class})
// public class QuantumJwtAutoConfiguration {

//     @Bean
//     @ConditionalOnMissingBean
//     public KeyManager keyManager(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
//         return new KeyManager(redisTemplate, properties);
//     }

//     @Bean
//     @ConditionalOnMissingBean
//     public KeyStore keyStore(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
//         return new KeyStore(redisTemplate, properties);
//     }

//     @Bean
//     @ConditionalOnMissingBean
//     public RateTracker rateTracker(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
//         return new RateTracker(redisTemplate, properties);
//     }

//     @Bean
//     @ConditionalOnMissingBean
//     public KeyCache keyCache(QuantumJwtProperties properties) {
//         return new KeyCache(properties.getCacheMaxSize());
//     }

//     @Bean
//     @ConditionalOnMissingBean
//     public QuantumJwtService quantumJwtService(KeyManager keyManager, KeyStore keyStore,
//                                                 RateTracker rateTracker, QuantumJwtProperties properties,
//                                                 KeyCache keyCache) {
//         return new QuantumJwtService(keyManager, keyStore, rateTracker, properties, keyCache);
//     }
// }




package com.quantum.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration(after = RedisAutoConfiguration.class)
@EnableConfigurationProperties(QuantumJwtProperties.class)
@ConditionalOnClass({StringRedisTemplate.class, Jwts.class})
public class QuantumJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeyManager keyManager(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        return new KeyManager(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyStore keyStore(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        return new KeyStore(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateTracker rateTracker(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        return new RateTracker(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyCache keyCache(QuantumJwtProperties properties) {
        return new KeyCache(properties.getCacheMaxSize());
    }

    @Bean
    @ConditionalOnMissingBean
    public QuantumJwtService quantumJwtService(KeyManager keyManager, KeyStore keyStore,
                                                RateTracker rateTracker, QuantumJwtProperties properties,
                                                KeyCache keyCache) {
        return new QuantumJwtService(keyManager, keyStore, rateTracker, properties, keyCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public QuantumMetrics quantumMetrics(QuantumJwtService jwtService, KeyManager keyManager) {
        return new QuantumMetrics(jwtService, keyManager);
    }
}