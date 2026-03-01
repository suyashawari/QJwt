package com.quantum.jwt;

public class EntropyExhaustedException extends RuntimeException {
    public EntropyExhaustedException(String message) {
        super(message);
    }
}
