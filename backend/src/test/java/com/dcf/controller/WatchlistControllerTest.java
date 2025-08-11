package com.dcf.controller;

import com.dcf.dto.WatchlistItemDto;
import com.dcf.dto.WatchlistRequest;
import com.dcf.dto.WatchlistStatsDto;
import com.dcf.service.WatchlistService;
import com.dcf.service.WatchlistService.WatchlistException;
import com.dcf.service.WatchlistService.WatchlistItem;
import com.dcf.service.WatchlistService.WatchlistStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WatchlistControllerTest {

    @Mock
    private WatchlistService watchlistService;

    @InjectMocks
    private WatchlistController watchlistController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(watchlistController).build();
        objectMapper = new ObjectMapper();
        
        // Set up authentication context
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken("test-user-id", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Unit Test: Get empty watchlist")
    void testGetEmptyWatchlist() throws Exception {
        when(watchlistService.getWatchlistWithFairValues("test-user-id"))
            .thenReturn(Arrays.asList());

        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(watchlistService).getWatchlistWithFairValues("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Get watchlist with items")
    void testGetWatchlistWithItems() throws Exception {
        WatchlistItem item1 = new WatchlistItem();
        item1.setTicker("AAPL");
        item1.setFairValuePerShare(150.0);
        item1.setCurrentPrice(140.0);
        item1.setValuation("Undervalued");
        item1.setLastCalculated(LocalDateTime.now());

        WatchlistItem item2 = new WatchlistItem();
        item2.setTicker("GOOGL");
        item2.setFairValuePerShare(2500.0);
        item2.setCurrentPrice(2600.0);
        item2.setValuation("Overvalued");
        item2.setLastCalculated(LocalDateTime.now());

        when(watchlistService.getWatchlistWithFairValues("test-user-id"))
            .thenReturn(Arrays.asList(item1, item2));

        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$[0].valuation").value("Undervalued"))
                .andExpect(jsonPath("$[1].ticker").value("GOOGL"))
                .andExpect(jsonPath("$[1].valuation").value("Overvalued"));

        verify(watchlistService).getWatchlistWithFairValues("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Add ticker to watchlist successfully")
    void testAddToWatchlistSuccess() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");
        
        when(watchlistService.addToWatchlist("test-user-id", "AAPL"))
            .thenReturn(true);

        mockMvc.perform(post("/watchlist/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticker added to watchlist successfully"));

        verify(watchlistService).addToWatchlist("test-user-id", "AAPL");
    }

    @Test
    @DisplayName("Unit Test: Add duplicate ticker to watchlist")
    void testAddDuplicateToWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");
        
        when(watchlistService.addToWatchlist("test-user-id", "AAPL"))
            .thenReturn(false);

        mockMvc.perform(post("/watchlist/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Ticker is already in your watchlist"));

        verify(watchlistService).addToWatchlist("test-user-id", "AAPL");
    }

    @Test
    @DisplayName("Unit Test: Remove ticker from watchlist successfully")
    void testRemoveFromWatchlistSuccess() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");
        
        when(watchlistService.removeFromWatchlist("test-user-id", "AAPL"))
            .thenReturn(true);

        mockMvc.perform(delete("/watchlist/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticker removed from watchlist successfully"));

        verify(watchlistService).removeFromWatchlist("test-user-id", "AAPL");
    }

    @Test
    @DisplayName("Unit Test: Remove non-existent ticker from watchlist")
    void testRemoveNonExistentFromWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");
        
        when(watchlistService.removeFromWatchlist("test-user-id", "AAPL"))
            .thenReturn(false);

        mockMvc.perform(delete("/watchlist/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Ticker is not in your watchlist"));

        verify(watchlistService).removeFromWatchlist("test-user-id", "AAPL");
    }

    @Test
    @DisplayName("Unit Test: Get watchlist tickers")
    void testGetWatchlistTickers() throws Exception {
        List<String> tickers = Arrays.asList("AAPL", "GOOGL", "MSFT");
        
        when(watchlistService.getWatchlistTickers("test-user-id"))
            .thenReturn(tickers);

        mockMvc.perform(get("/watchlist/tickers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("AAPL"))
                .andExpect(jsonPath("$[1]").value("GOOGL"))
                .andExpect(jsonPath("$[2]").value("MSFT"));

        verify(watchlistService).getWatchlistTickers("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Get watchlist stats")
    void testGetWatchlistStats() throws Exception {
        WatchlistStats stats = new WatchlistStats(5, 2, 2, 1, 15.5);
        
        when(watchlistService.getWatchlistStats("test-user-id"))
            .thenReturn(stats);

        mockMvc.perform(get("/watchlist/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStocks").value(5))
                .andExpect(jsonPath("$.undervaluedCount").value(2))
                .andExpect(jsonPath("$.overvaluedCount").value(2))
                .andExpect(jsonPath("$.fairValueCount").value(1))
                .andExpect(jsonPath("$.averageUpside").value(15.5));

        verify(watchlistService).getWatchlistStats("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Clear watchlist")
    void testClearWatchlist() throws Exception {
        when(watchlistService.clearWatchlist("test-user-id"))
            .thenReturn(3);

        mockMvc.perform(delete("/watchlist/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cleared 3 tickers from watchlist"));

        verify(watchlistService).clearWatchlist("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Check if ticker is in watchlist")
    void testIsInWatchlist() throws Exception {
        when(watchlistService.isInWatchlist("test-user-id", "AAPL"))
            .thenReturn(true);

        mockMvc.perform(get("/watchlist/contains/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.inWatchlist").value(true));

        verify(watchlistService).isInWatchlist("test-user-id", "AAPL");
    }

    @Test
    @DisplayName("Unit Test: Handle watchlist service exception")
    void testWatchlistServiceException() throws Exception {
        when(watchlistService.getWatchlistWithFairValues("test-user-id"))
            .thenThrow(new WatchlistException("User not found"));

        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(watchlistService).getWatchlistWithFairValues("test-user-id");
    }

    @Test
    @DisplayName("Unit Test: Handle unauthenticated request")
    void testUnauthenticatedRequest() throws Exception {
        // Clear authentication context
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("User not authenticated"));

        verify(watchlistService, never()).getWatchlistWithFairValues(any());
    }
}