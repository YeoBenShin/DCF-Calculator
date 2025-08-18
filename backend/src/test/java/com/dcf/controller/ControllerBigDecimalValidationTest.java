package com.dcf.controller;

import com.dcf.config.JacksonConfig;
import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to validate BigDecimal handling in controller DTOs and serialization.
 * These tests verify that the API layer properly handles BigDecimal values without
 * requiring a full Spring context.
 */
class ControllerBigDecimalValidationTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    @DisplayName("Unit Test: DCFInputDto BigDecimal serialization")
    void testDCFInputDtoBigDecimalSerialization() throws Exception {
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.123456"), 
            new BigDecimal("8.987654"), 
            new BigDecimal("3.456789"));

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(dcfInput);
        
        // Verify JSON doesn't contain scientific notation in numeric values
        assertFalse(json.matches(".*\"[^\"]*[0-9]+[Ee][+-]?[0-9]+[^\"]*\".*"), "JSON should not contain scientific notation: " + json);
        
        // Verify JSON contains expected values
        assertTrue(json.contains("10.123456"), "JSON should contain exact decimal value");
        assertTrue(json.contains("8.987654"), "JSON should contain exact decimal value");
        assertTrue(json.contains("3.456789"), "JSON should contain exact decimal value");
    }

    @Test
    @DisplayName("Unit Test: DCFInputDto BigDecimal deserialization")
    void testDCFInputDtoBigDecimalDeserialization() throws Exception {
        String json = """
            {
                "ticker": "AAPL",
                "discountRate": "10.123456",
                "growthRate": "8.987654",
                "terminalGrowthRate": "3.456789"
            }
            """;

        // Deserialize from JSON
        DCFInputDto dcfInput = objectMapper.readValue(json, DCFInputDto.class);
        
        // Verify BigDecimal values are correctly parsed
        assertEquals(new BigDecimal("10.123456"), dcfInput.getDiscountRate());
        assertEquals(new BigDecimal("8.987654"), dcfInput.getGrowthRate());
        assertEquals(new BigDecimal("3.456789"), dcfInput.getTerminalGrowthRate());
        assertEquals("AAPL", dcfInput.getTicker());
    }

    @Test
    @DisplayName("Unit Test: DCFOutputDto BigDecimal serialization")
    void testDCFOutputDtoBigDecimalSerialization() throws Exception {
        DCFOutputDto dcfOutput = new DCFOutputDto("AAPL", 
            new BigDecimal("150.123456"), 
            new BigDecimal("145.987654"), 
            "UNDERVALUED");

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(dcfOutput);
        
        // Verify JSON doesn't contain scientific notation in numeric values
        assertFalse(json.matches(".*\"[^\"]*[0-9]+[Ee][+-]?[0-9]+[^\"]*\".*"), "JSON should not contain scientific notation: " + json);
        
        // Verify JSON contains expected values
        assertTrue(json.contains("150.123456"), "JSON should contain exact decimal value");
        assertTrue(json.contains("145.987654"), "JSON should contain exact decimal value");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal precision handling")
    void testBigDecimalPrecisionHandling() throws Exception {
        // Test with high precision values
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.1234567890123456"), 
            new BigDecimal("8.9876543210987654"), 
            new BigDecimal("3.1415926535897932"));

        // Serialize and deserialize
        String json = objectMapper.writeValueAsString(dcfInput);
        DCFInputDto deserialized = objectMapper.readValue(json, DCFInputDto.class);
        
        // Verify precision is maintained
        assertEquals(dcfInput.getDiscountRate(), deserialized.getDiscountRate());
        assertEquals(dcfInput.getGrowthRate(), deserialized.getGrowthRate());
        assertEquals(dcfInput.getTerminalGrowthRate(), deserialized.getTerminalGrowthRate());
    }

    @Test
    @DisplayName("Unit Test: BigDecimal edge case values")
    void testBigDecimalEdgeCaseValues() throws Exception {
        // Test with very small values
        DCFInputDto smallValues = new DCFInputDto("AAPL", 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"), 
            new BigDecimal("0.000001"));

        String json = objectMapper.writeValueAsString(smallValues);
        assertFalse(json.matches(".*\"[^\"]*[0-9]+[Ee][+-]?[0-9]+[^\"]*\".*"), "Small values should not use scientific notation");

        // Test with large values
        DCFInputDto largeValues = new DCFInputDto("AAPL", 
            new BigDecimal("99.999999"), 
            new BigDecimal("999.999999"), 
            new BigDecimal("9.999999"));

        json = objectMapper.writeValueAsString(largeValues);
        assertFalse(json.matches(".*\"[^\"]*[0-9]+[Ee][+-]?[0-9]+[^\"]*\".*"), "Large values should not use scientific notation");
    }

    @Test
    @DisplayName("Unit Test: BigDecimal validation annotations compatibility")
    void testBigDecimalValidationAnnotationsCompatibility() {
        // Test that BigDecimal values work with validation annotations
        DCFInputDto dcfInput = new DCFInputDto("AAPL", 
            new BigDecimal("10.0"), 
            new BigDecimal("8.0"), 
            new BigDecimal("3.0"));

        // Verify that BigDecimal values are properly set
        assertNotNull(dcfInput.getDiscountRate());
        assertNotNull(dcfInput.getGrowthRate());
        assertNotNull(dcfInput.getTerminalGrowthRate());
        
        // Verify that BigDecimal comparisons work (used by validation annotations)
        assertTrue(dcfInput.getDiscountRate().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(dcfInput.getGrowthRate().compareTo(new BigDecimal("-100")) > 0);
        assertTrue(dcfInput.getTerminalGrowthRate().compareTo(BigDecimal.ZERO) > 0);
    }
}