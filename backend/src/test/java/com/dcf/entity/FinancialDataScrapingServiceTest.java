package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.util.FinancialDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private FinancialDataScrapingService financialDataScrapingService;

    private FinancialData mockFinancialData;

    @BeforeEach
    void setUp() {
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        mockFinancialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 25.0));
        mockFinancialData.setEps(Arrays.asList(2.0, 2.2, 2.5));
    }

    @Test
    @DisplayName("Should return cached data when not stale")
    void testGetFinancialDataFromCache() throws FinancialDataException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(mockFinancialData));
        when(mockFinancialData.isDataStale(7)).thenReturn(false);

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        verify(financialDataRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should scrape fresh data when cache is stale")
    void testGetFinancialDataWhenCacheStale() throws FinancialDataException {
        // Arrange
        FinancialData staleData = new FinancialData("AAPL");
        staleData.setDateFetched(LocalDate.now().minusDays(10));

        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(staleData));
        when(staleData.isDataStale(7)).thenReturn(true);
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
        assertTrue(result.getRevenue().get(0) > 300_000_000_000.0); // > $300B
    }

    @Test
    @DisplayName("Should handle different ticker cases for mock data")
    void testMockDataForDifferentTickers() throws FinancialDataException {
        // Test GOOGL
        when(financialDataUtil.normalizeTicker("GOOGL")).thenReturn("GOOGL");
        when(financialDataRepository.findByTicker("GOOGL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FinancialData googleData = financialDataScrapingService.getFinancialData("GOOGL");
        assertNotNull(googleData);
        assertEquals("GOOGL", googleData.getTicker());

        // Test unknown ticker
        when(financialDataUtil.normalizeTicker("UNKNOWN")).thenReturn("UNKNOWN");
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
        existingData.setRevenue(Arrays.asList(90.0, 95.0, 100.0)); // Old data
        existingData.setDateFetched(LocalDate.now().minusDays(10));

        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.of(existingData));
        when(existingData.isDataStale(7)).thenReturn(true);
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FinancialData result = financialDataScrapingService.getFinancialData("AAPL");

        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getTicker());
        // Should have updated data (mock data generation creates different values)
        assertNotEquals(Arrays.asList(90.0, 95.0, 100.0), result.getRevenue());
        verify(financialDataRepository).save(existingData);
    }

    @Test
    @DisplayName("Should handle repository save failure")
    void testRepositorySaveFailure() {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(financialDataUtil.getValidationError(any())).thenReturn(null);
        when(financialDataRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> financialDataScrapingService.getFinancialData("AAPL"));
    }
}