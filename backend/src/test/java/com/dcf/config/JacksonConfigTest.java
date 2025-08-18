package com.dcf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify Jackson configuration for BigDecimal serialization.
 * This test verifies that BigDecimal values are serialized as plain decimal strings
 * without scientific notation.
 */
public class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper with our custom configuration
        JacksonConfig jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
    }

    @Test
    void testBigDecimalSerializationAsPlainString() throws Exception {
        // Test data with various BigDecimal values that might trigger scientific notation
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("smallDecimal", new BigDecimal("10.5"));
        testData.put("largeNumber", new BigDecimal("365817000000")); // Large number that could use scientific notation
        testData.put("veryLargeNumber", new BigDecimal("999999999999999.123456"));
        testData.put("verySmallNumber", new BigDecimal("0.000000123456"));
        testData.put("preciseDecimal", new BigDecimal("150.123456789"));

        String json = objectMapper.writeValueAsString(testData);
        
        // Verify that all BigDecimal values are serialized as plain decimal strings
        assertTrue(json.contains("\"10.5\""), 
                   "Small decimal should be serialized as plain string");
        assertTrue(json.contains("\"365817000000\""), 
                   "Large number should be serialized as plain string (not scientific notation)");
        assertTrue(json.contains("\"999999999999999.123456\""), 
                   "Very large number should be serialized as plain string");
        assertTrue(json.contains("\"0.000000123456\""), 
                   "Very small number should be serialized as plain string");
        assertTrue(json.contains("\"150.123456789\""), 
                   "Precise decimal should be serialized as plain string");
        
        // Verify no scientific notation is present
        assertFalse(json.contains("E"), "JSON should not contain scientific notation (uppercase E)");
        assertFalse(json.contains("e"), "JSON should not contain scientific notation (lowercase e)");
        
        System.out.println("Serialized JSON: " + json);
    }

    @Test
    void testBigDecimalDeserialization() throws Exception {
        // Test that we can deserialize plain decimal strings back to BigDecimal
        String json = "{\"value1\":\"10.5\",\"value2\":\"365817000000\",\"value3\":\"0.000000123456\"}";
        
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> deserialized = objectMapper.readValue(json, Map.class);
        
        // Note: Jackson will deserialize to Double by default for Map<String, Object>
        // So we test with a specific structure
        TestBigDecimalContainer container = new TestBigDecimalContainer();
        container.setValue1(new BigDecimal("10.5"));
        container.setValue2(new BigDecimal("365817000000"));
        
        String containerJson = objectMapper.writeValueAsString(container);
        TestBigDecimalContainer deserializedContainer = objectMapper.readValue(containerJson, TestBigDecimalContainer.class);
        
        assertEquals(new BigDecimal("10.5"), deserializedContainer.getValue1());
        assertEquals(new BigDecimal("365817000000"), deserializedContainer.getValue2());
    }

    @Test
    void testCustomBigDecimalSerializer() throws Exception {
        // Test the custom BigDecimalPlainSerializer directly
        BigDecimalPlainSerializer serializer = new BigDecimalPlainSerializer();
        
        // Test with a value that would normally use scientific notation
        BigDecimal testValue = new BigDecimal("1.23456789E+15");
        
        // Serialize using ObjectMapper (which should use our custom serializer)
        Map<String, BigDecimal> testMap = Map.of("testValue", testValue);
        String json = objectMapper.writeValueAsString(testMap);
        
        // Should contain the plain string representation
        assertTrue(json.contains("1234567890000000"), 
                   "Should serialize large BigDecimal as plain string");
        assertFalse(json.contains("E"), "Should not contain scientific notation");
        assertFalse(json.contains("e"), "Should not contain scientific notation");
        
        System.out.println("Large number JSON: " + json);
    }

    @Test
    void testNullBigDecimalSerialization() throws Exception {
        TestBigDecimalContainer container = new TestBigDecimalContainer();
        container.setValue1(null);
        container.setValue2(new BigDecimal("100.50"));
        
        String json = objectMapper.writeValueAsString(container);
        
        assertTrue(json.contains("\"value1\":null"), "Null BigDecimal should serialize as null");
        assertTrue(json.contains("\"100.50\""), "Non-null BigDecimal should serialize as plain string");
        
        System.out.println("Null value JSON: " + json);
    }

    // Helper class for testing BigDecimal serialization/deserialization
    public static class TestBigDecimalContainer {
        private BigDecimal value1;
        private BigDecimal value2;

        public BigDecimal getValue1() {
            return value1;
        }

        public void setValue1(BigDecimal value1) {
            this.value1 = value1;
        }

        public BigDecimal getValue2() {
            return value2;
        }

        public void setValue2(BigDecimal value2) {
            this.value2 = value2;
        }
    }
}