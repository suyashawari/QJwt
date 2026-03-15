package com.quantum.jwt.exception;

public class EntropyExhaustedException extends RuntimeException {
    public EntropyExhaustedException(String message) {
        super(message);
    }
}