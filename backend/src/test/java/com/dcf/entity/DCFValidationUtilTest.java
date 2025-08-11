package com.dcf.util;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class DCFValidationUtilTest {

    private DCFValidationUtil dcfValidationUtil;
    private DCFInput dcfInput;
    private DCFOutput dcfOutput;

    @BeforeEach
    void setUp() {
        dcfValidationUtil = new DCFValidationUtil();
        dcfInput = new DCFInput();
        dcfOutput = new DCFOutput();
    }

    @Test
    @DisplayName("Valid DCF input should pass validation")
    void testValidDCFInput() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

        assertNull(dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("Null DCF input should fail validation")
    void testNullDCFInput() {
        assertEquals("DCF input is required", dcfValidationUtil.validateDCFInput(null));
    }

    @Test
    @DisplayName("DCF input with missing ticker should fail validation")
    void testMissingTicker() {
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

        assertEquals("Ticker symbol is required", dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("DCF input with excessive growth rate should fail validation")
    void testExcessiveGrowthRate() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(1500.0);
        dcfInput.setTerminalGrowthRate(3.0);

        assertEquals("Growth rate too high. Please input a realistic value.", 
                    dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("DCF input with terminal growth rate exceeding discount rate should fail")
    void testTerminalGrowthExceedsDiscount() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(8.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(10.0); // > discount rate

        assertEquals("Terminal growth rate cannot exceed discount rate", 
                    dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("Valid DCF output should pass validation")
    void testValidDCFOutput() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(140.0);
        dcfOutput.setValuation("Undervalued");

        assertNull(dcfValidationUtil.validateDCFOutput(dcfOutput));
    }

    @Test
    @DisplayName("Null DCF output should fail validation")
    void testNullDCFOutput() {
        assertEquals("DCF output is required", dcfValidationUtil.validateDCFOutput(null));
    }

    @Test
    @DisplayName("DCF output with zero fair value should fail validation")
    void testZeroFairValue() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(0.0);
        dcfOutput.setValuation("Undervalued");

        assertEquals("Fair value per share must be positive", 
                    dcfValidationUtil.validateDCFOutput(dcfOutput));
    }

    @Test
    @DisplayName("Reasonable growth rate should be identified correctly")
    void testReasonableGrowthRate() {
        assertTrue(dcfValidationUtil.isReasonableGrowthRate(25.0));
        assertTrue(dcfValidationUtil.isReasonableGrowthRate(-10.0));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(75.0));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(-75.0));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(null));
    }

    @Test
    @DisplayName("Reasonable discount rate should be identified correctly")
    void testReasonableDiscountRate() {
        assertTrue(dcfValidationUtil.isReasonableDiscountRate(10.0));
        assertTrue(dcfValidationUtil.isReasonableDiscountRate(15.0));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(3.0));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(25.0));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(null));
    }

    @Test
    @DisplayName("Conservative terminal growth rate should be identified correctly")
    void testConservativeTerminalGrowthRate() {
        assertTrue(dcfValidationUtil.isConservativeTerminalGrowthRate(3.0));
        assertTrue(dcfValidationUtil.isConservativeTerminalGrowthRate(2.5));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(1.0));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(5.0));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(null));
    }

    @Test
    @DisplayName("Parameter warnings should be generated for unrealistic values")
    void testParameterWarnings() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(25.0); // Unreasonable
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

        String warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Discount rate"));

        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(75.0); // Unreasonable

        warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Growth rate"));

        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(6.0); // Not conservative

        warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Terminal growth rate"));
    }

    @Test
    @DisplayName("No parameter warnings for reasonable values")
    void testNoParameterWarnings() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(12.0);
        dcfInput.setGrowthRate(20.0);
        dcfInput.setTerminalGrowthRate(3.0);

        assertNull(dcfValidationUtil.getParameterWarning(dcfInput));
    }

    @Test
    @DisplayName("Calculation reasonableness validation should work")
    void testCalculationReasonableness() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(15000.0); // Very high
        dcfOutput.setCurrentPrice(150.0);

        String warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("unusually high"));

        dcfOutput.setFairValuePerShare(1500.0); // 10x current price
        warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("10x current price"));

        dcfOutput.setFairValuePerShare(10.0); // Much lower than current price
        warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("10% of current price"));
    }

    @Test
    @DisplayName("Reasonable calculation should pass validation")
    void testReasonableCalculation() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(160.0);
        dcfOutput.setCurrentPrice(150.0);

        assertNull(dcfValidationUtil.validateCalculationReasonableness(dcfOutput));
    }

    @Test
    @DisplayName("hasAllRequiredFields should work correctly")
    void testHasAllRequiredFields() {
        assertFalse(dcfValidationUtil.hasAllRequiredFields(null));
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setTicker("AAPL");
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setDiscountRate(10.0);
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setGrowthRate(15.0);
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setTerminalGrowthRate(3.0);
        assertTrue(dcfValidationUtil.hasAllRequiredFields(dcfInput));
    }

    @Test
    @DisplayName("Validation should handle edge cases")
    void testValidationEdgeCases() {
        // Test with exactly boundary values
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(0.0); // Minimum allowed
        dcfInput.setGrowthRate(-100.0); // Minimum allowed
        dcfInput.setTerminalGrowthRate(0.0); // Minimum allowed

        assertNull(dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setDiscountRate(100.0); // Maximum allowed
        dcfInput.setGrowthRate(1000.0); // Maximum allowed
        dcfInput.setTerminalGrowthRate(10.0); // Maximum allowed

        assertNull(dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("Validation should handle missing required fields")
    void testMissingRequiredFields() {
        dcfInput.setTicker("AAPL");
        // Missing discount rate
        assertEquals("Discount rate is required", dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setDiscountRate(10.0);
        // Missing growth rate
        assertEquals("Growth rate is required", dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setGrowthRate(15.0);
        // Missing terminal growth rate
        assertEquals("Terminal growth rate is required", dcfValidationUtil.validateDCFInput(dcfInput));
    }
}