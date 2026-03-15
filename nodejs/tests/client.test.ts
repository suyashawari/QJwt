/**
 * Integration tests for QuantumClient.
 *
 * Uses ioredis-mock so no real Redis server is required.
 * Run with: npm test
 */

import RedisMock from "ioredis-mock";
import crypto from "crypto";
import jwt from "jsonwebtoken";
import { KeyRotator } from "../src/core/KeyRotator";
import { TokenManager, EntropyExhaustedError, InvalidTokenError } from "../src/core/TokenManager";
import { HoneyTokenTrap, HoneyTokenError } from "../src/core/HoneyTokenTrap";
import { QuantumJwtConfig, DEFAULT_CONFIG } from "../src/types/config";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeConfig(overrides: Partial<QuantumJwtConfig> = {}): QuantumJwtConfig {
    return { ...DEFAULT_CONFIG, ...overrides };
}

function seedPool(redis: any, count: number = 5): void {
    for (let i = 0; i < count; i++) {
        const keyB64 = crypto.randomBytes(32)
            .toString("base64")
            .replace(/\+/g, "-")
            .replace(/\//g, "_")
            .replace(/=+$/, "");
        redis.rpush("entropy:pool", keyB64);
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe("QuantumClient — Happy Path", () => {
    let redis: any;
    let keyRotator: KeyRotator;
    let tokenManager: TokenManager;

    beforeEach(() => {
        redis = new RedisMock();
        keyRotator = new KeyRotator(redis);
        tokenManager = new TokenManager(makeConfig({ ipBindingEnabled: false }));
    });

    afterEach(() => {
        redis.disconnect();
    });

    test("generate and validate a token", async () => {
        seedPool(redis, 1);

        const keyBuffer = await keyRotator.popKey();
        expect(keyBuffer).not.toBeNull();

        const keyData = await keyRotator.storeKey(keyBuffer!, 900);
        const token = tokenManager.sign("user_123", {}, keyData.kid, keyData.keyBuffer);

        // Retrieve key for validation
        const storedKey = await keyRotator.getKey(keyData.kid);
        expect(storedKey).not.toBeNull();

        const payload = tokenManager.verify(token, storedKey!);
        expect(payload.sub).toBe("user_123");
        expect(payload.iss).toBe("quantum-auth");
    });

    test("consecutive tokens have different kids", async () => {
        seedPool(redis, 2);

        const key1 = await keyRotator.popKey();
        const key2 = await keyRotator.popKey();
        const kd1 = await keyRotator.storeKey(key1!, 900);
        const kd2 = await keyRotator.storeKey(key2!, 900);

        expect(kd1.kid).not.toBe(kd2.kid);
    });

    test("extra claims are preserved", async () => {
        seedPool(redis, 1);

        const keyBuffer = await keyRotator.popKey();
        const keyData = await keyRotator.storeKey(keyBuffer!, 900);
        const token = tokenManager.sign(
            "admin",
            { role: "admin", org: "acme" },
            keyData.kid,
            keyData.keyBuffer
        );

        const storedKey = await keyRotator.getKey(keyData.kid);
        const payload = tokenManager.verify(token, storedKey!);
        expect(payload.role).toBe("admin");
        expect(payload.org).toBe("acme");
    });
});

describe("QuantumClient — IP Binding", () => {
    let redis: any;
    let keyRotator: KeyRotator;
    let tokenManager: TokenManager;

    beforeEach(() => {
        redis = new RedisMock();
        keyRotator = new KeyRotator(redis);
        tokenManager = new TokenManager(makeConfig({ ipBindingEnabled: true }));
    });

    afterEach(() => {
        redis.disconnect();
    });

    test("validate with matching IP succeeds", async () => {
        seedPool(redis, 1);

        const keyBuffer = await keyRotator.popKey();
        const keyData = await keyRotator.storeKey(keyBuffer!, 900);
        const token = tokenManager.sign("user_456", {}, keyData.kid, keyData.keyBuffer, "10.0.0.1");

        const storedKey = await keyRotator.getKey(keyData.kid);
        const payload = tokenManager.verify(token, storedKey!, "10.0.0.1");
        expect(payload.ip).toBe("10.0.0.1");
    });

    test("validate with mismatched IP throws", async () => {
        seedPool(redis, 1);

        const keyBuffer = await keyRotator.popKey();
        const keyData = await keyRotator.storeKey(keyBuffer!, 900);
        const token = tokenManager.sign("user_789", {}, keyData.kid, keyData.keyBuffer, "192.168.1.1");

        const storedKey = await keyRotator.getKey(keyData.kid);
        expect(() => tokenManager.verify(token, storedKey!, "10.0.0.99"))
            .toThrow(InvalidTokenError);
    });
});

describe("QuantumClient — Entropy Exhaustion", () => {
    test("empty pool returns null from popKey", async () => {
        const redis = new RedisMock();
        const keyRotator = new KeyRotator(redis);
        const keyBuffer = await keyRotator.popKey();
        expect(keyBuffer).toBeNull();
        redis.disconnect();
    });
});

describe("QuantumClient — Pool Size", () => {
    test("getPoolSize returns correct count", async () => {
        const redis = new RedisMock();
        const keyRotator = new KeyRotator(redis);
        seedPool(redis, 3);
        const size = await keyRotator.getPoolSize();
        expect(size).toBe(3);
        redis.disconnect();
    });
});
