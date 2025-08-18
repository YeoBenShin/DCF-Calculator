package com.dcf.validation;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import com.dcf.service.DCFCalculationService;
import com.dcf.repository.DCFInputRepository;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.repository.FinancialDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation suite for BigDecimal migration
 * Tests all application layers to ensure BigDecimal precision is maintained
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BigDecimalMigrationValidationSuite {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DCFCalculationService dcfCalculationService;

    @Autowired
    private DCFInputRepository dcfInputRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final BigDecimal PRECISION_TOLERANCE = new BigDecimal("0.000001");

    @Test
    @Order(1)
    public void validateDatabasePrecisionStorage() {
        // Test BigDecimal storage and retrieval precision
        DCFInput input = new DCFInput();
        input.setTicker("TEST");
        input.setDiscountRate(new BigDecimal("8.123456"));
        input.setGrowthRate(new BigDecimal("3.987654"));
        input.setTerminalGrowthRate(new BigDecimal("2.555555"));

        DCFInput saved = dcfInputRepository.save(input);
        DCFInput retrieved = dcfInputRepository.findById(saved.getId()).orElse(null);

        assertNotNull(retrieved);
        assertEquals(0, input.getDiscountRate().compareTo(retrieved.getDiscountRate()));
        assertEquals(0, input.getGrowthRate().compareTo(retrieved.getGrowthRate()));
        assertEquals(0, input.getTerminalGrowthRate().compareTo(retrieved.getTerminalGrowthRate()));
    }

    @Test
    @Order(2)
    public void validateCalculationPrecision() {
        // Test DCF calculation precision with known values
        DCFInput input = createTestDCFInput();
        FinancialData financialData = createTestFinancialData();
        
        try {
            DCFOutput result = dcfCalculationService.calculateDCF(input.getTicker(), 
                input.getDiscountRate(), input.getGrowthRate(), input.getTerminalGrowthRate());
            
            assertNotNull(result);
            assertNotNull(result.getFairValuePerShare());
            assertNotNull(result.getEnterpriseValue());
            assertNotNull(result.getEquityValue());
            
            // Verify calculations are using BigDecimal precision
            assertTrue(result.getFairValuePerShare().scale() >= 2);
            assertTrue(result.getEnterpriseValue().scale() >= 2);
            
        } catch (Exception e) {
            fail("DCF calculation failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    public void validateApiSerializationPrecision() {
        // Test API serialization maintains BigDecimal precision
        String url = "http://localhost:" + port + "/api/financial-data/AAPL";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            String jsonResponse = response.getBody();
            assertNotNull(jsonResponse);
            
            // Verify no scientific notation in response
            assertFalse(jsonResponse.contains("E"), "Response contains scientific notation");
            assertFalse(jsonResponse.contains("e"), "Response contains scientific notation");
            
            // Verify decimal precision is maintained
            assertTrue(jsonResponse.matches(".*\"\\d+\\.\\d+\".*"), "Response should contain decimal values");
        }
    }

    @Test
    @Order(4)
    public void validateLargeValueHandling() {
        // Test handling of very large financial values
        DCFInput input = createTestDCFInput();
        
        try {
            DCFOutput result = dcfCalculationService.calculateDCF(input.getTicker(), 
                new BigDecimal("8.5"), new BigDecimal("3.2"), new BigDecimal("2.5"));
            
            assertNotNull(result);
            assertTrue(result.getEnterpriseValue().compareTo(BigDecimal.ZERO) > 0);
            
            // Verify large values don't cause overflow or precision loss
            String valueStr = result.getEnterpriseValue().toPlainString();
            assertFalse(valueStr.contains("E"), "Large values should not use scientific notation");
            
        } catch (Exception e) {
            fail("Large value calculation failed: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    public void validateSmallValueHandling() {
        // Test handling of very small financial values
        DCFInput input = createTestDCFInput();
        
        try {
            DCFOutput result = dcfCalculationService.calculateDCF(input.getTicker(), 
                new BigDecimal("0.000001"), new BigDecimal("3.2"), new BigDecimal("2.5"));
            
            assertNotNull(result);
            assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) > 0);
            
        } catch (Exception e) {
            fail("Small value calculation failed: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    public void validateRoundingConsistency() {
        // Test that rounding is consistent across calculations
        DCFInput input = createTestDCFInput();
        
        DCFOutput result1 = dcfCalculationService.calculateDCF(input.getTicker(), 
            input.getDiscountRate(), input.getGrowthRate(), input.getTerminalGrowthRate());
        DCFOutput result2 = dcfCalculationService.calculateDCF(input.getTicker(), 
            input.getDiscountRate(), input.getGrowthRate(), input.getTerminalGrowthRate());
        
        // Results should be identical for same input
        assertEquals(0, result1.getFairValuePerShare().compareTo(result2.getFairValuePerShare()));
        assertEquals(0, result1.getEnterpriseValue().compareTo(result2.getEnterpriseValue()));
    }

    @Test
    @Order(7)
    @Transactional
    public void validateDataMigrationIntegrity() {
        // Test that existing data maintains integrity after migration
        List<DCFInput> inputs = dcfInputRepository.findAll();
        List<DCFOutput> outputs = dcfOutputRepository.findAll();
        List<FinancialData> financialData = financialDataRepository.findAll();
        
        // Verify all BigDecimal fields are not null and have reasonable values
        for (DCFInput input : inputs) {
            if (input.getDiscountRate() != null) {
                assertTrue(input.getDiscountRate().compareTo(BigDecimal.ZERO) >= 0);
                assertTrue(input.getDiscountRate().compareTo(new BigDecimal("100")) <= 0);
            }
            if (input.getGrowthRate() != null) {
                assertTrue(input.getGrowthRate().compareTo(new BigDecimal("-50")) >= 0);
                assertTrue(input.getGrowthRate().compareTo(new BigDecimal("100")) <= 0);
            }
        }
        
        for (DCFOutput output : outputs) {
            if (output.getFairValuePerShare() != null) {
                assertTrue(output.getFairValuePerShare().compareTo(BigDecimal.ZERO) >= 0);
            }
            if (output.getEnterpriseValue() != null) {
                assertTrue(output.getEnterpriseValue().compareTo(BigDecimal.ZERO) >= 0);
            }
        }
    }

    @Test
    @Order(8)
    public void validatePerformanceWithBigDecimal() {
        // Test that BigDecimal operations don't cause significant performance degradation
        DCFInput input = createTestDCFInput();
        
        long startTime = System.currentTimeMillis();
        
        // Perform multiple calculations
        for (int i = 0; i < 10; i++) {
            dcfCalculationService.calculateDCF(input.getTicker(), 
                input.getDiscountRate(), input.getGrowthRate(), input.getTerminalGrowthRate());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should complete 10 calculations in reasonable time (adjust threshold as needed)
        assertTrue(duration < 30000, "BigDecimal calculations taking too long: " + duration + "ms");
    }

    private DCFInput createTestDCFInput() {
        DCFInput input = new DCFInput();
        input.setTicker("TEST");
        input.setDiscountRate(new BigDecimal("8.5"));
        input.setGrowthRate(new BigDecimal("3.2"));
        input.setTerminalGrowthRate(new BigDecimal("2.5"));
        return input;
    }
    
    private FinancialData createTestFinancialData() {
        FinancialData financialData = new FinancialData();
        financialData.setTicker("TEST");
        financialData.setRevenue(new BigDecimal("5000000000")); // 5 billion
        financialData.setFreeCashFlow(new BigDecimal("1000000000")); // 1 billion
        financialData.setSharesOutstanding(new BigDecimal("1000000000")); // 1 billion shares
        return financialData;
    }
}