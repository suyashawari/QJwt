package com.quantum.auth.controller;

import com.quantum.jwt.core.EntropyPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MetricsController {

    private final EntropyPool entropyPool;
    private final StringRedisTemplate redisTemplate;

    public MetricsController(EntropyPool entropyPool, @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.entropyPool = entropyPool;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("poolSize", entropyPool.getPoolSize());
        metrics.put("maxPoolSize", 1000);

        if (redisTemplate != null) {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries("miner_stats");
            metrics.put("keysFromQuantum", parseLong(stats.get("keys_from_quantum"), 0L));
            metrics.put("keysFromClassical", parseLong(stats.get("keys_from_classical"), 0L));
            metrics.put("tokensGenerated", parseLong(stats.get("tokens_generated"), 0L));
            metrics.put("tokensValidated", parseLong(stats.get("tokens_validated"), 0L));
            metrics.put("tokensFailed", parseLong(stats.get("tokens_failed"), 0L));

            List<String> rawAlerts = redisTemplate.opsForList().range("security_alerts", 0, 9);
            List<Map<String, Object>> alerts = new ArrayList<>();
            if (rawAlerts != null) {
                for (int i = 0; i < rawAlerts.size(); i++) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("id", i);
                    alert.put("level", "WARNING");
                    alert.put("message", rawAlerts.get(i));
                    alert.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    alerts.add(alert);
                }
            }
            metrics.put("alerts", alerts);
        } else {
            metrics.put("keysFromQuantum", "N/A (Local)");
            metrics.put("keysFromClassical", "N/A (Local)");
            metrics.put("tokensGenerated", "N/A (Local)");
            metrics.put("tokensValidated", "N/A (Local)");
            metrics.put("tokensFailed", "N/A (Local)");
            metrics.put("alerts", new ArrayList<>());
        }
        
        metrics.put("minerUptime", redisTemplate != null ? "Live" : "Local Simulator Node");
        metrics.put("lastMineTime", redisTemplate != null ? "Active" : "Active (In-memory)");

        return ResponseEntity.ok(metrics);
    }

    private Long parseLong(Object val, Long defaultVal) {
        if (val == null)
            return defaultVal;
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @PostMapping("/admin/simulate-redis-crash")
    public ResponseEntity<Map<String, String>> simulateRedisCrash(@RequestParam boolean crash) {
        com.quantum.jwt.core.RedisEntropyPool.SIMULATE_CRASH = crash;
        String msg = crash ? "REDIS OFFLINE: Local Classical Fallback Active" : "REDIS ONLINE: Restoring normal operation";
        return ResponseEntity.ok(Map.of("status", msg));
    }
}
