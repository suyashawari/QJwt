/**
 * TokenManager — signs and validates JWTs using the `jsonwebtoken` library.
 *
 * Handles HS256 signing with kid in header, expiry checks, IP binding,
 * and optional quantum watermarking.
 */
import { QuantumJwtPayload } from "../types/payload";
import { QuantumJwtConfig } from "../types/config";
export declare class EntropyExhaustedError extends Error {
    constructor(message: string);
}
export declare class InvalidTokenError extends Error {
    constructor(message: string);
}
export declare class TokenManager {
    private config;
    constructor(config: QuantumJwtConfig);
    /**
     * Sign a JWT with HS256.
     */
    sign(subject: string, claims: Record<string, unknown>, kid: string, keyBuffer: Buffer, clientIp?: string): string;
    /**
     * Validate a JWT and return the decoded payload.
     */
    verify(token: string, keyBuffer: Buffer, clientIp?: string): QuantumJwtPayload;
    /**
     * Extract kid from a token without verifying signature.
     */
    extractKid(token: string): string | null;
    /**
     * Compute HMAC-SHA256 watermark over the payload (sorted, excluding qwm).
     */
    private computeWatermark;
}
//# sourceMappingURL=TokenManager.d.ts.map