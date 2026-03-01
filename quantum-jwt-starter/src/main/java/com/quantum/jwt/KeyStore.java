package com.quantum.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class KeyStore {
    private final StringRedisTemplate redisTemplate;
    private final QuantumJwtProperties properties;

    public KeyStore(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Generate a new key ID and store the key in Redis with TTL.
     * @param key the quantum key bytes
     * @return the generated kid
     */
    public String storeKey(byte[] key) {
        String kid = UUID.randomUUID().toString();
        String redisKey = properties.getKeyPrefix() + kid;
        String keyBase64 = Base64.getEncoder().encodeToString(key);
        redisTemplate.opsForValue().set(redisKey, keyBase64, properties.getKeyTtlSeconds(), TimeUnit.SECONDS);
        return kid;
    }

    /**
     * Retrieve a key by its ID.
     * @param kid the key identifier
     * @return the key bytes, or null if not found
     */
    public byte[] retrieveKey(String kid) {
        String redisKey = properties.getKeyPrefix() + kid;
        String keyBase64 = redisTemplate.opsForValue().get(redisKey);
        if (keyBase64 == null) {
            return null;
        }
        return Base64.getDecoder().decode(keyBase64);
    }
}
