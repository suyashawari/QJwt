package com.quantum.jwt.core;

import com.quantum.jwt.exception.EntropyExhaustedException;

public interface EntropyPool {
    /**
     * Pops one key from the pool, returns raw bytes.
     * @throws EntropyExhaustedException if the pool is empty
     */
    byte[] popKey();

    /**
     * Returns the current number of keys in the pool.
     */
    long getPoolSize();
}
