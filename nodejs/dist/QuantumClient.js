"use strict";
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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.QuantumClient = void 0;
const ioredis_1 = __importDefault(require("ioredis"));
const config_1 = require("./types/config");
const KeyRotator_1 = require("./core/KeyRotator");
const TokenManager_1 = require("./core/TokenManager");
const HoneyTokenTrap_1 = require("./core/HoneyTokenTrap");
const winston_1 = require("winston");
const logger = (0, winston_1.createLogger)({
    level: "info",
    format: winston_1.format.combine(winston_1.format.timestamp(), winston_1.format.simple()),
    transports: [new winston_1.transports.Console()],
});
class QuantumClient {
    redis;
    keyRotator;
    tokenManager;
    honeyTokenTrap;
    config;
    constructor(overrides = {}) {
        this.config = { ...config_1.DEFAULT_CONFIG, ...overrides };
        this.redis = new ioredis_1.default({
            host: this.config.redisHost,
            port: this.config.redisPort,
            password: this.config.redisPassword || undefined,
            db: this.config.redisDb,
            lazyConnect: true,
        });
        this.keyRotator = new KeyRotator_1.KeyRotator(this.redis);
        this.tokenManager = new TokenManager_1.TokenManager(this.config);
        this.honeyTokenTrap = new HoneyTokenTrap_1.HoneyTokenTrap(this.redis);
    }
    /**
     * Connect to Redis (call once before generating/validating tokens).
     */
    async connect() {
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
    async generateToken(subject, claims = {}, clientIp) {
        // 1. Pop a key from the entropy pool
        const keyBuffer = await this.keyRotator.popKey();
        if (keyBuffer === null) {
            throw new TokenManager_1.EntropyExhaustedError("No entropy available in pool");
        }
        // 2. Store the key under a new kid with TTL
        const keyData = await this.keyRotator.storeKey(keyBuffer, this.config.tokenTtlSeconds);
        // 3. Sign the token
        const token = this.tokenManager.sign(subject, claims, keyData.kid, keyData.keyBuffer, clientIp);
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
    async validateToken(token, clientIp) {
        // 1. Extract kid from header (without verifying)
        const kid = this.tokenManager.extractKid(token);
        if (!kid) {
            throw new TokenManager_1.InvalidTokenError("Missing kid in token header");
        }
        // 2. Check honey token
        await this.honeyTokenTrap.check(kid);
        // 3. Retrieve key from Redis
        const keyBuffer = await this.keyRotator.getKey(kid);
        if (keyBuffer === null) {
            throw new TokenManager_1.InvalidTokenError("Key expired or revoked");
        }
        // 4. Verify signature, expiry, IP, watermark
        return this.tokenManager.verify(token, keyBuffer, clientIp);
    }
    /**
     * Get the current entropy pool size.
     */
    async getPoolSize() {
        return this.keyRotator.getPoolSize();
    }
    /**
     * Gracefully disconnect from Redis.
     */
    async disconnect() {
        await this.redis.quit();
        logger.info("Disconnected from Redis");
    }
    /**
     * Expose the underlying Redis instance (for testing / advanced usage).
     */
    getRedis() {
        return this.redis;
    }
}
exports.QuantumClient = QuantumClient;
//# sourceMappingURL=QuantumClient.js.map