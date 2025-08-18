package com.dcf.controller;

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
        mockMvc.perform(get("/financials/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickers").isArray());

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

    @Test
    @DisplayName("Integration Test: BigDecimal serialization in financial data")
    void testBigDecimalSerializationInFinancialData() throws Exception {
        String response = mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").isArray())
                .andExpect(jsonPath("$.data.operatingIncome").isArray())
                .andExpect(jsonPath("$.data.freeCashFlow").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify that BigDecimal values are serialized as plain strings, not scientific notation
        assert !response.contains("E");
        assert !response.contains("e");
    }

    @Test
    @DisplayName("Integration Test: Financial data BigDecimal precision")
    void testFinancialDataBigDecimalPrecision() throws Exception {
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.revenue").isArray())
                .andExpect(jsonPath("$.data.operatingExpense").isArray())
                .andExpect(jsonPath("$.data.operatingIncome").isArray())
                .andExpect(jsonPath("$.data.operatingCashFlow").isArray())
                .andExpect(jsonPath("$.data.netProfit").isArray())
                .andExpect(jsonPath("$.data.capitalExpenditure").isArray())
                .andExpect(jsonPath("$.data.freeCashFlow").isArray())
                .andExpect(jsonPath("$.data.eps").isArray())
                .andExpect(jsonPath("$.data.totalDebt").isArray())
                .andExpect(jsonPath("$.data.ordinarySharesNumber").isArray());
    }

    @Test
    @DisplayName("Integration Test: Financial data consistency across multiple requests")
    void testFinancialDataConsistencyAcrossMultipleRequests() throws Exception {
        // First request
        String response1 = mockMvc.perform(get("/financials")
                .param("ticker", "MSFT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Second request (should return same data from cache)
        String response2 = mockMvc.perform(get("/financials")
                .param("ticker", "MSFT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify responses are identical (BigDecimal precision maintained)
        assert response1.equals(response2);
    }
}