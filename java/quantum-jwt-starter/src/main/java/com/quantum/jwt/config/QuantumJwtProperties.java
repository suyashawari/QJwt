package com.quantum.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "quantum.jwt")
public class QuantumJwtProperties {

    /**
     * Token expiration time (default: 15 minutes).
     */
    private Duration tokenTtl = Duration.ofMinutes(15);

    /**
     * Token issuer claim (iss).
     */
    private String issuer = "quantum-auth-service";

    /**
     * Enable IP Binding (Tokens are invalid if used from a different IP).
     */
    private boolean ipBindingEnabled = true;

    /**
     * Enable Quantum Watermarking (Adds a subtle identifier to the payload).
     */
    private boolean watermarkEnabled = true;

    /**
     * Enable Redis dependency (default: true). Set to false to run completely in-memory.
     */
    private boolean redisEnabled = true;

    /**
     * Optional IBM Quantum API Token (Used when redis-enabled is false).
     */
    private String ibmQuantumToken;

    /**
     * Optional backend name (Used when redis-enabled is false).
     */
    private String ibmBackendName = "ibmq_qasm_simulator";

    /**
     * Secret used to generate the quantum watermark.
     */
    private String watermarkSecret = "quantum-default-watermark-secret";

    // Getters and Setters
    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isIpBindingEnabled() {
        return ipBindingEnabled;
    }

    public void setIpBindingEnabled(boolean ipBindingEnabled) {
        this.ipBindingEnabled = ipBindingEnabled;
    }

    public boolean isWatermarkEnabled() {
        return watermarkEnabled;
    }

    public void setWatermarkEnabled(boolean watermarkEnabled) {
        this.watermarkEnabled = watermarkEnabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public String getIbmQuantumToken() {
        return ibmQuantumToken;
    }

    public void setIbmQuantumToken(String ibmQuantumToken) {
        this.ibmQuantumToken = ibmQuantumToken;
    }

    public String getIbmBackendName() {
        return ibmBackendName;
    }

    public void setIbmBackendName(String ibmBackendName) {
        this.ibmBackendName = ibmBackendName;
    }

    public String getWatermarkSecret() {
        return watermarkSecret;
    }

    public void setWatermarkSecret(String watermarkSecret) {
        this.watermarkSecret = watermarkSecret;
    }
}