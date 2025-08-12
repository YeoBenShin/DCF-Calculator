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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private DCFCalculationService dcfCalculationService;

    private DCFInput mockDCFInput;
    private FinancialData mockFinancialData;
    private DCFOutput mockDCFOutput;

    @BeforeEach
    void setUp() {
        // Setup mock DCF input
        mockDCFInput = new DCFInput("AAPL", 10.0, 15.0, 3.0);
        mockDCFInput.setUserId("user123");
        mockDCFInput.setProjectionYears(5);

        // Setup mock financial data
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0, 130.0, 140.0));
        mockFinancialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 24.0, 26.0, 28.0));
        mockFinancialData.setEps(Arrays.asList(2.0, 2.2, 2.4, 2.6, 2.8));
        mockFinancialData.setTotalDebt(Arrays.asList(50.0, 52.0, 54.0, 56.0, 58.0));
        mockFinancialData.setOrdinarySharesNumber(Arrays.asList(1000.0, 1000.0, 1000.0, 1000.0, 1000.0));

        // Setup mock DCF output
        mockDCFOutput = new DCFOutput("AAPL", 180.0, 175.0, "Undervalued");
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
        assertTrue(result.getFairValuePerShare() > 0);
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
        mockFinancialData.setFreeCashFlow(Arrays.asList(0.0, -5.0, 10.0, 15.0, 20.0));
        when(dcfValidationUtil.validateDCFInput(mockDCFInput)).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        DCFCalculationException exception = assertThrows(DCFCalculationException.class,
            () -> dcfCalculationService.calculateDCF(mockDCFInput));
        
        assertTrue(exception.getMessage().contains("DCF calculation failed"));
    }

    @Test
    @DisplayName("Should calculate DCF with no debt (zero debt)")
    void testCalculateDCFNoDeb() throws DCFCalculationException, FinancialDataException {
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
        assertTrue(result.getFairValuePerShare() > 0);
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
        // Arrange
        double[] growthRates = {0.10, 0.15, 0.20}; // 10%, 15%, 20%
        double[] discountRates = {0.08, 0.10, 0.12}; // 8%, 10%, 12%
        
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
        
        // Verify all combinations are present
        for (double growthRate : growthRates) {
            for (double discountRate : discountRates) {
                boolean found = analysis.getResults().stream()
                    .anyMatch(r -> Math.abs(r.getGrowthRate() - growthRate * 100) < 0.01 &&
                                  Math.abs(r.getDiscountRate() - discountRate * 100) < 0.01);
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
        DCFOutput undervaluedOutput = new DCFOutput("AAPL", 180.0, 175.0, "Undervalued");
        DCFOutput overvaluedOutput = new DCFOutput("GOOGL", 140.0, 150.0, "Overvalued");
        DCFOutput fairValueOutput = new DCFOutput("MSFT", 380.0, 378.0, "Fair Value");
        
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
        assertEquals(33.33, stats.getUndervaluedPercentage(), 0.01);
        assertEquals(33.33, stats.getOvervaluedPercentage(), 0.01);
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
        assertEquals(0.0, stats.getUndervaluedPercentage());
        assertEquals(0.0, stats.getOvervaluedPercentage());
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
        DCFInput googleInput = new DCFInput("GOOGL", 10.0, 15.0, 3.0);
        FinancialData googleData = new FinancialData("GOOGL");
        googleData.setFreeCashFlow(Arrays.asList(30.0));
        googleData.setOrdinarySharesNumber(Arrays.asList(1000.0));
        
        when(dcfValidationUtil.validateDCFInput(any())).thenReturn(null);
        when(financialDataScrapingService.getFinancialData("GOOGL")).thenReturn(googleData);
        when(dcfInputRepository.save(any(DCFInput.class))).thenReturn(googleInput);
        when(dcfOutputRepository.save(any(DCFOutput.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DCFOutput result = dcfCalculationService.calculateDCF(googleInput);

        // Assert
        assertNotNull(result);
        assertEquals("GOOGL", result.getTicker());
        // Should use different mock price for GOOGL (140.0) vs AAPL (175.0)
        assertEquals(140.0, result.getCurrentPrice());
    }
}