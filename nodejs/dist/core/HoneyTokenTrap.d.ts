/**
 * HoneyTokenTrap — checks if a key-id belongs to the `poison_keys` set.
 *
 * If a token's kid is found in the poison set, it means the key has been
 * planted as a canary and someone is attempting to use a stolen token.
 */
import Redis from "ioredis";
export declare class HoneyTokenError extends Error {
    constructor(message: string);
}
export declare class HoneyTokenTrap {
    private redis;
    constructor(redis: Redis);
    /**
     * Check if the given kid is a honey/poison token.
     * @throws HoneyTokenError if it is.
     */
    check(kid: string): Promise<void>;
}
//# sourceMappingURL=HoneyTokenTrap.d.ts.map