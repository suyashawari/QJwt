/**
 * Standard JWT payload produced & consumed by the Quantum JWT framework.
 */
export interface QuantumJwtPayload {
    /** Subject – the user identifier */
    sub: string;
    /** Issuer */
    iss: string;
    /** Issued-at timestamp (epoch seconds) */
    iat: number;
    /** Expiration timestamp (epoch seconds) */
    exp: number;
    /** Client IP address (present when IP binding is enabled) */
    ip?: string;
    /** Quantum watermark hash (present when watermarking is enabled) */
    qwm?: string;
    /** Allow arbitrary extra claims */
    [key: string]: unknown;
}
/**
 * JWT header structure used by this framework.
 */
export interface QuantumTokenHeader {
    alg: "HS256";
    typ: "JWT";
    kid: string;
}
//# sourceMappingURL=payload.d.ts.map