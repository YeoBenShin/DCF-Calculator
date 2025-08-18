package com.dcf.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caching service for expensive BigDecimal calculations
 * Caches intermediate results to avoid redundant computations
 */
@Service
public class BigDecimalCalculationCacheService {

    private static final Logger logger = LoggerFactory.getLogger(BigDecimalCalculationCacheService.class);

    private static final int DEFAULT_CACHE_DURATION_MINUTES = 30; // Cache for 30 minutes
    private static final int MAX_CACHE_SIZE = 500; // Maximum number of cached calculations

    private final ConcurrentMap<String, CalculationCacheEntry> calculationCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BigDecimal> powerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<BigDecimal>> projectionCache = new ConcurrentHashMap<>();

    /**
     * Get cached DCF calculation result
     * @param cacheKey the cache key for the calculation
     * @return cached DCF result or null if not cached or expired
     */
    public DCFCalculationResult getCachedDCFCalculation(String cacheKey) {
        CalculationCacheEntry entry = calculationCache.get(cacheKey);
        
        if (entry == null) {
            logger.debug("No cached DCF calculation found for key: {}", cacheKey);
            return null;
        }
        
        if (entry.isExpired()) {
            logger.debug("Cached DCF calculation expired for key: {}", cacheKey);
            calculationCache.remove(cacheKey);
            return null;
        }
        
        logger.debug("Returning cached DCF calculation for key: {}", cacheKey);
        return entry.getResult();
    }

    /**
     * Cache DCF calculation result
     * @param cacheKey the cache key
     * @param result the calculation result to cache
     */
    public void cacheDCFCalculation(String cacheKey, DCFCalculationResult result) {
        // Check cache size limit
        if (calculationCache.size() >= MAX_CACHE_SIZE) {
            evictOldestCalculations();
        }
        
        CalculationCacheEntry entry = new CalculationCacheEntry(result, DEFAULT_CACHE_DURATION_MINUTES);
        calculationCache.put(cacheKey, entry);
        
        logger.debug("Cached DCF calculation for key: {}", cacheKey);
    }

    /**
     * Get cached power calculation (base^exponent)
     * @param base the base value
     * @param exponent the exponent
     * @return cached power result or null if not cached
     */
    public BigDecimal getCachedPower(BigDecimal base, int exponent) {
        String key = createPowerKey(base, exponent);
        return powerCache.get(key);
    }

    /**
     * Cache power calculation result
     * @param base the base value
     * @param exponent the exponent
     * @param result the power calculation result
     */
    public void cachePower(BigDecimal base, int exponent, BigDecimal result) {
        String key = createPowerKey(base, exponent);
        powerCache.put(key, result);
        
        // Limit power cache size
        if (powerCache.size() > 1000) {
            powerCache.clear(); // Simple eviction strategy
        }
    }

    /**
     * Get cached cash flow projections
     * @param baseFCF the base free cash flow
     * @param growthRate the growth rate
     * @param years the number of years
     * @return cached projections or null if not cached
     */
    public List<BigDecimal> getCachedProjections(BigDecimal baseFCF, BigDecimal growthRate, int years) {
        String key = createProjectionKey(baseFCF, growthRate, years);
        return projectionCache.get(key);
    }

    /**
     * Cache cash flow projections
     * @param baseFCF the base free cash flow
     * @param growthRate the growth rate
     * @param years the number of years
     * @param projections the calculated projections
     */
    public void cacheProjections(BigDecimal baseFCF, BigDecimal growthRate, int years, List<BigDecimal> projections) {
        String key = createProjectionKey(baseFCF, growthRate, years);
        projectionCache.put(key, projections);
        
        // Limit projection cache size
        if (projectionCache.size() > 200) {
            projectionCache.clear(); // Simple eviction strategy
        }
    }

    /**
     * Create cache key for DCF calculation
     * @param ticker the ticker symbol
     * @param discountRate the discount rate
     * @param growthRate the growth rate
     * @param terminalGrowthRate the terminal growth rate
     * @param projectionYears the projection years
     * @param latestFCF the latest free cash flow
     * @return cache key string
     */
    public String createDCFCacheKey(String ticker, BigDecimal discountRate, BigDecimal growthRate, 
                                   BigDecimal terminalGrowthRate, int projectionYears, BigDecimal latestFCF) {
        return String.format("dcf_%s_%s_%s_%s_%d_%s", 
            ticker, 
            discountRate.toPlainString(), 
            growthRate.toPlainString(),
            terminalGrowthRate.toPlainString(),
            projectionYears,
            latestFCF.toPlainString()
        );
    }

    /**
     * Create cache key for power calculations
     */
    private String createPowerKey(BigDecimal base, int exponent) {
        return String.format("pow_%s_%d", base.toPlainString(), exponent);
    }

    /**
     * Create cache key for projection calculations
     */
    private String createProjectionKey(BigDecimal baseFCF, BigDecimal growthRate, int years) {
        return String.format("proj_%s_%s_%d", baseFCF.toPlainString(), growthRate.toPlainString(), years);
    }

    /**
     * Evict oldest calculation entries when cache is full
     */
    private void evictOldestCalculations() {
        // Remove 20% of entries (oldest first)
        int entriesToRemove = Math.max(1, MAX_CACHE_SIZE / 5);
        
        calculationCache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreatedAt().compareTo(e2.getValue().getCreatedAt()))
            .limit(entriesToRemove)
            .forEach(entry -> {
                calculationCache.remove(entry.getKey());
                logger.debug("Evicted cached calculation for key: {}", entry.getKey());
            });
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        calculationCache.clear();
        powerCache.clear();
        projectionCache.clear();
        logger.info("Cleared all BigDecimal calculation caches");
    }

    /**
     * Get cache statistics
     * @return cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        int totalCalculations = calculationCache.size();
        int expiredCalculations = 0;
        
        for (CalculationCacheEntry entry : calculationCache.values()) {
            if (entry.isExpired()) {
                expiredCalculations++;
            }
        }
        
        return new CacheStatistics(
            totalCalculations,
            expiredCalculations,
            powerCache.size(),
            projectionCache.size(),
            MAX_CACHE_SIZE
        );
    }

    /**
     * DCF calculation result for caching
     */
    public static class DCFCalculationResult {
        private final BigDecimal fairValuePerShare;
        private final BigDecimal enterpriseValue;
        private final BigDecimal equityValue;
        private final BigDecimal terminalValue;
        private final BigDecimal presentValueOfCashFlows;
        private final List<BigDecimal> projectedCashFlows;

        public DCFCalculationResult(BigDecimal fairValuePerShare, BigDecimal enterpriseValue, 
                                  BigDecimal equityValue, BigDecimal terminalValue, 
                                  BigDecimal presentValueOfCashFlows, List<BigDecimal> projectedCashFlows) {
            this.fairValuePerShare = fairValuePerShare;
            this.enterpriseValue = enterpriseValue;
            this.equityValue = equityValue;
            this.terminalValue = terminalValue;
            this.presentValueOfCashFlows = presentValueOfCashFlows;
            this.projectedCashFlows = projectedCashFlows;
        }

        // Getters
        public BigDecimal getFairValuePerShare() { return fairValuePerShare; }
        public BigDecimal getEnterpriseValue() { return enterpriseValue; }
        public BigDecimal getEquityValue() { return equityValue; }
        public BigDecimal getTerminalValue() { return terminalValue; }
        public BigDecimal getPresentValueOfCashFlows() { return presentValueOfCashFlows; }
        public List<BigDecimal> getProjectedCashFlows() { return projectedCashFlows; }
    }

    /**
     * Cache entry wrapper for calculations
     */
    private static class CalculationCacheEntry {
        private final DCFCalculationResult result;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;

        public CalculationCacheEntry(DCFCalculationResult result, int durationMinutes) {
            this.result = result;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = createdAt.plusMinutes(durationMinutes);
        }

        public DCFCalculationResult getResult() {
            return result;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStatistics {
        private final int totalCalculations;
        private final int expiredCalculations;
        private final int powerCacheSize;
        private final int projectionCacheSize;
        private final int maxCalculationCacheSize;

        public CacheStatistics(int totalCalculations, int expiredCalculations, 
                             int powerCacheSize, int projectionCacheSize, int maxCalculationCacheSize) {
            this.totalCalculations = totalCalculations;
            this.expiredCalculations = expiredCalculations;
            this.powerCacheSize = powerCacheSize;
            this.projectionCacheSize = projectionCacheSize;
            this.maxCalculationCacheSize = maxCalculationCacheSize;
        }

        public int getTotalCalculations() { return totalCalculations; }
        public int getExpiredCalculations() { return expiredCalculations; }
        public int getActiveCalculations() { return totalCalculations - expiredCalculations; }
        public int getPowerCacheSize() { return powerCacheSize; }
        public int getProjectionCacheSize() { return projectionCacheSize; }
        public int getMaxCalculationCacheSize() { return maxCalculationCacheSize; }
        
        public double getCalculationCacheUtilization() {
            return maxCalculationCacheSize > 0 ? (double) totalCalculations / maxCalculationCacheSize : 0.0;
        }
    }
}