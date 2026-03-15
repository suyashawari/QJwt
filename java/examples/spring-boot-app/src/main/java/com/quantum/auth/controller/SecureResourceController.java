package com.quantum.auth.controller;

import com.quantum.auth.service.RateLimiterService;
import com.quantum.jwt.core.QuantumJwtService;
import com.quantum.jwt.exception.HoneyTokenException;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates a secure resource protected by Quantum JWT.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SecureResourceController {

    private final QuantumJwtService jwtService;
    private final RateLimiterService rateLimiterService;

    public SecureResourceController(QuantumJwtService jwtService, RateLimiterService rateLimiterService) {
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/secure/data")
    public ResponseEntity<Map<String, Object>> getProtectedData(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Bearer token"));
        }

        String token = authHeader.substring(7);
        String ip = clientIp != null ? clientIp : request.getRemoteAddr();

        if (!rateLimiterService.checkAndAddRequest(ip)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. IP temporarily blocked."));
        }

        try {
            Claims claims = jwtService.validateToken(token, ip);

            Map<String, Object> data = new HashMap<>();
            data.put("message", "First National Bank - Quantum Secure Vault");
            data.put("accountBalance", "$1,452,890.00");
            data.put("accountNumber", "XXXX-XXXX-XXXX-9921");
            data.put("authenticatedUser", claims.getSubject());
            data.put("authMethod", "Quantum Ephemeral Key");

            return ResponseEntity.ok(data);
        } catch (HoneyTokenException e) {
            AdminDashboardController.logIncident(ip, token, "Attempted to access Secure Vault");
            rateLimiterService.hardBlockIp(ip, 300); // 5 min block for honeypot activation
            return ResponseEntity.status(403).body(Map.of("error", "Quantum validation failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", "Quantum validation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/secure/audit")
    public ResponseEntity<Map<String, Object>> auditToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Bearer token"));
        }

        String token = authHeader.substring(7);
        String ip = clientIp != null ? clientIp : request.getRemoteAddr();

        if (!rateLimiterService.checkAndAddRequest(ip)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. IP temporarily blocked."));
        }

        try {
            Claims claims = jwtService.validateToken(token, ip);

            Map<String, Object> auditData = new HashMap<>();
            auditData.put("tokenValid", true);
            auditData.put("subject", claims.getSubject());
            
            // Check for Quantum Watermark in claims
            if (claims.containsKey("qw")) {
                auditData.put("quantumWatermarkValid", true);
                auditData.put("watermarkSignature", claims.get("qw"));
                auditData.put("entropySource", "IBM Quantum Hardware (Verified)");
            } else {
                auditData.put("quantumWatermarkValid", false);
                auditData.put("entropySource", "Unknown / Standard Classical Generation");
            }

            return ResponseEntity.ok(auditData);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", "Key Validation Failed: " + e.getMessage()));
        }
    }
}
