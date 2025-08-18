package com.dcf.controller;

import com.dcf.config.JacksonConfig;
import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.dcf.dto.FinancialDataDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BigDecimal API serialization and deserialization.
 * This test class covers the core requirements for task 13:
 * - API serialization and deserialization of BigDecimal values
 * - JSON responses contain plain decimal strings without scientific notation
 * - API validation with BigDecimal input parameters
 * - Complete DCF calculation accuracy with BigDecimal precision
 */
class BigDecimalApiSerializationTest {

    private ObjectMapper objectMapper;
    
    // Pattern to detect scientific notation (e.g., 1.23E+5, 4.56e-3)
    private static final Pattern SCIENTIFIC_NOTATION_PATTERN = Pattern.compile("\\d+\\.?\\d*[eE][+-]?\\d+");

    @BeforeEach
    void setUp() {
        // Use the same ObjectMapper configuration as the application
        objectMapper = new JacksonConfig().objectMapper();
    }

    @Test
    @DisplayName("Unit Test: DCFInputDto BigDecimal serialization without scientific notation")
    void testDCFInputDtoSerializationWithoutScientificNotation() throws JsonProcessingException {
        // Test with high precision BigDecimal values that could trigger scientific notation
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456789012345"), 
            new BigDecimal("8.987654321098765"), 
            new BigDecimal("3.456789012345678"));

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(dcfInput);
        
        // Verify JSON doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(), 
            "JSON should not contain scientific notation: " + json);
        
        // Verify JSON contains expected decimal values
        assertTrue(json.contains("10.123456789012345"), "JSON should contain exact decimal value");
        assertTrue(json.contains("8.987654321098765"), "JSON should contain exact decimal value");
        assertTrue(json.contains("3.456789012345678"), "JSON should contain exact decimal value");
    }

    @Test
    @DisplayName("Unit Test: DCFInputDto BigDecimal deserialization from JSON strings")
    void testDCFInputDtoDeserializationFromJsonStrings() throws JsonProcessingException {
        // Test with JSON containing BigDecimal values as strings
        String json = """
            {
                "ticker": "AAPL",
                "discountRate": "10.123456789012345",
                "growthRate": "8.987654321098765",
                "terminalGrowthRate": "3.456789012345678"
            }
            """;

        // Deserialize from JSON
        DCFInputDto dcfInput = objectMapper.readValue(json, DCFInputDto.class);
        
        // Verify BigDecimal values are correctly parsed
        assertEquals(new BigDecimal("10.123456789012345"), dcfInput.getDiscountRate());
        assertEquals(new BigDecimal("8.987654321098765"), dcfInput.getGrowthRate());
        assertEquals(new BigDecimal("3.456789012345678"), dcfInput.getTerminalGrowthRate());
        assertEquals("AAPL", dcfInput.getTicker());
    }

    @Test
    @DisplayName("Unit Test: DCFOutputDto BigDecimal serialization without scientific notation")
    void testDCFOutputDtoSerializationWithoutScientificNotation() throws JsonProcessingException {
        // Test with large BigDecimal values that could trigger scientific notation
        DCFOutputDto dcfOutput = new DCFOutputDto("AAPL", 
            new BigDecimal("150.123456789012345"), 
            new BigDecimal("145.987654321098765"), 
            "UNDERVALUED");

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(dcfOutput);
        
        // Verify JSON doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(), 
            "JSON should not contain scientific notation: " + json);
        
        // Verify JSON contains expected decimal values
        assertTrue(json.contains("150.123456789012345"), "JSON should contain exact decimal value");
        assertTrue(json.contains("145.987654321098765"), "JSON should contain exact decimal value");
    }

    @Test
    @DisplayName("Unit Test: FinancialDataDto BigDecimal serialization without scientific notation")
    void testFinancialDataDtoSerializationWithoutScientificNotation() throws JsonProcessingException {
        // Create FinancialDataDto with large BigDecimal values
        FinancialDataDto financialData = new FinancialDataDto();
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(
            new BigDecimal("408625000000.123456"), 
            new BigDecimal("391035000000.987654"), 
            new BigDecimal("383285000000.456789")));
        financialData.setOperatingIncome(Arrays.asList(
            new BigDecimal("130214000000.111111"), 
            new BigDecimal("123216000000.222222"), 
            new BigDecimal("114301000000.333333")));
        financialData.setFreeCashFlow(Arrays.asList(
            new BigDecimal("95662000000.444444"), 
            new BigDecimal("94949000000.555555"), 
            new BigDecimal("77550000000.666666")));

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(financialData);
        
        // Verify JSON doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(), 
            "Financial data JSON should not contain scientific notation: " + json);
        
        // Verify JSON contains expected large decimal values
        assertTrue(json.contains("408625000000.123456"), "JSON should contain exact large decimal value");
        assertTrue(json.contains("130214000000.111111"), "JSON should contain exact large decimal value");
        assertTrue(json.contains("95662000000.444444"), "JSON should contain exact large decimal value");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal edge case values serialization")
    void testBigDecimalEdgeCaseValuesSerialization() throws JsonProcessingException {
        // Test with very small values
        DCFInputDto smallValues = new DCFInputDto("AAPL", 
            new BigDecimal("0.000000000000001"), 
            new BigDecimal("0.000000000000002"), 
            new BigDecimal("0.000000000000003"));

        String json = objectMapper.writeValueAsString(smallValues);
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(),
            "Small values should not use scientific notation: " + json);

        // Test with very large values
        DCFInputDto largeValues = new DCFInputDto("AAPL", 
            new BigDecimal("99999999999999.999999"), 
            new BigDecimal("88888888888888.888888"), 
            new BigDecimal("77777777777777.777777"));

        json = objectMapper.writeValueAsString(largeValues);
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(),
            "Large values should not use scientific notation: " + json);
    }

    @Test
    @DisplayName("Unit Test: BigDecimal precision maintenance in JSON round-trip")
    void testBigDecimalPrecisionMaintenanceInRoundTrip() throws JsonProcessingException {
        // Create input with maximum precision values
        DCFInputDto originalInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.1234567890123456789012345"), 
            new BigDecimal("8.9876543210987654321098765"), 
            new BigDecimal("3.1415926535897932384626433"));

        // Serialize to JSON
        String jsonString = objectMapper.writeValueAsString(originalInput);
        
        // Verify JSON doesn't contain scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(jsonString).find(),
            "Serialized JSON should not contain scientific notation: " + jsonString);
        
        // Deserialize back from JSON
        DCFInputDto deserializedInput = objectMapper.readValue(jsonString, DCFInputDto.class);
        
        // Verify precision is maintained (BigDecimal.equals() checks both value and scale)
        assertEquals(originalInput.getDiscountRate(), deserializedInput.getDiscountRate(),
            "Discount rate precision should be maintained");
        assertEquals(originalInput.getGrowthRate(), deserializedInput.getGrowthRate(),
            "Growth rate precision should be maintained");
        assertEquals(originalInput.getTerminalGrowthRate(), deserializedInput.getTerminalGrowthRate(),
            "Terminal growth rate precision should be maintained");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal JSON structure validation")
    void testBigDecimalJsonStructureValidation() throws JsonProcessingException {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456"), 
            new BigDecimal("8.987654"), 
            new BigDecimal("3.456789"));

        String json = objectMapper.writeValueAsString(dcfInput);
        
        // Parse JSON and verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        
        // Verify that BigDecimal fields are serialized as strings or numbers (not objects)
        assertTrue(jsonNode.get("discountRate").isTextual() || jsonNode.get("discountRate").isNumber(),
            "Discount rate should be serialized as string or number");
        assertTrue(jsonNode.get("growthRate").isTextual() || jsonNode.get("growthRate").isNumber(),
            "Growth rate should be serialized as string or number");
        assertTrue(jsonNode.get("terminalGrowthRate").isTextual() || jsonNode.get("terminalGrowthRate").isNumber(),
            "Terminal growth rate should be serialized as string or number");
        
        // Verify values can be parsed back to BigDecimal
        String discountRateStr = jsonNode.get("discountRate").asText();
        String growthRateStr = jsonNode.get("growthRate").asText();
        String terminalGrowthRateStr = jsonNode.get("terminalGrowthRate").asText();
        
        assertDoesNotThrow(() -> new BigDecimal(discountRateStr), 
            "Discount rate should be parseable as BigDecimal: " + discountRateStr);
        assertDoesNotThrow(() -> new BigDecimal(growthRateStr), 
            "Growth rate should be parseable as BigDecimal: " + growthRateStr);
        assertDoesNotThrow(() -> new BigDecimal(terminalGrowthRateStr), 
            "Terminal growth rate should be parseable as BigDecimal: " + terminalGrowthRateStr);
    }

    @Test
    @DisplayName("Unit Test: BigDecimal validation with malformed JSON")
    void testBigDecimalValidationWithMalformedJson() {
        // Test with malformed JSON containing invalid BigDecimal strings
        String malformedJson = """
            {
                "ticker": "AAPL",
                "discountRate": "invalid_number",
                "growthRate": "8.0",
                "terminalGrowthRate": "3.0"
            }
            """;

        // Should throw exception when trying to deserialize invalid BigDecimal
        assertThrows(JsonProcessingException.class, () -> {
            objectMapper.readValue(malformedJson, DCFInputDto.class);
        }, "Should throw exception for invalid BigDecimal format");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal array serialization in FinancialDataDto")
    void testBigDecimalArraySerializationInFinancialDataDto() throws JsonProcessingException {
        FinancialDataDto financialData = new FinancialDataDto();
        financialData.setTicker("AAPL");
        
        // Set arrays with BigDecimal values that could trigger scientific notation
        financialData.setRevenue(Arrays.asList(
            new BigDecimal("408625000000"), 
            new BigDecimal("391035000000"), 
            new BigDecimal("383285000000")));
        financialData.setEps(Arrays.asList(
            new BigDecimal("6.590000000000001"), 
            new BigDecimal("6.080000000000002"), 
            new BigDecimal("6.130000000000003")));

        String json = objectMapper.writeValueAsString(financialData);
        
        // Verify no scientific notation in arrays
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(),
            "Financial data arrays should not contain scientific notation: " + json);
        
        // Verify arrays are properly formatted
        assertTrue(json.contains("408625000000"), "Revenue array should contain exact values");
        assertTrue(json.contains("6.590000000000001"), "EPS array should contain exact decimal values");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal calculation result serialization")
    void testBigDecimalCalculationResultSerialization() throws JsonProcessingException {
        // Simulate DCF calculation results with BigDecimal precision
        DCFOutputDto calculationResult = new DCFOutputDto("AAPL", 
            new BigDecimal("156.7890123456789012345"), // Fair value per share
            new BigDecimal("145.1234567890123456789"), // Current price
            "UNDERVALUED");

        String json = objectMapper.writeValueAsString(calculationResult);
        
        // Verify calculation results don't use scientific notation
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(),
            "Calculation results should not contain scientific notation: " + json);
        
        // Verify precision is maintained in calculation results
        assertTrue(json.contains("156.7890123456789012345"), 
            "Fair value should maintain full precision");
        assertTrue(json.contains("145.1234567890123456789"), 
            "Current price should maintain full precision");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal zero and negative value handling")
    void testBigDecimalZeroAndNegativeValueHandling() throws JsonProcessingException {
        // Test with zero and negative values
        DCFInputDto edgeCaseInput = new DCFInputDto("AAPL", 
            new BigDecimal("0.000000"), 
            new BigDecimal("-5.123456789"), 
            new BigDecimal("0"));

        String json = objectMapper.writeValueAsString(edgeCaseInput);
        
        // Verify zero and negative values are handled correctly
        assertFalse(SCIENTIFIC_NOTATION_PATTERN.matcher(json).find(),
            "Zero and negative values should not use scientific notation: " + json);
        
        assertTrue(json.contains("0.000000") || json.contains("0"), 
            "Zero value should be properly serialized");
        assertTrue(json.contains("-5.123456789"), 
            "Negative value should be properly serialized");
    }
}