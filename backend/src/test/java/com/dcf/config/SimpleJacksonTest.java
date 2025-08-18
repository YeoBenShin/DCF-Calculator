package com.dcf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SimpleJacksonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JacksonConfig jacksonConfig = new JacksonConfig();
        objectMapper = jacksonConfig.objectMapper();
    }

    @Test
    void testActualOutput() throws Exception {
        Map<String, BigDecimal> testData = new HashMap<>();
        testData.put("smallDecimal", new BigDecimal("10.5"));
        testData.put("largeNumber", new BigDecimal("365817000000"));
        testData.put("veryLargeNumber", new BigDecimal("999999999999999.123456"));
        testData.put("verySmallNumber", new BigDecimal("0.000000123456"));
        testData.put("preciseDecimal", new BigDecimal("150.123456789"));

        String json = objectMapper.writeValueAsString(testData);
        System.out.println("Actual JSON output:");
        System.out.println(json);
        
        // Check for scientific notation patterns
        boolean hasUppercaseE = json.matches(".*\\d+E[+-]?\\d+.*");
        boolean hasLowercaseE = json.matches(".*\\d+e[+-]?\\d+.*");
        
        System.out.println("Contains uppercase E scientific notation: " + hasUppercaseE);
        System.out.println("Contains lowercase e scientific notation: " + hasLowercaseE);
        System.out.println("Contains any E: " + json.contains("E"));
        System.out.println("Contains any e: " + json.contains("e"));
    }
}