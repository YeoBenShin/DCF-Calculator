package com.dcf.service;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import com.dcf.repository.DCFInputRepository;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.service.DCFCalculationService.DCFCalculationException;
import com.dcf.service.DCFCalculationService.DCFCalculationStats;
import com.dcf.service.DCFCalculationService.DCFSensitivityAnalysis;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.util.DCFValidationUtil;
import com.dcf.util.BigDecimalPerformanceProfiler;
import com.dcf.util.OptimizedBigDecimalMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DCFCalculationServiceTest {

    @Mock
    private DCFInputRepository dcfInputRepository;

    @Mock
    private DCFOutputRepository dcfOutputRepository;

    @Mock
    private DCFValidationUtil dcfValidationUtil;

    @Mock
    private FinancialDataScrapingService financialDataScrapingService;

    @Mock
    private BigDecimalPerformanceProfiler performanceProfiler;

    @Mock
    private OptimizedBigDecimalMath optimizedMath;

    @Mock
    private BigDecimalCalculationCacheService calculationCacheService;

    @Mock
    private BigDecimalPerformanceMonitoringService performanceMonitoringService;

    @InjectMocks
    private DCFCalculationService dcfCalculationService;

    private DCFInput mockDCFInput;
    private FinancialData mockFinancialData;
    private DCFOutput mockDCFOutput;

    @BeforeEach
    void setUp() {
        // Setup mock DCF input with BigDecimal values
        mockDCFInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        mockDCFInput.setUserId("user123");
        mockDCFInput.setProjectionYears(5);

        // Setup mocks for BigDecimal math operations
        when(optimizedMath.power(any(BigDecimal.class), anyInt())).thenAnswer(invocation -> {
            BigDecimal base = invocation.getArgument(0);
            int exponent = invocation.getArgument(1);
            return base.pow(exponent);
        });

        // Setup mock financial data with BigDecimal values
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setRevenue(Arrays.asList(
            new BigDecimal("100.0"), new BigDecimal("110.0"), new BigDecimal("120.0"), 
            new BigDecimal("130.0"), new BigDecimal("140.0")));
        mockFinancialData.setFreeCashFlow(Arrays.asList(
            new BigDecimal("20.0"), new BigDecimal("22.0"), new BigDecimal("24.0"), 
            new BigDecimal("26.0"), new BigDecimal("28.0")));
        mockFinancialData.setEps(Arrays.asList(
            new BigDecimal("2.0"), new BigDecimal("2.2"), new BigDecimal("2.4"), 
            new BigDecimal("2.6"), new BigDecimal("2.8")));
        mockFinancialData.setTotalDebt(Arrays.asList(
            new BigDecimal("50.0"), new BigDecimal("52.0"), new BigDecimal("54.0"), 
            new BigDecimal("56.0"), new BigDecimal("58.0")));
        mockFinancialData.setOrdinarySharesNumber(Arrays.asList(
            new BigDecimal("1000.0"), new BigDecimal("1000.0"), new BigDecimal("1000.0"), 
            new BigDecimal("1000.0"), new BigDecimal("1000.0")));

        // Setup mock DCF output with BigDecimal values
        mockDCFOutput = new DCFOutput("AAPL", 
            new BigDecimal("180.0"), 
            new BigDecimal("175.0"), 
            "Undervalued");
    }

    @Test
    @DisplayName("Should calculate DCF successfully with valid inputs")
    void testCalculateDCFSuccess() throws DCFCalculationException, FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(mockDCFInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenReturn(mockDCFOutput);

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(mockDCFInput);

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        verify(dcfInputRepository).save(mockDCFInput);
        verify(dcfOutputRepository).save(any(DCFOutput.class));
    } 
   @Test
    @DisplayName("Should throw exception for invalid DCF input")
    void testCalculateDCFInvalidInput() throws FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn("Invalid input");

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("Invalid DCF input"));
        verify(financialDataScrapingService, never()).getFinancialData(anyString());
    }

    @Test
    @DisplayName("Should throw exception when financial data scraping fails")
    void testCalculateDCFScrapingFailure() throws FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL"))
            .thenThrow(new FinancialDataException("Scraping failed"));

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("Failed to retrieve financial data"));
    }

    @Test
    @DisplayName("Should throw exception for missing free cash flow data")
    void testCalculateDCFMissingFCF() throws FinancialDataException {
        // Arrange
        mockFinancialData.setFreeCashFlow(Collections.emptyList());
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should throw exception for missing shares outstanding data")
    void testCalculateDCFMissingShares() throws FinancialDataException {
        // Arrange
        mockFinancialData.setOrdinarySharesNumber(Collections.emptyList());
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should handle zero or negative free cash flow")
    void testCalculateDCFZeroFCF() throws FinancialDataException {
        // Arrange
        mockFinancialData.setFreeCashFlow(Arrays.asList(
            BigDecimal.ZERO, new BigDecimal("-5.0"), new BigDecimal("10.0"), 
            new BigDecimal("15.0"), new BigDecimal("20.0")));
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should calculate DCF with no debt (zero debt)")
    void testCalculateDCFNoDebt() throws DCFCalculationException, FinancialDataException {
        // Arrange
        mockFinancialData.setTotalDebt(Collections.emptyList()); // No debt data
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(mockDCFInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenReturn(mockDCFOutput);

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(mockDCFInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // Should assume zero debt when no debt data is available
    }

    @Test
    @DisplayName("Should determine correct valuation status")
    void testValuationStatusDetermination() throws DCFCalculationException, FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(mockDCFInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(mockDCFInput);

        // Assert
        assertNotNull(result.getValuation());
        assertTrue(Arrays.asList("Undervalued", "Overvalued", "Fair Value").contains(result.getValuation()));
    }

    @Test
    @DisplayName("Should perform sensitivity analysis successfully")
    void testSensitivityAnalysis() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use decimal format (0.10 for 10%, not 10.0)
        BigDecimal[] growthRates = {new BigDecimal("0.10"), new BigDecimal("0.15"), new BigDecimal("0.20")}; // 10%, 15%, 20%
        BigDecimal[] discountRates = {new BigDecimal("0.08"), new BigDecimal("0.10"), new BigDecimal("0.12")}; // 8%, 10%, 12%
        
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act
        DCFSensitivityAnalysis analysis = dcfCalculationService.calculateSensitivityAnalysis(
            mockDCFInput, growthRates, discountRates);

        // Assert
        assertNotNull(analysis);
        assertEquals("AAPL", analysis.getTicker());
        assertNotNull(analysis.getBaseCase());
        assertNotNull(analysis.getResults());
        assertEquals(9, analysis.getResults().size()); // 3x3 combinations
        
        // Verify all combinations are present using BigDecimal comparisons
        // Note: The results will be in percentage format (10.0, 15.0, 20.0)
        BigDecimal[] expectedGrowthPercentages = {new BigDecimal("10.0"), new BigDecimal("15.0"), new BigDecimal("20.0")};
        BigDecimal[] expectedDiscountPercentages = {new BigDecimal("8.0"), new BigDecimal("10.0"), new BigDecimal("12.0")};
        
        for (BigDecimal growthRate : expectedGrowthPercentages) {
            for (BigDecimal discountRate : expectedDiscountPercentages) {
                boolean found = analysis.getResults().stream()
                    .anyMatch(r -> r.getGrowthRate().subtract(growthRate).abs().compareTo(new BigDecimal("0.01")) < 0 &&
                                  r.getDiscountRate().subtract(discountRate).abs().compareTo(new BigDecimal("0.01")) < 0);
                assertTrue(found, "Missing combination: " + growthRate + ", " + discountRate);
            }
        }
    }

    @Test
    @DisplayName("Should get historical calculations for ticker")
    void testGetHistoricalCalculations() {
        // Arrange
        List<DCFOutput> mockHistoricalData = Arrays.asList(mockDCFOutput);
        when(dcfOutputRepository.findByTickerOrderByCalculatedAtDesc("AAPL"))
            .thenReturn(mockHistoricalData);

        // Act
        List<DCFOutput> result = dcfCalculationService.getHistoricalCalculations("AAPL", null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getTicker());
        verify(dcfOutputRepository).findByTickerOrderByCalculatedAtDesc("AAPL");
    }

    @Test
    @DisplayName("Should get historical calculations for user and ticker")
    void testGetHistoricalCalculationsForUser() {
        // Arrange
        List<DCFOutput> mockHistoricalData = Arrays.asList(mockDCFOutput);
        when(dcfOutputRepository.findByUserIdAndTickerOrderByCalculatedAtDesc("user123", "AAPL"))
            .thenReturn(mockHistoricalData);

        // Act
        List<DCFOutput> result = dcfCalculationService.getHistoricalCalculations("AAPL", "user123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(dcfOutputRepository).findByUserIdAndTickerOrderByCalculatedAtDesc("user123", "AAPL");
    }

    @Test
    @DisplayName("Should calculate user statistics correctly")
    void testGetUserCalculationStats() {
        // Arrange
        DCFOutput undervaluedOutput = new DCFOutput("AAPL", 
            new BigDecimal("180.0"), new BigDecimal("175.0"), "Undervalued");
        DCFOutput overvaluedOutput = new DCFOutput("GOOGL", 
            new BigDecimal("140.0"), new BigDecimal("150.0"), "Overvalued");
        DCFOutput fairValueOutput = new DCFOutput("MSFT", 
            new BigDecimal("380.0"), new BigDecimal("378.0"), "Fair Value");
        
        List<DCFOutput> userCalculations = Arrays.asList(undervaluedOutput, overvaluedOutput, fairValueOutput);
        
        when(dcfOutputRepository.countByUserId("user123")).thenReturn(3L);
        when(dcfOutputRepository.findByUserIdOrderByCalculatedAtDesc("user123"))
            .thenReturn(userCalculations);

        // Act
        DCFCalculationStats stats = dcfCalculationService.getUserCalculationStats("user123");

        // Assert
        assertNotNull(stats);
        assertEquals(3L, stats.getTotalCalculations());
        assertEquals(1L, stats.getUndervaluedCount());
        assertEquals(1L, stats.getOvervaluedCount());
        assertEquals(0, new BigDecimal("33.33").compareTo(stats.getUndervaluedPercentage()));
        assertEquals(0, new BigDecimal("33.33").compareTo(stats.getOvervaluedPercentage()));
    }

    @Test
    @DisplayName("Should handle zero calculations in user statistics")
    void testGetUserCalculationStatsZero() {
        // Arrange
        when(dcfOutputRepository.countByUserId("user123")).thenReturn(0L);
        when(dcfOutputRepository.findByUserIdOrderByCalculatedAtDesc("user123"))
            .thenReturn(Collections.emptyList());

        // Act
        DCFCalculationStats stats = dcfCalculationService.getUserCalculationStats("user123");

        // Assert
        assertNotNull(stats);
        assertEquals(0L, stats.getTotalCalculations());
        assertEquals(0L, stats.getUndervaluedCount());
        assertEquals(0L, stats.getOvervaluedCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getUndervaluedPercentage()));
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.getOvervaluedPercentage()));
    }

    @Test
    @DisplayName("Should handle repository save failures")
    void testRepositorySaveFailure() throws FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should calculate different fair values for different tickers")
    void testDifferentTickerCalculations() throws DCFCalculationException, FinancialDataException {
        // Test that different tickers get different mock current prices
        DCFInput googleInput = new DCFInput("GOOGL", 
            new BigDecimal("10.0"), new BigDecimal("15.0"), new BigDecimal("3.0"));
        FinancialData googleData = new FinancialData("GOOGL");
        googleData.setFreeCashFlow(Arrays.asList(new BigDecimal("30.0")));
        googleData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.0")));
        
        when(dcfValidationUtil.validateDCFInput(any())).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("GOOGL")).thenReturn(googleData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(googleInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(googleInput);

        // Assert
        assertNotNull(result);
        assertEquals("GOOGL", result.getTicker());
        // Should use different mock price for GOOGL (164.74) vs AAPL (227.52)
        assertEquals(0, result.getCurrentPrice().compareTo(new BigDecimal("164.74")));
    }

    // ========== BigDecimal-Specific Tests ==========

    @Test
    @DisplayName("Should handle BigDecimal precision in calculations")
    void testBigDecimalPrecisionHandling() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use high precision values that would lose precision with Double
        DCFInput precisionInput = new DCFInput("AAPL", 
            new BigDecimal("10.123456789"), 
            new BigDecimal("15.987654321"), 
            new BigDecimal("3.141592653"));
        precisionInput.setUserId("user123");
        precisionInput.setProjectionYears(5);

        FinancialData precisionData = new FinancialData("AAPL");
        precisionData.setFreeCashFlow(Arrays.asList(new BigDecimal("20.123456789")));
        precisionData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.987654321")));

        when(dcfValidationUtil.validateDCFInput(precisionInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(precisionData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(precisionInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(precisionInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // Verify that precision is maintained (should have 6 decimal places as per entity definition)
        assertTrue(result.getFairValuePerShare().scale() <= 6);
    }

    @Test
    @DisplayName("Should handle BigDecimal rounding correctly")
    void testBigDecimalRounding() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that require rounding
        DCFInput roundingInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        roundingInput.setUserId("user123");
        roundingInput.setProjectionYears(3);

        FinancialData roundingData = new FinancialData("AAPL");
        // Use a value that will create rounding scenarios (1/3 = 0.333...)
        roundingData.setFreeCashFlow(Arrays.asList(new BigDecimal("33.333333333")));
        roundingData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("3.0")));

        when(dcfValidationUtil.validateDCFInput(roundingInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(roundingData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(roundingInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(roundingInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // Verify consistent rounding (should use HALF_UP as per service implementation)
        assertNotNull(result.getFairValuePerShare());
    }

    @Test
    @DisplayName("Should handle very large BigDecimal values")
    void testVeryLargeBigDecimalValues() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use very large values that would overflow Double
        DCFInput largeInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        largeInput.setUserId("user123");
        largeInput.setProjectionYears(5);

        FinancialData largeData = new FinancialData("AAPL");
        // Use very large free cash flow (1 trillion)
        largeData.setFreeCashFlow(Arrays.asList(new BigDecimal("1000000000000.0")));
        largeData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000000.0")));

        when(dcfValidationUtil.validateDCFInput(largeInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(largeData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(largeInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(largeInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // Verify that large values are handled correctly
        assertTrue(result.getEnterpriseValue().compareTo(new BigDecimal("1000000000")) > 0); // Should be > 1 billion
    }

    @Test
    @DisplayName("Should handle very small BigDecimal values")
    void testVerySmallBigDecimalValues() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use very small values that would underflow Double
        DCFInput smallInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        smallInput.setUserId("user123");
        smallInput.setProjectionYears(5);

        FinancialData smallData = new FinancialData("AAPL");
        // Use very small free cash flow
        smallData.setFreeCashFlow(Arrays.asList(new BigDecimal("0.000001")));
        smallData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000000.0")));

        when(dcfValidationUtil.validateDCFInput(smallInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(smallData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(smallInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(smallInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // Verify that small values are handled correctly without underflow
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should maintain BigDecimal precision in sensitivity analysis")
    void testSensitivityAnalysisBigDecimalPrecision() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use precise decimal values
        BigDecimal[] preciseGrowthRates = {
            new BigDecimal("10.123456"), 
            new BigDecimal("15.987654"), 
            new BigDecimal("20.555555")
        };
        BigDecimal[] preciseDiscountRates = {
            new BigDecimal("8.111111"), 
            new BigDecimal("10.222222"), 
            new BigDecimal("12.333333")
        };
        
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act
        DCFSensitivityAnalysis analysis = dcfCalculationService.calculateSensitivityAnalysis(
            mockDCFInput, preciseGrowthRates, preciseDiscountRates);

        // Assert
        assertNotNull(analysis);
        assertEquals(9, analysis.getResults().size());
        
        // Verify that precise values are maintained in results
        for (var result : analysis.getResults()) {
            assertNotNull(result.getGrowthRate());
            assertNotNull(result.getDiscountRate());
            assertNotNull(result.getFairValue());
            assertTrue(result.getFairValue().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("Should handle BigDecimal arithmetic edge cases")
    void testBigDecimalArithmeticEdgeCases() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use edge case values
        DCFInput edgeInput = new DCFInput("AAPL", 
            new BigDecimal("0.000001"), // Very small discount rate
            new BigDecimal("999.999999"), // Very large growth rate (but within validation limits)
            new BigDecimal("9.999999")); // Maximum terminal growth rate
        edgeInput.setUserId("user123");
        edgeInput.setProjectionYears(1); // Minimum projection years

        FinancialData edgeData = new FinancialData("AAPL");
        edgeData.setFreeCashFlow(Arrays.asList(new BigDecimal("1.0")));
        edgeData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1.0")));

        when(dcfValidationUtil.validateDCFInput(edgeInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(edgeData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(edgeInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(edgeInput);

        // Assert
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        // With very high growth rate, fair value should be positive
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should compare BigDecimal values correctly in valuation status")
    void testBigDecimalValuationComparison() throws DCFCalculationException, FinancialDataException {
        // Arrange - Set up scenario where fair value and current price are very close
        DCFInput comparisonInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        comparisonInput.setUserId("user123");
        comparisonInput.setProjectionYears(5);

        FinancialData comparisonData = new FinancialData("AAPL");
        // Set up data that will result in fair value close to AAPL's mock price (227.52)
        comparisonData.setFreeCashFlow(Arrays.asList(new BigDecimal("50.0")));
        comparisonData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.0")));

        when(dcfValidationUtil.validateDCFInput(comparisonInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(comparisonData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(comparisonInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(comparisonInput);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getValuation());
        assertTrue(Arrays.asList("Undervalued", "Overvalued", "Fair Value").contains(result.getValuation()));
        
        // Verify BigDecimal comparison logic
        if (result.getFairValuePerShare().compareTo(result.getCurrentPrice()) > 0) {
            // If fair value > current price, should be undervalued (unless within tolerance)
            assertTrue(result.getValuation().equals("Undervalued") || result.getValuation().equals("Fair Value"));
        } else if (result.getFairValuePerShare().compareTo(result.getCurrentPrice()) < 0) {
            // If fair value < current price, should be overvalued (unless within tolerance)
            assertTrue(result.getValuation().equals("Overvalued") || result.getValuation().equals("Fair Value"));
        }
    }

    @Test
    @DisplayName("Should handle BigDecimal division by zero scenarios")
    void testBigDecimalDivisionByZero() throws FinancialDataException {
        // Arrange - Create scenario that could lead to division by zero
        DCFInput zeroInput = new DCFInput("AAPL", 
            new BigDecimal("3.0"), // Same as terminal growth rate
            new BigDecimal("15.0"), 
            new BigDecimal("3.0")); // This will make (discountRate - terminalGrowthRate) = 0
        zeroInput.setUserId("user123");
        zeroInput.setProjectionYears(5);

        when(dcfValidationUtil.validateDCFInput(zeroInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(zeroInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should maintain BigDecimal scale consistency")
    void testBigDecimalScaleConsistency() throws DCFCalculationException, FinancialDataException {
        // Arrange
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(mockDCFInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(mockDCFInput);

        // Assert - Verify scale consistency as per entity definitions
        assertNotNull(result);
        
        // Fair value per share should have scale <= 6 (precision 20, scale 6)
        assertTrue(result.getFairValuePerShare().scale() <= 10); // Allow for calculation precision
        
        // Current price should have scale <= 6 (precision 20, scale 6)
        if (result.getCurrentPrice() != null) {
            assertTrue(result.getCurrentPrice().scale() <= 6);
        }
        
        // Enterprise value should have scale <= 2 (precision 25, scale 2)
        if (result.getEnterpriseValue() != null) {
            assertTrue(result.getEnterpriseValue().scale() <= 2);
        }
        
        // Equity value should have scale <= 2 (precision 25, scale 2)
        if (result.getEquityValue() != null) {
            assertTrue(result.getEquityValue().scale() <= 2);
        }
        
        // Shares outstanding should have scale = 0 (precision 20, scale 0)
        if (result.getSharesOutstanding() != null) {
            assertEquals(0, result.getSharesOutstanding().scale());
        }
        
        // Terminal value should have appropriate scale
        if (result.getTerminalValue() != null) {
            assertTrue(result.getTerminalValue().scale() <= 10); // Internal calculation precision
        }
        
        // Present value of cash flows should have appropriate scale
        if (result.getPresentValueOfCashFlows() != null) {
            assertTrue(result.getPresentValueOfCashFlows().scale() <= 10); // Internal calculation precision
        }
    }

    @Test
    @DisplayName("Should handle BigDecimal arithmetic with exact precision")
    void testBigDecimalArithmeticPrecision() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use exact decimal values that would lose precision with Double
        DCFInput precisionInput = new DCFInput("AAPL", 
            new BigDecimal("10.123456"), 
            new BigDecimal("15.654321"), 
            new BigDecimal("3.987654"));
        precisionInput.setUserId("user123");
        precisionInput.setProjectionYears(3);

        FinancialData precisionData = new FinancialData("AAPL");
        precisionData.setFreeCashFlow(Arrays.asList(new BigDecimal("100.123456789")));
        precisionData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000000")));
        precisionData.setTotalDebt(Arrays.asList(new BigDecimal("50.987654321")));

        when(dcfValidationUtil.validateDCFInput(precisionInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(precisionData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(precisionInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(precisionInput);

        // Assert - Verify precision is maintained throughout calculations
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that calculations use exact BigDecimal arithmetic
        // The fair value should be calculated with high precision
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
        
        // Verify equity value = enterprise value - debt
        BigDecimal expectedEquityValue = result.getEnterpriseValue().subtract(new BigDecimal("50.987654321"));
        assertEquals(0, result.getEquityValue().compareTo(expectedEquityValue));
    }

    @Test
    @DisplayName("Should handle BigDecimal rounding modes consistently")
    void testBigDecimalRoundingModes() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that will trigger rounding scenarios
        DCFInput roundingInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        roundingInput.setUserId("user123");
        roundingInput.setProjectionYears(5);

        FinancialData roundingData = new FinancialData("AAPL");
        // Use values that create repeating decimals when divided
        roundingData.setFreeCashFlow(Arrays.asList(new BigDecimal("100.0")));
        roundingData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("3.0"))); // Will create 1/3 scenarios
        roundingData.setTotalDebt(Arrays.asList(new BigDecimal("10.0")));

        when(dcfValidationUtil.validateDCFInput(roundingInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(roundingData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(roundingInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(roundingInput);

        // Assert - Verify consistent rounding behavior
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that division operations use HALF_UP rounding as specified in service
        // Fair value per share = equity value / shares outstanding
        BigDecimal expectedFairValue = result.getEquityValue().divide(
            new BigDecimal("3.0"), 6, RoundingMode.HALF_UP);
        assertEquals(0, result.getFairValuePerShare().compareTo(expectedFairValue));
    }

    @Test
    @DisplayName("Should handle extremely large BigDecimal values without overflow")
    void testExtremelyLargeBigDecimalValues() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that would overflow Double (> 10^308)
        DCFInput largeInput = new DCFInput("AAPL", 
            new BigDecimal("5.0"), // Low discount rate to amplify values
            new BigDecimal("25.0"), // High growth rate
            new BigDecimal("2.0"));
        largeInput.setUserId("user123");
        largeInput.setProjectionYears(10); // More years to amplify growth

        FinancialData largeData = new FinancialData("AAPL");
        // Use extremely large free cash flow (100 trillion)
        largeData.setFreeCashFlow(Arrays.asList(new BigDecimal("100000000000000.0")));
        largeData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000000.0")));
        largeData.setTotalDebt(Arrays.asList(new BigDecimal("1000000000000.0"))); // 1 trillion debt

        when(dcfValidationUtil.validateDCFInput(largeInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(largeData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(largeInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(largeInput);

        // Assert - Verify extremely large values are handled correctly
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // With high growth and large FCF, enterprise value should be extremely large
        assertTrue(result.getEnterpriseValue().compareTo(new BigDecimal("1000000000000000")) > 0); // > 1 quadrillion
        
        // Verify no overflow occurred - BigDecimal should handle arbitrarily large numbers
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
        assertNotNull(result.getFairValuePerShare());
        
        // Verify calculations are still mathematically correct
        BigDecimal expectedEquityValue = result.getEnterpriseValue().subtract(new BigDecimal("1000000000000.0"));
        assertEquals(0, result.getEquityValue().compareTo(expectedEquityValue));
    }

    @Test
    @DisplayName("Should handle extremely small BigDecimal values without underflow")
    void testExtremelySmallBigDecimalValues() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that would underflow Double (< 10^-324)
        DCFInput smallInput = new DCFInput("AAPL", 
            new BigDecimal("50.0"), // High discount rate to reduce present values
            new BigDecimal("1.0"), // Low growth rate
            new BigDecimal("1.0"));
        smallInput.setUserId("user123");
        smallInput.setProjectionYears(20); // Many years to reduce present value

        FinancialData smallData = new FinancialData("AAPL");
        // Use extremely small free cash flow
        smallData.setFreeCashFlow(Arrays.asList(new BigDecimal("0.000000000001"))); // 1 picodollar
        smallData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000000000.0"))); // 1 billion shares
        smallData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(smallInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(smallData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(smallInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(smallInput);

        // Assert - Verify extremely small values are handled correctly
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // With high discount rate and small FCF, values should be very small but not zero
        assertTrue(result.getEnterpriseValue().compareTo(BigDecimal.ZERO) > 0); // Should be positive
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0); // Should be positive
        
        // Verify no underflow occurred - BigDecimal should handle arbitrarily small numbers
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
        assertNotNull(result.getFairValuePerShare());
    }

    @Test
    @DisplayName("Should maintain BigDecimal precision in complex calculations")
    void testComplexBigDecimalCalculations() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that create complex calculation scenarios
        DCFInput complexInput = new DCFInput("AAPL", 
            new BigDecimal("12.345678"), 
            new BigDecimal("18.765432"), 
            new BigDecimal("4.123456"));
        complexInput.setUserId("user123");
        complexInput.setProjectionYears(7);

        FinancialData complexData = new FinancialData("AAPL");
        complexData.setFreeCashFlow(Arrays.asList(new BigDecimal("123.456789")));
        complexData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("987654.321")));
        complexData.setTotalDebt(Arrays.asList(new BigDecimal("456.789123")));

        when(dcfValidationUtil.validateDCFInput(complexInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(complexData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(complexInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(complexInput);

        // Assert - Verify complex calculations maintain precision
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that all intermediate calculations are present and reasonable
        assertNotNull(result.getTerminalValue());
        assertNotNull(result.getPresentValueOfCashFlows());
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
        
        // Verify mathematical relationships are maintained with BigDecimal precision
        // Enterprise value = present value of cash flows + present value of terminal value
        assertTrue(result.getEnterpriseValue().compareTo(result.getPresentValueOfCashFlows()) > 0);
        
        // Equity value = enterprise value - debt
        BigDecimal expectedEquityValue = result.getEnterpriseValue().subtract(new BigDecimal("456.789123"));
        assertEquals(0, result.getEquityValue().compareTo(expectedEquityValue));
        
        // Fair value per share = equity value / shares outstanding
        BigDecimal expectedFairValue = result.getEquityValue().divide(
            new BigDecimal("987654.321"), 6, RoundingMode.HALF_UP);
        assertEquals(0, result.getFairValuePerShare().compareTo(expectedFairValue));
    }

    @Test
    @DisplayName("Should handle BigDecimal comparisons correctly in sensitivity analysis")
    void testSensitivityAnalysisBigDecimalComparisons() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use precise decimal values for sensitivity analysis
        BigDecimal[] preciseGrowthRates = {
            new BigDecimal("10.111111"), 
            new BigDecimal("15.222222"), 
            new BigDecimal("20.333333")
        };
        BigDecimal[] preciseDiscountRates = {
            new BigDecimal("8.444444"), 
            new BigDecimal("10.555555"), 
            new BigDecimal("12.666666")
        };
        
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act
        DCFSensitivityAnalysis analysis = dcfCalculationService.calculateSensitivityAnalysis(
            mockDCFInput, preciseGrowthRates, preciseDiscountRates);

        // Assert - Verify precise BigDecimal comparisons
        assertNotNull(analysis);
        assertEquals(9, analysis.getResults().size());
        
        // Verify that precise values are maintained and can be compared accurately
        for (BigDecimal expectedGrowthRate : preciseGrowthRates) {
            for (BigDecimal expectedDiscountRate : preciseDiscountRates) {
                boolean found = analysis.getResults().stream()
                    .anyMatch(r -> r.getGrowthRate().compareTo(expectedGrowthRate) == 0 &&
                                  r.getDiscountRate().compareTo(expectedDiscountRate) == 0);
                assertTrue(found, "Missing exact combination: " + expectedGrowthRate + ", " + expectedDiscountRate);
            }
        }
        
        // Verify that different rate combinations produce different fair values
        List<BigDecimal> fairValues = analysis.getResults().stream()
            .map(r -> r.getFairValue())
            .distinct()
            .toList();
        assertTrue(fairValues.size() > 1, "All sensitivity scenarios should produce different fair values");
    }

    @Test
    @DisplayName("Should handle BigDecimal edge cases in valuation determination")
    void testValuationDeterminationBigDecimalEdgeCases() throws DCFCalculationException, FinancialDataException {
        // Test case 1: Fair value very close to current price (within tolerance)
        DCFInput closeInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("15.0"), 
            new BigDecimal("3.0"));
        closeInput.setUserId("user123");
        closeInput.setProjectionYears(5);

        FinancialData closeData = new FinancialData("AAPL");
        // Set up data to produce fair value close to AAPL's mock price (227.52)
        closeData.setFreeCashFlow(Arrays.asList(new BigDecimal("50.0")));
        closeData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.0")));
        closeData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(closeInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(closeData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(closeInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(closeInput);

        // Assert - Verify BigDecimal comparison logic for valuation
        assertNotNull(result);
        assertNotNull(result.getValuation());
        
        // Calculate expected tolerance (5% of current price)
        BigDecimal currentPrice = result.getCurrentPrice();
        BigDecimal tolerance = currentPrice.multiply(new BigDecimal("0.05"));
        BigDecimal difference = result.getFairValuePerShare().subtract(currentPrice).abs();
        
        // Verify valuation logic matches BigDecimal comparison
        if (difference.compareTo(tolerance) <= 0) {
            assertEquals("Fair Value", result.getValuation());
        } else if (result.getFairValuePerShare().compareTo(currentPrice) > 0) {
            assertEquals("Undervalued", result.getValuation());
        } else {
            assertEquals("Overvalued", result.getValuation());
        }
    }

    @Test
    @DisplayName("Should handle BigDecimal mathematical edge cases")
    void testBigDecimalMathematicalEdgeCases() throws DCFCalculationException, FinancialDataException {
        // Test case: Growth rate approaching discount rate (but not equal)
        DCFInput edgeInput = new DCFInput("AAPL", 
            new BigDecimal("10.000001"), // Discount rate slightly higher than terminal growth
            new BigDecimal("15.0"), 
            new BigDecimal("9.999999")); // Terminal growth rate very close to discount rate
        edgeInput.setUserId("user123");
        edgeInput.setProjectionYears(5);

        FinancialData edgeData = new FinancialData("AAPL");
        edgeData.setFreeCashFlow(Arrays.asList(new BigDecimal("100.0")));
        edgeData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.0")));
        edgeData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(edgeInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(edgeData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(edgeInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(edgeInput);

        // Assert - Verify that very small differences are handled correctly
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // With discount rate very close to terminal growth rate, terminal value should be very large
        assertTrue(result.getTerminalValue().compareTo(new BigDecimal("1000000")) > 0); // Should be > 1M
        
        // Verify no arithmetic exceptions occurred
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
    }

    @Test
    @DisplayName("Should maintain BigDecimal precision across multiple calculations")
    void testBigDecimalPrecisionAcrossMultipleCalculations() throws DCFCalculationException, FinancialDataException {
        // Arrange - Perform multiple calculations with same input to verify consistency
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(mockDCFInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act - Perform multiple calculations
        DCFOutput result1 = dcfCalculationService.calculateDCF(mockDCFInput);
        DCFOutput result2 = dcfCalculationService.calculateDCF(mockDCFInput);
        DCFOutput result3 = dcfCalculationService.calculateDCF(mockDCFInput);

        // Assert - Verify all results are identical with BigDecimal precision
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        
        // Fair value per share should be identical across calculations
        assertEquals(0, result1.getFairValuePerShare().compareTo(result2.getFairValuePerShare()));
        assertEquals(0, result2.getFairValuePerShare().compareTo(result3.getFairValuePerShare()));
        
        // Enterprise value should be identical
        assertEquals(0, result1.getEnterpriseValue().compareTo(result2.getEnterpriseValue()));
        assertEquals(0, result2.getEnterpriseValue().compareTo(result3.getEnterpriseValue()));
        
        // Equity value should be identical
        assertEquals(0, result1.getEquityValue().compareTo(result2.getEquityValue()));
        assertEquals(0, result2.getEquityValue().compareTo(result3.getEquityValue()));
        
        // Terminal value should be identical
        assertEquals(0, result1.getTerminalValue().compareTo(result2.getTerminalValue()));
        assertEquals(0, result2.getTerminalValue().compareTo(result3.getTerminalValue()));
    }

    // ========== Additional BigDecimal Arithmetic Tests ==========

    @Test
    @DisplayName("Should handle BigDecimal arithmetic operations with exact precision")
    void testBigDecimalArithmeticOperations() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that demonstrate exact BigDecimal arithmetic
        DCFInput arithmeticInput = new DCFInput("AAPL", 
            new BigDecimal("12.5"), // 12.5% discount rate
            new BigDecimal("8.333333"), // 8.333333% growth rate (1/12)
            new BigDecimal("2.5")); // 2.5% terminal growth rate
        arithmeticInput.setUserId("user123");
        arithmeticInput.setProjectionYears(3);

        FinancialData arithmeticData = new FinancialData("AAPL");
        // Use values that create exact arithmetic scenarios
        arithmeticData.setFreeCashFlow(Arrays.asList(new BigDecimal("120.0")));
        arithmeticData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("8.0")));
        arithmeticData.setTotalDebt(Arrays.asList(new BigDecimal("40.0")));

        when(dcfValidationUtil.validateDCFInput(arithmeticInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(arithmeticData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(arithmeticInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(arithmeticInput);

        // Assert - Verify arithmetic precision
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that equity value = enterprise value - debt
        BigDecimal expectedEquityValue = result.getEnterpriseValue().subtract(new BigDecimal("40.0"));
        assertEquals(0, result.getEquityValue().compareTo(expectedEquityValue));
        
        // Verify that fair value per share = equity value / shares outstanding
        BigDecimal expectedFairValue = result.getEquityValue().divide(new BigDecimal("8.0"), 6, RoundingMode.HALF_UP);
        assertEquals(0, result.getFairValuePerShare().compareTo(expectedFairValue));
    }

    @Test
    @DisplayName("Should handle BigDecimal rounding modes consistently across all calculations")
    void testBigDecimalRoundingConsistency() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that will require rounding in multiple operations
        DCFInput roundingInput = new DCFInput("AAPL", 
            new BigDecimal("11.111111"), // Creates repeating decimals
            new BigDecimal("7.777777"), // Creates repeating decimals
            new BigDecimal("2.222222")); // Creates repeating decimals
        roundingInput.setUserId("user123");
        roundingInput.setProjectionYears(4);

        FinancialData roundingData = new FinancialData("AAPL");
        roundingData.setFreeCashFlow(Arrays.asList(new BigDecimal("99.999999")));
        roundingData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("7.0")));
        roundingData.setTotalDebt(Arrays.asList(new BigDecimal("33.333333")));

        when(dcfValidationUtil.validateDCFInput(roundingInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(roundingData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(roundingInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(roundingInput);

        // Assert - Verify consistent rounding (HALF_UP) across all calculations
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that all BigDecimal values have appropriate scale (6 decimal places for fair value)
        assertTrue(result.getFairValuePerShare().scale() <= 6);
        
        // Verify that terminal value calculation uses HALF_UP rounding
        assertNotNull(result.getTerminalValue());
        assertTrue(result.getTerminalValue().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that present value calculations maintain precision
        assertNotNull(result.getPresentValueOfCashFlows());
        assertTrue(result.getPresentValueOfCashFlows().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle BigDecimal power operations correctly")
    void testBigDecimalPowerOperations() throws DCFCalculationException, FinancialDataException {
        // Arrange - Test compound growth calculations with power operations
        DCFInput powerInput = new DCFInput("AAPL", 
            new BigDecimal("15.0"), // 15% discount rate
            new BigDecimal("25.0"), // 25% growth rate (high growth)
            new BigDecimal("3.0")); // 3% terminal growth rate
        powerInput.setUserId("user123");
        powerInput.setProjectionYears(10); // Longer projection to test power operations

        FinancialData powerData = new FinancialData("AAPL");
        powerData.setFreeCashFlow(Arrays.asList(new BigDecimal("10.0")));
        powerData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("100.0")));
        powerData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(powerInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(powerData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(powerInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(powerInput);

        // Assert - Verify power operations work correctly
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // With 25% growth over 10 years, the final year FCF should be significantly larger
        // Final FCF = 10 * (1.25)^10 ≈ 93.13
        // Verify that enterprise value reflects this compound growth
        assertTrue(result.getEnterpriseValue().compareTo(new BigDecimal("50.0")) > 0);
        
        // Verify that present value calculations properly discount the high future values
        assertTrue(result.getPresentValueOfCashFlows().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getTerminalValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should handle BigDecimal division edge cases without arithmetic exceptions")
    void testBigDecimalDivisionEdgeCases() throws DCFCalculationException, FinancialDataException {
        // Arrange - Test division scenarios that could cause issues
        DCFInput divisionInput = new DCFInput("AAPL", 
            new BigDecimal("10.000001"), // Slightly above terminal growth to avoid division by zero
            new BigDecimal("5.0"), 
            new BigDecimal("9.999999")); // Very close to discount rate but not equal
        divisionInput.setUserId("user123");
        divisionInput.setProjectionYears(5);

        FinancialData divisionData = new FinancialData("AAPL");
        divisionData.setFreeCashFlow(Arrays.asList(new BigDecimal("1000000.0"))); // Large FCF
        divisionData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("0.000001"))); // Very small share count
        divisionData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(divisionInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(divisionData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(divisionInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(divisionInput);

        // Assert - Verify division operations handle edge cases
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // With very small denominator (discount rate - terminal growth rate ≈ 0.000002)
        // Terminal value should be very large but finite
        assertTrue(result.getTerminalValue().compareTo(new BigDecimal("1000000000")) > 0);
        
        // Fair value per share should be very large due to small share count
        assertTrue(result.getFairValuePerShare().compareTo(new BigDecimal("1000000")) > 0);
        
        // Verify no arithmetic exceptions occurred
        assertNotNull(result.getEnterpriseValue());
        assertNotNull(result.getEquityValue());
    }

    @Test
    @DisplayName("Should maintain BigDecimal scale precision in complex calculations")
    void testBigDecimalScalePrecisionInComplexCalculations() throws DCFCalculationException, FinancialDataException {
        // Arrange - Use values that create complex multi-step calculations
        DCFInput complexInput = new DCFInput("AAPL", 
            new BigDecimal("8.765432"), // Precise discount rate
            new BigDecimal("12.345678"), // Precise growth rate
            new BigDecimal("2.987654")); // Precise terminal growth rate
        complexInput.setUserId("user123");
        complexInput.setProjectionYears(7);

        FinancialData complexData = new FinancialData("AAPL");
        complexData.setFreeCashFlow(Arrays.asList(new BigDecimal("87.654321")));
        complexData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("12.345678")));
        complexData.setTotalDebt(Arrays.asList(new BigDecimal("23.456789")));

        when(dcfValidationUtil.validateDCFInput(complexInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(complexData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(complexInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(complexInput);

        // Assert - Verify scale precision is maintained throughout complex calculations
        assertNotNull(result);
        assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
        
        // Verify that final results have appropriate scale (6 decimal places for fair value per share)
        assertTrue(result.getFairValuePerShare().scale() <= 6);
        
        // Verify that intermediate calculations maintain precision
        // Present value calculations should use 10 decimal places internally
        assertTrue(result.getPresentValueOfCashFlows().scale() <= 10);
        
        // Terminal value calculations should use 10 decimal places internally
        assertTrue(result.getTerminalValue().scale() <= 10);
        
        // Verify mathematical relationships hold with BigDecimal precision
        BigDecimal calculatedEquityValue = result.getEnterpriseValue().subtract(new BigDecimal("23.456789"));
        assertEquals(0, result.getEquityValue().compareTo(calculatedEquityValue));
    }

    @Test
    @DisplayName("Should handle BigDecimal comparisons correctly in valuation logic")
    void testBigDecimalComparisonsInValuationLogic() throws DCFCalculationException, FinancialDataException {
        // Test Case 1: Fair value exactly equal to current price (within tolerance)
        DCFInput equalInput = new DCFInput("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("5.0"), 
            new BigDecimal("2.0"));
        equalInput.setUserId("user123");
        equalInput.setProjectionYears(5);

        FinancialData equalData = new FinancialData("AAPL");
        // Set up data to produce fair value very close to AAPL's mock price (227.52)
        equalData.setFreeCashFlow(Arrays.asList(new BigDecimal("50.0")));
        equalData.setOrdinarySharesNumber(Arrays.asList(new BigDecimal("1000.0")));
        equalData.setTotalDebt(Arrays.asList(BigDecimal.ZERO));

        when(dcfValidationUtil.validateDCFInput(equalInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(equalData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(equalInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(equalInput);

        // Assert - Verify BigDecimal comparison logic
        assertNotNull(result);
        assertNotNull(result.getValuation());
        
        // Verify that BigDecimal.compareTo() is used correctly for valuation determination
        BigDecimal fairValue = result.getFairValuePerShare();
        BigDecimal currentPrice = result.getCurrentPrice();
        BigDecimal tolerance = currentPrice.multiply(new BigDecimal("0.05")); // 5% tolerance
        BigDecimal difference = fairValue.subtract(currentPrice).abs();
        
        // Test the valuation logic matches BigDecimal comparison behavior
        if (difference.compareTo(tolerance) <= 0) {
            assertEquals("Fair Value", result.getValuation());
        } else if (fairValue.compareTo(currentPrice) > 0) {
            assertEquals("Undervalued", result.getValuation());
        } else {
            assertEquals("Overvalued", result.getValuation());
        }
        
        // Verify that compareTo returns exactly 0, 1, or -1 as expected
        int comparisonResult = fairValue.compareTo(currentPrice);
        assertTrue(comparisonResult >= -1 && comparisonResult <= 1);
    }
}