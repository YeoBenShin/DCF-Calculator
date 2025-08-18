package com.dcf.repository;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and edge case tests for BigDecimal database operations
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class BigDecimalPerformanceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DCFInputRepository dcfInputRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @BeforeEach
    void setUp() {
        dcfInputRepository.deleteAll();
        dcfOutputRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Performance test: Bulk BigDecimal operations")
    void testBulkBigDecimalOperations() {
        // Create large dataset for performance testing
        List<DCFInput> inputs = new ArrayList<>();
        List<DCFOutput> outputs = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Create 1000 records with random BigDecimal values
        for (int i = 0; i < 1000; i++) {
            DCFInput input = new DCFInput();
            input.setTicker("PERF" + String.format("%04d", i));
            input.setDiscountRate(generateRandomBigDecimal(5.0, 15.0, 6));
            input.setGrowthRate(generateRandomBigDecimal(-10.0, 50.0, 6));
            input.setTerminalGrowthRate(generateRandomBigDecimal(1.0, 5.0, 6));
            input.setUserId("user" + (i % 100)); // 100 different users
            inputs.add(input);
            
            DCFOutput output = new DCFOutput();
            output.setTicker("PERF" + String.format("%04d", i));
            output.setFairValuePerShare(generateRandomBigDecimal(50.0, 500.0, 6));
            output.setCurrentPrice(generateRandomBigDecimal(40.0, 450.0, 6));
            output.setEnterpriseValue(generateRandomBigDecimal(1000000.0, 100000000.0, 2));
            output.setEquityValue(generateRandomBigDecimal(800000.0, 80000000.0, 2));
            output.setTerminalValue(generateRandomBigDecimal(500000.0, 50000000.0, 2));
            output.setPresentValueOfCashFlows(generateRandomBigDecimal(300000.0, 30000000.0, 2));
            output.setSharesOutstanding(generateRandomBigDecimal(1000000.0, 10000000.0, 0));
            output.setValuation(output.getFairValuePerShare().compareTo(output.getCurrentPrice()) > 0 ? "Undervalued" : "Overvalued");
            output.setUserId("user" + (i % 100));
            outputs.add(output);
        }
        
        long creationTime = System.currentTimeMillis();
        
        // Bulk save operations
        dcfInputRepository.saveAll(inputs);
        dcfOutputRepository.saveAll(outputs);
        entityManager.flush();
        
        long saveTime = System.currentTimeMillis();
        
        // Performance assertions (these are rough benchmarks)
        long totalTime = saveTime - startTime;
        assertTrue(totalTime < 30000, "Bulk operations should complete within 30 seconds, took: " + totalTime + "ms");
        
        // Verify all records were saved
        assertEquals(1000, dcfInputRepository.count());
        assertEquals(1000, dcfOutputRepository.count());
        
        System.out.println("Performance metrics:");
        System.out.println("Data creation: " + (creationTime - startTime) + "ms");
        System.out.println("Database save: " + (saveTime - creationTime) + "ms");
        System.out.println("Total time: " + totalTime + "ms");
    }

    @Test
    @DisplayName("Performance test: Complex BigDecimal queries")
    void testComplexBigDecimalQueries() {
        // Setup test data
        setupPerformanceTestData(500);
        
        long startTime = System.currentTimeMillis();
        
        // Complex query with multiple BigDecimal comparisons
        Query complexQuery = entityManager.getEntityManager().createQuery(
            "SELECT d FROM DCFOutput d WHERE " +
            "d.fairValuePerShare > :minFairValue AND " +
            "d.currentPrice < :maxCurrentPrice AND " +
            "d.enterpriseValue BETWEEN :minEV AND :maxEV AND " +
            "d.upsideDownsidePercentage > :minUpside " +
            "ORDER BY d.upsideDownsidePercentage DESC");
        
        complexQuery.setParameter("minFairValue", new BigDecimal("100.0"));
        complexQuery.setParameter("maxCurrentPrice", new BigDecimal("200.0"));
        complexQuery.setParameter("minEV", new BigDecimal("5000000.0"));
        complexQuery.setParameter("maxEV", new BigDecimal("50000000.0"));
        complexQuery.setParameter("minUpside", new BigDecimal("10.0"));
        
        @SuppressWarnings("unchecked")
        List<DCFOutput> results = complexQuery.getResultList();
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        // Performance assertion
        assertTrue(queryTime < 5000, "Complex query should complete within 5 seconds, took: " + queryTime + "ms");
        
        // Verify query correctness
        for (DCFOutput result : results) {
            assertTrue(result.getFairValuePerShare().compareTo(new BigDecimal("100.0")) > 0);
            assertTrue(result.getCurrentPrice().compareTo(new BigDecimal("200.0")) < 0);
            assertTrue(result.getEnterpriseValue().compareTo(new BigDecimal("5000000.0")) >= 0);
            assertTrue(result.getEnterpriseValue().compareTo(new BigDecimal("50000000.0")) <= 0);
            if (result.getUpsideDownsidePercentage() != null) {
                assertTrue(result.getUpsideDownsidePercentage().compareTo(new BigDecimal("10.0")) > 0);
            }
        }
        
        System.out.println("Complex query time: " + queryTime + "ms, Results: " + results.size());
    }

    @Test
    @DisplayName("Edge case: BigDecimal arithmetic overflow scenarios")
    void testBigDecimalArithmeticOverflow() {
        // Test with values that would overflow Double but should work with BigDecimal
        DCFOutput output = new DCFOutput();
        output.setTicker("OVERFLOW");
        
        // Maximum values that fit in column constraints
        BigDecimal maxFairValue = new BigDecimal("99999999999999.999999"); // Max for precision=20, scale=6
        BigDecimal maxEnterpriseValue = new BigDecimal("99999999999999999999999.99"); // Max for precision=25, scale=2
        
        output.setFairValuePerShare(maxFairValue);
        output.setCurrentPrice(maxFairValue.multiply(new BigDecimal("0.9"))); // 90% of fair value
        output.setEnterpriseValue(maxEnterpriseValue);
        output.setEquityValue(maxEnterpriseValue.multiply(new BigDecimal("0.8")));
        output.setTerminalValue(maxEnterpriseValue.multiply(new BigDecimal("0.6")));
        output.setPresentValueOfCashFlows(maxEnterpriseValue.multiply(new BigDecimal("0.4")));
        output.setSharesOutstanding(new BigDecimal("99999999999999999999")); // Max for precision=20, scale=0
        output.setValuation("Undervalued");
        
        // Should save without overflow
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify no data loss
        DCFOutput retrieved = dcfOutputRepository.findById(savedOutput.getId()).orElseThrow();
        
        assertEquals(0, maxFairValue.compareTo(retrieved.getFairValuePerShare()));
        assertEquals(0, maxEnterpriseValue.compareTo(retrieved.getEnterpriseValue()));
        
        // Verify calculated upside percentage is correct
        BigDecimal expectedUpside = new BigDecimal("11.111111"); // (1/0.9 - 1) * 100 ≈ 11.111111%
        assertEquals(0, expectedUpside.compareTo(retrieved.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Edge case: BigDecimal precision boundary conditions")
    void testBigDecimalPrecisionBoundaries() {
        // Test values at the boundary of precision limits
        DCFInput input = new DCFInput();
        input.setTicker("BOUNDARY");
        
        // Test minimum positive values
        BigDecimal minPositive = new BigDecimal("0.000001"); // Smallest positive with scale=6
        input.setDiscountRate(minPositive);
        input.setGrowthRate(minPositive);
        input.setTerminalGrowthRate(minPositive);
        
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        entityManager.clear();
        
        DCFInput retrieved = dcfInputRepository.findById(savedInput.getId()).orElseThrow();
        
        assertEquals(0, minPositive.compareTo(retrieved.getDiscountRate()));
        assertEquals(0, minPositive.compareTo(retrieved.getGrowthRate()));
        assertEquals(0, minPositive.compareTo(retrieved.getTerminalGrowthRate()));
        
        // Test maximum negative values (for growth rate which allows negative)
        BigDecimal maxNegative = new BigDecimal("-99.999999");
        retrieved.setGrowthRate(maxNegative);
        
        DCFInput updatedInput = dcfInputRepository.save(retrieved);
        entityManager.flush();
        entityManager.clear();
        
        DCFInput finalRetrieved = dcfInputRepository.findById(updatedInput.getId()).orElseThrow();
        assertEquals(0, maxNegative.compareTo(finalRetrieved.getGrowthRate()));
    }

    @Test
    @DisplayName("Edge case: BigDecimal rounding mode consistency")
    void testBigDecimalRoundingModeConsistency() {
        // Test that rounding is consistent across database operations
        DCFOutput output = new DCFOutput();
        output.setTicker("ROUNDING");
        
        // Values that require rounding
        BigDecimal fairValue = new BigDecimal("100.0");
        BigDecimal currentPrice = new BigDecimal("33.333333"); // Will create repeating decimal in division
        
        output.setFairValuePerShare(fairValue);
        output.setCurrentPrice(currentPrice);
        output.setValuation("Undervalued");
        
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        DCFOutput retrieved = dcfOutputRepository.findById(savedOutput.getId()).orElseThrow();
        
        // Verify upside calculation uses consistent rounding
        // (100 - 33.333333) / 33.333333 * 100 = 200.000000% (with HALF_UP rounding to 6 decimal places)
        BigDecimal expectedUpside = new BigDecimal("200.000000");
        assertEquals(0, expectedUpside.compareTo(retrieved.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Edge case: BigDecimal zero and negative value handling")
    void testBigDecimalZeroAndNegativeHandling() {
        // Test zero values
        DCFOutput zeroOutput = new DCFOutput();
        zeroOutput.setTicker("ZERO");
        zeroOutput.setFairValuePerShare(BigDecimal.ZERO);
        zeroOutput.setCurrentPrice(BigDecimal.ZERO);
        zeroOutput.setEnterpriseValue(BigDecimal.ZERO);
        zeroOutput.setEquityValue(BigDecimal.ZERO);
        zeroOutput.setTerminalValue(BigDecimal.ZERO);
        zeroOutput.setPresentValueOfCashFlows(BigDecimal.ZERO);
        zeroOutput.setSharesOutstanding(BigDecimal.ZERO);
        zeroOutput.setValuation("Unknown");
        
        DCFOutput savedZero = dcfOutputRepository.save(zeroOutput);
        entityManager.flush();
        
        // Verify zero values are handled correctly
        DCFOutput retrievedZero = dcfOutputRepository.findById(savedZero.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedZero.getFairValuePerShare()));
        assertEquals(0, BigDecimal.ZERO.compareTo(retrievedZero.getCurrentPrice()));
        
        // Upside should be null when current price is zero (division by zero prevention)
        assertNull(retrievedZero.getUpsideDownsidePercentage());
        
        // Test negative growth rates (allowed in DCFInput)
        DCFInput negativeInput = new DCFInput();
        negativeInput.setTicker("NEGATIVE");
        negativeInput.setDiscountRate(new BigDecimal("10.0"));
        negativeInput.setGrowthRate(new BigDecimal("-25.5")); // Negative growth
        negativeInput.setTerminalGrowthRate(new BigDecimal("2.0"));
        
        DCFInput savedNegative = dcfInputRepository.save(negativeInput);
        entityManager.flush();
        
        DCFInput retrievedNegative = dcfInputRepository.findById(savedNegative.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("-25.5").compareTo(retrievedNegative.getGrowthRate()));
    }

    @Test
    @DisplayName("Stress test: Concurrent BigDecimal operations")
    void testConcurrentBigDecimalOperations() {
        // Simulate concurrent operations by rapidly creating and querying data
        List<DCFInput> inputs = new ArrayList<>();
        
        // Create data rapidly
        for (int i = 0; i < 100; i++) {
            DCFInput input = new DCFInput();
            input.setTicker("CONCURRENT" + i);
            input.setDiscountRate(generateRandomBigDecimal(8.0, 12.0, 6));
            input.setGrowthRate(generateRandomBigDecimal(10.0, 30.0, 6));
            input.setTerminalGrowthRate(generateRandomBigDecimal(2.0, 4.0, 6));
            input.setUserId("stress-user");
            inputs.add(input);
        }
        
        // Save all at once
        List<DCFInput> savedInputs = dcfInputRepository.saveAll(inputs);
        entityManager.flush();
        
        // Perform multiple concurrent-style queries
        for (int i = 0; i < 10; i++) {
            BigDecimal threshold = new BigDecimal(String.valueOf(15.0 + i));
            List<DCFInput> results = dcfInputRepository.findByGrowthRateGreaterThan(threshold);
            
            // Verify query consistency
            for (DCFInput result : results) {
                assertTrue(result.getGrowthRate().compareTo(threshold) > 0);
            }
        }
        
        // Verify data integrity after stress operations
        assertEquals(100, dcfInputRepository.count());
        
        // Verify all saved inputs maintain precision
        for (DCFInput savedInput : savedInputs) {
            DCFInput retrieved = dcfInputRepository.findById(savedInput.getId()).orElseThrow();
            assertEquals(0, savedInput.getDiscountRate().compareTo(retrieved.getDiscountRate()));
            assertEquals(0, savedInput.getGrowthRate().compareTo(retrieved.getGrowthRate()));
            assertEquals(0, savedInput.getTerminalGrowthRate().compareTo(retrieved.getTerminalGrowthRate()));
        }
    }

    @Test
    @DisplayName("Memory test: Large BigDecimal dataset handling")
    void testLargeBigDecimalDatasetHandling() {
        // Test memory efficiency with large BigDecimal datasets
        int recordCount = 2000;
        
        // Create large dataset
        List<DCFOutput> outputs = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            DCFOutput output = new DCFOutput();
            output.setTicker("MEMORY" + String.format("%05d", i));
            
            // Use large precision values
            output.setFairValuePerShare(generateRandomBigDecimal(1000.0, 10000.0, 6));
            output.setCurrentPrice(generateRandomBigDecimal(900.0, 9000.0, 6));
            output.setEnterpriseValue(generateRandomBigDecimal(1000000000.0, 10000000000.0, 2));
            output.setEquityValue(generateRandomBigDecimal(800000000.0, 8000000000.0, 2));
            output.setTerminalValue(generateRandomBigDecimal(500000000.0, 5000000000.0, 2));
            output.setPresentValueOfCashFlows(generateRandomBigDecimal(300000000.0, 3000000000.0, 2));
            output.setSharesOutstanding(generateRandomBigDecimal(10000000.0, 100000000.0, 0));
            output.setValuation("Test");
            
            outputs.add(output);
        }
        
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Save in batches to avoid memory issues
        int batchSize = 100;
        for (int i = 0; i < outputs.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, outputs.size());
            dcfOutputRepository.saveAll(outputs.subList(i, endIndex));
            entityManager.flush();
            entityManager.clear(); // Clear persistence context to free memory
        }
        
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = endMemory - startMemory;
        
        // Verify all records were saved
        assertEquals(recordCount, dcfOutputRepository.count());
        
        // Memory usage should be reasonable (less than 100MB for this test)
        assertTrue(memoryUsed < 100 * 1024 * 1024, 
            "Memory usage should be reasonable, used: " + (memoryUsed / 1024 / 1024) + "MB");
        
        System.out.println("Memory used for " + recordCount + " records: " + (memoryUsed / 1024 / 1024) + "MB");
    }

    // Helper methods
    private void setupPerformanceTestData(int count) {
        List<DCFOutput> outputs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DCFOutput output = new DCFOutput();
            output.setTicker("PERF" + String.format("%04d", i));
            output.setFairValuePerShare(generateRandomBigDecimal(50.0, 300.0, 6));
            output.setCurrentPrice(generateRandomBigDecimal(40.0, 280.0, 6));
            output.setEnterpriseValue(generateRandomBigDecimal(1000000.0, 100000000.0, 2));
            output.setEquityValue(generateRandomBigDecimal(800000.0, 80000000.0, 2));
            output.setTerminalValue(generateRandomBigDecimal(500000.0, 50000000.0, 2));
            output.setPresentValueOfCashFlows(generateRandomBigDecimal(300000.0, 30000000.0, 2));
            output.setSharesOutstanding(generateRandomBigDecimal(1000000.0, 10000000.0, 0));
            output.setValuation(output.getFairValuePerShare().compareTo(output.getCurrentPrice()) > 0 ? "Undervalued" : "Overvalued");
            outputs.add(output);
        }
        dcfOutputRepository.saveAll(outputs);
        entityManager.flush();
    }

    private BigDecimal generateRandomBigDecimal(double min, double max, int scale) {
        double randomValue = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(randomValue).setScale(scale, RoundingMode.HALF_UP);
    }
}