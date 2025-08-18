package com.dcf.controller;

import com.dcf.dto.DCFInputDto;
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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests specifically for BigDecimal handling in API controllers.
 * This test class focuses on verifying that controllers properly handle BigDecimal
 * request/response objects and JSON serialization.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BigDecimalApiHandlingTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("API Test: BigDecimal request parsing and validation")
    void testBigDecimalRequestParsingAndValidation() throws Exception {
        // Test with high precision BigDecimal values
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456789"), 
            new BigDecimal("8.987654321"), 
            new BigDecimal("3.456789012"));

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("DCF input is valid"));
    }

    @Test
    @DisplayName("API Test: BigDecimal JSON serialization without scientific notation")
    void testBigDecimalJsonSerializationWithoutScientificNotation() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("99.999999"), 
            new BigDecimal("999.999999"), 
            new BigDecimal("9.999999"));

        String response = mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify that BigDecimal values are serialized as plain strings, not scientific notation
        assert !response.contains("E") : "Response contains scientific notation: " + response;
        assert !response.contains("e") : "Response contains scientific notation: " + response;
    }

    @Test
    @DisplayName("API Test: BigDecimal precision validation limits")
    void testBigDecimalPrecisionValidationLimits() throws Exception {
        // Test with values that exceed validation limits
        DCFInputDto invalidInput = new DCFInputDto("AAPL", 
            new BigDecimal("150.0"), // Exceeds 100% limit
            new BigDecimal("8.0"), 
            new BigDecimal("3.0"));

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API Test: Financial data BigDecimal serialization")
    void testFinancialDataBigDecimalSerialization() throws Exception {
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

        // Verify that BigDecimal values in financial data are serialized correctly
        assert !response.contains("E") : "Financial data response contains scientific notation: " + response;
        assert !response.contains("e") : "Financial data response contains scientific notation: " + response;
    }

    @Test
    @DisplayName("API Test: BigDecimal edge case values")
    void testBigDecimalEdgeCaseValues() throws Exception {
        // Test with very small decimal values
        DCFInputDto smallValueInput = new DCFInputDto("AAPL", 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"));

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(smallValueInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // Test with maximum allowed values
        DCFInputDto maxValueInput = new DCFInputDto("AAPL", 
            new BigDecimal("99.999999"), 
            new BigDecimal("999.999999"), 
            new BigDecimal("9.999999"));

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maxValueInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("API Test: BigDecimal request content type handling")
    void testBigDecimalRequestContentTypeHandling() throws Exception {
        // Test that BigDecimal values are properly parsed from JSON strings
        String jsonRequest = """
            {
                "ticker": "AAPL",
                "discountRate": "10.123456789",
                "growthRate": "8.987654321",
                "terminalGrowthRate": "3.456789012"
            }
            """;

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("API Test: BigDecimal sensitivity analysis handling")
    void testBigDecimalSensitivityAnalysisHandling() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.5"), 
            new BigDecimal("8.25"), 
            new BigDecimal("3.75"));

        String response = mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sensitivity analysis completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify sensitivity analysis results don't contain scientific notation
        assert !response.contains("E") : "Sensitivity analysis response contains scientific notation: " + response;
        assert !response.contains("e") : "Sensitivity analysis response contains scientific notation: " + response;
    }
}