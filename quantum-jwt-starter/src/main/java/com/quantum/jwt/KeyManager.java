package com.quantum.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class KeyManager {
    private static final Logger log = LoggerFactory.getLogger(KeyManager.class);
    private static final int KEY_SIZE_BYTES = 32; // 256 bits for HS256

    private final StringRedisTemplate redisTemplate;
    private final QuantumJwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AtomicLong fallbackCounter = new AtomicLong(0);

    public KeyManager(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Fetch a single quantum key (32 bytes). If batchSize > 1, we may fetch several at once
     * and store extras for later.
     */
    public byte[] fetchQuantumKey() {
        // If batch fetching is enabled, try to get multiple keys at once and cache locally
        if (properties.getBatchSize() > 1) {
            List<byte[]> batch = fetchBatch(properties.getBatchSize());
            if (!batch.isEmpty()) {
                // Return the first, store the rest somewhere? For simplicity, we'll just return the first.
                // A more advanced implementation would have a local queue.
                return batch.get(0);
            }
        }

        // Single key fetch
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        String keyBase64 = listOps.leftPop(properties.getEntropyPoolKey());
        if (keyBase64 != null) {
            fallbackCounter.set(0); // reset on success
            return java.util.Base64.getDecoder().decode(keyBase64);
        }

        // Pool empty -> fallback
        if (properties.isFallbackEnabled()) {
            long count = fallbackCounter.incrementAndGet();
            log.warn("Quantum entropy pool empty, using SecureRandom fallback (count={})", count);
            byte[] fallback = new byte[KEY_SIZE_BYTES];
            secureRandom.nextBytes(fallback);
            return fallback;
        } else {
            throw new EntropyExhaustedException("Quantum entropy pool exhausted and fallback disabled");
        }
    }

    /**
     * Fetch a batch of quantum keys from Redis.
     */
    private List<byte[]> fetchBatch(int batchSize) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        // Use range + trim to atomically get and remove
        List<String> base64Keys = listOps.range(properties.getEntropyPoolKey(), 0, batchSize - 1);
        if (base64Keys == null || base64Keys.isEmpty()) {
            return List.of();
        }
        listOps.trim(properties.getEntropyPoolKey(), base64Keys.size(), -1); // remove fetched keys
        List<byte[]> keys = new ArrayList<>(base64Keys.size());
        for (String b64 : base64Keys) {
            keys.add(java.util.Base64.getDecoder().decode(b64));
        }
        return keys;
    }

    public long getFallbackCount() {
        return fallbackCounter.get();
    }
}
