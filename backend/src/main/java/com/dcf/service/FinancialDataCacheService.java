package com.dcf.service;

import com.dcf.entity.FinancialData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class FinancialDataCacheService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataCacheService.class);
    
    private static final int DEFAULT_CACHE_DURATION_HOURS = 24; // Cache for 24 hours
    private static final int MAX_CACHE_SIZE = 1000; // Maximum number of cached entries

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Get cached financial data for a ticker
     * @param ticker the ticker symbol
     * @return cached FinancialData or null if not cached or expired
     */
    public FinancialData getCachedData(String ticker) {
        String normalizedTicker = ticker.toUpperCase();
        CacheEntry entry = cache.get(normalizedTicker);
        
        if (entry == null) {
            logger.debug("No cached data found for ticker: {}", normalizedTicker);
            return null;
        }
        
        if (entry.isExpired()) {
            logger.debug("Cached data expired for ticker: {}", normalizedTicker);
            cache.remove(normalizedTicker);
            return null;
        }
        
        logger.debug("Returning cached data for ticker: {}", normalizedTicker);
        return entry.getData();
    }

    /**
     * Cache financial data for a ticker
     * @param ticker the ticker symbol
     * @param data the financial data to cache
     */
    public void cacheData(String ticker, FinancialData data) {
        String normalizedTicker = ticker.toUpperCase();
        
        // Check cache size limit
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry(data, DEFAULT_CACHE_DURATION_HOURS);
        cache.put(normalizedTicker, entry);
        
        logger.debug("Cached financial data for ticker: {}", normalizedTicker);
    }

    /**
     * Cache financial data with custom duration
     * @param ticker the ticker symbol
     * @param data the financial data to cache
     * @param durationHours cache duration in hours
     */
    public void cacheData(String ticker, FinancialData data, int durationHours) {
        String normalizedTicker = ticker.toUpperCase();
        
        // Check cache size limit
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        CacheEntry entry = new CacheEntry(data, durationHours);
        cache.put(normalizedTicker, entry);
        
        logger.debug("Cached financial data for ticker: {} with duration: {} hours", normalizedTicker, durationHours);
    }

    /**
     * Check if data is cached and not expired
     * @param ticker the ticker symbol
     * @return true if cached and valid
     */
    public boolean isCached(String ticker) {
        String normalizedTicker = ticker.toUpperCase();
        CacheEntry entry = cache.get(normalizedTicker);
        return entry != null && !entry.isExpired();
    }

    /**
     * Invalidate cached data for a ticker
     * @param ticker the ticker symbol
     */
    public void invalidateCache(String ticker) {
        String normalizedTicker = ticker.toUpperCase();
        cache.remove(normalizedTicker);
        logger.debug("Invalidated cache for ticker: {}", normalizedTicker);
    }

    /**
     * Clear all cached data
     */
    public void clearCache() {
        cache.clear();
        logger.info("Cleared all cached financial data");
    }

    /**
     * Get cache statistics
     * @return CacheStats object with cache information
     */
    public CacheStats getCacheStats() {
        int totalEntries = cache.size();
        int expiredEntries = 0;
        
        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                expiredEntries++;
            }
        }
        
        return new CacheStats(totalEntries, expiredEntries, MAX_CACHE_SIZE);
    }

    /**
     * Evict oldest entries when cache is full
     */
    private void evictOldestEntries() {
        // Remove 10% of entries (oldest first)
        int entriesToRemove = Math.max(1, MAX_CACHE_SIZE / 10);
        
        cache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().getCreatedAt().compareTo(e2.getValue().getCreatedAt()))
            .limit(entriesToRemove)
            .forEach(entry -> {
                cache.remove(entry.getKey());
                logger.debug("Evicted cached entry for ticker: {}", entry.getKey());
            });
    }

    /**
     * Cache entry wrapper
     */
    private static class CacheEntry {
        private final FinancialData data;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;

        public CacheEntry(FinancialData data, int durationHours) {
            this.data = data;
            this.createdAt = LocalDateTime.now();
            this.expiresAt = createdAt.plusHours(durationHours);
        }

        public FinancialData getData() {
            return data;
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
    public static class CacheStats {
        private final int totalEntries;
        private final int expiredEntries;
        private final int maxSize;

        public CacheStats(int totalEntries, int expiredEntries, int maxSize) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
            this.maxSize = maxSize;
        }

        public int getTotalEntries() {
            return totalEntries;
        }

        public int getExpiredEntries() {
            return expiredEntries;
        }

        public int getActiveEntries() {
            return totalEntries - expiredEntries;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public double getCacheUtilization() {
            return maxSize > 0 ? (double) totalEntries / maxSize : 0.0;
        }
    }
}