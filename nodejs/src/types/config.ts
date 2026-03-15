/**
 * Configuration interface for the Quantum JWT client.
 */
export interface QuantumJwtConfig {
    /** Redis host (default: "localhost") */
    redisHost: string;

    /** Redis port (default: 6379) */
    redisPort: number;

    /** Redis password (default: "") */
    redisPassword: string;

    /** Redis database index (default: 0) */
    redisDb: number;

    /** Token time-to-live in seconds (default: 900 = 15 min) */
    tokenTtlSeconds: number;

    /** Issuer claim for generated tokens */
    issuer: string;

    /** Enable IP binding validation (default: true) */
    ipBindingEnabled: boolean;

    /** HMAC secret for quantum watermarking (null = disabled) */
    watermarkSecret: string | null;
}

/** Sensible defaults — merge with user-supplied overrides. */
export const DEFAULT_CONFIG: QuantumJwtConfig = {
    redisHost: "localhost",
    redisPort: 6379,
    redisPassword: "",
    redisDb: 0,
    tokenTtlSeconds: 900,
    issuer: "quantum-auth",
    ipBindingEnabled: true,
    watermarkSecret: null,
};
