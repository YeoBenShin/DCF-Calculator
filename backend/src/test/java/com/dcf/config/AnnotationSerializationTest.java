package com.dcf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;

/**
 * Test to verify that @JsonSerialize annotations work correctly with our
 * BigDecimalPlainSerializer for consistent decimal formatting.
 */
public class AnnotationSerializationTest {

    public static void main(String[] args) {
        try {
            // Initialize ObjectMapper with our custom configuration
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper objectMapper = jacksonConfig.objectMapper();

            System.out.println("=== @JsonSerialize Annotation Test ===");
            
            // Test class with @JsonSerialize annotations (similar to our DTOs)
            testAnnotatedClassSerialization(objectMapper);
            
            System.out.println("\n=== Annotation test completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("Annotation test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testAnnotatedClassSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- Annotated Class Serialization Test ---");
        
        TestAnnotatedClass testObj = new TestAnnotatedClass();
        testObj.setDiscountRate(new BigDecimal("10.5"));
        testObj.setGrowthRate(new BigDecimal("15.75"));
        testObj.setFairValuePerShare(new BigDecimal("150.123456"));
        testObj.setLargeValue(new BigDecimal("365817000000")); // Large number
        testObj.setSmallValue(new BigDecimal("0.000000123456")); // Small number

        String json = objectMapper.writeValueAsString(testObj);
        System.out.println("Annotated class JSON: " + json);
        
        // Verify BigDecimal fields are serialized as plain strings
        if (!json.contains("\"discountRate\":\"10.5\"") ||
            !json.contains("\"growthRate\":\"15.75\"") ||
            !json.contains("\"fairValuePerShare\":\"150.123456\"") ||
            !json.contains("\"largeValue\":\"365817000000\"") ||
            !json.contains("\"smallValue\":\"0.000000123456\"")) {
            throw new AssertionError("Annotated BigDecimal fields not serialized as plain strings: " + json);
        }
        
        // Verify no scientific notation
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("Annotated class JSON contains scientific notation: " + json);
        }
        
        // Test round-trip
        TestAnnotatedClass deserialized = objectMapper.readValue(json, TestAnnotatedClass.class);
        if (!testObj.getDiscountRate().equals(deserialized.getDiscountRate()) ||
            !testObj.getGrowthRate().equals(deserialized.getGrowthRate()) ||
            !testObj.getFairValuePerShare().equals(deserialized.getFairValuePerShare()) ||
            !testObj.getLargeValue().equals(deserialized.getLargeValue()) ||
            !testObj.getSmallValue().equals(deserialized.getSmallValue())) {
            throw new AssertionError("Annotated class round-trip serialization failed");
        }
        
        System.out.println("✓ Annotated class serialization test passed");
    }

    // Test class that mimics our DTO structure with @JsonSerialize annotations
    public static class TestAnnotatedClass {
        @JsonSerialize(using = BigDecimalPlainSerializer.class)
        private BigDecimal discountRate;
        
        @JsonSerialize(using = BigDecimalPlainSerializer.class)
        private BigDecimal growthRate;
        
        @JsonSerialize(using = BigDecimalPlainSerializer.class)
        private BigDecimal fairValuePerShare;
        
        @JsonSerialize(using = BigDecimalPlainSerializer.class)
        private BigDecimal largeValue;
        
        @JsonSerialize(using = BigDecimalPlainSerializer.class)
        private BigDecimal smallValue;

        // Getters and setters
        public BigDecimal getDiscountRate() { return discountRate; }
        public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
        
        public BigDecimal getGrowthRate() { return growthRate; }
        public void setGrowthRate(BigDecimal growthRate) { this.growthRate = growthRate; }
        
        public BigDecimal getFairValuePerShare() { return fairValuePerShare; }
        public void setFairValuePerShare(BigDecimal fairValuePerShare) { this.fairValuePerShare = fairValuePerShare; }
        
        public BigDecimal getLargeValue() { return largeValue; }
        public void setLargeValue(BigDecimal largeValue) { this.largeValue = largeValue; }
        
        public BigDecimal getSmallValue() { return smallValue; }
        public void setSmallValue(BigDecimal smallValue) { this.smallValue = smallValue; }
    }
}