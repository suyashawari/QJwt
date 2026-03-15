package com.quantum.jwt.core;


import com.quantum.jwt.config.QuantumJwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

public class RedisKeyManager implements KeyManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisKeyManager.class);
    private static final String KEY_PREFIX = "key:";

    private final StringRedisTemplate redisTemplate;

    public RedisKeyManager(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        this.redisTemplate = redisTemplate;
        // properties not used directly, kept for future extension
    }

    /**
     * Stores a raw key (bytes) in Redis under a generated key ID, with the given TTL.
     * @return a record containing the key ID and the corresponding SecretKey
     */
    public KeyData storeKey(byte[] keyBytes, Duration ttl) {
        String kid = UUID.randomUUID().toString();
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String keyStorageKey = KEY_PREFIX + kid;
        redisTemplate.opsForValue().set(keyStorageKey, base64, ttl);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        logger.debug("Stored key with kid: {}", kid);
        return new KeyData(kid, secretKey);
    }

    /**
     * Retrieves a SecretKey by its key ID from Redis.
     * @return the key, or null if not found (expired or never existed)
     */
    public SecretKey getKey(String kid) {
        String keyStorageKey = KEY_PREFIX + kid;
        String rawKeyBase64 = redisTemplate.opsForValue().get(keyStorageKey);
        if (rawKeyBase64 == null) {
            logger.debug("Key not found or expired: {}", kid);
            return null;
        }
        byte[] decoded = Base64.getUrlDecoder().decode(rawKeyBase64);
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    /**
     * Extracts the key ID (kid) from a JWT header.
     * @return the kid, or null if it cannot be parsed
     */
    public String extractKidFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            int index = headerJson.indexOf("\"kid\":");
            if (index == -1) return null;
            int start = headerJson.indexOf("\"", index + 6) + 1;
            int end = headerJson.indexOf("\"", start);
            return headerJson.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

}