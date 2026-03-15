"use strict";
/**
 * HoneyTokenTrap — checks if a key-id belongs to the `poison_keys` set.
 *
 * If a token's kid is found in the poison set, it means the key has been
 * planted as a canary and someone is attempting to use a stolen token.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.HoneyTokenTrap = exports.HoneyTokenError = void 0;
const winston_1 = require("winston");
const logger = (0, winston_1.createLogger)({
    level: "info",
    format: winston_1.format.combine(winston_1.format.timestamp(), winston_1.format.simple()),
    transports: [new winston_1.transports.Console()],
});
const POISON_KEYS_SET = "poison_keys";
class HoneyTokenError extends Error {
    constructor(message) {
        super(message);
        this.name = "HoneyTokenError";
    }
}
exports.HoneyTokenError = HoneyTokenError;
class HoneyTokenTrap {
    redis;
    constructor(redis) {
        this.redis = redis;
    }
    /**
     * Check if the given kid is a honey/poison token.
     * @throws HoneyTokenError if it is.
     */
    async check(kid) {
        const isPoison = await this.redis.sismember(POISON_KEYS_SET, kid);
        if (isPoison) {
            logger.error(`🍯 Honey token triggered! kid: ${kid}`);
            throw new HoneyTokenError("Honey token detected – possible breach");
        }
    }
}
exports.HoneyTokenTrap = HoneyTokenTrap;
//# sourceMappingURL=HoneyTokenTrap.js.map