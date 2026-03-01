// package com.quantum.jwt;

// import org.springframework.boot.context.properties.ConfigurationProperties;

// @ConfigurationProperties(prefix = "quantum.jwt")
// public class QuantumJwtProperties {

//     /**
//      * Redis connection details
//      */
//     private String redisHost = "localhost";
//     private int redisPort = 6379;
//     private String redisPassword = "";
//     private int redisDatabase = 0;

//     /**
//      * Quantum entropy pool key in Redis (list of byte arrays)
//      */
//     private String entropyPoolKey = "entropy:pool";

//     /**
//      * Prefix for storing keys by kid
//      */
//     private String keyPrefix = "key:";

//     /**
//      * Time-to-live for stored keys (seconds) – should match max token expiry
//      */
//     private long keyTtlSeconds = 900; // 15 minutes

//     /**
//      * Local LRU cache size for keys
//      */
//     private int cacheMaxSize = 100;

//     /**
//      * Batch size for fetching quantum keys from Redis (0 = disabled)
//      */
//     private int batchSize = 5;

//     /**
//      * Enable fallback to SecureRandom when quantum pool is empty
//      */
//     private boolean fallbackEnabled = true;

//     /**
//      * Adaptive expiry settings
//      */
//     private boolean dynamicExpiryEnabled = false;
//     private long baseExpirySeconds = 900; // 15 minutes
//     private int highRateThreshold = 30;    // requests per minute
//     private int lowRateThreshold = 5;       // requests per minute

//     /**
//      * Client fingerprinting
//      */
//     private boolean fingerprintEnabled = true;

//     // Getters and setters (generate with IDE or add manually)
//     public String getRedisHost() { return redisHost; }
//     public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

//     public int getRedisPort() { return redisPort; }
//     public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

//     public String getRedisPassword() { return redisPassword; }
//     public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

//     public int getRedisDatabase() { return redisDatabase; }
//     public void setRedisDatabase(int redisDatabase) { this.redisDatabase = redisDatabase; }

//     public String getEntropyPoolKey() { return entropyPoolKey; }
//     public void setEntropyPoolKey(String entropyPoolKey) { this.entropyPoolKey = entropyPoolKey; }

//     public String getKeyPrefix() { return keyPrefix; }
//     public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

//     public long getKeyTtlSeconds() { return keyTtlSeconds; }
//     public void setKeyTtlSeconds(long keyTtlSeconds) { this.keyTtlSeconds = keyTtlSeconds; }

//     public int getCacheMaxSize() { return cacheMaxSize; }
//     public void setCacheMaxSize(int cacheMaxSize) { this.cacheMaxSize = cacheMaxSize; }

//     public int getBatchSize() { return batchSize; }
//     public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

//     public boolean isFallbackEnabled() { return fallbackEnabled; }
//     public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

//     public boolean isDynamicExpiryEnabled() { return dynamicExpiryEnabled; }
//     public void setDynamicExpiryEnabled(boolean dynamicExpiryEnabled) { this.dynamicExpiryEnabled = dynamicExpiryEnabled; }

//     public long getBaseExpirySeconds() { return baseExpirySeconds; }
//     public void setBaseExpirySeconds(long baseExpirySeconds) { this.baseExpirySeconds = baseExpirySeconds; }

//     public int getHighRateThreshold() { return highRateThreshold; }
//     public void setHighRateThreshold(int highRateThreshold) { this.highRateThreshold = highRateThreshold; }

//     public int getLowRateThreshold() { return lowRateThreshold; }
//     public void setLowRateThreshold(int lowRateThreshold) { this.lowRateThreshold = lowRateThreshold; }

//     public boolean isFingerprintEnabled() { return fingerprintEnabled; }
//     public void setFingerprintEnabled(boolean fingerprintEnabled) { this.fingerprintEnabled = fingerprintEnabled; }
// }


package com.quantum.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quantum.jwt")
public class QuantumJwtProperties {

    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String redisPassword = "";
    private int redisDatabase = 0;
    private String entropyPoolKey = "entropy:pool";
    private String keyPrefix = "key:";
    private long keyTtlSeconds = 900;
    private int cacheMaxSize = 100;
    private int batchSize = 5;
    private boolean fallbackEnabled = true;
    private boolean dynamicExpiryEnabled = false;
    private long baseExpirySeconds = 900;
    private int highRateThreshold = 30;
    private int lowRateThreshold = 5;
    private boolean fingerprintEnabled = true;

    // Getters and setters (add all)
    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public int getRedisDatabase() { return redisDatabase; }
    public void setRedisDatabase(int redisDatabase) { this.redisDatabase = redisDatabase; }

    public String getEntropyPoolKey() { return entropyPoolKey; }
    public void setEntropyPoolKey(String entropyPoolKey) { this.entropyPoolKey = entropyPoolKey; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public long getKeyTtlSeconds() { return keyTtlSeconds; }
    public void setKeyTtlSeconds(long keyTtlSeconds) { this.keyTtlSeconds = keyTtlSeconds; }

    public int getCacheMaxSize() { return cacheMaxSize; }
    public void setCacheMaxSize(int cacheMaxSize) { this.cacheMaxSize = cacheMaxSize; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

    public boolean isDynamicExpiryEnabled() { return dynamicExpiryEnabled; }
    public void setDynamicExpiryEnabled(boolean dynamicExpiryEnabled) { this.dynamicExpiryEnabled = dynamicExpiryEnabled; }

    public long getBaseExpirySeconds() { return baseExpirySeconds; }
    public void setBaseExpirySeconds(long baseExpirySeconds) { this.baseExpirySeconds = baseExpirySeconds; }

    public int getHighRateThreshold() { return highRateThreshold; }
    public void setHighRateThreshold(int highRateThreshold) { this.highRateThreshold = highRateThreshold; }

    public int getLowRateThreshold() { return lowRateThreshold; }
    public void setLowRateThreshold(int lowRateThreshold) { this.lowRateThreshold = lowRateThreshold; }

    public boolean isFingerprintEnabled() { return fingerprintEnabled; }
    public void setFingerprintEnabled(boolean fingerprintEnabled) { this.fingerprintEnabled = fingerprintEnabled; }
}