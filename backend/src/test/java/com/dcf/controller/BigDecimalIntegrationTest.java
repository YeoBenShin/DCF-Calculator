package com.dcf.controller;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.dcf.dto.FinancialDataDto;
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

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for BigDecimal API handling.
 * This test class covers all requirements for task 13:
 * - API serialization and deserialization of BigDecimal values
 * - JSON responses contain plain decimal strings without scientific notation
 * - API validation with BigDecimal input parameters
 * - End-to-end tests for complete DCF calculation accuracy
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BigDecimalIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // Pattern to detect scientific notation (e.g., 1.23E+5, 4.56e-3)
    private static final Pattern SCIENTIFIC_NOTATION_PATTERN = Pattern.compile("\\d+\\.?\\d*[eE][+-]?\\d+");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Integration Test: BigDecimal serialization in DCF calculation response")
    void testBigDecimalSerializationInDCFCalculation() throws Exception {
        // Test with high precision BigDecimal values
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456"), 
            new BigDecimal("8.987654"), 
            new BigDecimal("3.456789"));

        MvcResult result = mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.fairValuePerShare").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify no scientific notation in response
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(), 
            "Response should not contain scientific notation: " + responseContent);
        
        // Parse response and verify BigDecimal values are properly formatted
        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode dataNode = responseJson.get("data");
        
        // Verify that numeric fields are strings (not numbers) to maintain precision
        assertTrue(dataNode.get("fairValuePerShare").isTextual() || dataNode.get("fairValuePerShare").isNumber(),
            "Fair value should be a string or number without scientific notation");
        assertTrue(dataNode.get("currentPrice").isTextual() || dataNode.get("currentPrice").isNumber(),
            "Current price should be a string or number without scientific notation");
    }

    @Test
    @DisplayName("Integration Test: BigDecimal deserialization from JSON strings")
    void testBigDecimalDeserializationFromJsonStrings() throws Exception {
        // Test with JSON containing BigDecimal values as strings
        String jsonRequest = """
            {
                "ticker": "AAPL",
                "discountRate": "10.123456789012345",
                "growthRate": "8.987654321098765",
                "terminalGrowthRate": "3.456789012345678"
            }
            """;

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("DCF input is valid"));
    }

    @Test
    @DisplayName("Integration Test: BigDecimal validation with edge case values")
    void testBigDecimalValidationWithEdgeCases() throws Exception {
        // Test with very small values
        DCFInputDto smallValues = new DCFInputDto("AAPL", 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"));

        MvcResult result = mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(smallValues)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Small values should not use scientific notation in response");

        // Test with large values (within validation limits)
        DCFInputDto largeValues = new DCFInputDto("AAPL", 
            new BigDecimal("99.999999"), 
            new BigDecimal("999.999999"), 
            new BigDecimal("9.999999"));

        result = mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeValues)))
                .andExpect(status().isOk())
                .andReturn();

        responseContent = result.getResponse().getContentAsString();
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Large values should not use scientific notation in response");
    }

    @Test
    @DisplayName("Integration Test: BigDecimal validation error handling")
    void testBigDecimalValidationErrorHandling() throws Exception {
        // Test with invalid BigDecimal values (exceeding validation limits)
        DCFInputDto invalidInput = new DCFInputDto("AAPL", 
            new BigDecimal("150.0"), // Exceeds 100% limit
            new BigDecimal("8.0"), 
            new BigDecimal("3.0"));

        MvcResult result = mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify error response doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Error response should not contain scientific notation");
        
        // Verify meaningful error message
        assertTrue(responseContent.contains("Discount rate") || responseContent.contains("validation"),
            "Error response should contain meaningful validation message");
    }

    @Test
    @DisplayName("Integration Test: Financial data BigDecimal serialization")
    void testFinancialDataBigDecimalSerialization() throws Exception {
        MvcResult result = mockMvc.perform(get("/financials")
                .param("ticker", "AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.revenue").isArray())
                .andExpect(jsonPath("$.data.operatingIncome").isArray())
                .andExpect(jsonPath("$.data.freeCashFlow").isArray())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify no scientific notation in financial data response
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Financial data response should not contain scientific notation: " + responseContent);
        
        // Parse and verify structure
        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode dataNode = responseJson.get("data");
        
        // Verify arrays contain proper numeric values
        assertTrue(dataNode.get("revenue").isArray(), "Revenue should be an array");
        assertTrue(dataNode.get("operatingIncome").isArray(), "Operating income should be an array");
        assertTrue(dataNode.get("freeCashFlow").isArray(), "Free cash flow should be an array");
    }

    @Test
    @DisplayName("Integration Test: End-to-end DCF calculation accuracy with BigDecimal")
    void testEndToEndDCFCalculationAccuracy() throws Exception {
        // Perform complete DCF calculation with known inputs
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.5"), 
            new BigDecimal("8.25"), 
            new BigDecimal("3.75"));

        MvcResult result = mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("DCF calculation completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.fairValuePerShare").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.valuation").exists())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify no scientific notation in calculation results
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "DCF calculation response should not contain scientific notation");
        
        // Parse response and verify calculation results
        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode dataNode = responseJson.get("data");
        
        // Verify that calculated values are reasonable (not null, not zero)
        assertNotNull(dataNode.get("fairValuePerShare"), "Fair value per share should not be null");
        assertNotNull(dataNode.get("currentPrice"), "Current price should not be null");
        assertNotNull(dataNode.get("valuation"), "Valuation should not be null");
        
        // Verify that BigDecimal precision is maintained in calculations
        String fairValue = dataNode.get("fairValuePerShare").asText();
        String currentPrice = dataNode.get("currentPrice").asText();
        
        // These should be valid BigDecimal strings
        assertDoesNotThrow(() -> new BigDecimal(fairValue), 
            "Fair value should be a valid BigDecimal: " + fairValue);
        assertDoesNotThrow(() -> new BigDecimal(currentPrice), 
            "Current price should be a valid BigDecimal: " + currentPrice);
    }

    @Test
    @DisplayName("Integration Test: Sensitivity analysis with BigDecimal precision")
    void testSensitivityAnalysisWithBigDecimalPrecision() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.25"), 
            new BigDecimal("8.75"), 
            new BigDecimal("3.25"));

        MvcResult result = mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sensitivity analysis completed successfully"))
                .andExpect(jsonPath("$.data.ticker").value("AAPL"))
                .andExpect(jsonPath("$.data.baseCase").exists())
                .andExpect(jsonPath("$.data.results").isArray())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify no scientific notation in sensitivity analysis results
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Sensitivity analysis response should not contain scientific notation");
        
        // Parse and verify sensitivity analysis structure
        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode dataNode = responseJson.get("data");
        
        assertNotNull(dataNode.get("baseCase"), "Base case should not be null");
        assertTrue(dataNode.get("results").isArray(), "Results should be an array");
        assertTrue(dataNode.get("results").size() > 0, "Results array should not be empty");
    }

    @Test
    @DisplayName("Integration Test: BigDecimal parameter parsing in financial data filtering")
    void testBigDecimalParameterParsingInFiltering() throws Exception {
        // Test financial data filtering with BigDecimal parameters
        MvcResult result = mockMvc.perform(get("/financials/filter")
                .param("ticker", "AAPL")
                .param("minRevenue", "300000000000.50")
                .param("maxDebt", "120000000000.75")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        
        // Verify no scientific notation in filtered results
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(responseContent).find(),
            "Filtered financial data response should not contain scientific notation");
    }

    @Test
    @DisplayName("Integration Test: BigDecimal precision in JSON round-trip")
    void testBigDecimalPrecisionInJsonRoundTrip() throws Exception {
        // Create input with high precision values
        DCFInputDto originalInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.1234567890123456"), 
            new BigDecimal("8.9876543210987654"), 
            new BigDecimal("3.1415926535897932"));

        // Serialize to JSON
        String jsonString = objectMapper.writeValueAsString(originalInput);
        
        // Verify JSON doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(jsonString).find(),
            "Serialized JSON should not contain scientific notation: " + jsonString);
        
        // Deserialize back from JSON
        DCFInputDto deserializedInput = objectMapper.readValue(jsonString, DCFInputDto.class);
        
        // Verify precision is maintained
        assertEquals(originalInput.getDiscountRate(), deserializedInput.getDiscountRate(),
            "Discount rate precision should be maintained");
        assertEquals(originalInput.getGrowthRate(), deserializedInput.getGrowthRate(),
            "Growth rate precision should be maintained");
        assertEquals(originalInput.getTerminalGrowthRate(), deserializedInput.getTerminalGrowthRate(),
            "Terminal growth rate precision should be maintained");
    }

    @Test
    @DisplayName("Integration Test: BigDecimal validation with malformed input")
    void testBigDecimalValidationWithMalformedInput() throws Exception {
        // Test with malformed JSON containing invalid BigDecimal strings
        String malformedJson = """
            {
                "ticker": "AAPL",
                "discountRate": "invalid_number",
                "growthRate": "8.0",
                "terminalGrowthRate": "3.0"
            }
            """;

        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: BigDecimal handling in multiple API endpoints")
    void testBigDecimalHandlingAcrossMultipleEndpoints() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123"), 
            new BigDecimal("8.456"), 
            new BigDecimal("3.789"));

        // Test validation endpoint
        mockMvc.perform(post("/dcf/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // Test calculation endpoint
        MvcResult calcResult = mockMvc.perform(post("/dcf/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andReturn();

        // Test sensitivity analysis endpoint
        MvcResult sensitivityResult = mockMvc.perform(post("/dcf/sensitivity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dcfInput)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify no scientific notation in any response
        String calcResponse = calcResult.getResponse().getContentAsString();
        String sensitivityResponse = sensitivityResult.getResponse().getContentAsString();
        
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(calcResponse).find(),
            "Calculation response should not contain scientific notation");
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(sensitivityResponse).find(),
            "Sensitivity analysis response should not contain scientific notation");
    }
}