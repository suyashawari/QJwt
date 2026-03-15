package com.quantum.jwt.core;

import com.quantum.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class TokenValidator {
    /**
     * Validates a JWT using the provided secret key.
     * @return the claims if valid
     * @throws InvalidTokenException if the token is invalid or expired
     */
    public Claims validate(String token, SecretKey key) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (JwtException e) {
            throw new InvalidTokenException("Token validation failed: " + e.getMessage());
        }
    }
}