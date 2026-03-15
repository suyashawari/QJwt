/**
 * @quantum-auth/jwt-client
 *
 * Quantum JWT client library for Node.js.
 * Uses quantum-derived entropy from Redis to sign and verify JWTs.
 */

// Main client
export { QuantumClient } from "./QuantumClient";

// Core components (for advanced usage)
export { KeyRotator, KeyData } from "./core/KeyRotator";
export { TokenManager, EntropyExhaustedError, InvalidTokenError } from "./core/TokenManager";
export { HoneyTokenTrap, HoneyTokenError } from "./core/HoneyTokenTrap";

// Types
export { QuantumJwtConfig, DEFAULT_CONFIG } from "./types/config";
export { QuantumJwtPayload, QuantumTokenHeader } from "./types/payload";
