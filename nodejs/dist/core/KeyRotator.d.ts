/**
 * KeyRotator — manages the Redis entropy pool and per-token key storage.
 *
 * - Pops keys from `entropy:pool` (LPOP).
 * - Stores each key under `key:{kid}` with a TTL matching token expiry.
 * - Retrieves keys by `kid` for validation.
 */
import Redis from "ioredis";
export interface KeyData {
    kid: string;
    keyBuffer: Buffer;
}
export declare class KeyRotator {
    private redis;
    constructor(redis: Redis);
    /**
     * Pop one base64-encoded key from the entropy pool.
     * @returns raw key bytes, or null if the pool is empty.
     */
    popKey(): Promise<Buffer | null>;
    /**
     * Store a signing key in Redis under a new UUID key-id, with the given TTL.
     * @returns the generated kid and key buffer
     */
    storeKey(keyBuffer: Buffer, ttlSeconds: number): Promise<KeyData>;
    /**
     * Retrieve a key by its kid.
     * @returns raw key bytes, or null if expired / not found.
     */
    getKey(kid: string): Promise<Buffer | null>;
    /**
     * Returns the current number of keys in the entropy pool.
     */
    getPoolSize(): Promise<number>;
}
//# sourceMappingURL=KeyRotator.d.ts.map