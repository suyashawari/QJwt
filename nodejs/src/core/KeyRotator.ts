/**
 * KeyRotator — manages the Redis entropy pool and per-token key storage.
 *
 * - Pops keys from `entropy:pool` (LPOP).
 * - Stores each key under `key:{kid}` with a TTL matching token expiry.
 * - Retrieves keys by `kid` for validation.
 */

import Redis from "ioredis";
import { v4 as uuidv4 } from "uuid";
import { createLogger, format, transports } from "winston";

const logger = createLogger({
    level: "info",
    format: format.combine(format.timestamp(), format.simple()),
    transports: [new transports.Console()],
});

const ENTROPY_POOL = "entropy:pool";
const KEY_PREFIX = "key:";

export interface KeyData {
    kid: string;
    keyBuffer: Buffer;
}

export class KeyRotator {
    private redis: Redis;

    constructor(redis: Redis) {
        this.redis = redis;
    }

    /**
     * Pop one base64-encoded key from the entropy pool.
     * @returns raw key bytes, or null if the pool is empty.
     */
    async popKey(): Promise<Buffer | null> {
        const keyB64 = await this.redis.lpop(ENTROPY_POOL);
        if (keyB64 === null) {
            return null;
        }
        // URL-safe base64 → standard base64, then decode
        const padded = keyB64.replace(/-/g, "+").replace(/_/g, "/");
        return Buffer.from(padded, "base64");
    }

    /**
     * Store a signing key in Redis under a new UUID key-id, with the given TTL.
     * @returns the generated kid and key buffer
     */
    async storeKey(keyBuffer: Buffer, ttlSeconds: number): Promise<KeyData> {
        const kid = uuidv4();
        const keyB64 = keyBuffer
            .toString("base64")
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");
        await this.redis.setex(`${KEY_PREFIX}${kid}`, ttlSeconds, keyB64);
        logger.debug(`Stored key with kid: ${kid}`);
        return { kid, keyBuffer };
    }

    /**
     * Retrieve a key by its kid.
     * @returns raw key bytes, or null if expired / not found.
     */
    async getKey(kid: string): Promise<Buffer | null> {
        const keyB64 = await this.redis.get(`${KEY_PREFIX}${kid}`);
        if (keyB64 === null) {
            logger.debug(`Key not found or expired: ${kid}`);
            return null;
        }
        const padded = keyB64.replace(/-/g, "+").replace(/_/g, "/");
        return Buffer.from(padded, "base64");
    }

    /**
     * Returns the current number of keys in the entropy pool.
     */
    async getPoolSize(): Promise<number> {
        return await this.redis.llen(ENTROPY_POOL);
    }
}
