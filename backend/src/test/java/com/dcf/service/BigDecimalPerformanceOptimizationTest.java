package com.dcf.service;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import com.dcf.util.BigDecimalPerformanceProfiler;
import com.dcf.util.OptimizedBigDecimalMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BigDecimal performance optimizations
 */
@SpringBootTest
@ActiveProfiles("test")
public class BigDecimalPerformanceOptimizationTest {

    @Autowired
    private DCFCalculationService dcfCalculationService;

    @Autowired
    private BigDecimalPerformanceProfiler performanceProfiler;

    @Autowired
    private OptimizedBigDecimalMath optimizedMath;

    @Autowired
    private BigDecimalCalculationCacheService calculationCacheService;

    @Autowired
    private BigDecimalPerformanceMonitoringService performanceMonitoringService;

    private DCFInput testDcfInput;
    private FinancialData testFinancialData;

    @BeforeEach
    void setUp() {
        // Clear caches and stats before each test
        calculationCacheService.clearAllCaches();
        performanceProfiler.clearStats();
        
        // Create test DCF input
        testDcfInput = new DCFInput();
        testDcfInput.setTicker("AAPL");
        testDcfInput.setDiscountRate(new BigDecimal("10.0"));
        testDcfInput.setGrowthRate(new BigDecimal("5.0"));
        testDcfInput.setTerminalGrowthRate(new BigDecimal("2.5"));
        testDcfInput.setProjectionYears(5);
        testDcfInput.setUserId("test-user");

        // Create test financial data
        testFinancialData = new FinancialData();
        testFinancialData.setTicker("AAPL");
        testFinancialData.setLatestFreeCashFlow(new BigDecimal("100000000000")); // $100B
        testFinancialData.setLatestDebt(new BigDecimal("50000000000")); // $50B
        testFinancialData.setLatestSharesOutstanding(new BigDecimal("16000000000")); // 16B shares
    }

    @Test
    void testOptimizedBigDecimalMathOperations() {
        // Test optimized power calculations
        BigDecimal base = new BigDecimal("1.05");
        int exponent = 10;
        
        Instant start = Instant.now();
        BigDecimal result1 = optimizedMath.pow(base, exponent);
        Duration duration1 = Duration.between(start, Instant.now());
        
        // Second call should be faster due to caching
        start = Instant.now();
        BigDecimal result2 = optimizedMath.pow(base, exponent);
        Duration duration2 = Duration.between(start, Instant.now());
        
        assertEquals(result1, result2);
        assertTrue(duration2.toNanos() <= duration1.toNanos(), 
                  "Cached operation should be faster or equal");
    }

    @Test
    void testCashFlowProjectionCaching() {
        BigDecimal baseFCF = new BigDecimal("100000000000");
        BigDecimal growthRate = new BigDecimal("0.05");
        int years = 5;
        
        // First call - should calculate and cache
        Instant start = Instant.now();
        List<BigDecimal> projections1 = optimizedMath.projectCashFlows(baseFCF, growthRate, years);
        Duration duration1 = Duration.between(start, Instant.now());
        
        // Second call with same parameters - should use cache
        start = Instant.now();
        List<BigDecimal> projections2 = optimizedMath.projectCashFlows(baseFCF, growthRate, years);
        Duration duration2 = Duration.between(start, Instant.now());
        
        assertEquals(projections1, projections2);
        assertEquals(years, projections1.size());
        
        // Verify cache statistics
        OptimizedBigDecimalMath.CacheStats cacheStats = optimizedMath.getCacheStats();
        assertTrue(cacheStats.getPowerCacheSize() > 0, "Power cache should contain entries");
    }

    @Test
    void testDCFCalculationCaching() throws DCFCalculationService.DCFCalculationException {
        // First DCF calculation - should calculate and cache
        Instant start = Instant.now();
        DCFOutput result1 = dcfCalculationService.calculateDCF(testDcfInput);
        Duration duration1 = Duration.between(start, Instant.now());
        
        // Second calculation with same parameters - should use cache
        start = Instant.now();
        DCFOutput result2 = dcfCalculationService.calculateDCF(testDcfInput);
        Duration duration2 = Duration.between(start, Instant.now());
        
        // Results should be identical
        assertEquals(result1.getFairValuePerShare(), result2.getFairValuePerShare());
        assertEquals(result1.getEnterpriseValue(), result2.getEnterpriseValue());
        assertEquals(result1.getEquityValue(), result2.getEquityValue());
        
        // Second calculation should be significantly faster
        assertTrue(duration2.toMillis() < duration1.toMillis() / 2, 
                  "Cached DCF calculation should be at least 50% faster");
        
        // Verify cache statistics
        BigDecimalCalculationCacheService.CacheStatistics cacheStats = 
            calculationCacheService.getCacheStatistics();
        assertTrue(cacheStats.getActiveCalculations() > 0, "Cache should contain active calculations");
    }

    @Test
    void testPerformanceProfilerTracking() throws DCFCalculationService.DCFCalculationException {
        // Perform some calculations to generate profiler data
        dcfCalculationService.calculateDCF(testDcfInput);
        
        // Check that profiler captured the operations
        BigDecimalPerformanceProfiler.OperationStats dcfStats = 
            performanceProfiler.getOperationStats("dcf_calculation");
        
        assertNotNull(dcfStats, "DCF calculation should be tracked by profiler");
        assertTrue(dcfStats.getExecutionCount() > 0, "Execution count should be greater than 0");
        assertTrue(dcfStats.getTotalExecutionTime() > 0, "Total execution time should be greater than 0");
        
        // Test performance report generation
        String report = performanceProfiler.getPerformanceReport();
        assertNotNull(report);
        assertTrue(report.contains("BigDecimal Performance Report"), "Report should contain header");
        assertTrue(report.contains("dcf_calculation"), "Report should contain DCF calculation stats");
    }

    @Test
    void testPerformanceMonitoringService() {
        // Generate some performance data
        performanceMonitoringService.recordOperation("test_operation", 50);
        performanceMonitoringService.recordOperation("slow_operation", 150);
        
        // Get current metrics
        BigDecimalPerformanceMonitoringService.PerformanceMetrics metrics = 
            performanceMonitoringService.getCurrentMetrics();
        
        assertNotNull(metrics);
        assertTrue(metrics.getTotalOperations() >= 2, "Should have recorded at least 2 operations");
        assertTrue(metrics.getSlowOperations() >= 1, "Should have recorded at least 1 slow operation");
        
        // Test performance report generation
        BigDecimalPerformanceMonitoringService.PerformanceReport report = 
            performanceMonitoringService.generateReport();
        
        assertNotNull(report);
        assertTrue(report.getTotalOperations() >= 2, "Report should show recorded operations");
    }

    @Test
    void testLargeScalePerformance() throws DCFCalculationService.DCFCalculationException {
        // Test performance with multiple calculations
        List<DCFInput> inputs = createMultipleDCFInputs(10);
        List<Long> executionTimes = new ArrayList<>();
        
        for (DCFInput input : inputs) {
            Instant start = Instant.now();
            dcfCalculationService.calculateDCF(input);
            Duration duration = Duration.between(start, Instant.now());
            executionTimes.add(duration.toMillis());
        }
        
        // Calculate average execution time
        double averageTime = executionTimes.stream()
                                          .mapToLong(Long::longValue)
                                          .average()
                                          .orElse(0.0);
        
        // Performance assertion - average should be reasonable (< 1000ms)
        assertTrue(averageTime < 1000, 
                  String.format("Average execution time (%.2fms) should be less than 1000ms", averageTime));
        
        // Verify cache effectiveness - later calculations should be faster
        List<Long> firstHalf = executionTimes.subList(0, 5);
        List<Long> secondHalf = executionTimes.subList(5, 10);
        
        double firstHalfAverage = firstHalf.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double secondHalfAverage = secondHalf.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // Second half should be faster due to caching (allowing some variance)
        assertTrue(secondHalfAverage <= firstHalfAverage * 1.2, 
                  "Second half should benefit from caching");
    }

    @Test
    void testMemoryEfficiency() {
        // Test that caches don't grow unbounded
        BigDecimalCalculationCacheService.CacheStatistics initialStats = 
            calculationCacheService.getCacheStatistics();
        
        // Perform many calculations with different parameters
        for (int i = 0; i < 100; i++) {
            DCFInput input = new DCFInput();
            input.setTicker("TEST" + i);
            input.setDiscountRate(new BigDecimal("10." + i));
            input.setGrowthRate(new BigDecimal("5." + i));
            input.setTerminalGrowthRate(new BigDecimal("2." + i));
            input.setProjectionYears(5);
            input.setUserId("test-user");
            
            try {
                dcfCalculationService.calculateDCF(input);
            } catch (Exception e) {
                // Ignore calculation errors for this test
            }
        }
        
        BigDecimalCalculationCacheService.CacheStatistics finalStats = 
            calculationCacheService.getCacheStatistics();
        
        // Cache should not grow unbounded
        assertTrue(finalStats.getTotalCalculations() <= finalStats.getMaxCalculationCacheSize(),
                  "Cache size should not exceed maximum");
    }

    private List<DCFInput> createMultipleDCFInputs(int count) {
        List<DCFInput> inputs = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            DCFInput input = new DCFInput();
            input.setTicker("TEST" + (i % 3)); // Reuse some tickers for caching
            input.setDiscountRate(new BigDecimal("10.0"));
            input.setGrowthRate(new BigDecimal("5.0"));
            input.setTerminalGrowthRate(new BigDecimal("2.5"));
            input.setProjectionYears(5);
            input.setUserId("test-user");
            inputs.add(input);
        }
        
        return inputs;
    }
}