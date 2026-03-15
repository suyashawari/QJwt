package com.quantum.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final int WINDOW_SECONDS = 10;
    private static final int YELLOW_WARNING_THRESHOLD = 5;
    private static final int RED_BLOCK_THRESHOLD = 15;

    // Map of IP -> TrafficInfo
    private final Map<String, TrafficInfo> trafficMap = new ConcurrentHashMap<>();

    // Map of IP -> Block Until Expiration Time
    private final Map<String, Instant> blockMap = new ConcurrentHashMap<>();

    public static class TrafficInfo {
        public AtomicInteger requestCount = new AtomicInteger(0);
        public Instant windowStart = Instant.now();
        public String zone = "GREEN"; // GREEN, YELLOW, RED
    }

    /**
     * Checks if the IP is allowed and updates traffic stats.
     * @return true if allowed, false if blocked (rate limited).
     */
    public boolean checkAndAddRequest(String ip) {
        Instant now = Instant.now();

        // 1. Clean up or check block list
        if (blockMap.containsKey(ip)) {
            if (now.isAfter(blockMap.get(ip))) {
                blockMap.remove(ip); // Block expired
            } else {
                return false; // Still blocked
            }
        }

        // 2. Process current traffic window
        TrafficInfo info = trafficMap.computeIfAbsent(ip, k -> new TrafficInfo());

        // Reset window if it's too old
        if (now.isAfter(info.windowStart.plusSeconds(WINDOW_SECONDS))) {
            info.requestCount.set(0);
            info.windowStart = now;
            info.zone = "GREEN";
        }

        int count = info.requestCount.incrementAndGet();

        // 3. Determine Zone
        if (count >= RED_BLOCK_THRESHOLD) {
            info.zone = "RED";
            blockMap.put(ip, now.plusSeconds(30)); // Block for 30 seconds
            return false;
        } else if (count >= YELLOW_WARNING_THRESHOLD) {
            info.zone = "YELLOW";
        } else {
            info.zone = "GREEN";
        }

        return true;
    }

    /**
     * Instantly blocks an IP (e.g., if they hit a Honey Token).
     */
    public void hardBlockIp(String ip, int seconds) {
        blockMap.put(ip, Instant.now().plusSeconds(seconds));
        TrafficInfo info = trafficMap.computeIfAbsent(ip, k -> new TrafficInfo());
        info.zone = "RED";
        info.requestCount.set(RED_BLOCK_THRESHOLD);
    }

    /**
     * Returns a snapshot of current traffic zones for the Admin Dashboard.
     */
    public Map<String, Map<String, Object>> getTrafficZones() {
        Map<String, Map<String, Object>> snapshot = new HashMap<>();
        Instant now = Instant.now();

        for (Map.Entry<String, TrafficInfo> entry : trafficMap.entrySet()) {
            String ip = entry.getKey();
            TrafficInfo info = entry.getValue();

            // Clear out stale green traffic from snapshot
            if (now.isAfter(info.windowStart.plusSeconds(WINDOW_SECONDS)) && "GREEN".equals(info.zone)) {
                continue; 
            }

            Map<String, Object> data = new HashMap<>();
            data.put("count", info.requestCount.get());
            
            if (blockMap.containsKey(ip) && now.isBefore(blockMap.get(ip))) {
                data.put("zone", "RED");
            } else {
                data.put("zone", info.zone);
            }
            
            snapshot.put(ip, data);
        }

        return snapshot;
    }
}
