package com.quantum.jwt;

import java.util.LinkedHashMap;
import java.util.Map;

public class KeyCache {
    private final int maxSize;
    private final Map<String, byte[]> cache;

    public KeyCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > maxSize;
            }
        };
    }

    public synchronized byte[] get(String kid) {
        return cache.get(kid);
    }

    public synchronized void put(String kid, byte[] key) {
        cache.put(kid, key);
    }
}
