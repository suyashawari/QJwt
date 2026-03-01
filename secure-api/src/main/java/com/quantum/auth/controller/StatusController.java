package com.quantum.auth.controller;

import com.quantum.jwt.QuantumMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class StatusController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private QuantumMetrics quantumMetrics;

    @GetMapping("/status")
    public Map<String, Object> status() {
        Long poolSize = redisTemplate.opsForList().size("entropy:pool");
        if (poolSize == null) poolSize = 0L;

        Map<String, Object> status = new HashMap<>();
        status.put("poolSize", poolSize);
        status.put("quantumKeys", quantumMetrics.getTokensIssued());      // adjust as needed
        status.put("classicalKeys", quantumMetrics.getFallbackCount());
        status.put("honeyTokens", 0); // placeholder
        status.put("securityStatus", "🛡️ Secure");
        return status;
    }
}