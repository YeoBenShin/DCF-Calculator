package com.dcf.controller;

import com.dcf.entity.FinancialData;
import com.dcf.service.FinancialDataScrapingService;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinancialDataController.class)
class FinancialDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialDataScrapingService financialDataScrapingService;

    @Autowired
    private ObjectMapper objectMapper;

    private FinancialData mockFinancialData;

    @BeforeEach
    void setUp() {
        mockFinancialData = new FinancialData("AAPL");
        mockFinancialData.setRevenue(Arrays.asList(
            new BigDecimal("365000000000"), new BigDecimal("350000000000"), new BigDecimal("330000000000")));
        mockFinancialData.setOperatingIncome(Arrays.asList(
            new BigDecimal("91250000000"), new BigDecimal("87500000000"), new BigDecimal("82500000000")));
        mockFinancialData.setNetProfit(Arrays.asList(
            new BigDecimal("54750000000"), new BigDecimal("52500000000"), new BigDecimal("49500000000")));
        mockFinancialData.setOperatingCashFlow(Arrays.asList(
            new BigDecimal("73000000000"), new BigDecimal("70000000000"), new BigDecimal("66000000000")));
        mockFinancialData.setFreeCashFlow(Arrays.asList(
            new BigDecimal("43800000000"), new BigDecimal("42000000000"), new BigDecimal("39600000000")));
        mockFinancialData.setEps(Arrays.asList(
            new BigDecimal("54.75"), new BigDecimal("52.50"), new BigDecimal("49.50")));
        mockFinancialData.setTotalDebt(Arrays.asList(
            new BigDecimal("109500000000"), new BigDecimal("105000000000"), new BigDecimal("99000000000")));
        mockFinancialData.setOrdinarySharesNumber(Arrays.asList(
            new BigDecimal("1000000000"), new BigDecimal("1000000000"), new BigDecimal("1000000000")));
        mockFinancialData.setDateFetched(LocalDate.now());
    }

    @Test
    @DisplayName("GET /financials - Should return financial data successfully")
    void testGetFinancialDataSuccess() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Financial data retrieved successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.revenue").isArray())
                .andExpect(jsonPath("$.data.revenue[0]").value("365000000000"))
                .andExpect(jsonPath("$.data.operatingIncome").isArray())
                .andExpect(jsonPath("$.data.netProfit").isArray())
                .andExpect(jsonPath("$.data.dateFetched").exists());

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials - Should return 400 for invalid ticker")
    void testGetFinancialDataInvalidTicker() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("INVALID"))
                .thenThrow(new FinancialDataException("Invalid ticker symbol: INVALID"));

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid ticker symbol: INVALID"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(financialDataScrapingService).getFinancialData("INVALID");
    }

    @Test
    @DisplayName("GET /financials - Should return 400 for empty ticker")
    void testGetFinancialDataEmptyTicker() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials - Should return 400 for missing ticker parameter")
    void testGetFinancialDataMissingTicker() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials - Should return 400 for ticker with invalid characters")
    void testGetFinancialDataInvalidCharacters() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL@#$")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials - Should return 400 for ticker too long")
    void testGetFinancialDataTickerTooLong() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "VERYLONGTICKER")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials - Should return 500 for server error")
    void testGetFinancialDataServerError() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Unable to retrieve financial data at this time"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/validate - Should validate ticker successfully")
    void testValidateTickerSuccess() throws Exception {
        // Arrange
        when(financialDataScrapingService.isValidTicker("AAPL")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Ticker is valid"))
                .andExpect(jsonPath("$.ticker").value("AAPL"));

        verify(financialDataScrapingService).isValidTicker("AAPL");
    }

    @Test
    @DisplayName("GET /financials/validate - Should return false for invalid ticker")
    void testValidateTickerInvalid() throws Exception {
        // Arrange
        when(financialDataScrapingService.isValidTicker("INVALID")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Ticker not found or invalid"))
                .andExpect(jsonPath("$.ticker").value("INVALID"));

        verify(financialDataScrapingService).isValidTicker("INVALID");
    }

    @Test
    @DisplayName("GET /financials/validate - Should return 500 for validation error")
    void testValidateTickerError() throws Exception {
        // Arrange
        when(financialDataScrapingService.isValidTicker("AAPL"))
                .thenThrow(new RuntimeException("Network error"));

        // Act & Assert
        mockMvc.perform(get("/financials/validate")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Unable to validate ticker at this time"));

        verify(financialDataScrapingService).isValidTicker("AAPL");
    }

    @Test
    @DisplayName("GET /financials/popular - Should return popular tickers")
    void testGetPopularTickers() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials/popular")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Popular tickers retrieved successfully"))
                .andExpect(jsonPath("$.tickers").isArray())
                .andExpect(jsonPath("$.tickers[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$.tickers[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$.tickers[1].symbol").value("GOOGL"))
                .andExpect(jsonPath("$.tickers[1].name").value("Alphabet Inc."));

        // Should not call any service methods
        verifyNoInteractions(financialDataScrapingService);
    }

    @Test
    @DisplayName("GET /financials - Should handle case insensitive ticker")
    void testGetFinancialDataCaseInsensitive() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "aapl")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials - Should trim whitespace from ticker")
    void testGetFinancialDataTrimWhitespace() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "  AAPL  ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should filter financial data with BigDecimal parameters")
    void testGetFilteredFinancialDataSuccess() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "300000000000")
                .param("maxDebt", "120000000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Filtered financial data retrieved successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.revenue[0]").value("365000000000"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should reject data not meeting revenue filter")
    void testGetFilteredFinancialDataRevenueFilter() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert - Set minimum revenue higher than actual
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "400000000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Ticker does not meet the specified financial criteria"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should reject data not meeting debt filter")
    void testGetFilteredFinancialDataDebtFilter() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert - Set maximum debt lower than actual
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("maxDebt", "100000000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Ticker does not meet the specified financial criteria"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should return 400 for invalid BigDecimal parameter")
    void testGetFilteredFinancialDataInvalidBigDecimal() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "invalid_number")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid number format")));

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials/filter - Should return 400 for negative BigDecimal parameter")
    void testGetFilteredFinancialDataNegativeBigDecimal() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "-1000000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be greater than or equal to")));

        verify(financialDataScrapingService, never()).getFinancialData(any());
    }

    @Test
    @DisplayName("GET /financials - Should handle BigDecimal serialization errors")
    void testGetFinancialDataBigDecimalSerializationError() throws Exception {
        // Arrange - Create financial data that will cause conversion error
        FinancialData corruptedData = new FinancialData("AAPL");
        // This will be handled by the convertToDto method error handling
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(corruptedData);

        // Act & Assert
        mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Should still work with null values
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should work with very large BigDecimal values")
    void testGetFilteredFinancialDataLargeBigDecimal() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert - Test with very large number
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "999999999999999999999.123456")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Ticker does not meet the specified financial criteria"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }

    @Test
    @DisplayName("GET /financials/filter - Should work with decimal BigDecimal values")
    void testGetFilteredFinancialDataDecimalBigDecimal() throws Exception {
        // Arrange
        when(financialDataScrapingService.getFinancialData("AAPL")).thenReturn(mockFinancialData);

        // Act & Assert - Test with decimal values
        mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "300000000000.50")
                .param("maxDebt", "120000000000.75")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Filtered financial data retrieved successfully"));

        verify(financialDataScrapingService).getFinancialData("AAPL");
    }
}