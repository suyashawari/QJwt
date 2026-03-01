// package com.quantum.auth.service;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;

// /**
//  * JWT Service - Handles token creation and verification.
//  * Uses quantum-seeded keys for signing.
//  */
// @Service
// public class JwtService {

//     private static final Logger log = LoggerFactory.getLogger(JwtService.class);

//     private final QuantumService quantumService;

//     @Value("${jwt.default-ttl-seconds:900}")
//     private long defaultTtlSeconds;

//     @Value("${jwt.high-risk-ttl-seconds:10}")
//     private long highRiskTtlSeconds;

//     public JwtService(QuantumService quantumService) {
//         this.quantumService = quantumService;
//     }

//     /**
//      * Generate a JWT signed with a quantum-seeded key.
//      */
//     public Map<String, Object> generateToken(String username, int riskScore) {
//         // Fetch quantum key from pool
//         String quantumKey = quantumService.fetchQuantumKey();

//         // Store the key with adaptive TTL and get kid
//         String kid = quantumService.storeKeyMapping(quantumKey, riskScore);

//         // Calculate TTL based on risk
//         long ttlSeconds = riskScore > 70 ? highRiskTtlSeconds : defaultTtlSeconds;
//         long expirationMs = System.currentTimeMillis() + (ttlSeconds * 1000);

//         // Create signing key
//         SecretKey signingKey = Keys.hmacShaKeyFor(
//                 quantumKey.getBytes(StandardCharsets.UTF_8));

//         // Build JWT with kid in header
//         String token = Jwts.builder()
//                 .header()
//                 .keyId(kid)
//                 .add("typ", "QWT") // Quantum Web Token
//                 .and()
//                 .subject(username)
//                 .issuedAt(new Date())
//                 .expiration(new Date(expirationMs))
//                 .claim("risk_score", riskScore)
//                 .claim("key_source", quantumService.getKeySource())
//                 .signWith(signingKey)
//                 .compact();

//         log.info("Generated QWT for user: {}, kid: {}, expires: {}s", username, kid, ttlSeconds);

//         Map<String, Object> result = new HashMap<>();
//         result.put("token", token);
//         result.put("kid", kid);
//         result.put("expiresIn", ttlSeconds);
//         result.put("keySource", quantumService.getKeySource());

//         return result;
//     }

//     /**
//      * Verify and decode a JWT.
//      */
//     public Claims verifyToken(String token, String clientIp) {
//         try {
//             // First, parse without verification to get kid from header
//             String[] parts = token.split("\\.");
//             if (parts.length != 3) {
//                 log.warn("Invalid token format");
//                 return null;
//             }

//             // Decode header to get kid
//             String headerJson = new String(
//                     java.util.Base64.getUrlDecoder().decode(parts[0]),
//                     StandardCharsets.UTF_8);

//             // Simple JSON parsing for kid (avoid extra dependencies)
//             String kid = extractKidFromHeader(headerJson);
//             if (kid == null) {
//                 log.warn("No kid found in token header");
//                 return null;
//             }

//             // Look up the key by kid
//             String quantumKey = quantumService.getKeyByKid(kid, clientIp);
//             if (quantumKey == null) {
//                 log.warn("Key not found or expired for kid: {}", kid);
//                 return null;
//             }

//             // Build signing key
//             SecretKey signingKey = Keys.hmacShaKeyFor(
//                     quantumKey.getBytes(StandardCharsets.UTF_8));

//             // Verify and parse
//             Claims claims = Jwts.parser()
//                     .verifyWith(signingKey)
//                     .build()
//                     .parseSignedClaims(token)
//                     .getPayload();

//             log.debug("Token verified successfully for subject: {}", claims.getSubject());
//             return claims;

//         } catch (Exception e) {
//             log.error("Token verification failed: {}", e.getMessage());
//             return null;
//         }
//     }

//     private String extractKidFromHeader(String headerJson) {
//         // Simple extraction - look for "kid":"value"
//         int kidIndex = headerJson.indexOf("\"kid\"");
//         if (kidIndex == -1)
//             return null;

//         int colonIndex = headerJson.indexOf(":", kidIndex);
//         int quoteStart = headerJson.indexOf("\"", colonIndex);
//         int quoteEnd = headerJson.indexOf("\"", quoteStart + 1);

//         if (quoteStart != -1 && quoteEnd != -1) {
//             return headerJson.substring(quoteStart + 1, quoteEnd);
//         }
//         return null;
//     }
// }
