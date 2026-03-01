package com.quantum.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;

public class RateTracker {
    private final StringRedisTemplate redisTemplate;
    private final QuantumJwtProperties properties;

    public RateTracker(StringRedisTemplate redisTemplate, QuantumJwtProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Record a request for a user and return the adjusted token expiry in seconds.
     */
    public long getAdjustedExpiry(String username) {
        if (!properties.isDynamicExpiryEnabled()) {
            return properties.getBaseExpirySeconds();
        }

        String key = "user:requests:" + username;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - 60; // last 60 seconds

        // Add current timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        // Remove old entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart - 1);
        // Set expiry on the key itself (auto-cleanup)
        redisTemplate.expire(key, 60, java.util.concurrent.TimeUnit.SECONDS);

        // Count requests in last minute
        Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
        if (count == null) count = 0L;

        long base = properties.getBaseExpirySeconds();
        if (count >= properties.getHighRateThreshold()) {
            return Math.max(base / 2, 60); // at least 1 minute
        } else if (count <= properties.getLowRateThreshold()) {
            return Math.min(base * 2, 3600); // at most 1 hour
        } else {
            return base;
        }
    }
}
