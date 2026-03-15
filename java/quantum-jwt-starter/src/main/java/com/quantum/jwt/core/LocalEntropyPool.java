package com.quantum.jwt.core;

import com.quantum.jwt.config.QuantumJwtProperties;
import com.quantum.jwt.exception.EntropyExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalEntropyPool implements EntropyPool {
    private static final Logger logger = LoggerFactory.getLogger(LocalEntropyPool.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentLinkedQueue<byte[]> pool = new ConcurrentLinkedQueue<>();
    private final IbmQuantumClient quantumClient;
    private final AtomicBoolean isRefilling = new AtomicBoolean(false);
    
    private static final int POOL_SIZE = 100;
    private static final int KEY_LENGTH = 32;

    public LocalEntropyPool(QuantumJwtProperties properties) {
        if (properties.getIbmQuantumToken() != null && !properties.getIbmQuantumToken().isEmpty()) {
            this.quantumClient = new IbmQuantumClient(properties.getIbmQuantumToken(), properties.getIbmBackendName());
            logger.info("🔮 Standalone IBM Quantum Mode Active. Backend: {}", properties.getIbmBackendName());
        } else {
            this.quantumClient = null;
            logger.info("Local Quantum Entropy Pool initialized with classical fallback (No IBM Token)");
        }
        refillPool();
    }

    private void refillPool() {
        if (!isRefilling.compareAndSet(false, true)) return;
        
        try {
            while (pool.size() < POOL_SIZE) {
                byte[] entropy = null;
                
                if (quantumClient != null) {
                    entropy = quantumClient.fetchEntropy(1); // Fetch 1 key at a time for simplicity
                }
                
                if (entropy == null) {
                    // Fallback to classical
                    entropy = new byte[KEY_LENGTH];
                    secureRandom.nextBytes(entropy);
                }
                
                pool.offer(entropy);
            }
        } finally {
            isRefilling.set(false);
        }
    }

    @Override
    public byte[] popKey() {
        byte[] key = pool.poll();
        
        if (key == null) {
            refillPool();
            key = pool.poll();
            if (key == null) {
                throw new EntropyExhaustedException("Entropy pool exhausted. Check quantum connectivity.");
            }
        }
        
        // Asynchronously refill when pool drops below threshold
        if (pool.size() < POOL_SIZE / 2 && !isRefilling.get()) {
            CompletableFuture.runAsync(this::refillPool);
        }
        
        return key;
    }

    @Override
    public long getPoolSize() {
        return pool.size();
    }
}
