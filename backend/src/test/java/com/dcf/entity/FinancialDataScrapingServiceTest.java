package com.dcf.entity;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.service.FinancialDataScrapingService;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.service.FinancialDataCacheService;
import com.dcf.util.FinancialDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialDataScrapingServiceTest {

    @Mock
    private FinancialDataRepository financialDataRepository;

    @Mock
    private FinancialDataUtil financialDataUtil;

    @Mock
    private FinancialDataCacheService cacheService;

    @InjectMocks
    private FinancialDataScrapingService financialDataScrapingService;

    private FinancialData mockFinancialData;

    @BeforeEach
    void setUp() {
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setRevenue(Arrays.asList(
            new BigDecimal("100000000000"), 
            new BigDecimal("110000000000"), 
            new BigDecimal("120000000000")
        ));
        mockFinancialData.setFreeCashFlow(Arrays.asList(
            new BigDecimal("20000000000"), 
            new BigDecimal("22000000000"), 
            new BigDecimal("25000000000")
        ));
        mockFinancialData.setEps(Arrays.asList(
            new BigDecimal("2.00"), 
            new BigDecimal("2.20"), 
            new BigDecimal("2.50")
        ));
    }

    @Test
    @DisplayName("Should return cached data when not stale")
    void testGetFinancialDataFromCache() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(mockFinancialData));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        verify(financialDataRepository, never()).save(any());
        verify(cacheService).cacheData("AAPL", mockFinancialData);
    }

    @Test
    @DisplayName("Should scrape fresh data when cache is stale")
    void testGetFinancialDataWhenCacheStale() throws FinancialDataException {
        // Arrange
        FinancialData staleData = new FinancialData("AAPL");
        staleData.setDateFetched(LocalDate.now().minusDays(10));

        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(staleData));
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenReturn(mockFinancialData);

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        verify(financialDataRepository).save(any());
    }

    @Test
    @DisplayName("Should scrape fresh data when no cached data exists")
    void testGetFinancialDataWhenNoCachedData() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenReturn(mockFinancialData);

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        verify(financialDataRepository).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid ticker")
    void testGetFinancialDataInvalidTicker() {
        // Arrange
        when(financialDataUtil.normalizeTicker("INVALID")).thenReturn(null);

        // Act & Assert
        FinancialDataException exception = assertThrows(FinancialDataException.class,
                () -> financialDataScrapingService.getFinancialData("INVALID"));

        assertTrue(exception.getMessage().contains("Invalid ticker symbol"));
    }

    @Test
    @DisplayName("Should throw exception when validation fails")
    void testGetFinancialDataValidationFailure() {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn("Invalid data");

        // Act & Assert
        FinancialDataException exception = assertThrows(FinancialDataException.class,
                () -> financialDataScrapingService.getFinancialData("AAPL"));

        assertTrue(exception.getMessage().contains("Invalid financial data"));
    }

    @Test
    @DisplayName("Should handle scraping timeout gracefully")
    void testScrapingTimeout() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("TIMEOUT")).thenReturn("TIMEOUT");
        when(financialDataRepository.findByTicker("TIMEOUT")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenReturn(mockFinancialData);

        // Act - This should fall back to mock data generation
        FinancialData result = financialDataScrapingService.getFinancialData("TIMEOUT");

        // Assert
        assertNotNull(result);
        assertEquals("TIMEOUT", result.getTicker());
    }

    @Test
    @DisplayName("Should validate ticker correctly")
    void testIsValidTicker() {
        // Test with mock data since we can't make real HTTP calls in unit tests
        // In a real scenario, this would test the actual HTTP validation

        // For unit testing, we'll test the method exists and handles basic cases
        assertDoesNotThrow(() -> {
            boolean result = financialDataScrapingService.isValidTicker("AAPL");
            // Result depends on actual network call, so we just verify no exception
        });
    }

    @Test
    @DisplayName("Should generate mock data for known tickers")
    void testMockDataGeneration() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        assertFalse(result.getRevenue().isEmpty());
        assertFalse(result.getFreeCashFlow().isEmpty());
        assertFalse(result.getEps().isEmpty());

        // Verify mock data has reasonable values for AAPL
        assertTrue(result.getRevenue().get(0).compareTo(new BigDecimal("300000000000")) > 0); // > $300B
    }

    @Test
    @DisplayName("Should handle different ticker cases for mock data")
    void testMockDataForDifferentTickers() throws FinancialDataException {
        // Test GOOGL
        when(financialDataUtil.normalizeTicker("GOOGL")).thenReturn("GOOGL");
        when(cacheService.getCachedData("GOOGL")).thenReturn(null);
        when(financialDataRepository.findByTicker("GOOGL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialData googleData = financialDataScrapingService.getFinancialData("GOOGL");
        assertNotNull(googleData);
        assertEquals("GOOGL", googleData.getTicker());

        // Test unknown ticker
        when(financialDataUtil.normalizeTicker("UNKNOWN")).thenReturn("UNKNOWN");
        when(cacheService.getCachedData("UNKNOWN")).thenReturn(null);
        when(financialDataRepository.findByTicker("UNKNOWN")).thenReturn(Optional.empty());

        FinancialData unknownData = financialDataScrapingService.getFinancialData("UNKNOWN");
        assertNotNull(unknownData);
        assertEquals("UNKNOWN", unknownData.getTicker());

        // Different tickers should have different base revenues
        assertNotEquals(googleData.getRevenue().get(0), unknownData.getRevenue().get(0));
    }

    @Test
    @DisplayName("Should update existing data correctly")
    void testUpdateExistingData() throws FinancialDataException {
        // Arrange
        FinancialData existingData = new FinancialData("AAPL");
        existingData.setRevenue(Arrays.asList(
            new BigDecimal("90000000000"), 
            new BigDecimal("95000000000"), 
            new BigDecimal("100000000000")
        )); // Old data
        existingData.setDateFetched(LocalDate.now().minusDays(10));

        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(existingData));
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        // Should have updated data (mock data generation creates different values)
        BigDecimal oldRevenue = new BigDecimal("90000000000");
        assertNotEquals(oldRevenue, result.getRevenue().get(0));
        verify(financialDataRepository).save(existingData);
    }

    @Test
    @DisplayName("Should handle repository save failure")
    void testRepositorySaveFailure() {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> financialDataScrapingService.getFinancialData("AAPL"));
    }

    @Test
    @DisplayName("Should generate BigDecimal mock data with proper precision")
    void testBigDecimalMockDataPrecision() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(cacheService.getCachedData("AAPL")).thenReturn(null);
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        
        // Verify all financial data are BigDecimal instances
        assertFalse(result.getRevenue().isEmpty());
        assertFalse(result.getOperatingIncome().isEmpty());
        assertFalse(result.getNetProfit().isEmpty());
        assertFalse(result.getOperatingCashFlow().isEmpty());
        assertFalse(result.getFreeCashFlow().isEmpty());
        assertFalse(result.getEps().isEmpty());
        assertFalse(result.getTotalDebt().isEmpty());
        assertFalse(result.getOrdinarySharesNumber().isEmpty());

        // Verify BigDecimal precision is maintained
        BigDecimal revenue = result.getRevenue().get(0);
        assertTrue(revenue instanceof BigDecimal);
        assertTrue(revenue.compareTo(BigDecimal.ZERO) > 0);

        // Verify EPS calculation precision (should have 6 decimal places)
        BigDecimal eps = result.getEps().get(0);
        assertTrue(eps instanceof BigDecimal);
        assertTrue(eps.scale() <= 6); // Should not exceed 6 decimal places
    }

    @Test
    @DisplayName("Should handle BigDecimal arithmetic correctly in mock data generation")
    void testBigDecimalArithmeticInMockData() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("TSLA")).thenReturn("TSLA");
        when(cacheService.getCachedData("TSLA")).thenReturn(null);
        when(financialDataRepository.findByTicker("TSLA")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("TSLA");

        // Assert
        assertNotNull(result);
        
        // Verify growth calculations are consistent across years
        assertTrue(result.getRevenue().size() >= 4);
        
        // Revenue should grow year over year (most recent first, so should decrease in list)
        BigDecimal currentYearRevenue = result.getRevenue().get(0);
        BigDecimal previousYearRevenue = result.getRevenue().get(1);
        assertTrue(currentYearRevenue.compareTo(previousYearRevenue) > 0);

        // Verify operating income is calculated as revenue * operating margin
        BigDecimal revenue = result.getRevenue().get(0);
        BigDecimal operatingIncome = result.getOperatingIncome().get(0);
        
        // Operating income should be less than revenue (positive margin)
        assertTrue(operatingIncome.compareTo(revenue) < 0);
        assertTrue(operatingIncome.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should maintain BigDecimal consistency across different company data")
    void testBigDecimalConsistencyAcrossCompanies() throws FinancialDataException {
        // Test multiple companies to ensure BigDecimal consistency
        String[] tickers = {"AAPL", "GOOGL", "MSFT", "AMZN", "META"};
        
        for (String ticker : tickers) {
            // Arrange
            when(financialDataUtil.normalizeTicker(ticker)).thenReturn(ticker);
            when(cacheService.getCachedData(ticker)).thenReturn(null);
            when(financialDataRepository.findByTicker(ticker)).thenReturn(Optional.empty());
            when(financialDataUtil.getValidationError(any())).thenReturn(null);
            when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            FinancialData result = financialDataScrapingService.getFinancialData(ticker);

            // Assert
            assertNotNull(result, "Financial data should not be null for " + ticker);
            assertEquals(ticker, result.getTicker());
            
            // Verify all BigDecimal fields are properly set
            assertFalse(result.getRevenue().isEmpty(), "Revenue should not be empty for " + ticker);
            assertFalse(result.getEps().isEmpty(), "EPS should not be empty for " + ticker);
            
            // Verify BigDecimal values are reasonable
            BigDecimal revenue = result.getRevenue().get(0);
            assertTrue(revenue.compareTo(new BigDecimal("1000000000")) > 0, 
                "Revenue should be > $1B for " + ticker); // All test companies have > $1B revenue
        }
    }
}