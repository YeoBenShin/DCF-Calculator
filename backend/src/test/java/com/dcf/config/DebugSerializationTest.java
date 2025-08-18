package com.dcf.config;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

public class DebugSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JacksonConfig jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
    }

    @Test
    void debugDCFInputSerialization() throws Exception {
        DCFInputDto dto = new DCFInputDto();
        dto.setTicker("AAPL");
        dto.setDiscountRate(new BigDecimal("10.5"));
        dto.setGrowthRate(new BigDecimal("15.75"));
        dto.setTerminalGrowthRate(new BigDecimal("2.5"));

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("DCFInputDto JSON: " + json);
        
        // Check for scientific notation
        boolean hasScientificNotation = json.contains("E") || json.contains("e");
        System.out.println("Has scientific notation: " + hasScientificNotation);
    }

    @Test
    void debugDCFOutputSerialization() throws Exception {
        DCFOutputDto dto = new DCFOutputDto();
        dto.setTicker("AAPL");
        dto.setFairValuePerShare(new BigDecimal("150.123456"));
        dto.setCurrentPrice(new BigDecimal("145.67"));
        dto.setValuation("Undervalued");

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("DCFOutputDto JSON: " + json);
        
        // Check for scientific notation
        boolean hasScientificNotation = json.contains("E") || json.contains("e");
        System.out.println("Has scientific notation: " + hasScientificNotation);
    }

    @Test
    void debugLargeBigDecimalSerialization() throws Exception {
        // Test with the same value that's failing in the other test
        BigDecimal testValue = new BigDecimal("1.23456789E+15");
        System.out.println("Original BigDecimal: " + testValue);
        System.out.println("Plain string: " + testValue.toPlainString());
        
        // Test direct serialization
        String json = objectMapper.writeValueAsString(testValue);
        System.out.println("Direct BigDecimal JSON: " + json);
        
        // Test in a map
        Map<String, BigDecimal> testMap = new HashMap<>();
        testMap.put("testValue", testValue);
        String mapJson = objectMapper.writeValueAsString(testMap);
        System.out.println("Map JSON: " + mapJson);
        
        // Test in a simple object
        TestContainer container = new TestContainer();
        container.setValue1(testValue);
        String containerJson = objectMapper.writeValueAsString(container);
        System.out.println("Container JSON: " + containerJson);
    }

    public static class TestContainer {
        private BigDecimal value1;

        public BigDecimal getValue1() {
            return value1;
        }

        public void setValue1(BigDecimal value1) {
            this.value1 = value1;
        }
    }
}