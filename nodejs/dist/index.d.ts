/**
 * @quantum-auth/jwt-client
 *
 * Quantum JWT client library for Node.js.
 * Uses quantum-derived entropy from Redis to sign and verify JWTs.
 */
export { QuantumClient } from "./QuantumClient";
export { KeyRotator, KeyData } from "./core/KeyRotator";
export { TokenManager, EntropyExhaustedError, InvalidTokenError } from "./core/TokenManager";
export { HoneyTokenTrap, HoneyTokenError } from "./core/HoneyTokenTrap";
export { QuantumJwtConfig, DEFAULT_CONFIG } from "./types/config";
export { QuantumJwtPayload, QuantumTokenHeader } from "./types/payload";
//# sourceMappingURL=index.d.ts.map