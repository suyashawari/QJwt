package com.quantum.jwt.security;

import com.quantum.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

@Component
public class IpBindingValidator {
    /**
     * Compares the IP address stored in the token with the client's actual IP.
     * @throws InvalidTokenException if they do not match
     */
    public void validate(Claims claims, String clientIp) {
        if (clientIp == null) return;
        String tokenIp = claims.get("ip", String.class);
        if (tokenIp != null && !tokenIp.equals(clientIp)) {
            throw new InvalidTokenException("IP address mismatch");
        }
    }
}