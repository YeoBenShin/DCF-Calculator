package com.dcf.controller;

import com.dcf.service.BigDecimalPerformanceMonitoringService;
import com.dcf.service.BigDecimalCalculationCacheService;
import com.dcf.util.BigDecimalPerformanceProfiler;
import com.dcf.util.OptimizedBigDecimalMath;
import com.dcf.util.BigDecimalDatabaseOptimizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for BigDecimal performance monitoring and optimization
 */
@RestController
@RequestMapping("/api/performance/bigdecimal")
@CrossOrigin(origins = "*")
public class BigDecimalPerformanceController {

    @Autowired
    private BigDecimalPerformanceMonitoringService performanceMonitoringService;

    @Autowired
    private BigDecimalCalculationCacheService calculationCacheService;

    @Autowired
    private BigDecimalPerformanceProfiler performanceProfiler;

    @Autowired
    private OptimizedBigDecimalMath optimizedMath;

    @Autowired
    private BigDecimalDatabaseOptimizer databaseOptimizer;

    /**
     * Get current performance metrics
     * @return performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<BigDecimalPerformanceMonitoringService.PerformanceMetrics> getPerformanceMetrics() {
        BigDecimalPerformanceMonitoringService.PerformanceMetrics metrics = 
            performanceMonitoringService.getCurrentMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get detailed performance report
     * @return performance report
     */
    @GetMapping("/report")
    public ResponseEntity<BigDecimalPerformanceMonitoringService.PerformanceReport> getPerformanceReport() {
        BigDecimalPerformanceMonitoringService.PerformanceReport report = 
            performanceMonitoringService.generateReport();
        return ResponseEntity.ok(report);
    }

    /**
     * Get cache statistics
     * @return cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Calculation cache statistics
        BigDecimalCalculationCacheService.CacheStatistics calcStats = 
            calculationCacheService.getCacheStatistics();
        stats.put("calculationCache", calcStats);
        
        // Math utility cache statistics
        OptimizedBigDecimalMath.CacheStats mathStats = optimizedMath.getCacheStats();
        stats.put("mathCache", mathStats);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear all performance caches
     * @return success message
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCaches() {
        calculationCacheService.clearAllCaches();
        optimizedMath.clearCaches();
        performanceProfiler.clearStats();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All BigDecimal performance caches cleared successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get profiler statistics
     * @return profiler statistics
     */
    @GetMapping("/profiler/stats")
    public ResponseEntity<Map<String, BigDecimalPerformanceProfiler.OperationStats>> getProfilerStats() {
        return ResponseEntity.ok(performanceProfiler.getPerformanceStats());
    }

    /**
     * Get detailed profiler report
     * @return profiler report as string
     */
    @GetMapping("/profiler/report")
    public ResponseEntity<Map<String, String>> getProfilerReport() {
        Map<String, String> response = new HashMap<>();
        response.put("report", performanceProfiler.getPerformanceReport());
        return ResponseEntity.ok(response);
    }

    /**
     * Check if performance is degraded
     * @return performance status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPerformanceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isPerformanceDegraded", performanceMonitoringService.isPerformanceDegraded());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    /**
     * Get database performance statistics
     * @return database performance statistics
     */
    @GetMapping("/database/stats")
    public ResponseEntity<BigDecimalDatabaseOptimizer.DatabasePerformanceStats> getDatabaseStats() {
        BigDecimalDatabaseOptimizer.DatabasePerformanceStats stats = 
            databaseOptimizer.getDatabasePerformanceStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get database optimization recommendations
     * @return optimization recommendations
     */
    @GetMapping("/database/recommendations")
    public ResponseEntity<Map<String, Object>> getDatabaseRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        recommendations.put("indexRecommendations", databaseOptimizer.generateIndexRecommendations());
        recommendations.put("storageOptimizations", databaseOptimizer.getStorageOptimizationRecommendations());
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Analyze query performance
     * @param query the query to analyze
     * @return query analysis result
     */
    @PostMapping("/database/analyze")
    public ResponseEntity<Map<String, String>> analyzeQuery(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Query parameter is required");
            return ResponseEntity.badRequest().body(error);
        }

        String analysis = databaseOptimizer.analyzeQueryPerformance(query);
        Map<String, String> response = new HashMap<>();
        response.put("analysis", analysis);
        return ResponseEntity.ok(response);
    }

    /**
     * Get comprehensive performance dashboard data
     * @return dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getPerformanceDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Current metrics
        dashboard.put("metrics", performanceMonitoringService.getCurrentMetrics());
        
        // Cache statistics
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("calculationCache", calculationCacheService.getCacheStatistics());
        cacheStats.put("mathCache", optimizedMath.getCacheStats());
        dashboard.put("cacheStats", cacheStats);
        
        // Performance status
        dashboard.put("isPerformanceDegraded", performanceMonitoringService.isPerformanceDegraded());
        
        // Top operations by execution time
        dashboard.put("profilerStats", performanceProfiler.getPerformanceStats());
        
        // Database recommendations
        dashboard.put("databaseRecommendations", databaseOptimizer.generateIndexRecommendations().size());
        
        dashboard.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(dashboard);
    }
}