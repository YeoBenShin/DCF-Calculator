package com.dcf.controller;

import com.dcf.dto.DCFInputDto;
import com.dcf.repository.DCFInputRepository;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.repository.FinancialDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class DCFCalculationControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DCFInputRepository dcfInputRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        dcfInputRepository.deleteAll();
        dcfOutputRepository.deleteAll();
        financialDataRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Complete DCF calculation flow")
    void testCompleteDCFCalculationFlow() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));

        // Perform DCF calculation
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DCF calculation completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.fairValuePerShare").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.valuation").exists());

        // Verify data is persisted
        assert dcfInputRepository.count() > 0;
        assert dcfOutputRepository.count() > 0;
    }

    @Test
    @DisplayName("Integration Test: DCF calculation with sensitivity analysis")
    void testDCFCalculationWithSensitivityAnalysis() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("GOOGL", new BigDecimal("12.0"), new BigDecimal("10.0"), new BigDecimal("2.5"));

        // Perform sensitivity analysis
        mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sensitivity analysis completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("GOOGL"))
                .andExpect(jsonPath("$.data.baseCase").exists())
                .andExpect(jsonPath("$.data.results").isArray());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Integration Test: User-specific calculations and statistics")
    void testUserSpecificCalculationsAndStats() throws Exception {
        DCFInputDto dcfInput1 = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        DCFInputDto dcfInput2 = new DCFInputDto("MSFT", new BigDecimal("11.0"), new BigDecimal("7.0"), new BigDecimal("2.8"));

        // Perform multiple calculations
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput2)))
                .andExpect(status().isOk());

        // Get user statistics
        mockMvc.perform(get("/dcf/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalCalculations").value(2))
                .andExpect(jsonPath("$.data.undervaluedCount").exists())
                .andExpect(jsonPath("$.data.overvaluedCount").exists());
    }

    @Test
    @DisplayName("Integration Test: Historical calculations retrieval")
    void testHistoricalCalculationsRetrieval() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("TSLA", new BigDecimal("15.0"), new BigDecimal("12.0"), new BigDecimal("4.0"));

        // Perform calculation
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk());

        // Get historical calculations
        mockMvc.perform(get("/dcf/history/TSLA")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Historical calculations retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].ticker").value("TSLA"));
    }

    @Test
    @DisplayName("Integration Test: Input validation")
    void testInputValidation() throws Exception {
        // Test invalid ticker (empty)
        DCFInputDto invalidInput1 = new DCFInputDto("", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput1)))
                .andExpect(status().isBadRequest());

        // Test invalid discount rate (negative)
        DCFInputDto invalidInput2 = new DCFInputDto("AAPL", new BigDecimal("-5.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput2)))
                .andExpect(status().isBadRequest());

        // Test invalid growth rate (too high)
        DCFInputDto invalidInput3 = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("1500.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput3)))
                .andExpect(status().isBadRequest());

        // Test invalid terminal growth rate (negative)
        DCFInputDto invalidInput4 = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("-2.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput4)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: DCF validation endpoint")
    void testDCFValidationEndpoint() throws Exception {
        // Test valid input
        DCFInputDto validInput = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("DCF input is valid"));

        // Test invalid input
        DCFInputDto invalidInput = new DCFInputDto("", new BigDecimal("-5.0"), new BigDecimal("1500.0"), new BigDecimal("-2.0"));
        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: Multiple tickers DCF calculations")
    void testMultipleTickersDCFCalculations() throws Exception {
        String[] tickers = {"AAPL", "GOOGL", "MSFT", "AMZN"};
        
        for (String ticker : tickers) {
            DCFInputDto dcfInput = new DCFInputDto(ticker, new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
            
            mockMvc.perform(post("/dcf/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dcfInput)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.ticker").value(ticker))
                    .andExpect(jsonPath("$.data.fairValuePerShare").exists());
        }
    }

    @Test
    @DisplayName("Integration Test: Edge case growth rates")
    void testEdgeCaseGrowthRates() throws Exception {
        // Test maximum allowed growth rate
        DCFInputDto maxGrowthInput = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("999.0"), new BigDecimal("9.9"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maxGrowthInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        // Test minimum allowed growth rate
        DCFInputDto minGrowthInput = new DCFInputDto("AAPL", new BigDecimal("10.0"), new BigDecimal("-99.0"), new BigDecimal("0.1"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minGrowthInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Case insensitive ticker handling")
    void testCaseInsensitiveTickerHandling() throws Exception {
        // Test lowercase ticker
        DCFInputDto lowercaseInput = new DCFInputDto("aapl", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lowercaseInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));

        // Test mixed case ticker
        DCFInputDto mixedCaseInput = new DCFInputDto("AaPl", new BigDecimal("10.0"), new BigDecimal("8.0"), new BigDecimal("3.0"));
        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mixedCaseInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: Unauthenticated user statistics access")
    void testUnauthenticatedUserStatsAccess() throws Exception {
        mockMvc.perform(get("/dcf/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Integration Test: BigDecimal precision handling")
    void testBigDecimalPrecisionHandling() throws Exception {
        // Test with high precision BigDecimal values
        DCFInputDto precisionInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456"), 
            new BigDecimal("8.987654"), 
            new BigDecimal("3.456789"));

        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(precisionInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.fairValuePerShare").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists());
    }

    @Test
    @DisplayName("Integration Test: BigDecimal JSON serialization format")
    void testBigDecimalJsonSerializationFormat() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("8.0"), 
            new BigDecimal("3.0"));

        String response = mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify that BigDecimal values are serialized as plain strings, not scientific notation
        assert !response.contains("E");
        assert !response.contains("e");
    }

    @Test
    @DisplayName("Integration Test: Large BigDecimal values")
    void testLargeBigDecimalValues() throws Exception {
        // Test with very large values that would cause scientific notation with Double
        DCFInputDto largeValueInput = new DCFInputDto("AAPL", 
            new BigDecimal("99.999999"), 
            new BigDecimal("999.999999"), 
            new BigDecimal("9.999999"));

        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeValueInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }

    @Test
    @DisplayName("Integration Test: BigDecimal validation precision limits")
    void testBigDecimalValidationPrecisionLimits() throws Exception {
        // Test with too many decimal places (should still work due to BigDecimal flexibility)
        DCFInputDto highPrecisionInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.1234567890123456"), 
            new BigDecimal("8.9876543210987654"), 
            new BigDecimal("3.1415926535897932"));

        mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(highPrecisionInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"));
    }
}