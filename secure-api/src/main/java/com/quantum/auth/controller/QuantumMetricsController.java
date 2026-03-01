package com.quantum.auth.controller;

import com.quantum.jwt.QuantumMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/quantum-metrics")
public class QuantumMetricsController {

    private final QuantumMetrics quantumMetrics;

    public QuantumMetricsController(QuantumMetrics quantumMetrics) {
        this.quantumMetrics = quantumMetrics;
    }

    @GetMapping
    public Map<String, Object> metrics() {
        return Map.of(
            "tokensIssued", quantumMetrics.getTokensIssued(),
            "fallbackCount", quantumMetrics.getFallbackCount(),
            "adaptiveTriggers", quantumMetrics.getAdaptiveTriggers()
        );
    }
}