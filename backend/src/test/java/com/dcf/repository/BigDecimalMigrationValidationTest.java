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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate BigDecimal migration scenarios and data integrity
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
class BigDecimalMigrationValidationTest {

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
    @DisplayName("Validate BigDecimal column precision and scale constraints")
    void testBigDecimalColumnConstraints() {
        // Test DCFInput precision and scale limits
        DCFInput input = new DCFInput();
        input.setTicker("PRECISION");
        
        // Test maximum precision for discount rate (precision=10, scale=6) within validation constraints
        input.setDiscountRate(new BigDecimal("99.999999")); // Within 100% limit
        input.setGrowthRate(new BigDecimal("999.999999")); // Within 1000% limit
        input.setTerminalGrowthRate(new BigDecimal("9.999999")); // Within 10% limit
        
        // Should save successfully
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        assertNotNull(savedInput.getId());
        
        // Test DCFOutput precision and scale limits
        DCFOutput output = new DCFOutput();
        output.setTicker("PRECISION");
        
        // Test maximum precision for fair value (precision=20, scale=6)
        output.setFairValuePerShare(new BigDecimal("99999999999999.999999")); // 14 integer + 6 decimal = 20 total
        output.setCurrentPrice(new BigDecimal("99999999999999.999999"));
        
        // Test maximum precision for enterprise value (precision=25, scale=2)
        output.setEnterpriseValue(new BigDecimal("99999999999999999999999.99")); // 23 integer + 2 decimal = 25 total
        output.setEquityValue(new BigDecimal("99999999999999999999999.99"));
        output.setTerminalValue(new BigDecimal("99999999999999999999999.99"));
        output.setPresentValueOfCashFlows(new BigDecimal("99999999999999999999999.99"));
        
        // Test shares outstanding (precision=20, scale=0)
        output.setSharesOutstanding(new BigDecimal("99999999999999999999")); // 20 digits, no decimal
        
        output.setValuation("Test");
        
        // Should save successfully
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        assertNotNull(savedOutput.getId());
    }

    @Test
    @DisplayName("Validate data conversion from Double to BigDecimal scenarios")
    void testDoubleToBigDecimalConversion() {
        // Simulate data that might have been stored as Double and converted to BigDecimal
        
        // Test conversion of typical Double values that might lose precision
        double doubleDiscountRate = 10.123456789; // Double precision limit
        double doubleGrowthRate = 15.987654321;
        double doubleTerminalRate = 3.456789012;
        
        DCFInput input = new DCFInput();
        input.setTicker("CONVERT");
        input.setDiscountRate(BigDecimal.valueOf(doubleDiscountRate));
        input.setGrowthRate(BigDecimal.valueOf(doubleGrowthRate));
        input.setTerminalGrowthRate(BigDecimal.valueOf(doubleTerminalRate));
        
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify conversion maintained reasonable precision
        DCFInput retrieved = dcfInputRepository.findById(savedInput.getId()).orElseThrow();
        
        // BigDecimal.valueOf() should maintain Double precision
        assertEquals(0, BigDecimal.valueOf(doubleDiscountRate).compareTo(retrieved.getDiscountRate()));
        assertEquals(0, BigDecimal.valueOf(doubleGrowthRate).compareTo(retrieved.getGrowthRate()));
        assertEquals(0, BigDecimal.valueOf(doubleTerminalRate).compareTo(retrieved.getTerminalGrowthRate()));
    }

    @Test
    @DisplayName("Validate BigDecimal rounding behavior in database operations")
    void testBigDecimalRoundingInDatabase() {
        // Test values that require rounding to fit column scale
        DCFInput input = new DCFInput();
        input.setTicker("ROUNDING");
        
        // Values with more decimal places than column scale (6)
        BigDecimal preciseDiscountRate = new BigDecimal("10.1234567890123456");
        BigDecimal preciseGrowthRate = new BigDecimal("15.9876543210987654");
        
        // Round to match column scale before saving
        input.setDiscountRate(preciseDiscountRate.setScale(6, RoundingMode.HALF_UP));
        input.setGrowthRate(preciseGrowthRate.setScale(6, RoundingMode.HALF_UP));
        input.setTerminalGrowthRate(new BigDecimal("3.123456"));
        
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify rounding was applied correctly
        DCFInput retrieved = dcfInputRepository.findById(savedInput.getId()).orElseThrow();
        
        assertEquals(0, new BigDecimal("10.123457").compareTo(retrieved.getDiscountRate())); // Rounded up
        assertEquals(0, new BigDecimal("15.987654").compareTo(retrieved.getGrowthRate())); // Rounded down
        assertEquals(0, new BigDecimal("3.123456").compareTo(retrieved.getTerminalGrowthRate()));
    }

    @Test
    @DisplayName("Validate BigDecimal arithmetic consistency in database queries")
    void testBigDecimalArithmeticConsistency() {
        // Create test data with precise calculations
        DCFOutput output1 = new DCFOutput();
        output1.setTicker("ARITH1");
        output1.setFairValuePerShare(new BigDecimal("100.000000"));
        output1.setCurrentPrice(new BigDecimal("90.000000"));
        output1.setValuation("Undervalued");
        
        DCFOutput output2 = new DCFOutput();
        output2.setTicker("ARITH2");
        output2.setFairValuePerShare(new BigDecimal("200.000000"));
        output2.setCurrentPrice(new BigDecimal("180.000000"));
        output2.setValuation("Undervalued");
        
        dcfOutputRepository.saveAll(List.of(output1, output2));
        entityManager.flush();
        
        // Test aggregate function precision
        BigDecimal averageFairValue = dcfOutputRepository.getAverageFairValueByTicker("ARITH1");
        assertEquals(0, new BigDecimal("100.000000").compareTo(averageFairValue));
        
        // Test arithmetic operations in queries
        Query query = entityManager.getEntityManager().createQuery(
            "SELECT d FROM DCFOutput d WHERE d.fairValuePerShare - d.currentPrice > :threshold");
        query.setParameter("threshold", new BigDecimal("15.0"));
        
        @SuppressWarnings("unchecked")
        List<DCFOutput> results = query.getResultList();
        assertEquals(1, results.size());
        assertEquals("ARITH2", results.get(0).getTicker());
    }

    @Test
    @DisplayName("Validate BigDecimal null handling in database operations")
    void testBigDecimalNullHandling() {
        // Test entities with some null BigDecimal fields
        DCFOutput output = new DCFOutput();
        output.setTicker("NULLTEST");
        output.setFairValuePerShare(new BigDecimal("150.0"));
        output.setCurrentPrice(null); // Null current price
        output.setValuation("Unknown");
        output.setTerminalValue(null);
        output.setPresentValueOfCashFlows(new BigDecimal("1000.0"));
        output.setEnterpriseValue(null);
        output.setEquityValue(new BigDecimal("2000.0"));
        output.setSharesOutstanding(null);
        
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify null handling
        DCFOutput retrieved = dcfOutputRepository.findById(savedOutput.getId()).orElseThrow();
        
        assertNotNull(retrieved.getFairValuePerShare());
        assertNull(retrieved.getCurrentPrice());
        assertNull(retrieved.getTerminalValue());
        assertNotNull(retrieved.getPresentValueOfCashFlows());
        assertNull(retrieved.getEnterpriseValue());
        assertNotNull(retrieved.getEquityValue());
        assertNull(retrieved.getSharesOutstanding());
        
        // Verify upside/downside is null when current price is null
        assertNull(retrieved.getUpsideDownsidePercentage());
    }

    @Test
    @DisplayName("Validate BigDecimal comparison operations in database queries")
    void testBigDecimalComparisonQueries() {
        // Create test data with various BigDecimal values
        DCFInput input1 = createTestInput("COMP1", "10.000000", "20.000000", "3.000000");
        DCFInput input2 = createTestInput("COMP2", "10.000001", "20.000001", "3.000001");
        DCFInput input3 = createTestInput("COMP3", "9.999999", "19.999999", "2.999999");
        
        dcfInputRepository.saveAll(List.of(input1, input2, input3));
        entityManager.flush();
        
        // Test exact equality comparison
        List<DCFInput> exactMatch = dcfInputRepository.findAll().stream()
            .filter(input -> new BigDecimal("10.000000").compareTo(input.getDiscountRate()) == 0)
            .toList();
        assertEquals(1, exactMatch.size());
        assertEquals("COMP1", exactMatch.get(0).getTicker());
        
        // Test greater than comparison
        BigDecimal threshold = new BigDecimal("20.000000");
        List<DCFInput> aboveThreshold = dcfInputRepository.findByGrowthRateGreaterThan(threshold);
        assertEquals(1, aboveThreshold.size());
        assertEquals("COMP2", aboveThreshold.get(0).getTicker());
        
        // Test range queries
        Query rangeQuery = entityManager.getEntityManager().createQuery(
            "SELECT d FROM DCFInput d WHERE d.discountRate BETWEEN :min AND :max");
        rangeQuery.setParameter("min", new BigDecimal("9.999999"));
        rangeQuery.setParameter("max", new BigDecimal("10.000000"));
        
        @SuppressWarnings("unchecked")
        List<DCFInput> rangeResults = rangeQuery.getResultList();
        assertEquals(2, rangeResults.size());
    }

    @Test
    @DisplayName("Validate BigDecimal index performance and accuracy")
    void testBigDecimalIndexPerformance() {
        // Create multiple records to test index usage
        for (int i = 0; i < 100; i++) {
            DCFInput input = new DCFInput();
            input.setTicker("PERF" + i);
            input.setDiscountRate(new BigDecimal(String.valueOf(10.0 + i * 0.1)));
            input.setGrowthRate(new BigDecimal(String.valueOf(15.0 + i * 0.2)));
            input.setTerminalGrowthRate(new BigDecimal(String.valueOf(3.0 + i * 0.01)));
            input.setUserId("user" + (i % 10)); // 10 different users
            dcfInputRepository.save(input);
        }
        entityManager.flush();
        
        // Test indexed queries (assuming indexes on ticker, userId, etc.)
        List<DCFInput> userInputs = dcfInputRepository.findByUserIdOrderByCreatedAtDesc("user5");
        assertEquals(10, userInputs.size());
        
        // Test BigDecimal range query performance
        BigDecimal minRate = new BigDecimal("12.0");
        BigDecimal maxRate = new BigDecimal("15.0");
        
        Query rangeQuery = entityManager.getEntityManager().createQuery(
            "SELECT d FROM DCFInput d WHERE d.discountRate BETWEEN :min AND :max ORDER BY d.discountRate");
        rangeQuery.setParameter("min", minRate);
        rangeQuery.setParameter("max", maxRate);
        
        @SuppressWarnings("unchecked")
        List<DCFInput> rangeResults = rangeQuery.getResultList();
        
        // Verify results are within range and ordered correctly
        assertTrue(rangeResults.size() > 0);
        for (int i = 0; i < rangeResults.size() - 1; i++) {
            assertTrue(rangeResults.get(i).getDiscountRate().compareTo(rangeResults.get(i + 1).getDiscountRate()) <= 0);
        }
    }

    @Test
    @DisplayName("Validate BigDecimal data integrity across transactions")
    void testBigDecimalTransactionIntegrity() {
        // Test that BigDecimal precision is maintained across transaction boundaries
        DCFInput input = new DCFInput();
        input.setTicker("INTEGRITY");
        input.setDiscountRate(new BigDecimal("10.123456"));
        input.setGrowthRate(new BigDecimal("15.987654"));
        input.setTerminalGrowthRate(new BigDecimal("3.456789"));
        
        // Save in one transaction
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        String inputId = savedInput.getId();
        
        // Clear persistence context to simulate new transaction
        entityManager.clear();
        
        // Retrieve in "new transaction"
        DCFInput retrieved = dcfInputRepository.findById(inputId).orElseThrow();
        
        // Verify precision maintained across transaction boundary
        assertEquals(0, new BigDecimal("10.123456").compareTo(retrieved.getDiscountRate()));
        assertEquals(0, new BigDecimal("15.987654").compareTo(retrieved.getGrowthRate()));
        assertEquals(0, new BigDecimal("3.456789").compareTo(retrieved.getTerminalGrowthRate()));
        
        // Modify and save again
        retrieved.setDiscountRate(new BigDecimal("11.654321"));
        DCFInput updated = dcfInputRepository.save(retrieved);
        entityManager.flush();
        entityManager.clear();
        
        // Verify update maintained precision
        DCFInput finalRetrieved = dcfInputRepository.findById(inputId).orElseThrow();
        assertEquals(0, new BigDecimal("11.654321").compareTo(finalRetrieved.getDiscountRate()));
        assertEquals(0, new BigDecimal("15.987654").compareTo(finalRetrieved.getGrowthRate())); // Unchanged
    }

    @Test
    @DisplayName("Validate BigDecimal scientific notation handling")
    void testBigDecimalScientificNotationHandling() {
        // Test values that might be represented in scientific notation
        DCFOutput output = new DCFOutput();
        output.setTicker("SCIENTIFIC");
        
        // Very large values that could be in scientific notation
        output.setFairValuePerShare(new BigDecimal("1.23456E+15")); // 1234560000000000
        output.setCurrentPrice(new BigDecimal("9.87654E+14")); // 987654000000000
        output.setEnterpriseValue(new BigDecimal("5.55555E+20")); // 555555000000000000000
        
        // Very small values
        output.setTerminalValue(new BigDecimal("1.11111E-5")); // 0.000011111
        
        output.setValuation("Test");
        
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify values are stored as exact decimals, not scientific notation
        DCFOutput retrieved = dcfOutputRepository.findById(savedOutput.getId()).orElseThrow();
        
        assertEquals(0, new BigDecimal("1234560000000000").compareTo(retrieved.getFairValuePerShare()));
        assertEquals(0, new BigDecimal("987654000000000").compareTo(retrieved.getCurrentPrice()));
        assertEquals(0, new BigDecimal("555555000000000000000").compareTo(retrieved.getEnterpriseValue()));
        assertEquals(0, new BigDecimal("0.000011111").compareTo(retrieved.getTerminalValue()));
    }

    // Helper method
    private DCFInput createTestInput(String ticker, String discountRate, String growthRate, String terminalRate) {
        DCFInput input = new DCFInput();
        input.setTicker(ticker);
        input.setDiscountRate(new BigDecimal(discountRate));
        input.setGrowthRate(new BigDecimal(growthRate));
        input.setTerminalGrowthRate(new BigDecimal(terminalRate));
        input.setUserId("test-user");
        return input;
    }
}