package com.dcf.service;

import com.dcf.util.BigDecimalPerformanceProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring BigDecimal operation performance
 * Provides periodic reporting and alerting for performance issues
 */
@Service
public class BigDecimalPerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(BigDecimalPerformanceMonitoringService.class);

    @Autowired
    private BigDecimalPerformanceProfiler profiler;

    @Autowired
    private BigDecimalCalculationCacheService cacheService;

    private final AtomicLong totalOperations = new AtomicLong(0);
    private final AtomicLong slowOperations = new AtomicLong(0);
    private LocalDateTime lastReportTime = LocalDateTime.now();

    // Performance thresholds
    private static final long SLOW_OPERATION_THRESHOLD_MS = 100;
    private static final double SLOW_OPERATION_PERCENTAGE_THRESHOLD = 5.0; // 5%

    /**
     * Record a BigDecimal operation for monitoring
     * @param operationName the name of the operation
     * @param executionTimeMs the execution time in milliseconds
     */
    public void recordOperation(String operationName, long executionTimeMs) {
        totalOperations.incrementAndGet();
        
        if (executionTimeMs > SLOW_OPERATION_THRESHOLD_MS) {
            slowOperations.incrementAndGet();
            logger.warn("Slow BigDecimal operation detected: {} took {}ms", operationName, executionTimeMs);
        }
    }

    /**
     * Generate performance report every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void generatePerformanceReport() {
        try {
            PerformanceReport report = generateReport();
            
            if (report.shouldAlert()) {
                logger.warn("BigDecimal Performance Alert:\n{}", report.toString());
            } else {
                logger.info("BigDecimal Performance Report:\n{}", report.toString());
            }
            
            // Reset counters for next period
            resetCounters();
            
        } catch (Exception e) {
            logger.error("Error generating BigDecimal performance report", e);
        }
    }

    /**
     * Generate comprehensive performance report
     * @return performance report
     */
    public PerformanceReport generateReport() {
        long totalOps = totalOperations.get();
        long slowOps = slowOperations.get();
        double slowPercentage = totalOps > 0 ? (double) slowOps / totalOps * 100 : 0.0;
        
        BigDecimalCalculationCacheService.CacheStatistics cacheStats = cacheService.getCacheStatistics();
        BigDecimalPerformanceProfiler.OperationStats dcfStats = profiler.getOperationStats("dcf_calculation");
        
        return new PerformanceReport(
            totalOps,
            slowOps,
            slowPercentage,
            cacheStats,
            dcfStats,
            LocalDateTime.now()
        );
    }

    /**
     * Check if performance is degraded
     * @return true if performance issues detected
     */
    public boolean isPerformanceDegraded() {
        long totalOps = totalOperations.get();
        long slowOps = slowOperations.get();
        
        if (totalOps == 0) return false;
        
        double slowPercentage = (double) slowOps / totalOps * 100;
        return slowPercentage > SLOW_OPERATION_PERCENTAGE_THRESHOLD;
    }

    /**
     * Get current performance metrics
     * @return performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        return new PerformanceMetrics(
            totalOperations.get(),
            slowOperations.get(),
            cacheService.getCacheStatistics(),
            profiler.getPerformanceStats().size()
        );
    }

    /**
     * Reset performance counters
     */
    private void resetCounters() {
        totalOperations.set(0);
        slowOperations.set(0);
        lastReportTime = LocalDateTime.now();
    }

    /**
     * Performance report data structure
     */
    public static class PerformanceReport {
        private final long totalOperations;
        private final long slowOperations;
        private final double slowPercentage;
        private final BigDecimalCalculationCacheService.CacheStatistics cacheStats;
        private final BigDecimalPerformanceProfiler.OperationStats dcfStats;
        private final LocalDateTime reportTime;

        public PerformanceReport(long totalOperations, long slowOperations, double slowPercentage,
                               BigDecimalCalculationCacheService.CacheStatistics cacheStats,
                               BigDecimalPerformanceProfiler.OperationStats dcfStats,
                               LocalDateTime reportTime) {
            this.totalOperations = totalOperations;
            this.slowOperations = slowOperations;
            this.slowPercentage = slowPercentage;
            this.cacheStats = cacheStats;
            this.dcfStats = dcfStats;
            this.reportTime = reportTime;
        }

        public boolean shouldAlert() {
            return slowPercentage > SLOW_OPERATION_PERCENTAGE_THRESHOLD;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BigDecimal Performance Report - ").append(reportTime).append("\n");
            sb.append("=".repeat(60)).append("\n");
            sb.append(String.format("Total Operations: %d\n", totalOperations));
            sb.append(String.format("Slow Operations: %d (%.2f%%)\n", slowOperations, slowPercentage));
            
            if (shouldAlert()) {
                sb.append("⚠️  PERFORMANCE ALERT: High percentage of slow operations!\n");
            }
            
            sb.append("\nCache Statistics:\n");
            sb.append(String.format("  Active Calculations: %d\n", cacheStats.getActiveCalculations()));
            sb.append(String.format("  Power Cache Size: %d\n", cacheStats.getPowerCacheSize()));
            sb.append(String.format("  Projection Cache Size: %d\n", cacheStats.getProjectionCacheSize()));
            sb.append(String.format("  Cache Utilization: %.2f%%\n", cacheStats.getCalculationCacheUtilization() * 100));
            
            if (dcfStats != null) {
                sb.append("\nDCF Calculation Performance:\n");
                sb.append(String.format("  Total DCF Calculations: %d\n", dcfStats.getExecutionCount()));
                sb.append(String.format("  Average DCF Time: %.2fms\n", dcfStats.getAverageExecutionTime()));
                sb.append(String.format("  Max DCF Time: %dms\n", dcfStats.getMaxExecutionTime()));
            }
            
            return sb.toString();
        }

        // Getters
        public long getTotalOperations() { return totalOperations; }
        public long getSlowOperations() { return slowOperations; }
        public double getSlowPercentage() { return slowPercentage; }
        public BigDecimalCalculationCacheService.CacheStatistics getCacheStats() { return cacheStats; }
        public BigDecimalPerformanceProfiler.OperationStats getDcfStats() { return dcfStats; }
        public LocalDateTime getReportTime() { return reportTime; }
    }

    /**
     * Current performance metrics
     */
    public static class PerformanceMetrics {
        private final long totalOperations;
        private final long slowOperations;
        private final BigDecimalCalculationCacheService.CacheStatistics cacheStats;
        private final int trackedOperationTypes;

        public PerformanceMetrics(long totalOperations, long slowOperations,
                                BigDecimalCalculationCacheService.CacheStatistics cacheStats,
                                int trackedOperationTypes) {
            this.totalOperations = totalOperations;
            this.slowOperations = slowOperations;
            this.cacheStats = cacheStats;
            this.trackedOperationTypes = trackedOperationTypes;
        }

        public long getTotalOperations() { return totalOperations; }
        public long getSlowOperations() { return slowOperations; }
        public BigDecimalCalculationCacheService.CacheStatistics getCacheStats() { return cacheStats; }
        public int getTrackedOperationTypes() { return trackedOperationTypes; }
        
        public double getSlowOperationPercentage() {
            return totalOperations > 0 ? (double) slowOperations / totalOperations * 100 : 0.0;
        }
    }
}