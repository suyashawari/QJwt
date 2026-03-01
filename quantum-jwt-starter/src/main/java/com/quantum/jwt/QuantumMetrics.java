package com.quantum.jwt;

public class QuantumMetrics {
    private final QuantumJwtService jwtService;
    private final KeyManager keyManager;

    public QuantumMetrics(QuantumJwtService jwtService, KeyManager keyManager) {
        this.jwtService = jwtService;
        this.keyManager = keyManager;
    }

    public long getTokensIssued() {
        return jwtService.getTokenCount();
    }

    public long getFallbackCount() {
        return keyManager.getFallbackCount();
    }

    public long getAdaptiveTriggers() {
        return jwtService.getAdaptiveTriggerCount();
    }
}
