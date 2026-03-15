package com.quantum.jwt.security;

import com.quantum.jwt.exception.HoneyTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

public class HoneyTokenEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(HoneyTokenEvaluator.class);
    private static final String POISON_KEYS_SET = "poison_keys";
    private final StringRedisTemplate redisTemplate;

    public HoneyTokenEvaluator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the given key ID is marked as a honey token.
     * @throws HoneyTokenException if it is a honey token
     */
    public void checkForHoneyToken(String kid) {
        if (redisTemplate == null) {
            return; // No Redis — honey token checking disabled
        }
        Boolean isPoison = redisTemplate.opsForSet().isMember(POISON_KEYS_SET, kid);
        if (Boolean.TRUE.equals(isPoison)) {
            logger.error("Honey token triggered! kid: {}", kid);
            throw new HoneyTokenException("Honey token detected – possible breach");
        }
    }
}