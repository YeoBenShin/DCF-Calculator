package com.dcf.service;

import com.dcf.entity.DCFOutput;
import com.dcf.entity.User;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.repository.UserRepository;
import com.dcf.service.WatchlistService.PopularTicker;
import com.dcf.service.WatchlistService.WatchlistException;
import com.dcf.service.WatchlistService.WatchlistItem;
import com.dcf.service.WatchlistService.WatchlistStats;
import com.dcf.util.FinancialDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DCFOutputRepository dcfOutputRepository;

    @Mock
    private FinancialDataScrapingService financialDataScrapingService;

    @Mock
    private FinancialDataUtil financialDataUtil;

    @InjectMocks
    private WatchlistService watchlistService;

    private User mockUser;
    private DCFOutput mockDCFOutput;

    @BeforeEach
    void setUp() {
        mockUser = new User("test@example.com", "hashedPassword");
        mockUser.setUserId("user123");

        mockDCFOutput = new DCFOutput("AAPL", 180.0, 175.0, "Undervalued");
        mockDCFOutput.setCalculatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should add ticker to watchlist successfully")
    void testAddToWatchlistSuccess() throws WatchlistException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        boolean result = watchlistService.addToWatchlist("user123", "AAPL");

        // Assert
        assertTrue(result);
        assertTrue(mockUser.isInWatchlist("AAPL"));
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should return false when ticker already in watchlist")
    void testAddToWatchlistAlreadyExists() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        boolean result = watchlistService.addToWatchlist("user123", "AAPL");

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid ticker")
    void testAddToWatchlistInvalidTicker() {
        // Arrange
        when(financialDataUtil.normalizeTicker("INVALID")).thenReturn(null);

        // Act & Assert
        WatchlistException exception = assertThrows(WatchlistException.class,
            () -> watchlistService.addToWatchlist("user123", "INVALID"));
        
        assertTrue(exception.getMessage().contains("Invalid ticker symbol"));
    }

    @Test
    @DisplayName("Should throw exception for non-existent ticker")
    void testAddToWatchlistTickerNotFound() {
        // Arrange
        when(financialDataUtil.normalizeTicker("FAKE")).thenReturn("FAKE");
        when(financialDataScrapingService.isValidTicker("FAKE")).thenReturn(false);

        // Act & Assert
        WatchlistException exception = assertThrows(WatchlistException.class,
            () -> watchlistService.addToWatchlist("user123", "FAKE"));
        
        assertTrue(exception.getMessage().contains("Ticker not found"));
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void testAddToWatchlistUserNotFound() {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        WatchlistException exception = assertThrows(WatchlistException.class,
            () -> watchlistService.addToWatchlist("nonexistent", "AAPL"));
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Should remove ticker from watchlist successfully")
    void testRemoveFromWatchlistSuccess() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        boolean result = watchlistService.removeFromWatchlist("user123", "AAPL");

        // Assert
        assertTrue(result);
        assertFalse(mockUser.isInWatchlist("AAPL"));
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should return false when ticker not in watchlist")
    void testRemoveFromWatchlistNotExists() throws WatchlistException {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        boolean result = watchlistService.removeFromWatchlist("user123", "AAPL");

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get watchlist with fair values successfully")
    void testGetWatchlistWithFairValues() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        mockUser.addToWatchlist("GOOGL");
        
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "AAPL"))
            .thenReturn(Optional.of(mockDCFOutput));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "GOOGL"))
            .thenReturn(Optional.empty());

        // Act
        List<WatchlistItem> result = watchlistService.getWatchlistWithFairValues("user123");

        // Assert
        assertEquals(2, result.size());
        
        WatchlistItem appleItem = result.stream()
            .filter(item -> "AAPL".equals(item.getTicker()))
            .findFirst().orElse(null);
        assertNotNull(appleItem);
        assertEquals(180.0, appleItem.getFairValuePerShare());
        assertEquals("Undervalued", appleItem.getValuation());
        assertTrue(appleItem.hasCalculation());
        
        WatchlistItem googleItem = result.stream()
            .filter(item -> "GOOGL".equals(item.getTicker()))
            .findFirst().orElse(null);
        assertNotNull(googleItem);
        assertEquals("Not Calculated", googleItem.getValuation());
        assertFalse(googleItem.hasCalculation());
    }

    @Test
    @DisplayName("Should get watchlist tickers only")
    void testGetWatchlistTickers() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        mockUser.addToWatchlist("GOOGL");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        List<String> result = watchlistService.getWatchlistTickers("user123");

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains("AAPL"));
        assertTrue(result.contains("GOOGL"));
    }

    @Test
    @DisplayName("Should check if ticker is in watchlist")
    void testIsInWatchlist() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataUtil.normalizeTicker("GOOGL")).thenReturn("GOOGL");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertTrue(watchlistService.isInWatchlist("user123", "AAPL"));
        assertFalse(watchlistService.isInWatchlist("user123", "GOOGL"));
    }

    @Test
    @DisplayName("Should return false for invalid ticker in watchlist check")
    void testIsInWatchlistInvalidTicker() throws WatchlistException {
        // Arrange
        when(financialDataUtil.normalizeTicker("INVALID")).thenReturn(null);

        // Act
        boolean result = watchlistService.isInWatchlist("user123", "INVALID");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should calculate watchlist statistics correctly")
    void testGetWatchlistStats() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        mockUser.addToWatchlist("GOOGL");
        mockUser.addToWatchlist("MSFT");
        
        DCFOutput undervaluedOutput = new DCFOutput("AAPL", 180.0, 175.0, "Undervalued");
        undervaluedOutput.setCalculatedAt(LocalDateTime.now());
        
        DCFOutput overvaluedOutput = new DCFOutput("GOOGL", 140.0, 150.0, "Overvalued");
        overvaluedOutput.setCalculatedAt(LocalDateTime.now());
        
        DCFOutput fairValueOutput = new DCFOutput("MSFT", 380.0, 378.0, "Fair Value");
        fairValueOutput.setCalculatedAt(LocalDateTime.now());
        
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "AAPL"))
            .thenReturn(Optional.of(undervaluedOutput));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "GOOGL"))
            .thenReturn(Optional.of(overvaluedOutput));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "MSFT"))
            .thenReturn(Optional.of(fairValueOutput));

        // Act
        WatchlistStats stats = watchlistService.getWatchlistStats("user123");

        // Assert
        assertEquals(3, stats.getTotalStocks());
        assertEquals(1, stats.getUndervaluedCount());
        assertEquals(1, stats.getOvervaluedCount());
        assertEquals(1, stats.getFairValueCount());
        assertEquals(33.33, stats.getUndervaluedPercentage(), 0.01);
        assertEquals(33.33, stats.getOvervaluedPercentage(), 0.01);
        assertEquals(33.33, stats.getFairValuePercentage(), 0.01);
    }

    @Test
    @DisplayName("Should clear watchlist successfully")
    void testClearWatchlist() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        mockUser.addToWatchlist("GOOGL");
        mockUser.addToWatchlist("MSFT");
        
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        int result = watchlistService.clearWatchlist("user123");

        // Assert
        assertEquals(3, result);
        assertTrue(mockUser.getWatchlist().isEmpty());
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should get popular tickers correctly")
    void testGetPopularTickers() {
        // Arrange
        User user1 = new User("user1@example.com", "password");
        user1.addToWatchlist("AAPL");
        user1.addToWatchlist("GOOGL");
        
        User user2 = new User("user2@example.com", "password");
        user2.addToWatchlist("AAPL");
        user2.addToWatchlist("MSFT");
        
        User user3 = new User("user3@example.com", "password");
        user3.addToWatchlist("AAPL");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));

        // Act
        List<PopularTicker> result = watchlistService.getPopularTickers(5);

        // Assert
        assertEquals(3, result.size());
        
        // AAPL should be most popular (3 occurrences)
        PopularTicker mostPopular = result.get(0);
        assertEquals("AAPL", mostPopular.getTicker());
        assertEquals(3, mostPopular.getCount());
        
        // GOOGL and MSFT should have 1 occurrence each
        assertTrue(result.stream().anyMatch(t -> "GOOGL".equals(t.getTicker()) && t.getCount() == 1));
        assertTrue(result.stream().anyMatch(t -> "MSFT".equals(t.getTicker()) && t.getCount() == 1));
    }

    @Test
    @DisplayName("Should handle empty watchlist")
    void testEmptyWatchlist() throws WatchlistException {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        List<WatchlistItem> result = watchlistService.getWatchlistWithFairValues("user123");
        List<String> tickers = watchlistService.getWatchlistTickers("user123");
        WatchlistStats stats = watchlistService.getWatchlistStats("user123");

        // Assert
        assertTrue(result.isEmpty());
        assertTrue(tickers.isEmpty());
        assertEquals(0, stats.getTotalStocks());
        assertEquals(0.0, stats.getAverageUpside());
    }

    @Test
    @DisplayName("Should handle errors when creating watchlist items")
    void testWatchlistItemCreationError() throws WatchlistException {
        // Arrange
        mockUser.addToWatchlist("AAPL");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(dcfOutputRepository.findMostRecentByUserAndTicker("user123", "AAPL"))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<WatchlistItem> result = watchlistService.getWatchlistWithFairValues("user123");

        // Assert
        assertEquals(1, result.size());
        WatchlistItem item = result.get(0);
        assertEquals("AAPL", item.getTicker());
        assertTrue(item.hasError());
        assertEquals("Unable to retrieve data", item.getError());
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testRepositoryExceptions() {
        // Arrange
        when(financialDataUtil.normalizeTicker("AAPL")).thenReturn("AAPL");
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);
        when(userRepository.findById("user123")).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> watchlistService.addToWatchlist("user123", "AAPL"));
    }

    @Test
    @DisplayName("Should limit popular tickers correctly")
    void testGetPopularTickersLimit() {
        // Arrange
        User user1 = new User("user1@example.com", "password");
        user1.addToWatchlist("AAPL");
        user1.addToWatchlist("GOOGL");
        user1.addToWatchlist("MSFT");
        user1.addToWatchlist("AMZN");
        user1.addToWatchlist("TSLA");
        
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));

        // Act
        List<PopularTicker> result = watchlistService.getPopularTickers(3);

        // Assert
        assertEquals(3, result.size()); // Should be limited to 3
    }

    @Test
    @DisplayName("Should handle case insensitive ticker operations")
    void testCaseInsensitiveTickers() throws WatchlistException {
        // Arrange
        when(financialDataUtil.normalizeTicker("aapl")).thenReturn("AAPL");
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        boolean result = watchlistService.addToWatchlist("user123", "aapl");

        // Assert
        assertTrue(result);
        assertTrue(mockUser.isInWatchlist("AAPL"));
        assertFalse(mockUser.isInWatchlist("aapl")); // Should be normalized to uppercase
    }
}