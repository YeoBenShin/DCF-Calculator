package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.service.FinancialDataUpdateService.FinancialDataStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialDataUpdateServiceTest {

    @Mock
    private FinancialDataRepository financialDataRepository;

    @Mock
    private FinancialDataScrapingService scrapingService;

    @InjectMocks
    private FinancialDataUpdateService updateService;

    private FinancialData mockFinancialData;

    @BeforeEach
    void setUp() {
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setDateFetched(LocalDate.now().minusDays(10));
    }

    @Test
    @DisplayName("Should update stale financial data successfully")
    void testUpdateStaleFinancialDataSuccess() throws FinancialDataException {
        // Arrange
        List<FinancialData> staleData = Arrays.asList(mockFinancialData);
        when(financialDataRepository.findStaleData(any(LocalDate.class))).thenReturn(staleData);
        when(scrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act
        updateService.updateStaleFinancialData();

        // Assert
        verify(financialDataRepository).findStaleData(any(LocalDate.class));
        verify(scrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("Should handle scraping failures during update")
    void testUpdateStaleFinancialDataWithFailures() throws FinancialDataException {
        // Arrange
        List<FinancialData> staleData = Arrays.asList(mockFinancialData);
        when(financialDataRepository.findStaleData(any(LocalDate.class))).thenReturn(staleData);
        when(scrapingService.getFinancialData("AAPL"))
            .thenThrow(new FinancialDataException("Scraping failed"));

        // Act
        updateService.updateStaleFinancialData();

        // Assert
        verify(financialDataRepository).findStaleData(any(LocalDate.class));
        verify(scrapingService).getFinancialData("AAPL");
        // Should continue despite failure
    }

    @Test
    @DisplayName("Should handle empty stale data list")
    void testUpdateStaleFinancialDataEmpty() throws FinancialDataException {
        // Arrange
        when(financialDataRepository.findStaleData(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // Act
        updateService.updateStaleFinancialData();

        // Assert
        verify(financialDataRepository).findStaleData(any(LocalDate.class));
        verify(scrapingService, never()).getFinancialData(anyString());
    }

    @Test
    @DisplayName("Should cleanup old financial data")
    void testCleanupOldFinancialData() throws FinancialDataException {
        // Arrange
        when(financialDataRepository.deleteStaleData(any(LocalDate.class))).thenReturn(5);

        // Act
        updateService.cleanupOldFinancialData();

        // Assert
        verify(financialDataRepository).deleteStaleData(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should update popular tickers")
    void testUpdatePopularTickers() throws FinancialDataException {
        // Arrange
        when(scrapingService.getFinancialData(anyString())).thenReturn(mockFinancialData);

        // Act
        updateService.updatePopularTickers();

        // Assert
        // Should call scraping service for each popular ticker
        verify(scrapingService, atLeast(10)).getFinancialData(anyString());
    }

    @Test
    @DisplayName("Should handle failures in popular ticker updates")
    void testUpdatePopularTickersWithFailures() throws FinancialDataException {
        // Arrange
        when(scrapingService.getFinancialData(anyString()))
            .thenThrow(new FinancialDataException("Scraping failed"));

        // Act
        updateService.updatePopularTickers();

        // Assert
        // Should continue despite failures
        verify(scrapingService, atLeast(10)).getFinancialData(anyString());
    }

    @Test
    @DisplayName("Should manually update ticker successfully")
    void testUpdateTickerSuccess() throws FinancialDataException {
        // Arrange
        when(scrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act
        boolean result = updateService.updateTicker("AAPL");

        // Assert
        assertTrue(result);
        verify(scrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("Should handle manual ticker update failure")
    void testUpdateTickerFailure() throws FinancialDataException {
        // Arrange
        when(scrapingService.getFinancialData("AAPL"))
            .thenThrow(new FinancialDataException("Scraping failed"));

        // Act
        boolean result = updateService.updateTicker("AAPL");

        // Assert
        assertFalse(result);
        verify(scrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("Should calculate data freshness statistics correctly")
    void testGetDataFreshnessStats() throws FinancialDataException {
        // Arrange
        when(financialDataRepository.countAllRecords()).thenReturn(100L);
        
        List<FinancialData> staleData = Arrays.asList(mockFinancialData, new FinancialData("GOOGL"));
        when(financialDataRepository.findStaleData(any(LocalDate.class))).thenReturn(staleData);
        
        List<FinancialData> recentData = Arrays.asList(
            new FinancialData("MSFT"), 
            new FinancialData("AMZN"), 
            new FinancialData("TSLA")
        );
        when(financialDataRepository.findRecentlyUpdated(any(LocalDate.class))).thenReturn(recentData);

        // Act
        FinancialDataStats stats = updateService.getDataFreshnessStats();

        // Assert
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalRecords());
        assertEquals(2L, stats.getStaleRecords());
        assertEquals(3L, stats.getRecentRecords());
        assertEquals(2.0, stats.getStalePercentage(), 0.01);
        assertEquals(3.0, stats.getRecentPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should handle zero records in statistics")
    void testGetDataFreshnessStatsZeroRecords() throws FinancialDataException {
        // Arrange
        when(financialDataRepository.countAllRecords()).thenReturn(0L);
        when(financialDataRepository.findStaleData(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        when(financialDataRepository.findRecentlyUpdated(any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // Act
        FinancialDataStats stats = updateService.getDataFreshnessStats();

        // Assert
        assertNotNull(stats);
        assertEquals(0L, stats.getTotalRecords());
        assertEquals(0L, stats.getStaleRecords());
        assertEquals(0L, stats.getRecentRecords());
        assertEquals(0.0, stats.getStalePercentage());
        assertEquals(0.0, stats.getRecentPercentage());
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testHandleRepositoryExceptions() throws FinancialDataException {
        // Arrange
        when(financialDataRepository.findStaleData(any(LocalDate.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertDoesNotThrow(() -> updateService.updateStaleFinancialData());
    }

    @Test
    @DisplayName("Should handle interrupted exception during updates")
    void testHandleInterruptedException() throws FinancialDataException {
        // Arrange
        List<FinancialData> staleData = Arrays.asList(mockFinancialData, new FinancialData("GOOGL"));
        when(financialDataRepository.findStaleData(any(LocalDate.class))).thenReturn(staleData);
        when(scrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);
        
        // Interrupt the current thread to simulate interruption
        Thread.currentThread().interrupt();

        // Act
        updateService.updateStaleFinancialData();

        // Assert
        assertTrue(Thread.currentThread().isInterrupted());
        // Should have processed at least one ticker before interruption
        verify(scrapingService, atLeastOnce()).getFinancialData(anyString());
    }
}