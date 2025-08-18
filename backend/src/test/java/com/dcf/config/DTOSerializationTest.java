package com.dcf.config;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

/**
 * Integration test to verify that DTOs with BigDecimal fields serialize correctly
 * using our Jackson configuration.
 */
public class DTOSerializationTest {

    public static void main(String[] args) {
        try {
            // Initialize ObjectMapper with our custom configuration
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper objectMapper = jacksonConfig.objectMapper();

            System.out.println("=== DTO BigDecimal Serialization Test ===");
            
            // Test DCFInputDto serialization
            testDCFInputDtoSerialization(objectMapper);
            
            // Test DCFOutputDto serialization
            testDCFOutputDtoSerialization(objectMapper);
            
            System.out.println("\n=== All DTO tests completed successfully! ===");
            
        } catch (Exception e) {
            System.err.println("DTO test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testDCFInputDtoSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- DCFInputDto Serialization Test ---");
        
        DCFInputDto inputDto = new DCFInputDto();
        inputDto.setTicker("AAPL");
        inputDto.setDiscountRate(new BigDecimal("10.5"));
        inputDto.setGrowthRate(new BigDecimal("15.75"));
        inputDto.setTerminalGrowthRate(new BigDecimal("2.5"));

        String json = objectMapper.writeValueAsString(inputDto);
        System.out.println("DCFInputDto JSON: " + json);
        
        // Verify BigDecimal fields are serialized as plain strings
        if (!json.contains("\"discountRate\":\"10.5\"") ||
            !json.contains("\"growthRate\":\"15.75\"") ||
            !json.contains("\"terminalGrowthRate\":\"2.5\"")) {
            throw new AssertionError("DCFInputDto BigDecimal fields not serialized as plain strings: " + json);
        }
        
        // Verify no scientific notation
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("DCFInputDto JSON contains scientific notation: " + json);
        }
        
        // Test round-trip
        DCFInputDto deserialized = objectMapper.readValue(json, DCFInputDto.class);
        if (!inputDto.getDiscountRate().equals(deserialized.getDiscountRate()) ||
            !inputDto.getGrowthRate().equals(deserialized.getGrowthRate()) ||
            !inputDto.getTerminalGrowthRate().equals(deserialized.getTerminalGrowthRate())) {
            throw new AssertionError("DCFInputDto round-trip serialization failed");
        }
        
        System.out.println("✓ DCFInputDto serialization test passed");
    }

    private static void testDCFOutputDtoSerialization(ObjectMapper objectMapper) throws Exception {
        System.out.println("\n--- DCFOutputDto Serialization Test ---");
        
        DCFOutputDto outputDto = new DCFOutputDto();
        outputDto.setId("test-id-123");
        outputDto.setTicker("AAPL");
        outputDto.setFairValuePerShare(new BigDecimal("150.123456"));
        outputDto.setCurrentPrice(new BigDecimal("145.67"));
        outputDto.setValuation("Undervalued");
        outputDto.setUpsideDownsidePercentage(new BigDecimal("3.05"));
        outputDto.setTerminalValue(new BigDecimal("1500000000.50"));
        outputDto.setPresentValueOfCashFlows(new BigDecimal("1200000000.75"));
        outputDto.setEnterpriseValue(new BigDecimal("2700000000.25"));
        outputDto.setEquityValue(new BigDecimal("2500000000.00"));
        outputDto.setSharesOutstanding(new BigDecimal("16666666667"));
        outputDto.setDcfInputId("input-id-456");
        outputDto.setUserId("user-id-789");

        String json = objectMapper.writeValueAsString(outputDto);
        System.out.println("DCFOutputDto JSON: " + json);
        
        // Verify BigDecimal fields are serialized as plain strings
        if (!json.contains("\"fairValuePerShare\":\"150.123456\"") ||
            !json.contains("\"currentPrice\":\"145.67\"") ||
            !json.contains("\"upsideDownsidePercentage\":\"3.05\"") ||
            !json.contains("\"terminalValue\":\"1500000000.50\"") ||
            !json.contains("\"presentValueOfCashFlows\":\"1200000000.75\"") ||
            !json.contains("\"enterpriseValue\":\"2700000000.25\"") ||
            !json.contains("\"equityValue\":\"2500000000.00\"") ||
            !json.contains("\"sharesOutstanding\":\"16666666667\"")) {
            throw new AssertionError("DCFOutputDto BigDecimal fields not serialized as plain strings: " + json);
        }
        
        // Verify no scientific notation
        if (json.matches(".*[eE][+-]?\\d+.*")) {
            throw new AssertionError("DCFOutputDto JSON contains scientific notation: " + json);
        }
        
        // Test round-trip
        DCFOutputDto deserialized = objectMapper.readValue(json, DCFOutputDto.class);
        if (!outputDto.getFairValuePerShare().equals(deserialized.getFairValuePerShare()) ||
            !outputDto.getCurrentPrice().equals(deserialized.getCurrentPrice()) ||
            !outputDto.getUpsideDownsidePercentage().equals(deserialized.getUpsideDownsidePercentage()) ||
            !outputDto.getTerminalValue().equals(deserialized.getTerminalValue()) ||
            !outputDto.getPresentValueOfCashFlows().equals(deserialized.getPresentValueOfCashFlows()) ||
            !outputDto.getEnterpriseValue().equals(deserialized.getEnterpriseValue()) ||
            !outputDto.getEquityValue().equals(deserialized.getEquityValue()) ||
            !outputDto.getSharesOutstanding().equals(deserialized.getSharesOutstanding())) {
            throw new AssertionError("DCFOutputDto round-trip serialization failed");
        }
        
        System.out.println("✓ DCFOutputDto serialization test passed");
    }
}