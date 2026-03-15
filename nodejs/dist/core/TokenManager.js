"use strict";
/**
 * TokenManager — signs and validates JWTs using the `jsonwebtoken` library.
 *
 * Handles HS256 signing with kid in header, expiry checks, IP binding,
 * and optional quantum watermarking.
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.TokenManager = exports.InvalidTokenError = exports.EntropyExhaustedError = void 0;
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const crypto_1 = __importDefault(require("crypto"));
class EntropyExhaustedError extends Error {
    constructor(message) {
        super(message);
        this.name = "EntropyExhaustedError";
    }
}
exports.EntropyExhaustedError = EntropyExhaustedError;
class InvalidTokenError extends Error {
    constructor(message) {
        super(message);
        this.name = "InvalidTokenError";
    }
}
exports.InvalidTokenError = InvalidTokenError;
class TokenManager {
    config;
    constructor(config) {
        this.config = config;
    }
    /**
     * Sign a JWT with HS256.
     */
    sign(subject, claims, kid, keyBuffer, clientIp) {
        const now = Math.floor(Date.now() / 1000);
        const payload = {
            sub: subject,
            iss: this.config.issuer,
            iat: now,
            exp: now + this.config.tokenTtlSeconds,
            ...claims,
        };
        // IP binding
        if (this.config.ipBindingEnabled && clientIp) {
            payload.ip = clientIp;
        }
        // Quantum watermark
        if (this.config.watermarkSecret) {
            payload.qwm = this.computeWatermark(payload, this.config.watermarkSecret);
        }
        return jsonwebtoken_1.default.sign(payload, keyBuffer, {
            algorithm: "HS256",
            header: {
                alg: "HS256",
                typ: "JWT",
                kid,
            },
        });
    }
    /**
     * Validate a JWT and return the decoded payload.
     */
    verify(token, keyBuffer, clientIp) {
        let decoded;
        try {
            decoded = jsonwebtoken_1.default.verify(token, keyBuffer, {
                algorithms: ["HS256"],
            });
        }
        catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            throw new InvalidTokenError(`Token validation failed: ${message}`);
        }
        // IP binding check
        if (this.config.ipBindingEnabled && clientIp) {
            const tokenIp = decoded.ip;
            if (tokenIp && tokenIp !== clientIp) {
                throw new InvalidTokenError("IP address mismatch");
            }
        }
        // Watermark check
        if (this.config.watermarkSecret && decoded.qwm) {
            const expected = this.computeWatermark(decoded, this.config.watermarkSecret);
            if (decoded.qwm !== expected) {
                throw new InvalidTokenError("Invalid watermark");
            }
        }
        return decoded;
    }
    /**
     * Extract kid from a token without verifying signature.
     */
    extractKid(token) {
        try {
            const decoded = jsonwebtoken_1.default.decode(token, { complete: true });
            if (!decoded)
                return null;
            return decoded.header.kid ?? null;
        }
        catch {
            return null;
        }
    }
    /**
     * Compute HMAC-SHA256 watermark over the payload (sorted, excluding qwm).
     */
    computeWatermark(payload, secret) {
        const data = { ...payload };
        delete data.qwm;
        const sorted = JSON.stringify(data, Object.keys(data).sort());
        return crypto_1.default
            .createHmac("sha256", secret)
            .update(sorted)
            .digest("hex");
    }
}
exports.TokenManager = TokenManager;
//# sourceMappingURL=TokenManager.js.map