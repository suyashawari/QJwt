// package com.quantum.auth.service;

// import com.quantum.auth.exception.EntropyExhaustedException;
// import com.quantum.auth.exception.HoneyTokenException;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.stereotype.Service;

// import java.util.Map;
// import java.util.UUID;
// import java.util.concurrent.TimeUnit;

// /**
//  * Quantum Service - Manages quantum entropy keys from Redis.
//  * 
//  * This service acts as the "Consumer" in our Producer-Consumer architecture,
//  * popping quantum-generated keys from the pool and managing JWT key mappings.
//  */
// @Service
// public class QuantumService {

//     private static final Logger log = LoggerFactory.getLogger(QuantumService.class);

//     private final StringRedisTemplate redisTemplate;

//     @Value("${quantum.pool.key:entropy_pool}")
//     private String entropyPoolKey;

//     @Value("${quantum.pool.poison-set:poison_keys}")
//     private String poisonKeysSet;

//     @Value("${quantum.pool.auth-prefix:auth:kid:}")
//     private String authKeyPrefix;

//     @Value("${jwt.default-ttl-seconds:900}")
//     private long defaultTtlSeconds;

//     @Value("${jwt.high-risk-ttl-seconds:10}")
//     private long highRiskTtlSeconds;

//     public QuantumService(StringRedisTemplate redisTemplate) {
//         this.redisTemplate = redisTemplate;
//     }

//     /**
//      * Fetch a quantum key from the entropy pool (LPOP).
//      * Throws EntropyExhaustedException if pool is empty.
//      */
//     public String fetchQuantumKey() {
//         String key = redisTemplate.opsForList().leftPop(entropyPoolKey);

//         if (key == null) {
//             log.error("Entropy pool exhausted!");
//             throw new EntropyExhaustedException();
//         }

//         // Check if this is a honey token
//         if (key.startsWith("POISON_")) {
//             log.warn("Honey token fetched: {}", key);
//             // Remove from poison set (it's been used legitimately)
//             redisTemplate.opsForSet().remove(poisonKeysSet, key);
//             // Return the key portion after POISON_ prefix
//             return key.substring(7);
//         }

//         log.debug("Fetched quantum key from pool");
//         return key;
//     }

//     /**
//      * Store a key mapping for JWT verification.
//      * Uses SETEX for automatic expiration.
//      */
//     public String storeKeyMapping(String key, int riskScore) {
//         String kid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

//         // Adaptive TTL based on risk score
//         long ttl = riskScore > 70 ? highRiskTtlSeconds : defaultTtlSeconds;

//         String redisKey = authKeyPrefix + kid;
//         redisTemplate.opsForValue().set(redisKey, key, ttl, TimeUnit.SECONDS);

//         log.info("Stored key mapping: kid={}, ttl={}s, riskScore={}", kid, ttl, riskScore);

//         return kid;
//     }

//     /**
//      * Retrieve a key by its Key ID for JWT verification.
//      */
//     public String getKeyByKid(String kid, String clientIp) {
//         String redisKey = authKeyPrefix + kid;
//         String key = redisTemplate.opsForValue().get(redisKey);

//         if (key == null) {
//             log.warn("Key not found or expired: kid={}", kid);
//             return null;
//         }

//         // Check if this key is in the poison set (honey token detection)
//         if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(poisonKeysSet, "POISON_" + key))) {
//             log.error("🚨 SECURITY ALERT: Honey token used! IP: {}, kid: {}", clientIp, kid);
//             blockIp(clientIp);
//             throw new HoneyTokenException(clientIp);
//         }

//         return key;
//     }

//     /**
//      * Block an IP address (add to blocklist).
//      */
//     public void blockIp(String ip) {
//         String blockKey = "blocked_ip:" + ip;
//         redisTemplate.opsForValue().set(blockKey, "honey_token_trigger", 24, TimeUnit.HOURS);
//         log.error("🚫 IP BLOCKED: {} for 24 hours", ip);
//     }

//     /**
//      * Check if an IP is blocked.
//      */
//     public boolean isIpBlocked(String ip) {
//         String blockKey = "blocked_ip:" + ip;
//         return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
//     }

//     /**
//      * Get current pool size.
//      */
//     public Long getPoolSize() {
//         return redisTemplate.opsForList().size(entropyPoolKey);
//     }

//     /**
//      * Get miner statistics.
//      */
//     public Map<Object, Object> getMinerStats() {
//         return redisTemplate.opsForHash().entries("miner_stats");
//     }

//     /**
//      * Determine key source (quantum vs classical) based on stats.
//      */
//     public String getKeySource() {
//         Map<Object, Object> stats = getMinerStats();
//         Long quantum = parseLong(stats.get("keys_from_quantum"));
//         Long classical = parseLong(stats.get("keys_from_classical"));

//         if (quantum > classical) {
//             return "quantum";
//         } else if (classical > 0) {
//             return "classical";
//         }
//         return "unknown";
//     }

//     private Long parseLong(Object value) {
//         if (value == null)
//             return 0L;
//         try {
//             return Long.parseLong(value.toString());
//         } catch (NumberFormatException e) {
//             return 0L;
//         }
//     }
// }
