package com.dcf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo class to test BigDecimal serialization configuration.
 * This demonstrates that our Jackson configuration properly serializes
 * BigDecimal values as plain decimal strings without scientific notation.
 */
public class BigDecimalSerializationDemo {

    public static void main(String[] args) {
        try {
            // Initialize ObjectMapper with our custom configuration
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper objectMapper = jacksonConfig.objectMapper();

            System.out.println("=== BigDecimal Serialization Test ===");
            
            // Test 1: Basic BigDecimal serialization
            testBasicSerialization(objectMapper);
            
            // Test 2: Large numbers that might use scientific notation
            testLargeNumberSerialization(objectMapper);
            
            // Test 3: Very small numbers
            testSmallNumberSerialization(objectMapper);
            
            // Test 4: Round-trip serialization/deserialization
            testRoundTripSerialization(objectMapper);
            
            System.out.println("\n=== All tests completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testBasicSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- Test 1: Basic BigDecimal Serialization ---");
        
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("discountRate", new BigDecimal("10.5"));
        testData.put("growthRate", new BigDecimal("15.75"));
        testData.put("fairValue", new BigDecimal("150.123456"));

        String json = objectMapper.writeValueAsString(testData);
        System.out.println("JSON: " + json);
        
        // Verify no scientific notation (check for E+ or E- patterns)
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("JSON contains scientific notation: " + json);
        }
        
        // Verify plain decimal strings
        if (!json.contains("\"10.5\"") || !json.contains("\"15.75\"") || !json.contains("\"150.123456\"")) {
            throw new AssertionError("JSON does not contain expected plain decimal strings: " + json);
        }
        
        System.out.println("✓ Basic serialization test passed");
    }

    private static void testLargeNumberSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- Test 2: Large Number Serialization ---");
        
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("revenue", new BigDecimal("365817000000")); // 365.8 billion
        testData.put("marketCap", new BigDecimal("2500000000000")); // 2.5 trillion
        testData.put("veryLarge", new BigDecimal("999999999999999.123456"));

        String json = objectMapper.writeValueAsString(testData);
        System.out.println("JSON: " + json);
        
        // Verify no scientific notation for large numbers (check for E+ or E- patterns)
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("JSON contains scientific notation for large numbers: " + json);
        }
        
        // Verify large numbers are serialized as plain strings
        if (!json.contains("\"365817000000\"") || !json.contains("\"2500000000000\"")) {
            throw new AssertionError("Large numbers not serialized as plain strings: " + json);
        }
        
        System.out.println("✓ Large number serialization test passed");
    }

    private static void testSmallNumberSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- Test 3: Small Number Serialization ---");
        
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("smallDecimal", new BigDecimal("0.000000123456"));
        testData.put("percentage", new BigDecimal("0.025")); // 2.5%
        testData.put("precision", new BigDecimal("0.123456789012345"));

        String json = objectMapper.writeValueAsString(testData);
        System.out.println("JSON: " + json);
        
        // Verify no scientific notation for small numbers (check for E+ or E- patterns)
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("JSON contains scientific notation for small numbers: " + json);
        }
        
        // Verify small numbers are serialized as plain strings
        if (!json.contains("\"0.000000123456\"") || !json.contains("\"0.025\"")) {
            throw new AssertionError("Small numbers not serialized as plain strings: " + json);
        }
        
        System.out.println("✓ Small number serialization test passed");
    }

    private static void testRoundTripSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- Test 4: Round-trip Serialization ---");
        
        TestContainer original = new TestContainer();
        original.setValue1(new BigDecimal("10.5"));
        original.setValue2(new BigDecimal("365817000000"));
        original.setValue3(new BigDecimal("0.000000123456"));

        // Serialize
        String json = objectMapper.writeValueAsString(original);
        System.out.println("Serialized JSON: " + json);
        
        // Deserialize
        TestContainer deserialized = objectMapper.readValue(json, TestContainer.class);
        
        // Verify values are preserved
        if (!original.getValue1().equals(deserialized.getValue1()) ||
            !original.getValue2().equals(deserialized.getValue2()) ||
            !original.getValue3().equals(deserialized.getValue3())) {
            throw new AssertionError("Round-trip serialization failed to preserve values");
        }
        
        System.out.println("✓ Round-trip serialization test passed");
    }

    // Helper class for testing
    public static class TestContainer {
        private BigDecimal value1;
        private BigDecimal value2;
        private BigDecimal value3;

        public BigDecimal getValue1() { return value1; }
        public void setValue1(BigDecimal value1) { this.value1 = value1; }
        
        public BigDecimal getValue2() { return value2; }
        public void setValue2(BigDecimal value2) { this.value2 = value2; }
        
        public BigDecimal getValue3() { return value3; }
        public void setValue3(BigDecimal value3) { this.value3 = value3; }
    }
}