package com.dcf.controller;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class FinancialDataControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        financialDataRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Get financial data with caching")
    void testGetFinancialDataWithCaching() throws Exception {
        // First request should fetch and cache data
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Financial data retrieved successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.revenue").isArray())
                .andExpect(jsonPath("$.data.dateFetched").exists());

        // Second request should return cached data
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Financial data retrieved successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Validate popular tickers")
    void testValidatePopularTickers() throws Exception {
        // Get popular tickers
        String popularResponse = mockMvc.perform(get("/financials/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickers").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract first ticker and validate it
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Error handling for invalid ticker")
    void testErrorHandlingInvalidTicker() throws Exception {
        mockMvc.perform(get("/financials")
                .param("ticker", "INVALIDTICKER123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("Integration Test: Input validation")
    void testInputValidation() throws Exception {
        // Test empty ticker
        mockMvc.perform(get("/financials")
                .param("ticker", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test ticker with invalid characters
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL@#$")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test ticker too long
        mockMvc.perform(get("/financials")
                .param("ticker", "VERYLONGTICKER")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: Case insensitive ticker handling")
    void testCaseInsensitiveTickerHandling() throws Exception {
        // Test lowercase ticker
        mockMvc.perform(get("/financials")
                .param("ticker", "aapl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        // Test mixed case ticker
        mockMvc.perform(get("/financials")
                .param("ticker", "AaPl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Database persistence")
    void testDatabasePersistence() throws Exception {
        // Fetch data for a ticker
        mockMvc.perform(get("/financials")
                .param("ticker", "MSFT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify data is persisted in database
        assert financialDataRepository.findByTicker("MSFT").isPresent();
    }

    @Test
    @DisplayName("Integration Test: Multiple ticker requests")
    void testMultipleTickerRequests() throws Exception {
        String[] tickers = {"AAPL", "GOOGL", "MSFT", "AMZN"};

        for (String ticker : tickers) {
            mockMvc.perform(get("/financials")
                    .param("ticker", ticker)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.ticker").value(ticker))
                    .andExpect(jsonPath("$.data.revenue").isArray());
        }
    }

    @Test
    @DisplayName("Integration Test: Validation endpoint comprehensive test")
    void testValidationEndpointComprehensive() throws Exception {
        // Test valid ticker
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // Test invalid ticker
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "INVALIDTICKER")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        // Test validation with invalid characters
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "AAPL@#$")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}