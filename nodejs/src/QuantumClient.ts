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
import { QuantumJwtConfig, DEFAULT_CONFIG } from "./types/config";
import { QuantumJwtPayload } from "./types/payload";
import { KeyRotator } from "./core/KeyRotator";
import { TokenManager, EntropyExhaustedError, InvalidTokenError } from "./core/TokenManager";
import { HoneyTokenTrap, HoneyTokenError } from "./core/HoneyTokenTrap";
import { createLogger, format, transports } from "winston";

const logger = createLogger({
    level: "info",
    format: format.combine(format.timestamp(), format.simple()),
    transports: [new transports.Console()],
});

export class QuantumClient {
    private redis: Redis;
    private keyRotator: KeyRotator;
    private tokenManager: TokenManager;
    private honeyTokenTrap: HoneyTokenTrap;
    private config: QuantumJwtConfig;

    constructor(overrides: Partial<QuantumJwtConfig> = {}) {
        this.config = { ...DEFAULT_CONFIG, ...overrides };

        this.redis = new Redis({
            host: this.config.redisHost,
            port: this.config.redisPort,
            password: this.config.redisPassword || undefined,
            db: this.config.redisDb,
            lazyConnect: true,
        });

        this.keyRotator = new KeyRotator(this.redis);
        this.tokenManager = new TokenManager(this.config);
        this.honeyTokenTrap = new HoneyTokenTrap(this.redis);
    }

    /**
     * Connect to Redis (call once before generating/validating tokens).
     */
    async connect(): Promise<void> {
        await this.redis.connect();
        logger.info("Connected to Redis");
    }

    /**
     * Generate a signed JWT using a quantum-derived key from the entropy pool.
     *
     * @param subject  - user identifier (sub claim)
     * @param claims   - extra claims to embed
     * @param clientIp - client IP for IP binding
     * @returns signed JWT string
     * @throws EntropyExhaustedError if the pool is empty
     */
    async generateToken(
        subject: string,
        claims: Record<string, unknown> = {},
        clientIp?: string
    ): Promise<string> {
        // 1. Pop a key from the entropy pool
        const keyBuffer = await this.keyRotator.popKey();
        if (keyBuffer === null) {
            throw new EntropyExhaustedError("No entropy available in pool");
        }

        // 2. Store the key under a new kid with TTL
        const keyData = await this.keyRotator.storeKey(
            keyBuffer,
            this.config.tokenTtlSeconds
        );

        // 3. Sign the token
        const token = this.tokenManager.sign(
            subject,
            claims,
            keyData.kid,
            keyData.keyBuffer,
            clientIp
        );

        logger.debug(`Token generated for subject: ${subject}, kid: ${keyData.kid}`);
        return token;
    }

    /**
     * Validate a JWT.
     *
     * @param token    - the JWT string
     * @param clientIp - client IP for IP binding check
     * @returns decoded payload
     * @throws InvalidTokenError | HoneyTokenError
     */
    async validateToken(
        token: string,
        clientIp?: string
    ): Promise<QuantumJwtPayload> {
        // 1. Extract kid from header (without verifying)
        const kid = this.tokenManager.extractKid(token);
        if (!kid) {
            throw new InvalidTokenError("Missing kid in token header");
        }

        // 2. Check honey token
        await this.honeyTokenTrap.check(kid);

        // 3. Retrieve key from Redis
        const keyBuffer = await this.keyRotator.getKey(kid);
        if (keyBuffer === null) {
            throw new InvalidTokenError("Key expired or revoked");
        }

        // 4. Verify signature, expiry, IP, watermark
        return this.tokenManager.verify(token, keyBuffer, clientIp);
    }

    /**
     * Get the current entropy pool size.
     */
    async getPoolSize(): Promise<number> {
        return this.keyRotator.getPoolSize();
    }

    /**
     * Gracefully disconnect from Redis.
     */
    async disconnect(): Promise<void> {
        await this.redis.quit();
        logger.info("Disconnected from Redis");
    }

    /**
     * Expose the underlying Redis instance (for testing / advanced usage).
     */
    getRedis(): Redis {
        return this.redis;
    }
}
