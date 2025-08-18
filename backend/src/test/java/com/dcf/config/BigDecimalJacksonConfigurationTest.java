package com.dcf.config;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to verify Jackson configuration for BigDecimal serialization.
 * This test validates that BigDecimal values are serialized as plain decimal strings
 * without scientific notation, meeting the requirements of task 9.
 */
public class BigDecimalJacksonConfigurationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JacksonConfig jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
    }

    @Test
    void testBigDecimalSerializationAsPlainStrings() throws Exception {
        // Test various BigDecimal values that could trigger scientific notation
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("smallDecimal", new BigDecimal("10.5"));
        testData.put("largeNumber", new BigDecimal("365817000000"));
        testData.put("veryLargeNumber", new BigDecimal("999999999999999.123456"));
        testData.put("verySmallNumber", new BigDecimal("0.000000123456"));
        testData.put("preciseDecimal", new BigDecimal("150.123456789"));
        testData.put("scientificInput", new BigDecimal("1.23456789E+15"));

        String json = objectMapper.writeValueAsString(testData);
        System.out.println("Serialized JSON: " + json);

        // Verify all values are serialized as plain decimal strings
        assertTrue(json.contains("\"10.5\""), "Small decimal should be plain string");
        assertTrue(json.contains("\"365817000000\""), "Large number should be plain string");
        assertTrue(json.contains("\"999999999999999.123456\""), "Very large number should be plain string");
        assertTrue(json.contains("\"0.000000123456\""), "Very small number should be plain string");
        assertTrue(json.contains("\"150.123456789\""), "Precise decimal should be plain string");
        assertTrue(json.contains("\"1234567890000000\""), "Scientific input should be plain string");

        // Verify no scientific notation patterns exist
        assertFalse(json.matches(".*\\d+[eE][+-]?\\d+.*"), "JSON should not contain scientific notation patterns");
    }

    @Test
    void testDCFInputDtoSerialization() throws Exception {
        DCFInputDto dto = new DCFInputDto();
        dto.setTicker("AAPL");
        dto.setDiscountRate(new BigDecimal("10.5"));
        dto.setGrowthRate(new BigDecimal("15.75"));
        dto.setTerminalGrowthRate(new BigDecimal("2.5"));

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("DCFInputDto JSON: " + json);

        // Verify BigDecimal fields are serialized as plain strings
        assertTrue(json.contains("\"discountRate\":\"10.5\""), "Discount rate should be plain string");
        assertTrue(json.contains("\"growthRate\":\"15.75\""), "Growth rate should be plain string");
        assertTrue(json.contains("\"terminalGrowthRate\":\"2.5\""), "Terminal growth rate should be plain string");

        // Verify no scientific notation
        assertFalse(json.matches(".*\\d+[eE][+-]?\\d+.*"), "JSON should not contain scientific notation");
    }

    @Test
    void testDCFOutputDtoSerialization() throws Exception {
        DCFOutputDto dto = new DCFOutputDto();
        dto.setTicker("AAPL");
        dto.setFairValuePerShare(new BigDecimal("150.123456"));
        dto.setCurrentPrice(new BigDecimal("145.67"));
        dto.setValuation("Undervalued");
        dto.setEnterpriseValue(new BigDecimal("2500000000000")); // 2.5 trillion

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("DCFOutputDto JSON: " + json);

        // Verify BigDecimal fields are serialized as plain strings
        assertTrue(json.contains("\"fairValuePerShare\":\"150.123456\""), "Fair value should be plain string");
        assertTrue(json.contains("\"currentPrice\":\"145.67\""), "Current price should be plain string");
        assertTrue(json.contains("\"enterpriseValue\":\"2500000000000\""), "Enterprise value should be plain string");

        // Verify no scientific notation
        assertFalse(json.matches(".*\\d+[eE][+-]?\\d+.*"), "JSON should not contain scientific notation");
    }

    @Test
    void testExtremeValuesSerialization() throws Exception {
        Map<String, BigDecimal> extremeValues = new HashMap<>();
        extremeValues.put("veryLarge", new BigDecimal("9.999999999999999E+20"));
        extremeValues.put("verySmall", new BigDecimal("1.23456789E-10"));
        extremeValues.put("maxPrecision", new BigDecimal("123456789012345678901234567890.123456789"));

        String json = objectMapper.writeValueAsString(extremeValues);
        System.out.println("Extreme values JSON: " + json);

        // Verify extreme values are serialized as plain strings
        assertTrue(json.contains("\"999999999999999900000\""), "Very large value should be plain string");
        assertTrue(json.contains("\"0.000000000123456789\""), "Very small value should be plain string");

        // Verify no scientific notation
        assertFalse(json.matches(".*\\d+[eE][+-]?\\d+.*"), "JSON should not contain scientific notation");
    }

    @Test
    void testRoundTripSerialization() throws Exception {
        DCFInputDto original = new DCFInputDto();
        original.setTicker("TEST");
        original.setDiscountRate(new BigDecimal("10.5"));
        original.setGrowthRate(new BigDecimal("15.75"));
        original.setTerminalGrowthRate(new BigDecimal("2.5"));

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        
        // Deserialize
        DCFInputDto deserialized = objectMapper.readValue(json, DCFInputDto.class);

        // Verify values are preserved
        assertEquals(original.getTicker(), deserialized.getTicker());
        assertEquals(0, original.getDiscountRate().compareTo(deserialized.getDiscountRate()));
        assertEquals(0, original.getGrowthRate().compareTo(deserialized.getGrowthRate()));
        assertEquals(0, original.getTerminalGrowthRate().compareTo(deserialized.getTerminalGrowthRate()));
    }

    @Test
    void testNullBigDecimalSerialization() throws Exception {
        DCFInputDto dto = new DCFInputDto();
        dto.setTicker("TEST");
        dto.setDiscountRate(null);
        dto.setGrowthRate(new BigDecimal("15.75"));
        dto.setTerminalGrowthRate(null);

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("Null values JSON: " + json);

        // Verify null values are handled correctly
        assertTrue(json.contains("\"discountRate\":null"), "Null BigDecimal should serialize as null");
        assertTrue(json.contains("\"growthRate\":\"15.75\""), "Non-null BigDecimal should serialize as plain string");
        assertTrue(json.contains("\"terminalGrowthRate\":null"), "Null BigDecimal should serialize as null");
    }
}