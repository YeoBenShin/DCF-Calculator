package com.dcf.repository;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class BigDecimalDatabaseIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DCFInputRepository dcfInputRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        dcfInputRepository.deleteAll();
        dcfOutputRepository.deleteAll();
        financialDataRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("DCFInput BigDecimal storage and retrieval maintains precision")
    void testDCFInputBigDecimalPersistence() {
        // Create DCFInput with high precision BigDecimal values
        DCFInput input = new DCFInput();
        input.setTicker("AAPL");
        input.setDiscountRate(new BigDecimal("10.123456"));
        input.setGrowthRate(new BigDecimal("15.987654"));
        input.setTerminalGrowthRate(new BigDecimal("3.456789"));
        input.setProjectionYears(7);
        input.setUserId("test-user-123");

        // Save to database
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        entityManager.clear();

        // Retrieve from database
        Optional<DCFInput> retrievedInput = dcfInputRepository.findById(savedInput.getId());
        assertTrue(retrievedInput.isPresent());

        DCFInput retrieved = retrievedInput.get();
        
        // Verify precision is maintained
        assertEquals(0, new BigDecimal("10.123456").compareTo(retrieved.getDiscountRate()));
        assertEquals(0, new BigDecimal("15.987654").compareTo(retrieved.getGrowthRate()));
        assertEquals(0, new BigDecimal("3.456789").compareTo(retrieved.getTerminalGrowthRate()));
        assertEquals("AAPL", retrieved.getTicker());
        assertEquals(7, retrieved.getProjectionYears());
        assertEquals("test-user-123", retrieved.getUserId());
    }

    @Test
    @DisplayName("DCFOutput BigDecimal storage and retrieval maintains precision")
    void testDCFOutputBigDecimalPersistence() {
        // Create DCFOutput with high precision BigDecimal values
        DCFOutput output = new DCFOutput();
        output.setTicker("GOOGL");
        output.setFairValuePerShare(new BigDecimal("2750.123456"));
        output.setCurrentPrice(new BigDecimal("2650.987654"));
        output.setValuation("Undervalued");
        output.setTerminalValue(new BigDecimal("1234567890123.45"));
        output.setPresentValueOfCashFlows(new BigDecimal("987654321098.76"));
        output.setEnterpriseValue(new BigDecimal("2222222222222.22"));
        output.setEquityValue(new BigDecimal("1111111111111.11"));
        output.setSharesOutstanding(new BigDecimal("404040404"));
        output.setUserId("test-user-456");
        output.setDcfInputId("input-123");

        // Save to database
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();

        // Retrieve from database
        Optional<DCFOutput> retrievedOutput = dcfOutputRepository.findById(savedOutput.getId());
        assertTrue(retrievedOutput.isPresent());

        DCFOutput retrieved = retrievedOutput.get();
        
        // Verify precision is maintained for all BigDecimal fields
        assertEquals(0, new BigDecimal("2750.123456").compareTo(retrieved.getFairValuePerShare()));
        assertEquals(0, new BigDecimal("2650.987654").compareTo(retrieved.getCurrentPrice()));
        assertEquals(0, new BigDecimal("1234567890123.45").compareTo(retrieved.getTerminalValue()));
        assertEquals(0, new BigDecimal("987654321098.76").compareTo(retrieved.getPresentValueOfCashFlows()));
        assertEquals(0, new BigDecimal("2222222222222.22").compareTo(retrieved.getEnterpriseValue()));
        assertEquals(0, new BigDecimal("1111111111111.11").compareTo(retrieved.getEquityValue()));
        assertEquals(0, new BigDecimal("404040404").compareTo(retrieved.getSharesOutstanding()));
        assertEquals("GOOGL", retrieved.getTicker());
        assertEquals("Undervalued", retrieved.getValuation());
        assertEquals("test-user-456", retrieved.getUserId());
        assertEquals("input-123", retrieved.getDcfInputId());
    }

    @Test
    @DisplayName("FinancialData BigDecimal storage and retrieval maintains precision")
    void testFinancialDataBigDecimalPersistence() {
        // Create FinancialData with BigDecimal lists
        FinancialData financialData = new FinancialData();
        financialData.setTicker("MSFT");
        
        // Set BigDecimal lists with high precision values
        List<BigDecimal> revenue = List.of(
            new BigDecimal("168088000000.12"),
            new BigDecimal("143015000000.34"),
            new BigDecimal("125843000000.56")
        );
        List<BigDecimal> operatingIncome = List.of(
            new BigDecimal("69916000000.78"),
            new BigDecimal("52959000000.90"),
            new BigDecimal("42959000000.12")
        );
        List<BigDecimal> freeCashFlow = List.of(
            new BigDecimal("56118000000.34"),
            new BigDecimal("45234000000.56"),
            new BigDecimal("38260000000.78")
        );
        
        financialData.setRevenue(revenue);
        financialData.setOperatingIncome(operatingIncome);
        financialData.setFreeCashFlow(freeCashFlow);
        financialData.setDateFetched(LocalDate.now());

        // Save to database
        FinancialData savedData = financialDataRepository.save(financialData);
        entityManager.flush();
        entityManager.clear();

        // Retrieve from database
        Optional<FinancialData> retrievedData = financialDataRepository.findByTicker("MSFT");
        assertTrue(retrievedData.isPresent());

        FinancialData retrieved = retrievedData.get();
        
        // Verify BigDecimal list precision is maintained
        List<BigDecimal> retrievedRevenue = retrieved.getRevenue();
        List<BigDecimal> retrievedOperatingIncome = retrieved.getOperatingIncome();
        List<BigDecimal> retrievedFreeCashFlow = retrieved.getFreeCashFlow();
        
        assertEquals(3, retrievedRevenue.size());
        assertEquals(0, new BigDecimal("168088000000.12").compareTo(retrievedRevenue.get(0)));
        assertEquals(0, new BigDecimal("143015000000.34").compareTo(retrievedRevenue.get(1)));
        assertEquals(0, new BigDecimal("125843000000.56").compareTo(retrievedRevenue.get(2)));
        
        assertEquals(3, retrievedOperatingIncome.size());
        assertEquals(0, new BigDecimal("69916000000.78").compareTo(retrievedOperatingIncome.get(0)));
        assertEquals(0, new BigDecimal("52959000000.90").compareTo(retrievedOperatingIncome.get(1)));
        assertEquals(0, new BigDecimal("42959000000.12").compareTo(retrievedOperatingIncome.get(2)));
        
        assertEquals(3, retrievedFreeCashFlow.size());
        assertEquals(0, new BigDecimal("56118000000.34").compareTo(retrievedFreeCashFlow.get(0)));
        assertEquals(0, new BigDecimal("45234000000.56").compareTo(retrievedFreeCashFlow.get(1)));
        assertEquals(0, new BigDecimal("38260000000.78").compareTo(retrievedFreeCashFlow.get(2)));
    }

    @Test
    @DisplayName("Database queries with BigDecimal parameters work correctly")
    void testBigDecimalQueryParameters() {
        // Create test data with various growth rates
        DCFInput input1 = createTestDCFInput("AAPL", new BigDecimal("10.0"), new BigDecimal("25.5"), new BigDecimal("3.0"));
        DCFInput input2 = createTestDCFInput("GOOGL", new BigDecimal("12.0"), new BigDecimal("50.75"), new BigDecimal("2.5"));
        DCFInput input3 = createTestDCFInput("MSFT", new BigDecimal("8.0"), new BigDecimal("15.25"), new BigDecimal("3.5"));
        
        dcfInputRepository.saveAll(List.of(input1, input2, input3));
        entityManager.flush();

        // Test query with BigDecimal parameter
        BigDecimal threshold = new BigDecimal("20.0");
        List<DCFInput> highGrowthInputs = dcfInputRepository.findByGrowthRateGreaterThan(threshold);
        
        assertEquals(2, highGrowthInputs.size());
        assertTrue(highGrowthInputs.stream().allMatch(input -> input.getGrowthRate().compareTo(threshold) > 0));
        
        // Verify specific values
        assertTrue(highGrowthInputs.stream().anyMatch(input -> 
            "AAPL".equals(input.getTicker()) && new BigDecimal("25.5").compareTo(input.getGrowthRate()) == 0));
        assertTrue(highGrowthInputs.stream().anyMatch(input -> 
            "GOOGL".equals(input.getTicker()) && new BigDecimal("50.75").compareTo(input.getGrowthRate()) == 0));
    }

    @Test
    @DisplayName("BigDecimal aggregate functions maintain precision")
    void testBigDecimalAggregateFunctions() {
        // Create test DCF outputs with various fair values
        DCFOutput output1 = createTestDCFOutput("AAPL", new BigDecimal("150.123456"), new BigDecimal("140.0"));
        DCFOutput output2 = createTestDCFOutput("AAPL", new BigDecimal("155.654321"), new BigDecimal("145.0"));
        DCFOutput output3 = createTestDCFOutput("AAPL", new BigDecimal("148.987654"), new BigDecimal("142.0"));
        
        dcfOutputRepository.saveAll(List.of(output1, output2, output3));
        entityManager.flush();

        // Test average calculation
        BigDecimal averageFairValue = dcfOutputRepository.getAverageFairValueByTicker("AAPL");
        assertNotNull(averageFairValue);
        
        // Calculate expected average: (150.123456 + 155.654321 + 148.987654) / 3 = 151.588477
        BigDecimal expectedAverage = new BigDecimal("151.588477");
        assertEquals(0, expectedAverage.compareTo(averageFairValue));
    }

    @Test
    @DisplayName("BigDecimal range queries work correctly")
    void testBigDecimalRangeQueries() {
        // Create DCF outputs with various upside percentages
        DCFOutput undervalued1 = createTestDCFOutput("AAPL", new BigDecimal("150.0"), new BigDecimal("130.0")); // ~15% upside
        DCFOutput undervalued2 = createTestDCFOutput("GOOGL", new BigDecimal("2800.0"), new BigDecimal("2400.0")); // ~16.67% upside
        DCFOutput fairValue = createTestDCFOutput("MSFT", new BigDecimal("300.0"), new BigDecimal("295.0")); // ~1.69% upside
        DCFOutput overvalued = createTestDCFOutput("TSLA", new BigDecimal("200.0"), new BigDecimal("250.0")); // -20% upside
        
        dcfOutputRepository.saveAll(List.of(undervalued1, undervalued2, fairValue, overvalued));
        entityManager.flush();

        // Test finding outputs with upside above 10%
        BigDecimal upsideThreshold = new BigDecimal("10.0");
        List<DCFOutput> significantUpside = dcfOutputRepository.findWithUpsideAbove(upsideThreshold);
        
        assertEquals(2, significantUpside.size());
        assertTrue(significantUpside.stream().allMatch(output -> 
            output.getUpsideDownsidePercentage().compareTo(upsideThreshold) > 0));
        
        // Test finding undervalued stocks
        List<DCFOutput> undervaluedStocks = dcfOutputRepository.findUndervaluedStocks();
        assertEquals(3, undervaluedStocks.size()); // All except overvalued
        
        // Test finding overvalued stocks
        List<DCFOutput> overvaluedStocks = dcfOutputRepository.findOvervaluedStocks();
        assertEquals(1, overvaluedStocks.size());
        assertEquals("TSLA", overvaluedStocks.get(0).getTicker());
    }

    @Test
    @DisplayName("BigDecimal precision is maintained during complex calculations")
    void testComplexBigDecimalCalculations() {
        // Create DCFInput and perform calculations that would be stored
        DCFInput input = new DCFInput();
        input.setTicker("COMPLEX");
        input.setDiscountRate(new BigDecimal("10.123456"));
        input.setGrowthRate(new BigDecimal("15.987654"));
        input.setTerminalGrowthRate(new BigDecimal("3.456789"));
        
        DCFInput savedInput = dcfInputRepository.save(input);
        
        // Simulate complex calculation results
        DCFOutput output = new DCFOutput();
        output.setTicker("COMPLEX");
        output.setDcfInputId(savedInput.getId());
        
        // Calculate values using BigDecimal arithmetic (simulating service layer)
        BigDecimal discountRateDecimal = savedInput.getDiscountRateAsDecimal();
        BigDecimal growthRateDecimal = savedInput.getGrowthRateAsDecimal();
        
        // Simulate present value calculation: 1000 / (1 + discountRate)^5
        BigDecimal baseCashFlow = new BigDecimal("1000.0");
        BigDecimal discountFactor = BigDecimal.ONE.add(discountRateDecimal);
        BigDecimal presentValue = baseCashFlow.divide(
            discountFactor.pow(5), 10, RoundingMode.HALF_UP);
        
        output.setPresentValueOfCashFlows(presentValue);
        output.setFairValuePerShare(presentValue.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        output.setCurrentPrice(new BigDecimal("5.0"));
        output.setValuation("Undervalued");
        
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify precision is maintained
        Optional<DCFOutput> retrievedOutput = dcfOutputRepository.findById(savedOutput.getId());
        assertTrue(retrievedOutput.isPresent());
        
        DCFOutput retrieved = retrievedOutput.get();
        
        // Verify the calculated values maintain precision
        assertNotNull(retrieved.getPresentValueOfCashFlows());
        assertNotNull(retrieved.getFairValuePerShare());
        
        // The present value should be approximately 620.921323 (1000 / 1.10123456^5)
        BigDecimal expectedPV = new BigDecimal("620.9213230");
        assertEquals(0, expectedPV.compareTo(retrieved.getPresentValueOfCashFlows()));
        
        // Fair value per share should be PV / 100 = 6.209213
        BigDecimal expectedFairValue = new BigDecimal("6.209213");
        assertEquals(0, expectedFairValue.compareTo(retrieved.getFairValuePerShare()));
    }

    @Test
    @DisplayName("Database handles very large BigDecimal values correctly")
    void testVeryLargeBigDecimalValues() {
        // Test with values that would cause overflow with Double
        DCFOutput output = new DCFOutput();
        output.setTicker("LARGE");
        output.setFairValuePerShare(new BigDecimal("999999999999999.999999"));
        output.setCurrentPrice(new BigDecimal("500000000000000.000000"));
        output.setEnterpriseValue(new BigDecimal("99999999999999999999999.99"));
        output.setEquityValue(new BigDecimal("88888888888888888888888.88"));
        output.setTerminalValue(new BigDecimal("77777777777777777777777.77"));
        output.setPresentValueOfCashFlows(new BigDecimal("66666666666666666666666.66"));
        output.setSharesOutstanding(new BigDecimal("99999999999999999999"));
        output.setValuation("Undervalued");
        
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify large values are maintained
        Optional<DCFOutput> retrievedOutput = dcfOutputRepository.findById(savedOutput.getId());
        assertTrue(retrievedOutput.isPresent());
        
        DCFOutput retrieved = retrievedOutput.get();
        assertEquals(0, new BigDecimal("999999999999999.999999").compareTo(retrieved.getFairValuePerShare()));
        assertEquals(0, new BigDecimal("500000000000000.000000").compareTo(retrieved.getCurrentPrice()));
        assertEquals(0, new BigDecimal("99999999999999999999999.99").compareTo(retrieved.getEnterpriseValue()));
        assertEquals(0, new BigDecimal("88888888888888888888888.88").compareTo(retrieved.getEquityValue()));
        assertEquals(0, new BigDecimal("77777777777777777777777.77").compareTo(retrieved.getTerminalValue()));
        assertEquals(0, new BigDecimal("66666666666666666666666.66").compareTo(retrieved.getPresentValueOfCashFlows()));
        assertEquals(0, new BigDecimal("99999999999999999999").compareTo(retrieved.getSharesOutstanding()));
    }

    @Test
    @DisplayName("Database handles very small BigDecimal values correctly")
    void testVerySmallBigDecimalValues() {
        // Test with very small precision values
        DCFInput input = new DCFInput();
        input.setTicker("SMALL");
        input.setDiscountRate(new BigDecimal("0.000001"));
        input.setGrowthRate(new BigDecimal("0.000002"));
        input.setTerminalGrowthRate(new BigDecimal("0.000003"));
        
        DCFInput savedInput = dcfInputRepository.save(input);
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve and verify small values are maintained
        Optional<DCFInput> retrievedInput = dcfInputRepository.findById(savedInput.getId());
        assertTrue(retrievedInput.isPresent());
        
        DCFInput retrieved = retrievedInput.get();
        assertEquals(0, new BigDecimal("0.000001").compareTo(retrieved.getDiscountRate()));
        assertEquals(0, new BigDecimal("0.000002").compareTo(retrieved.getGrowthRate()));
        assertEquals(0, new BigDecimal("0.000003").compareTo(retrieved.getTerminalGrowthRate()));
    }

    @Test
    @DisplayName("Database transactions maintain BigDecimal consistency")
    void testBigDecimalTransactionConsistency() {
        // Create related DCFInput and DCFOutput in same transaction
        DCFInput input = createTestDCFInput("TRANS", new BigDecimal("10.5"), new BigDecimal("20.25"), new BigDecimal("3.75"));
        DCFInput savedInput = dcfInputRepository.save(input);
        
        DCFOutput output = createTestDCFOutput("TRANS", new BigDecimal("125.125"), new BigDecimal("100.875"));
        output.setDcfInputId(savedInput.getId());
        DCFOutput savedOutput = dcfOutputRepository.save(output);
        
        entityManager.flush();
        entityManager.clear();
        
        // Retrieve both and verify relationship and precision
        Optional<DCFInput> retrievedInput = dcfInputRepository.findById(savedInput.getId());
        Optional<DCFOutput> retrievedOutput = dcfOutputRepository.findByDcfInputId(savedInput.getId());
        
        assertTrue(retrievedInput.isPresent());
        assertTrue(retrievedOutput.isPresent());
        
        assertEquals(retrievedInput.get().getId(), retrievedOutput.get().getDcfInputId());
        assertEquals(0, new BigDecimal("10.5").compareTo(retrievedInput.get().getDiscountRate()));
        assertEquals(0, new BigDecimal("125.125").compareTo(retrievedOutput.get().getFairValuePerShare()));
    }

    // Helper methods
    private DCFInput createTestDCFInput(String ticker, BigDecimal discountRate, BigDecimal growthRate, BigDecimal terminalGrowthRate) {
        DCFInput input = new DCFInput();
        input.setTicker(ticker);
        input.setDiscountRate(discountRate);
        input.setGrowthRate(growthRate);
        input.setTerminalGrowthRate(terminalGrowthRate);
        input.setProjectionYears(5);
        input.setUserId("test-user");
        return input;
    }

    private DCFOutput createTestDCFOutput(String ticker, BigDecimal fairValue, BigDecimal currentPrice) {
        DCFOutput output = new DCFOutput();
        output.setTicker(ticker);
        output.setFairValuePerShare(fairValue);
        output.setCurrentPrice(currentPrice);
        output.setValuation(fairValue.compareTo(currentPrice) > 0 ? "Undervalued" : "Overvalued");
        output.setUserId("test-user");
        return output;
    }
}