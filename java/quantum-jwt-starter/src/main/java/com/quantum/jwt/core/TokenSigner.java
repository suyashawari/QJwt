package com.quantum.jwt.core;

import com.quantum.jwt.config.QuantumJwtProperties;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class TokenSigner {
    private final QuantumJwtProperties properties;

    public TokenSigner(QuantumJwtProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates a signed JWT with the given subject, claims, key ID and signing key.
     */
    public String sign(String subject, Map<String, Object> claims, String kid, SecretKey key) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getTokenTtl().toMillis());

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .header().keyId(kid).and()
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}