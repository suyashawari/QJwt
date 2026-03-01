package com.quantum.auth.dto;

/**
 * Authentication response DTO.
 */
public class AuthResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private String keyId;
    private String keySource;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, Long expiresIn, String keyId, String keySource) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.keyId = keyId;
        this.keySource = keySource;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType;
        private Long expiresIn;
        private String keyId;
        private String keySource;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public Builder keySource(String keySource) {
            this.keySource = keySource;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, expiresIn, keyId, keySource);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySource() {
        return keySource;
    }

    public void setKeySource(String keySource) {
        this.keySource = keySource;
    }
}
