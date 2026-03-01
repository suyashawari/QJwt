// package com.quantum.auth.controller;

// import com.quantum.auth.dto.AuthResponse;
// import com.quantum.auth.dto.LoginRequest;
// import com.quantum.auth.dto.StatusResponse;
// import com.quantum.auth.service.JwtService;
// import com.quantum.auth.service.QuantumService;
// import io.jsonwebtoken.Claims;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.validation.Valid;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.Map;

// /**
//  * Authentication Controller
//  * 
//  * Provides endpoints for quantum-seeded JWT authentication.
//  */
// @RestController
// @RequestMapping("/api")
// @CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
// public class AuthController {

//     private static final Logger log = LoggerFactory.getLogger(AuthController.class);

//     private final JwtService jwtService;
//     private final QuantumService quantumService;

//     public AuthController(JwtService jwtService, QuantumService quantumService) {
//         this.jwtService = jwtService;
//         this.quantumService = quantumService;
//     }

//     /**
//      * Login endpoint - validates user and issues quantum-signed JWT.
//      */
//     @PostMapping("/auth/login")
//     public ResponseEntity<?> login(
//             @Valid @RequestBody LoginRequest request,
//             HttpServletRequest httpRequest) {
//         String clientIp = getClientIp(httpRequest);

//         // Check if IP is blocked
//         if (quantumService.isIpBlocked(clientIp)) {
//             log.warn("Blocked IP attempted login: {}", clientIp);
//             return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                     .body(Map.of(
//                             "error", "Access denied",
//                             "message", "Your IP has been blocked due to security violations"));
//         }

//         // Simple user validation (replace with real authentication)
//         if (!validateUser(request.getUsername(), request.getPassword())) {
//             log.warn("Failed login attempt for user: {}", request.getUsername());
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                     .body(Map.of("error", "Invalid credentials"));
//         }

//         // Generate quantum-signed token
//         int riskScore = request.getRiskScore() != null ? request.getRiskScore() : 0;
//         Map<String, Object> tokenData = jwtService.generateToken(request.getUsername(), riskScore);

//         AuthResponse response = AuthResponse.builder()
//                 .token((String) tokenData.get("token"))
//                 .tokenType("Bearer")
//                 .expiresIn((Long) tokenData.get("expiresIn"))
//                 .keyId((String) tokenData.get("kid"))
//                 .keySource((String) tokenData.get("keySource"))
//                 .build();

//         log.info("Successful login for user: {}, keySource: {}",
//                 request.getUsername(), response.getKeySource());

//         return ResponseEntity.ok(response);
//     }

//     /**
//      * Verify token endpoint.
//      */
//     @GetMapping("/auth/verify")
//     public ResponseEntity<?> verifyToken(
//             @RequestHeader("Authorization") String authHeader,
//             HttpServletRequest httpRequest) {
//         String clientIp = getClientIp(httpRequest);

//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                     .body(Map.of("error", "Missing or invalid Authorization header"));
//         }

//         String token = authHeader.substring(7);
//         Claims claims = jwtService.verifyToken(token, clientIp);

//         if (claims == null) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                     .body(Map.of(
//                             "valid", false,
//                             "error", "Invalid or expired token"));
//         }

//         return ResponseEntity.ok(Map.of(
//                 "valid", true,
//                 "subject", claims.getSubject(),
//                 "issuedAt", claims.getIssuedAt(),
//                 "expiration", claims.getExpiration(),
//                 "riskScore", claims.get("risk_score"),
//                 "keySource", claims.get("key_source")));
//     }

//     /**
//      * System status endpoint - for dashboard.
//      */
//     @GetMapping("/status")
//     public ResponseEntity<StatusResponse> getStatus() {
//         Long poolSize = quantumService.getPoolSize();
//         Map<Object, Object> stats = quantumService.getMinerStats();

//         String status;
//         if (poolSize > 500) {
//             status = "HEALTHY";
//         } else if (poolSize > 100) {
//             status = "LOW";
//         } else if (poolSize > 0) {
//             status = "CRITICAL";
//         } else {
//             status = "EXHAUSTED";
//         }

//         StatusResponse response = StatusResponse.builder()
//                 .poolSize(poolSize)
//                 .status(status)
//                 .totalKeysGenerated(parseLong(stats.get("total_keys_generated")))
//                 .quantumKeys(parseLong(stats.get("keys_from_quantum")))
//                 .classicalKeys(parseLong(stats.get("keys_from_classical")))
//                 .honeyTokensInjected(parseLong(stats.get("honey_tokens_injected")))
//                 .lastGeneration((String) stats.get("last_generation"))
//                 .healthy(poolSize > 100)
//                 .build();

//         return ResponseEntity.ok(response);
//     }

//     /**
//      * Health check endpoint.
//      */
//     @GetMapping("/health")
//     public ResponseEntity<Map<String, Object>> health() {
//         try {
//             Long poolSize = quantumService.getPoolSize();
//             return ResponseEntity.ok(Map.of(
//                     "status", "UP",
//                     "poolSize", poolSize,
//                     "timestamp", System.currentTimeMillis()));
//         } catch (Exception e) {
//             return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                     .body(Map.of(
//                             "status", "DOWN",
//                             "error", e.getMessage()));
//         }
//     }

//     // Simple user validation - replace with real authentication
//     private boolean validateUser(String username, String password) {
//         // Demo: Accept any non-empty credentials
//         // In production, integrate with your user database
//         return username != null && !username.isEmpty()
//                 && password != null && password.length() >= 4;
//     }

//     private String getClientIp(HttpServletRequest request) {
//         String xForwardedFor = request.getHeader("X-Forwarded-For");
//         if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
//             return xForwardedFor.split(",")[0].trim();
//         }
//         return request.getRemoteAddr();
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


package com.quantum.auth.controller;

import com.quantum.jwt.QuantumJwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final QuantumJwtService jwtService;

    public AuthController(QuantumJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, HttpServletRequest request) {
        return jwtService.generateToken(username, request.getRemoteAddr());
    }

    @GetMapping("/hello")
    public String hello(@RequestHeader("Authorization") String token, HttpServletRequest request) {
        token = token.replace("Bearer ", "");
        String user = jwtService.validateToken(token, request.getRemoteAddr());
        return "Hello " + user;
    }
}
