package com.quantum.jwt.core;

import com.quantum.jwt.config.QuantumJwtProperties;
import com.quantum.jwt.exception.InvalidTokenException;
import com.quantum.jwt.security.HoneyTokenEvaluator;
import com.quantum.jwt.security.IpBindingValidator;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QuantumJwtService {
    private final KeyManager keyManager;
    private final EntropyPool entropyPool;
    private final StringRedisTemplate redisTemplate;
    private final TokenSigner tokenSigner;
    private final TokenValidator tokenValidator;
    private final HoneyTokenEvaluator honeyTokenEvaluator;
    private final IpBindingValidator ipBindingValidator;
    private final QuantumJwtProperties properties;

    public QuantumJwtService(KeyManager keyManager,
            EntropyPool entropyPool,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            TokenSigner tokenSigner,
            TokenValidator tokenValidator,
            HoneyTokenEvaluator honeyTokenEvaluator,
            IpBindingValidator ipBindingValidator,
            QuantumJwtProperties properties) {
        this.keyManager = keyManager;
        this.entropyPool = entropyPool;
        this.redisTemplate = redisTemplate;
        this.tokenSigner = tokenSigner;
        this.tokenValidator = tokenValidator;
        this.honeyTokenEvaluator = honeyTokenEvaluator;
        this.ipBindingValidator = ipBindingValidator;
        this.properties = properties;
    }

    /**
     * Generates a new JWT using a key from the entropy pool.
     * 
     * @param subject          the user identifier (sub claim)
     * @param additionalClaims any extra claims to include
     * @param clientIp         the client IP address (may be null; if null and IP
     *                         binding is enabled, the ip claim is omitted)
     * @return a signed JWT string
     */
    public String generateToken(String subject, Map<String, Object> additionalClaims, String clientIp) {
        byte[] keyBytes = entropyPool.popKey();
        KeyManager.KeyData keyData = keyManager.storeKey(keyBytes, properties.getTokenTtl());

        Map<String, Object> claims = new HashMap<>(additionalClaims);
        if (clientIp != null && properties.isIpBindingEnabled()) {
            claims.put("ip", clientIp);
        }

        if (properties.isWatermarkEnabled()) {
            claims.put("qw", generateWatermark(subject));
        }

        String token = tokenSigner.sign(subject, claims, keyData.kid(), keyData.secretKey());
        if (redisTemplate != null) {
            redisTemplate.opsForHash().increment("miner_stats", "tokens_generated", 1);
        }
        return token;
    }

    /**
     * Generates a "Honey Token" JWT.
     * This token looks structurally identical to a normal QJWT, but its Key ID (kid)
     * is instantly added to the active threat intelligence blocklist.
     * Any attempt to use this token will trigger a HoneyTokenException.
     *
     * @param subject          the mock user identifier
     * @param additionalClaims any extra claims to include
     * @param clientIp         the client IP address
     * @return a signed Honey Token string
     */
    public String generateHoneyToken(String subject, Map<String, Object> additionalClaims, String clientIp) {
        byte[] keyBytes = entropyPool.popKey();
        // Honey tokens can have longer TTLs to sit inactive in a repo
        KeyManager.KeyData keyData = keyManager.storeKey(keyBytes, properties.getTokenTtl().multipliedBy(10));

        Map<String, Object> claims = new HashMap<>(additionalClaims);
        if (clientIp != null && properties.isIpBindingEnabled()) {
            claims.put("ip", clientIp);
        }

        if (properties.isWatermarkEnabled()) {
            claims.put("qw", generateWatermark(subject));
        }

        String honeyToken = tokenSigner.sign(subject, claims, keyData.kid(), keyData.secretKey());
        
        if (redisTemplate != null) {
            // Instantly poison this key ID so any usage triggers the alarm
            redisTemplate.opsForSet().add("poison_keys", keyData.kid());
            // It's a honey token, but we still count it as a generated token for metrics
            redisTemplate.opsForHash().increment("miner_stats", "tokens_generated", 1);
        }
        return honeyToken;
    }

    /**
     * Validates a JWT.
     * 
     * @param token    the JWT string
     * @param clientIp the client IP address (may be null; if null and IP binding is
     *                 enabled, IP validation is skipped)
     * @return the claims extracted from the token
     * @throws InvalidTokenException if the token is invalid, expired, or a honey
     *                               token
     */
    public Claims validateToken(String token, String clientIp) {
        try {
            String kid = keyManager.extractKidFromToken(token);
            if (kid == null) {
                throw new InvalidTokenException("Missing kid in token header");
            }

            // Check if this is a honey token
            honeyTokenEvaluator.checkForHoneyToken(kid);

            var secretKey = keyManager.getKey(kid);
            if (secretKey == null) {
                throw new InvalidTokenException("Key expired or revoked");
            }

            Claims claims = tokenValidator.validate(token, secretKey);

            if (properties.isIpBindingEnabled()) {
                ipBindingValidator.validate(claims, clientIp);
            }

            if (redisTemplate != null) {
                redisTemplate.opsForHash().increment("miner_stats", "tokens_validated", 1);
            }
            return claims;
        } catch (Exception e) {
            if (redisTemplate != null) {
                redisTemplate.opsForHash().increment("miner_stats", "tokens_failed", 1);
            }
            throw e;
        }
    }

    private String generateWatermark(String subject) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    properties.getWatermarkSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(subject.getBytes(StandardCharsets.UTF_8));

            // Return first 8 hex characters as a subtle watermark
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "q-null"; // Fallback if HMAC fails
        }
    }
}