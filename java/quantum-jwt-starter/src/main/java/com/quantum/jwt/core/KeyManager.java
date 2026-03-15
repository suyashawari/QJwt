package com.quantum.jwt.core;

import javax.crypto.SecretKey;
import java.time.Duration;

public interface KeyManager {
    KeyData storeKey(byte[] keyBytes, Duration ttl);
    SecretKey getKey(String kid);
    String extractKidFromToken(String token);

    record KeyData(String kid, SecretKey secretKey) {}
}
