package com.quantum.auth.controller;

import com.quantum.jwt.core.QuantumJwtService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final QuantumJwtService jwtService;
    private final com.quantum.auth.service.TraditionalJwtService traditionalJwtService;
    private final com.quantum.auth.service.RateLimiterService rateLimiterService;
    
    // In-memory mock database for Demo purposes (Username -> Password)
    private final Map<String, String> userDatabase = new HashMap<>();

    public AuthController(QuantumJwtService jwtService, 
                          com.quantum.auth.service.TraditionalJwtService traditionalJwtService,
                          com.quantum.auth.service.RateLimiterService rateLimiterService) {
        this.jwtService = jwtService;
        this.traditionalJwtService = traditionalJwtService;
        this.rateLimiterService = rateLimiterService;
    }

    // --- SHARED SIGNUP ENDPOINT ---
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestParam String username, @RequestParam String password) {
        if (userDatabase.containsKey(username)) {
            return ResponseEntity.status(409).body(Map.of("error", "User already exists"));
        }
        userDatabase.put(username, password);
        return ResponseEntity.ok(Map.of("message", "User created successfully"));
    }

    // --- QUANTUM JWT ENDPOINTS ---

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(
            @RequestParam String username,
            @RequestParam String password,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {

        String ip = clientIp != null ? clientIp : request.getRemoteAddr();
        
        // 0. Check Rate Limiter
        if (!rateLimiterService.checkAndAddRequest(ip)) {
            return ResponseEntity.status(429).body(Map.of("error", "Rate limit exceeded. IP temporarily blocked."));
        }

        // 1. Verify Credentials
        if (!userDatabase.containsKey(username) || !userDatabase.get(username).equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        String token = jwtService.generateToken(username, new HashMap<>(), ip);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/honey-token")
    public ResponseEntity<Map<String, String>> generateHoneyToken(
            @RequestParam(defaultValue = "decoy_user") String username,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {

        String ip = clientIp != null ? clientIp : request.getRemoteAddr();
        String honeyToken = jwtService.generateHoneyToken(username, new HashMap<>(), ip);

        Map<String, String> response = new HashMap<>();
        response.put("token", honeyToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Bearer token"));
        }

        String token = authHeader.substring(7);
        String ip = clientIp != null ? clientIp : request.getRemoteAddr();

        try {
            Claims claims = jwtService.validateToken(token, ip);
            Map<String, Object> response = new HashMap<>();
            response.put("subject", claims.getSubject());
            response.put("issuer", claims.getIssuer());
            response.put("expires", claims.getExpiration());
            response.put("valid", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // --- TRADITIONAL JWT ENDPOINTS ---

    @PostMapping("/standard/token")
    public ResponseEntity<Map<String, String>> generateStandardToken(
            @RequestParam String username,
            @RequestParam String password) {
            
        // 1. Verify Credentials
        if (!userDatabase.containsKey(username) || !userDatabase.get(username).equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        String token = traditionalJwtService.generateToken(username);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/standard/validate")
    public ResponseEntity<Map<String, Object>> validateStandardToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Bearer token"));
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = traditionalJwtService.validateToken(token);
            Map<String, Object> response = new HashMap<>();
            response.put("subject", claims.getSubject());
            response.put("issuer", claims.getIssuer());
            response.put("expires", claims.getExpiration());
            response.put("valid", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}