package com.dcf.controller;

import com.dcf.dto.DCFInputDto;
import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.service.DCFCalculationService;
import com.dcf.service.DCFCalculationService.DCFCalculationException;
import com.dcf.service.DCFCalculationService.DCFSensitivityAnalysis;
import com.dcf.service.DCFCalculationService.DCFCalculationStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DCFCalculationController.class)
class DCFCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DCFCalculationService dcfCalculationService;

    @Autowired
    private ObjectMapper objectMapper;

    private DCFInputDto validDCFInput;
    private DCFOutput mockDCFOutput;

    @BeforeEach
    void setUp() {
        validDCFInput = new DCFInputDto("AAPL", 10.0, 8.0, 3.0);
        
        mockDCFOutput = new DCFOutput("AAPL", 180.0, 175.0, "Undervalued");
        mockDCFOutput.setId("output123");
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should calculate DCF successfully")
    void testCalculateDCFSuccess() throws Exception {
        // Arrange
        when(dcfCalculationService.calculateDCF(any(DCFInput.class))).thenReturn(mockDCFOutput);

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("DCF calculation completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.fairValuePerShare").value(180.0))
                .andExpect(jsonPath("$.data.currentPrice").value(175.0))
                .andExpect(jsonPath("$.data.valuation").value("Undervalued"));

        verify(dcfCalculationService).calculateDCF(any(DCFInput.class));
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should return 400 for invalid input")
    void testCalculateDCFInvalidInput() throws Exception {
        // Arrange
        DCFInputDto invalidInput = new DCFInputDto("", -5.0, 1500.0, -2.0);

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());

        verify(dcfCalculationService, never()).calculateDCF(any(DCFInput.class));
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should return 400 for DCF calculation error")
    void testCalculateDCFCalculationError() throws Exception {
        // Arrange
        when(dcfCalculationService.calculateDCF(any(DCFInput.class)))
                .thenThrow(new DCFCalculationException("Invalid financial data"));

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid financial data"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(dcfCalculationService).calculateDCF(any(DCFInput.class));
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should return 500 for server error")
    void testCalculateDCFServerError() throws Exception {
        // Arrange
        when(dcfCalculationService.calculateDCF(any(DCFInput.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("DCF calculation failed due to server error"));

        verify(dcfCalculationService).calculateDCF(any(DCFInput.class));
    }

    @Test
    @DisplayName("POST /dcf/sensitivity - Should perform sensitivity analysis successfully")
    void testSensitivityAnalysisSuccess() throws Exception {
        // Arrange
        DCFSensitivityAnalysis mockAnalysis = new DCFSensitivityAnalysis();
        mockAnalysis.setTicker("AAPL");
        mockAnalysis.setBaseCase(mockDCFOutput);
        
        when(dcfCalculationService.calculateSensitivityAnalysis(any(DCFInput.class), any(double[].class), any(double[].class)))
                .thenReturn(mockAnalysis);

        // Act & Assert
        mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Sensitivity analysis completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.baseCase").exists());

        verify(dcfCalculationService).calculateSensitivityAnalysis(any(DCFInput.class), any(double[].class), any(double[].class));
    }

    @Test
    @DisplayName("POST /dcf/sensitivity - Should return 400 for sensitivity analysis error")
    void testSensitivityAnalysisError() throws Exception {
        // Arrange
        when(dcfCalculationService.calculateSensitivityAnalysis(any(DCFInput.class), any(double[].class), any(double[].class)))
                .thenThrow(new DCFCalculationException("Failed to retrieve financial data"));

        // Act & Assert
        mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Failed to retrieve financial data"));

        verify(dcfCalculationService).calculateSensitivityAnalysis(any(DCFInput.class), any(double[].class), any(double[].class));
    }

    @Test
    @DisplayName("GET /dcf/history/{ticker} - Should return historical calculations")
    void testGetHistoricalCalculations() throws Exception {
        // Arrange
        List<DCFOutput> historicalOutputs = Arrays.asList(mockDCFOutput);
        when(dcfCalculationService.getHistoricalCalculations(eq("AAPL"), any()))
                .thenReturn(historicalOutputs);

        // Act & Assert
        mockMvc.perform(get("/dcf/history/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Historical calculations retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].ticker").value("AAPL"))
                .andExpect(jsonPath("$.data[0].fairValuePerShare").value(180.0));

        verify(dcfCalculationService).getHistoricalCalculations(eq("AAPL"), any());
    }

    @Test
    @DisplayName("GET /dcf/history/{ticker} - Should handle empty history")
    void testGetHistoricalCalculationsEmpty() throws Exception {
        // Arrange
        when(dcfCalculationService.getHistoricalCalculations(eq("AAPL"), any()))
                .thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/dcf/history/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Historical calculations retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(dcfCalculationService).getHistoricalCalculations(eq("AAPL"), any());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("GET /dcf/stats - Should return user statistics")
    void testGetUserStats() throws Exception {
        // Arrange
        DCFCalculationStats mockStats = new DCFCalculationStats(10, 6, 4);
        when(dcfCalculationService.getUserCalculationStats("user123")).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/dcf/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalCalculations").value(10))
                .andExpect(jsonPath("$.data.undervaluedCount").value(6))
                .andExpect(jsonPath("$.data.overvaluedCount").value(4));

        verify(dcfCalculationService).getUserCalculationStats("user123");
    }

    @Test
    @DisplayName("GET /dcf/stats - Should return 401 for unauthenticated user")
    void testGetUserStatsUnauthenticated() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/dcf/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verify(dcfCalculationService, never()).getUserCalculationStats(any());
    }

    @Test
    @DisplayName("POST /dcf/validate - Should validate DCF input successfully")
    void testValidateDCFInputSuccess() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("DCF input is valid"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("POST /dcf/validate - Should return validation errors for invalid input")
    void testValidateDCFInputInvalid() throws Exception {
        // Arrange
        DCFInputDto invalidInput = new DCFInputDto("", -5.0, 1500.0, -2.0);

        // Act & Assert
        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should handle missing required fields")
    void testCalculateDCFMissingFields() throws Exception {
        // Arrange
        DCFInputDto incompleteInput = new DCFInputDto();
        incompleteInput.setTicker("AAPL");
        // Missing discount rate, growth rate, terminal growth rate

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incompleteInput)))
                .andExpect(status().isBadRequest());

        verify(dcfCalculationService, never()).calculateDCF(any(DCFInput.class));
    }

    @Test
    @DisplayName("POST /dcf/calculate - Should handle edge case growth rates")
    void testCalculateDCFEdgeCaseGrowthRates() throws Exception {
        // Arrange
        DCFInputDto edgeCaseInput = new DCFInputDto("AAPL", 10.0, 999.0, 9.9);
        when(dcfCalculationService.calculateDCF(any(DCFInput.class))).thenReturn(mockDCFOutput);

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(edgeCaseInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        verify(dcfCalculationService).calculateDCF(any(DCFInput.class));
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("POST /dcf/calculate - Should include user ID when authenticated")
    void testCalculateDCFWithAuthentication() throws Exception {
        // Arrange
        when(dcfCalculationService.calculateDCF(any(DCFInput.class))).thenReturn(mockDCFOutput);

        // Act & Assert
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validDCFInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        // Verify that the service was called with a DCFInput that has the user ID
        verify(dcfCalculationService).calculateDCF(argThat(input -> 
            "user123".equals(input.getUserId()) && "AAPL".equals(input.getTicker())));
    }

    @Test
    @DisplayName("GET /dcf/history/{ticker} - Should handle service errors gracefully")
    void testGetHistoricalCalculationsServiceError() throws Exception {
        // Arrange
        when(dcfCalculationService.getHistoricalCalculations(eq("AAPL"), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/dcf/history/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Failed to retrieve historical calculations"));

        verify(dcfCalculationService).getHistoricalCalculations(eq("AAPL"), any());
    }
}