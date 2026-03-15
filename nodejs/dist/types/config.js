"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DEFAULT_CONFIG = void 0;
/** Sensible defaults — merge with user-supplied overrides. */
exports.DEFAULT_CONFIG = {
    redisHost: "localhost",
    redisPort: 6379,
    redisPassword: "",
    redisDb: 0,
    tokenTtlSeconds: 900,
    issuer: "quantum-auth",
    ipBindingEnabled: true,
    watermarkSecret: null,
};
//# sourceMappingURL=config.js.map