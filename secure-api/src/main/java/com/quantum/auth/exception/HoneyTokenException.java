package com.quantum.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a honey token (poison key) is detected.
 * This indicates a potential security breach.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class HoneyTokenException extends RuntimeException {

    private final String clientIp;

    public HoneyTokenException(String clientIp) {
        super("Security violation detected: Honey token triggered");
        this.clientIp = clientIp;
    }

    public String getClientIp() {
        return clientIp;
    }
}
