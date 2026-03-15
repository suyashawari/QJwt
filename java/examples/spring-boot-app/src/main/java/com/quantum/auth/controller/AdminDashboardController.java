package com.quantum.auth.controller;

import com.quantum.auth.service.RateLimiterService;
import com.quantum.jwt.config.QuantumJwtProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    private final RateLimiterService rateLimiterService;
    private final QuantumJwtProperties properties;

    // In-memory log of honey token trigger events
    public static final ConcurrentLinkedQueue<Map<String, String>> HONEY_INCIDENTS = new ConcurrentLinkedQueue<>();

    public AdminDashboardController(RateLimiterService rateLimiterService, QuantumJwtProperties properties) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
    }

    @GetMapping("/traffic")
    public ResponseEntity<Map<String, Map<String, Object>>> getTrafficZones() {
        return ResponseEntity.ok(rateLimiterService.getTrafficZones());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<Map<String, String>>> getHoneyIncidents() {
        return ResponseEntity.ok(new ArrayList<>(HONEY_INCIDENTS));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "redisEnabled", properties.isRedisEnabled(),
            "quantumToken", "********" + properties.getIbmQuantumToken().substring(Math.max(0, properties.getIbmQuantumToken().length() - 4)),
            "apiBackendName", properties.getIbmBackendName()
        ));
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> payload) {
        if (payload.containsKey("quantumToken") && !payload.get("quantumToken").contains("***")) {
            properties.setIbmQuantumToken(payload.get("quantumToken"));
        }
        if (payload.containsKey("apiBackendName")) {
            properties.setIbmBackendName(payload.get("apiBackendName"));
        }
        if (payload.containsKey("redisEnabled")) {
            properties.setRedisEnabled(Boolean.parseBoolean(payload.get("redisEnabled")));
        }
        return ResponseEntity.ok(Map.of("message", "Configuration updated successfully."));
    }

    public static void logIncident(String ip, String interceptedToken, String action) {
        HONEY_INCIDENTS.offer(Map.of(
            "time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            "ip", ip,
            "tokenPreview", interceptedToken.substring(0, Math.min(20, interceptedToken.length())) + "...",
            "action", action
        ));
        // Keep list bounded to last 50 incidents
        if (HONEY_INCIDENTS.size() > 50) {
            HONEY_INCIDENTS.poll();
        }
    }
}
