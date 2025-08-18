package com.dcf.config;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.dcf.dto.FinancialDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class BigDecimalSerializationTest {

    private ObjectMapper objectMapper;

    private DCFInputDto dcfInputDto;
    private DCFOutputDto dcfOutputDto;
    private FinancialDataDto financialDataDto;

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper with our custom configuration
        JacksonConfig jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
        
        // Create test data with various BigDecimal values including large numbers that might use scientific notation
        dcfInputDto = new DCFInputDto();
        dcfInputDto.setTicker("AAPL");
        dcfInputDto.setDiscountRate(new BigDecimal("10.5"));
        dcfInputDto.setGrowthRate(new BigDecimal("15.75"));
        dcfInputDto.setTerminalGrowthRate(new BigDecimal("2.5"));

        dcfOutputDto = new DCFOutputDto();
        dcfOutputDto.setTicker("AAPL");
        dcfOutputDto.setFairValuePerShare(new BigDecimal("150.123456"));
        dcfOutputDto.setCurrentPrice(new BigDecimal("145.67"));
        dcfOutputDto.setValuation("Undervalued");

        financialDataDto = new FinancialDataDto();
        financialDataDto.setTicker("AAPL");
        financialDataDto.setRevenue(Arrays.asList(
            new BigDecimal("365817000000"), // Large number that could trigger scientific notation
            new BigDecimal("274515000000"),
            new BigDecimal("260174000000")
        ));
        financialDataDto.setFreeCashFlow(Arrays.asList(
            new BigDecimal("92953000000"),
            new BigDecimal("73365000000"),
            new BigDecimal("58896000000")
        ));
    }

    @Test
    void testDCFInputDtoSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(dcfInputDto);
        
        // Verify that BigDecimal values are serialized as plain decimal strings
        assertTrue(json.contains("\"discountRate\":\"10.5\""), 
                   "Discount rate should be serialized as plain decimal string");
        assertTrue(json.contains("\"growthRate\":\"15.75\""), 
                   "Growth rate should be serialized as plain decimal string");
        assertTrue(json.contains("\"terminalGrowthRate\":\"2.5\""), 
                   "Terminal growth rate should be serialized as plain decimal string");
        
        // Verify no scientific notation
        assertFalse(json.contains("E"), "JSON should not contain scientific notation");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation");
        
        System.out.println("DCFInputDto JSON: " + json);
    }

    @Test
    void testDCFOutputDtoSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(dcfOutputDto);
        
        // Verify that BigDecimal values are serialized as plain decimal strings
        assertTrue(json.contains("\"fairValuePerShare\":\"150.123456\""), 
                   "Fair value per share should be serialized as plain decimal string");
        assertTrue(json.contains("\"currentPrice\":\"145.67\""), 
                   "Current price should be serialized as plain decimal string");
        
        // Verify no scientific notation
        assertFalse(json.contains("E"), "JSON should not contain scientific notation");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation");
        
        System.out.println("DCFOutputDto JSON: " + json);
    }

    @Test
    void testFinancialDataDtoSerialization() throws Exception {
        String json = objectMapper.writeValueAsString(financialDataDto);
        
        // Verify that large BigDecimal values are serialized as plain decimal strings (not scientific notation)
        assertTrue(json.contains("\"365817000000\""), 
                   "Large revenue value should be serialized as plain decimal string");
        assertTrue(json.contains("\"274515000000\""), 
                   "Revenue value should be serialized as plain decimal string");
        assertTrue(json.contains("\"92953000000\""), 
                   "Free cash flow value should be serialized as plain decimal string");
        
        // Verify no scientific notation for large numbers
        assertFalse(json.contains("E"), "JSON should not contain scientific notation");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation");
        
        System.out.println("FinancialDataDto JSON: " + json);
    }

    @Test
    void testVeryLargeBigDecimalSerialization() throws Exception {
        DCFOutputDto largeValueDto = new DCFOutputDto();
        largeValueDto.setTicker("TEST");
        // Test with very large numbers that would definitely use scientific notation by default
        largeValueDto.setFairValuePerShare(new BigDecimal("999999999999999.123456"));
        largeValueDto.setCurrentPrice(new BigDecimal("0.000000123456"));

        String json = objectMapper.writeValueAsString(largeValueDto);
        
        // Verify that even very large and very small numbers are serialized as plain strings
        assertTrue(json.contains("\"999999999999999.123456\""), 
                   "Very large value should be serialized as plain decimal string");
        assertTrue(json.contains("\"0.000000123456\""), 
                   "Very small value should be serialized as plain decimal string");
        
        // Verify no scientific notation
        assertFalse(json.contains("E"), "JSON should not contain scientific notation");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation");
        
        System.out.println("Large value JSON: " + json);
    }

    @Test
    void testBigDecimalDeserialization() throws Exception {
        // Test that we can deserialize plain decimal strings back to BigDecimal
        String json = "{\"ticker\":\"AAPL\",\"discountRate\":\"10.5\",\"growthRate\":\"15.75\",\"terminalGrowthRate\":\"2.5\"}";
        
        DCFInputDto deserialized = objectMapper.readValue(json, DCFInputDto.class);
        
        assertEquals(new BigDecimal("10.5"), deserialized.getDiscountRate());
        assertEquals(new BigDecimal("15.75"), deserialized.getGrowthRate());
        assertEquals(new BigDecimal("2.5"), deserialized.getTerminalGrowthRate());
    }

    @Test
    void testBigDecimalRoundTripSerialization() throws Exception {
        // Test that serialization and deserialization preserve exact values
        String originalJson = objectMapper.writeValueAsString(dcfInputDto);
        DCFInputDto deserialized = objectMapper.readValue(originalJson, DCFInputDto.class);
        String reserializedJson = objectMapper.writeValueAsString(deserialized);
        
        assertEquals(originalJson, reserializedJson, 
                     "Round-trip serialization should preserve exact JSON representation");
    }
}