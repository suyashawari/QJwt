package com.quantum.auth.dto;

/**
 * System status response DTO.
 */
public class StatusResponse {

    private Long poolSize;
    private String status;
    private Long totalKeysGenerated;
    private Long quantumKeys;
    private Long classicalKeys;
    private Long honeyTokensInjected;
    private String lastGeneration;
    private Boolean healthy;

    public StatusResponse() {
    }

    public StatusResponse(Long poolSize, String status, Long totalKeysGenerated, Long quantumKeys,
            Long classicalKeys, Long honeyTokensInjected, String lastGeneration, Boolean healthy) {
        this.poolSize = poolSize;
        this.status = status;
        this.totalKeysGenerated = totalKeysGenerated;
        this.quantumKeys = quantumKeys;
        this.classicalKeys = classicalKeys;
        this.honeyTokensInjected = honeyTokensInjected;
        this.lastGeneration = lastGeneration;
        this.healthy = healthy;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long poolSize;
        private String status;
        private Long totalKeysGenerated;
        private Long quantumKeys;
        private Long classicalKeys;
        private Long honeyTokensInjected;
        private String lastGeneration;
        private Boolean healthy;

        public Builder poolSize(Long poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder totalKeysGenerated(Long totalKeysGenerated) {
            this.totalKeysGenerated = totalKeysGenerated;
            return this;
        }

        public Builder quantumKeys(Long quantumKeys) {
            this.quantumKeys = quantumKeys;
            return this;
        }

        public Builder classicalKeys(Long classicalKeys) {
            this.classicalKeys = classicalKeys;
            return this;
        }

        public Builder honeyTokensInjected(Long honeyTokensInjected) {
            this.honeyTokensInjected = honeyTokensInjected;
            return this;
        }

        public Builder lastGeneration(String lastGeneration) {
            this.lastGeneration = lastGeneration;
            return this;
        }

        public Builder healthy(Boolean healthy) {
            this.healthy = healthy;
            return this;
        }

        public StatusResponse build() {
            return new StatusResponse(poolSize, status, totalKeysGenerated, quantumKeys,
                    classicalKeys, honeyTokensInjected, lastGeneration, healthy);
        }
    }

    // Getters and Setters
    public Long getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Long poolSize) {
        this.poolSize = poolSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTotalKeysGenerated() {
        return totalKeysGenerated;
    }

    public void setTotalKeysGenerated(Long totalKeysGenerated) {
        this.totalKeysGenerated = totalKeysGenerated;
    }

    public Long getQuantumKeys() {
        return quantumKeys;
    }

    public void setQuantumKeys(Long quantumKeys) {
        this.quantumKeys = quantumKeys;
    }

    public Long getClassicalKeys() {
        return classicalKeys;
    }

    public void setClassicalKeys(Long classicalKeys) {
        this.classicalKeys = classicalKeys;
    }

    public Long getHoneyTokensInjected() {
        return honeyTokensInjected;
    }

    public void setHoneyTokensInjected(Long honeyTokensInjected) {
        this.honeyTokensInjected = honeyTokensInjected;
    }

    public String getLastGeneration() {
        return lastGeneration;
    }

    public void setLastGeneration(String lastGeneration) {
        this.lastGeneration = lastGeneration;
    }

    public Boolean getHealthy() {
        return healthy;
    }

    public void setHealthy(Boolean healthy) {
        this.healthy = healthy;
    }
}
