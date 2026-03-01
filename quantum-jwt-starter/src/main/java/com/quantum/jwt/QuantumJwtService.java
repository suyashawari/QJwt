// package com.quantum.jwt;

// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.SignatureAlgorithm;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.security.Key;
// import java.util.Date;
// import java.util.concurrent.atomic.AtomicLong;

// public class QuantumJwtService {
//     private static final Logger log = LoggerFactory.getLogger(QuantumJwtService.class);

//     private final KeyManager keyManager;
//     private final KeyStore keyStore;
//     private final RateTracker rateTracker;
//     private final QuantumJwtProperties properties;
//     private final KeyCache keyCache;
//     private final AtomicLong tokenCounter = new AtomicLong(0);
//     private final AtomicLong adaptiveTriggerCounter = new AtomicLong(0);

//     public QuantumJwtService(KeyManager keyManager, KeyStore keyStore, RateTracker rateTracker,
//                              QuantumJwtProperties properties, KeyCache keyCache) {
//         this.keyManager = keyManager;
//         this.keyStore = keyStore;
//         this.rateTracker = rateTracker;
//         this.properties = properties;
//         this.keyCache = keyCache;
//     }

//     public String generateToken(String username, String clientIp) {
//         // 1. Get a fresh quantum key
//         byte[] keyBytes = keyManager.fetchQuantumKey();
//         // 2. Store it with a kid
//         String kid = keyStore.storeKey(keyBytes);
//         // 3. Compute adaptive expiry
//         long expirySeconds = rateTracker.getAdjustedExpiry(username);
//         if (expirySeconds != properties.getBaseExpirySeconds()) {
//             adaptiveTriggerCounter.incrementAndGet();
//         }
//         Date now = new Date();
//         Date expiry = new Date(now.getTime() + expirySeconds * 1000);

//         // 4. Build JWT
//         var builder = Jwts.builder()
//                 .setSubject(username)
//                 .setIssuedAt(now)
//                 .setExpiration(expiry)
//                 .claim("kid", kid)
//                 .signWith(new io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes).get(), SignatureAlgorithm.HS256);

//         if (properties.isFingerprintEnabled()) {
//             builder.claim("ip", clientIp); // simple; consider hashing for privacy
//         }

//         String token = builder.compact();
//         tokenCounter.incrementAndGet();
//         return token;
//     }

//     public String validateToken(String token, String clientIp) {
//         // 1. Parse without verification to get kid
//         var unverified = Jwts.parserBuilder().build().parseClaimsJwt(token);
//         String kid = unverified.getBody().get("kid", String.class);
//         if (kid == null) {
//             throw new RuntimeException("Missing kid in token");
//         }

//         // 2. Retrieve key (cache -> Redis)
//         byte[] keyBytes = keyCache.get(kid);
//         if (keyBytes == null) {
//             keyBytes = keyStore.retrieveKey(kid);
//             if (keyBytes == null) {
//                 throw new RuntimeException("Key not found for kid: " + kid);
//             }
//             keyCache.put(kid, keyBytes);
//         }

//         // 3. Verify token
//         var claims = Jwts.parserBuilder()
//                 .setSigningKey(keyBytes)
//                 .build()
//                 .parseClaimsJws(token)
//                 .getBody();

//         // 4. Validate fingerprint
//         if (properties.isFingerprintEnabled()) {
//             String tokenIp = claims.get("ip", String.class);
//             if (!clientIp.equals(tokenIp)) {
//                 throw new RuntimeException("IP address mismatch");
//             }
//         }

//         return claims.getSubject();
//     }

//     public long getTokenCount() {
//         return tokenCounter.get();
//     }

//     public long getAdaptiveTriggerCount() {
//         return adaptiveTriggerCounter.get();
//     }
// }



package com.quantum.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class QuantumJwtService {
    private static final Logger log = LoggerFactory.getLogger(QuantumJwtService.class);

    private final KeyManager keyManager;
    private final KeyStore keyStore;
    private final RateTracker rateTracker;
    private final QuantumJwtProperties properties;
    private final KeyCache keyCache;
    private final AtomicLong tokenCounter = new AtomicLong(0);
    private final AtomicLong adaptiveTriggerCounter = new AtomicLong(0);

    public QuantumJwtService(KeyManager keyManager, KeyStore keyStore, RateTracker rateTracker,
                             QuantumJwtProperties properties, KeyCache keyCache) {
        this.keyManager = keyManager;
        this.keyStore = keyStore;
        this.rateTracker = rateTracker;
        this.properties = properties;
        this.keyCache = keyCache;
    }

    public String generateToken(String username, String clientIp) {
        // 1. Get a fresh quantum key
        byte[] keyBytes = keyManager.fetchQuantumKey();
        // 2. Store it with a kid
        String kid = keyStore.storeKey(keyBytes);
        // 3. Compute adaptive expiry
        long expirySeconds = rateTracker.getAdjustedExpiry(username);
        if (expirySeconds != properties.getBaseExpirySeconds()) {
            adaptiveTriggerCounter.incrementAndGet();
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirySeconds * 1000);

        // 4. Build JWT
        var builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("kid", kid)
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256);   // <-- FIXED

        if (properties.isFingerprintEnabled()) {
            builder.claim("ip", clientIp);
        }

        String token = builder.compact();
        tokenCounter.incrementAndGet();
        return token;
    }

    public String validateToken(String token, String clientIp) {
        // 1. Parse without verification to get kid
        var unverified = Jwts.parserBuilder().build().parseClaimsJwt(token);
        String kid = unverified.getBody().get("kid", String.class);
        if (kid == null) {
            throw new RuntimeException("Missing kid in token");
        }

        // 2. Retrieve key (cache -> Redis)
        byte[] keyBytes = keyCache.get(kid);
        if (keyBytes == null) {
            keyBytes = keyStore.retrieveKey(kid);
            if (keyBytes == null) {
                throw new RuntimeException("Key not found for kid: " + kid);
            }
            keyCache.put(kid, keyBytes);
        }

        // 3. Verify token
        var claims = Jwts.parserBuilder()
                .setSigningKey(keyBytes)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // 4. Validate fingerprint
        if (properties.isFingerprintEnabled()) {
            String tokenIp = claims.get("ip", String.class);
            if (!clientIp.equals(tokenIp)) {
                throw new RuntimeException("IP address mismatch");
            }
        }

        return claims.getSubject();
    }

    public long getTokenCount() {
        return tokenCounter.get();
    }

    public long getAdaptiveTriggerCount() {
        return adaptiveTriggerCounter.get();
    }
}   