/**
 * TokenManager — signs and validates JWTs using the `jsonwebtoken` library.
 *
 * Handles HS256 signing with kid in header, expiry checks, IP binding,
 * and optional quantum watermarking.
 */

import jwt, { JwtPayload, JwtHeader } from "jsonwebtoken";
import crypto from "crypto";
import { QuantumJwtPayload, QuantumTokenHeader } from "../types/payload";
import { QuantumJwtConfig } from "../types/config";

export class EntropyExhaustedError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "EntropyExhaustedError";
    }
}

export class InvalidTokenError extends Error {
    constructor(message: string) {
        super(message);
        this.name = "InvalidTokenError";
    }
}

export class TokenManager {
    private config: QuantumJwtConfig;

    constructor(config: QuantumJwtConfig) {
        this.config = config;
    }

    /**
     * Sign a JWT with HS256.
     */
    sign(
        subject: string,
        claims: Record<string, unknown>,
        kid: string,
        keyBuffer: Buffer,
        clientIp?: string
    ): string {
        const now = Math.floor(Date.now() / 1000);
        const payload: Record<string, unknown> = {
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

        return jwt.sign(payload, keyBuffer, {
            algorithm: "HS256",
            header: {
                alg: "HS256",
                typ: "JWT",
                kid,
            } as unknown as JwtHeader,
        });
    }

    /**
     * Validate a JWT and return the decoded payload.
     */
    verify(token: string, keyBuffer: Buffer, clientIp?: string): QuantumJwtPayload {
        let decoded: JwtPayload;
        try {
            decoded = jwt.verify(token, keyBuffer, {
                algorithms: ["HS256"],
            }) as JwtPayload;
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : String(err);
            throw new InvalidTokenError(`Token validation failed: ${message}`);
        }

        // IP binding check
        if (this.config.ipBindingEnabled && clientIp) {
            const tokenIp = decoded.ip as string | undefined;
            if (tokenIp && tokenIp !== clientIp) {
                throw new InvalidTokenError("IP address mismatch");
            }
        }

        // Watermark check
        if (this.config.watermarkSecret && decoded.qwm) {
            const expected = this.computeWatermark(
                decoded as Record<string, unknown>,
                this.config.watermarkSecret
            );
            if (decoded.qwm !== expected) {
                throw new InvalidTokenError("Invalid watermark");
            }
        }

        return decoded as unknown as QuantumJwtPayload;
    }

    /**
     * Extract kid from a token without verifying signature.
     */
    extractKid(token: string): string | null {
        try {
            const decoded = jwt.decode(token, { complete: true });
            if (!decoded) return null;
            return (decoded.header as unknown as Record<string, unknown>).kid as string ?? null;
        } catch {
            return null;
        }
    }

    /**
     * Compute HMAC-SHA256 watermark over the payload (sorted, excluding qwm).
     */
    private computeWatermark(
        payload: Record<string, unknown>,
        secret: string
    ): string {
        const data = { ...payload };
        delete data.qwm;
        const sorted = JSON.stringify(data, Object.keys(data).sort());
        return crypto
            .createHmac("sha256", secret)
            .update(sorted)
            .digest("hex");
    }
}
