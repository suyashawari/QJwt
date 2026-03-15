/**
 * Honey token detection tests.
 *
 * Uses ioredis-mock.
 */

import RedisMock from "ioredis-mock";
import crypto from "crypto";
import { KeyRotator } from "../src/core/KeyRotator";
import { TokenManager } from "../src/core/TokenManager";
import { HoneyTokenTrap, HoneyTokenError } from "../src/core/HoneyTokenTrap";
import { DEFAULT_CONFIG } from "../src/types/config";

function seedPool(redis: any, count: number = 1): void {
    for (let i = 0; i < count; i++) {
        const keyB64 = crypto.randomBytes(32)
            .toString("base64")
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");
        redis.rpush("entropy:pool", keyB64);
    }
}

describe("HoneyTokenTrap", () => {
    let redis: any;
    let trap: HoneyTokenTrap;

    beforeEach(() => {
        redis = new RedisMock();
        trap = new HoneyTokenTrap(redis);
    });

    afterEach(() => {
        redis.disconnect();
    });

    test("does not throw for normal kid", async () => {
        await expect(trap.check("normal-kid")).resolves.toBeUndefined();
    });

    test("throws HoneyTokenError for poison kid", async () => {
        await redis.sadd("poison_keys", "poisoned-kid");
        await expect(trap.check("poisoned-kid")).rejects.toThrow(HoneyTokenError);
    });

    test("full flow: generate token → poison kid → validate throws", async () => {
        seedPool(redis, 1);
        const keyRotator = new KeyRotator(redis);
        const tokenManager = new TokenManager({
            ...DEFAULT_CONFIG,
            ipBindingEnabled: false,
        });

        // Generate
        const keyBuffer = await keyRotator.popKey();
        const keyData = await keyRotator.storeKey(keyBuffer!, 900);
        const token = tokenManager.sign("user_test", {}, keyData.kid, keyData.keyBuffer);

        // Mark as poison AFTER generation
        await redis.sadd("poison_keys", keyData.kid);

        // Should throw when checking
        await expect(trap.check(keyData.kid)).rejects.toThrow(HoneyTokenError);
    });
});
