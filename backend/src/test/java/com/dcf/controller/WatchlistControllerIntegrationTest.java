package com.dcf.controller;

import com.dcf.dto.AuthRequest;
import com.dcf.dto.WatchlistRequest;
import com.dcf.entity.User;
import com.dcf.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class WatchlistControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String authToken;
    private String userId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
        
        // Create a test user and get auth token
        setupTestUser();
    }

    private void setupTestUser() throws Exception {
        AuthRequest authRequest = new AuthRequest("watchlist@example.com", "Password123!");

        MvcResult result = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(response);
        authToken = jsonNode.get("token").asText();
        userId = jsonNode.get("user").get("userId").asText();
    }

    @Test
    @DisplayName("Integration Test: Get empty watchlist")
    void testGetEmptyWatchlist() throws Exception {
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Integration Test: Add ticker to watchlist")
    void testAddToWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");

        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticker added to watchlist successfully"));

        // Verify ticker was added
        mockMvc.perform(get("/watchlist/tickers")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Add duplicate ticker to watchlist")
    void testAddDuplicateToWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");

        // Add ticker first time
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Try to add same ticker again
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Ticker is already in your watchlist"));
    }

    @Test
    @DisplayName("Integration Test: Remove ticker from watchlist")
    void testRemoveFromWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");

        // Add ticker first
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Remove ticker
        mockMvc.perform(delete("/watchlist/remove")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ticker removed from watchlist successfully"));

        // Verify ticker was removed
        mockMvc.perform(get("/watchlist/tickers")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Integration Test: Remove non-existent ticker from watchlist")
    void testRemoveNonExistentFromWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");

        mockMvc.perform(delete("/watchlist/remove")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Ticker is not in your watchlist"));
    }

    @Test
    @DisplayName("Integration Test: Get watchlist with multiple tickers")
    void testGetWatchlistWithMultipleTickers() throws Exception {
        // Add multiple tickers
        String[] tickers = {"AAPL", "GOOGL", "MSFT"};
        
        for (String ticker : tickers) {
            WatchlistRequest request = new WatchlistRequest(ticker);
            mockMvc.perform(post("/watchlist/add")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Get full watchlist
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].ticker", containsInAnyOrder("AAPL", "GOOGL", "MSFT")));
    }

    @Test
    @DisplayName("Integration Test: Get watchlist tickers")
    void testGetWatchlistTickers() throws Exception {
        // Add tickers
        String[] tickers = {"AAPL", "GOOGL"};
        
        for (String ticker : tickers) {
            WatchlistRequest request = new WatchlistRequest(ticker);
            mockMvc.perform(post("/watchlist/add")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Get tickers only
        mockMvc.perform(get("/watchlist/tickers")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("AAPL", "GOOGL")));
    }

    @Test
    @DisplayName("Integration Test: Get watchlist stats")
    void testGetWatchlistStats() throws Exception {
        // Add some tickers
        String[] tickers = {"AAPL", "GOOGL", "MSFT"};
        
        for (String ticker : tickers) {
            WatchlistRequest request = new WatchlistRequest(ticker);
            mockMvc.perform(post("/watchlist/add")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Get stats
        mockMvc.perform(get("/watchlist/stats")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStocks").value(3))
                .andExpect(jsonPath("$.undervaluedCount").exists())
                .andExpect(jsonPath("$.overvaluedCount").exists())
                .andExpect(jsonPath("$.fairValueCount").exists())
                .andExpect(jsonPath("$.averageUpside").exists());
    }

    @Test
    @DisplayName("Integration Test: Clear watchlist")
    void testClearWatchlist() throws Exception {
        // Add some tickers
        String[] tickers = {"AAPL", "GOOGL", "MSFT"};
        
        for (String ticker : tickers) {
            WatchlistRequest request = new WatchlistRequest(ticker);
            mockMvc.perform(post("/watchlist/add")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Clear watchlist
        mockMvc.perform(delete("/watchlist/clear")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cleared 3 tickers from watchlist"));

        // Verify watchlist is empty
        mockMvc.perform(get("/watchlist/tickers")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Integration Test: Check if ticker is in watchlist")
    void testIsInWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest("AAPL");

        // Check before adding
        mockMvc.perform(get("/watchlist/contains/AAPL")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.inWatchlist").value(false));

        // Add ticker
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Check after adding
        mockMvc.perform(get("/watchlist/contains/AAPL")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.inWatchlist").value(true));
    }

    @Test
    @DisplayName("Integration Test: Get popular tickers")
    void testGetPopularTickers() throws Exception {
        // This endpoint doesn't require authentication
        mockMvc.perform(get("/watchlist/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test with limit parameter
        mockMvc.perform(get("/watchlist/popular?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("Integration Test: Get popular tickers with invalid limit")
    void testGetPopularTickersInvalidLimit() throws Exception {
        mockMvc.perform(get("/watchlist/popular?limit=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Limit must be between 1 and 100"));

        mockMvc.perform(get("/watchlist/popular?limit=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Limit must be between 1 and 100"));
    }

    @Test
    @DisplayName("Integration Test: Unauthorized access to watchlist endpoints")
    void testUnauthorizedAccess() throws Exception {
        // Test without token
        mockMvc.perform(get("/watchlist"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/watchlist/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WatchlistRequest("AAPL"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/watchlist/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WatchlistRequest("AAPL"))))
                .andExpect(status().isUnauthorized());

        // Test with invalid token
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Integration Test: Invalid ticker validation")
    void testInvalidTickerValidation() throws Exception {
        // Test with empty ticker
        WatchlistRequest emptyRequest = new WatchlistRequest("");
        
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        // Test with null ticker
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticker\": null}"))
                .andExpect(status().isBadRequest());

        // Test with too long ticker
        WatchlistRequest longRequest = new WatchlistRequest("VERYLONGTICKER");
        
        mockMvc.perform(post("/watchlist/add")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: Complete watchlist workflow")
    void testCompleteWatchlistWorkflow() throws Exception {
        // Start with empty watchlist
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Add multiple tickers
        String[] tickers = {"AAPL", "GOOGL", "MSFT", "TSLA"};
        
        for (String ticker : tickers) {
            WatchlistRequest request = new WatchlistRequest(ticker);
            mockMvc.perform(post("/watchlist/add")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Verify all tickers added
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        // Remove one ticker
        WatchlistRequest removeRequest = new WatchlistRequest("TSLA");
        mockMvc.perform(delete("/watchlist/remove")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(removeRequest)))
                .andExpect(status().isOk());

        // Verify ticker removed
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].ticker", not(hasItem("TSLA"))));

        // Get stats
        mockMvc.perform(get("/watchlist/stats")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStocks").value(3));

        // Clear all
        mockMvc.perform(delete("/watchlist/clear")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cleared 3 tickers from watchlist"));

        // Verify empty
        mockMvc.perform(get("/watchlist")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}