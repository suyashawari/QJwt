package com.quantum.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the quantum entropy pool is exhausted.
 * This indicates the Python miner needs to generate more keys.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EntropyExhaustedException extends RuntimeException {
    
    public EntropyExhaustedException() {
        super("Quantum entropy pool exhausted. Please wait for the miner to replenish keys.");
    }
    
    public EntropyExhaustedException(String message) {
        super(message);
    }
}
