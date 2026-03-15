package com.quantum.jwt.core;

import com.quantum.jwt.exception.EntropyExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.util.Base64;

public class RedisEntropyPool implements EntropyPool {
    private static final Logger logger = LoggerFactory.getLogger(RedisEntropyPool.class);
    private static final String ENTROPY_POOL = "entropy:pool";
    public static boolean SIMULATE_CRASH = false;
    private final StringRedisTemplate redisTemplate;
    private final LocalEntropyPool fallbackPool;

    public RedisEntropyPool(StringRedisTemplate redisTemplate, com.quantum.jwt.config.QuantumJwtProperties props) {
        this.redisTemplate = redisTemplate;
        this.fallbackPool = new LocalEntropyPool(props);
    }

    /**
     * Pops one base64-encoded key from the Redis pool, decodes it and returns the raw bytes.
     * @throws EntropyExhaustedException if the pool is empty
     */
    public byte[] popKey() {
        if (SIMULATE_CRASH) {
            logger.warn("Simulated Redis Crash! Resilient Switch to Local Entropy.");
            return fallbackPool.popKey();
        }
        
        try {
            String base64Key = redisTemplate.opsForList().leftPop(ENTROPY_POOL);
            if (base64Key == null) {
                logger.warn("Redis Entropy pool exhausted. Falling back to Local.");
                return fallbackPool.popKey();
            }
            return Base64.getUrlDecoder().decode(base64Key);
        } catch (Exception e) {
            logger.error("Redis Connection Failed! Resilient Fallback Activated.", e);
            return fallbackPool.popKey();
        }
    }

    /**
     * Returns the current number of keys in the pool.
     */
    public long getPoolSize() {
        if (SIMULATE_CRASH) {
            return fallbackPool.getPoolSize();
        }
        try {
            Long size = redisTemplate.opsForList().size(ENTROPY_POOL);
            return size == null ? 0 : size;
        } catch (Exception e) {
            return fallbackPool.getPoolSize();
        }
    }
}