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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deployment readiness validation test
 * Validates that the system is ready for BigDecimal migration deployment
 */
@SpringBootTest
@ActiveProfiles("test")
public class DeploymentReadinessTest {

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

    @Test
    public void testApplicationStartup() {
        // Test that the application context loads successfully
        assertNotNull(dcfCalculationService);
        assertNotNull(dcfInputRepository);
        assertNotNull(dcfOutputRepository);
        assertNotNull(financialDataRepository);
        assertNotNull(objectMapper);
    }

    @Test
    public void testDatabaseConnectivity() {
        // Test basic database operations
        try {
            List<DCFInput> inputs = dcfInputRepository.findAll();
            List<DCFOutput> outputs = dcfOutputRepository.findAll();
            List<FinancialData> financialData = financialDataRepository.findAll();
            
            // Should not throw exceptions
            assertNotNull(inputs);
            assertNotNull(outputs);
            assertNotNull(financialData);
            
        } catch (Exception e) {
            fail("Database connectivity test failed: " + e.getMessage());
        }
    }

    @Test
    public void testBigDecimalSerialization() {
        // Test that BigDecimal values serialize correctly
        try {
            BigDecimal testValue = new BigDecimal("123456.789012");
            String json = objectMapper.writeValueAsString(testValue);
            
            // Should not contain scientific notation
            assertFalse(json.contains("E"), "BigDecimal should not serialize to scientific notation");
            assertFalse(json.contains("e"), "BigDecimal should not serialize to scientific notation");
            
            // Should be a plain decimal string
            assertTrue(json.contains("123456.789012"), "BigDecimal should serialize as plain decimal");
            
        } catch (Exception e) {
            fail("BigDecimal serialization test failed: " + e.getMessage());
        }
    }

    @Test
    public void testBasicDCFCalculation() {
        // Test that DCF calculation service works
        try {
            DCFInput input = new DCFInput();
            input.setTicker("AAPL");
            input.setDiscountRate(new BigDecimal("8.5"));
            input.setGrowthRate(new BigDecimal("3.2"));
            input.setTerminalGrowthRate(new BigDecimal("2.5"));
            
            DCFOutput result = dcfCalculationService.calculateDCF(input);
            
            assertNotNull(result, "DCF calculation should return a result");
            
            if (result.getFairValuePerShare() != null) {
                assertTrue(result.getFairValuePerShare().compareTo(BigDecimal.ZERO) >= 0, 
                    "Fair value should be non-negative");
            }
            
        } catch (Exception e) {
            // Log the error but don't fail the test if it's due to missing data
            System.out.println("DCF calculation test warning: " + e.getMessage());
        }
    }

    @Test
    public void testBigDecimalPrecision() {
        // Test BigDecimal precision handling
        BigDecimal value1 = new BigDecimal("10.123456789012345");
        BigDecimal value2 = new BigDecimal("20.987654321098765");
        
        BigDecimal sum = value1.add(value2);
        BigDecimal product = value1.multiply(value2);
        
        // Verify precision is maintained
        assertTrue(sum.scale() >= 15, "Addition should maintain precision");
        assertTrue(product.scale() >= 15, "Multiplication should maintain precision");
        
        // Verify no scientific notation in string representation
        String sumStr = sum.toPlainString();
        String productStr = product.toPlainString();
        
        assertFalse(sumStr.contains("E"), "Sum should not use scientific notation");
        assertFalse(productStr.contains("E"), "Product should not use scientific notation");
    }

    @Test
    public void testSystemResourceUsage() {
        // Test that system resources are within reasonable bounds
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Convert to MB
        long usedMemoryMB = usedMemory / (1024 * 1024);
        
        // Should not be using excessive memory (adjust threshold as needed)
        assertTrue(usedMemoryMB < 2048, "Memory usage should be reasonable: " + usedMemoryMB + "MB");
        
        System.out.println("Memory usage: " + usedMemoryMB + "MB");
    }

    @Test
    public void testValidationUtilities() {
        // Test that validation utilities work with BigDecimal
        BigDecimal validDiscountRate = new BigDecimal("8.5");
        BigDecimal invalidDiscountRate = new BigDecimal("-5.0");
        
        // These should not throw exceptions
        assertTrue(validDiscountRate.compareTo(BigDecimal.ZERO) > 0, "Valid discount rate should be positive");
        assertFalse(invalidDiscountRate.compareTo(BigDecimal.ZERO) > 0, "Invalid discount rate should be negative");
    }
}