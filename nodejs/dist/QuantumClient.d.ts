/**
 * QuantumClient — primary public API for the Quantum JWT Node.js client.
 *
 * Usage:
 * ```ts
 * import { QuantumClient } from "@quantum-auth/jwt-client";
 *
 * const client = new QuantumClient({ redisHost: "localhost" });
 * const token  = await client.generateToken("user_123", {}, "10.0.0.1");
 * const claims = await client.validateToken(token, "10.0.0.1");
 * await client.disconnect();
 * ```
 */
import Redis from "ioredis";
import { QuantumJwtConfig } from "./types/config";
import { QuantumJwtPayload } from "./types/payload";
export declare class QuantumClient {
    private redis;
    private keyRotator;
    private tokenManager;
    private honeyTokenTrap;
    private config;
    constructor(overrides?: Partial<QuantumJwtConfig>);
    /**
     * Connect to Redis (call once before generating/validating tokens).
     */
    connect(): Promise<void>;
    /**
     * Generate a signed JWT using a quantum-derived key from the entropy pool.
     *
     * @param subject  - user identifier (sub claim)
     * @param claims   - extra claims to embed
     * @param clientIp - client IP for IP binding
     * @returns signed JWT string
     * @throws EntropyExhaustedError if the pool is empty
     */
    generateToken(subject: string, claims?: Record<string, unknown>, clientIp?: string): Promise<string>;
    /**
     * Validate a JWT.
     *
     * @param token    - the JWT string
     * @param clientIp - client IP for IP binding check
     * @returns decoded payload
     * @throws InvalidTokenError | HoneyTokenError
     */
    validateToken(token: string, clientIp?: string): Promise<QuantumJwtPayload>;
    /**
     * Get the current entropy pool size.
     */
    getPoolSize(): Promise<number>;
    /**
     * Gracefully disconnect from Redis.
     */
    disconnect(): Promise<void>;
    /**
     * Expose the underlying Redis instance (for testing / advanced usage).
     */
    getRedis(): Redis;
}
//# sourceMappingURL=QuantumClient.d.ts.map