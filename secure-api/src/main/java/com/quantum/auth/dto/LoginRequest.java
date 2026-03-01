package com.quantum.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO.
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Risk score from 0-100, used for adaptive TTL.
     * Higher scores result in shorter token lifetimes.
     */
    private Integer riskScore = 0;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password, Integer riskScore) {
        this.username = username;
        this.password = password;
        this.riskScore = riskScore;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }
}
