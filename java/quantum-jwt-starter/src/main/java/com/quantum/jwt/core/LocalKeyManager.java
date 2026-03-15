package com.quantum.jwt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalKeyManager implements KeyManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalKeyManager.class);
    
    private record LocalKeyEntry(SecretKey secretKey, Instant expiry) {}
    private final Map<String, LocalKeyEntry> keyStore = new ConcurrentHashMap<>();

    @Override
    public KeyData storeKey(byte[] keyBytes, Duration ttl) {
        String kid = UUID.randomUUID().toString();
        SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        Instant expiry = Instant.now().plus(ttl);
        
        keyStore.put(kid, new LocalKeyEntry(secretKey, expiry));
        logger.debug("Stored local key with kid: {}", kid);
        return new KeyData(kid, secretKey);
    }

    @Override
    public SecretKey getKey(String kid) {
        LocalKeyEntry entry = keyStore.get(kid);
        if (entry == null) {
            logger.debug("Local key not found: {}", kid);
            return null;
        }
        
        if (Instant.now().isAfter(entry.expiry())) {
            logger.debug("Local key expired: {}", kid);
            keyStore.remove(kid);
            return null;
        }
        
        return entry.secretKey();
    }

    @Override
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
