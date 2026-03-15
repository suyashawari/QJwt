/**
 * HoneyTokenTrap — checks if a key-id belongs to the `poison_keys` set.
 *
 * If a token's kid is found in the poison set, it means the key has been
 * planted as a canary and someone is attempting to use a stolen token.
 */

import Redis from "ioredis";
import { createLogger, format, transports } from "winston";

const logger = createLogger({
    level: "info",
    format: format.combine(format.timestamp(), format.simple()),
    transports: [new transports.Console()],
});

const POISON_KEYS_SET = "poison_keys";

export class HoneyTokenError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "HoneyTokenError";
    }
}

export class HoneyTokenTrap {
    private redis: Redis;

    constructor(redis: Redis) {
        this.redis = redis;
    }

    /**
     * Check if the given kid is a honey/poison token.
     * @throws HoneyTokenError if it is.
     */
    async check(kid: string): Promise<void> {
        const isPoison = await this.redis.sismember(POISON_KEYS_SET, kid);
        if (isPoison) {
            logger.error(`🍯 Honey token triggered! kid: ${kid}`);
            throw new HoneyTokenError("Honey token detected – possible breach");
        }
    }
}
