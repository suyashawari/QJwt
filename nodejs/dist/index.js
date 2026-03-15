"use strict";
/**
 * @quantum-auth/jwt-client
 *
 * Quantum JWT client library for Node.js.
 * Uses quantum-derived entropy from Redis to sign and verify JWTs.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_CONFIG = exports.HoneyTokenError = exports.HoneyTokenTrap = exports.InvalidTokenError = exports.EntropyExhaustedError = exports.TokenManager = exports.KeyRotator = exports.QuantumClient = void 0;
// Main client
var QuantumClient_1 = require("./QuantumClient");
Object.defineProperty(exports, "QuantumClient", { enumerable: true, get: function () { return QuantumClient_1.QuantumClient; } });
// Core components (for advanced usage)
var KeyRotator_1 = require("./core/KeyRotator");
Object.defineProperty(exports, "KeyRotator", { enumerable: true, get: function () { return KeyRotator_1.KeyRotator; } });
var TokenManager_1 = require("./core/TokenManager");
Object.defineProperty(exports, "TokenManager", { enumerable: true, get: function () { return TokenManager_1.TokenManager; } });
Object.defineProperty(exports, "EntropyExhaustedError", { enumerable: true, get: function () { return TokenManager_1.EntropyExhaustedError; } });
Object.defineProperty(exports, "InvalidTokenError", { enumerable: true, get: function () { return TokenManager_1.InvalidTokenError; } });
var HoneyTokenTrap_1 = require("./core/HoneyTokenTrap");
Object.defineProperty(exports, "HoneyTokenTrap", { enumerable: true, get: function () { return HoneyTokenTrap_1.HoneyTokenTrap; } });
Object.defineProperty(exports, "HoneyTokenError", { enumerable: true, get: function () { return HoneyTokenTrap_1.HoneyTokenError; } });
// Types
var config_1 = require("./types/config");
Object.defineProperty(exports, "DEFAULT_CONFIG", { enumerable: true, get: function () { return config_1.DEFAULT_CONFIG; } });
//# sourceMappingURL=index.js.map