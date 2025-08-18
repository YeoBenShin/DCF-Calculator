package com.dcf.entity;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.util.DCFValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;

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
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

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
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        assertEquals("Ticker symbol is required", dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("DCF input with excessive growth rate should fail validation")
    void testExcessiveGrowthRate() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("1500.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        assertEquals("Growth rate too high. Please input a realistic value.", 
                    dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("DCF input with terminal growth rate exceeding discount rate should fail")
    void testTerminalGrowthExceedsDiscount() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("8.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("10.0")); // > discount rate

        assertEquals("Terminal growth rate cannot exceed discount rate", 
                    dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("Valid DCF output should pass validation")
    void testValidDCFOutput() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
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
        dcfOutput.setFairValuePerShare(new BigDecimal("0.0"));
        dcfOutput.setValuation("Undervalued");

        assertEquals("Fair value per share must be positive", 
                    dcfValidationUtil.validateDCFOutput(dcfOutput));
    }

    @Test
    @DisplayName("Reasonable growth rate should be identified correctly")
    void testReasonableGrowthRate() {
        assertTrue(dcfValidationUtil.isReasonableGrowthRate(new BigDecimal("25.0")));
        assertTrue(dcfValidationUtil.isReasonableGrowthRate(new BigDecimal("-10.0")));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(new BigDecimal("75.0")));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(new BigDecimal("-75.0")));
        assertFalse(dcfValidationUtil.isReasonableGrowthRate(null));
    }

    @Test
    @DisplayName("Reasonable discount rate should be identified correctly")
    void testReasonableDiscountRate() {
        assertTrue(dcfValidationUtil.isReasonableDiscountRate(new BigDecimal("10.0")));
        assertTrue(dcfValidationUtil.isReasonableDiscountRate(new BigDecimal("15.0")));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(new BigDecimal("3.0")));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(new BigDecimal("25.0")));
        assertFalse(dcfValidationUtil.isReasonableDiscountRate(null));
    }

    @Test
    @DisplayName("Conservative terminal growth rate should be identified correctly")
    void testConservativeTerminalGrowthRate() {
        assertTrue(dcfValidationUtil.isConservativeTerminalGrowthRate(new BigDecimal("3.0")));
        assertTrue(dcfValidationUtil.isConservativeTerminalGrowthRate(new BigDecimal("2.5")));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(new BigDecimal("1.0")));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(new BigDecimal("5.0")));
        assertFalse(dcfValidationUtil.isConservativeTerminalGrowthRate(null));
    }

    @Test
    @DisplayName("Parameter warnings should be generated for unrealistic values")
    void testParameterWarnings() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("25.0")); // Unreasonable
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        String warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Discount rate"));

        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("75.0")); // Unreasonable

        warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Growth rate"));

        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("6.0")); // Not conservative

        warning = dcfValidationUtil.getParameterWarning(dcfInput);
        assertNotNull(warning);
        assertTrue(warning.contains("Terminal growth rate"));
    }

    @Test
    @DisplayName("No parameter warnings for reasonable values")
    void testNoParameterWarnings() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("12.0"));
        dcfInput.setGrowthRate(new BigDecimal("20.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        assertNull(dcfValidationUtil.getParameterWarning(dcfInput));
    }

    @Test
    @DisplayName("Calculation reasonableness validation should work")
    void testCalculationReasonableness() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(new BigDecimal("15000.0")); // Very high
        dcfOutput.setCurrentPrice(new BigDecimal("150.0"));

        String warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("unusually high"));

        dcfOutput.setFairValuePerShare(new BigDecimal("1500.0")); // 10x current price
        warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("10x current price"));

        dcfOutput.setFairValuePerShare(new BigDecimal("10.0")); // Much lower than current price
        warning = dcfValidationUtil.validateCalculationReasonableness(dcfOutput);
        assertNotNull(warning);
        assertTrue(warning.contains("10% of current price"));
    }

    @Test
    @DisplayName("Reasonable calculation should pass validation")
    void testReasonableCalculation() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(new BigDecimal("160.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("150.0"));

        assertNull(dcfValidationUtil.validateCalculationReasonableness(dcfOutput));
    }

    @Test
    @DisplayName("hasAllRequiredFields should work correctly")
    void testHasAllRequiredFields() {
        assertFalse(dcfValidationUtil.hasAllRequiredFields(null));
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setTicker("AAPL");
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        assertFalse(dcfValidationUtil.hasAllRequiredFields(dcfInput));

        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));
        assertTrue(dcfValidationUtil.hasAllRequiredFields(dcfInput));
    }

    @Test
    @DisplayName("Validation should handle edge cases")
    void testValidationEdgeCases() {
        // Test with exactly boundary values
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("0.0")); // Minimum allowed
        dcfInput.setGrowthRate(new BigDecimal("-100.0")); // Minimum allowed
        dcfInput.setTerminalGrowthRate(new BigDecimal("0.0")); // Minimum allowed

        assertNull(dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setDiscountRate(new BigDecimal("100.0")); // Maximum allowed
        dcfInput.setGrowthRate(new BigDecimal("1000.0")); // Maximum allowed
        dcfInput.setTerminalGrowthRate(new BigDecimal("10.0")); // Maximum allowed

        assertNull(dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("Validation should handle missing required fields")
    void testMissingRequiredFields() {
        dcfInput.setTicker("AAPL");
        // Missing discount rate
        assertEquals("Discount rate is required", dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        // Missing growth rate
        assertEquals("Growth rate is required", dcfValidationUtil.validateDCFInput(dcfInput));

        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        // Missing terminal growth rate
        assertEquals("Terminal growth rate is required", dcfValidationUtil.validateDCFInput(dcfInput));
    }

    @Test
    @DisplayName("BigDecimal precision and scale validation should work correctly")
    void testBigDecimalPrecisionAndScaleValidation() {
        // Test valid precision and scale
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            new BigDecimal("12.345678"), "Test field", 10, 6));

        // Test precision exceeds maximum
        assertEquals("Test field precision exceeds maximum allowed (5 digits)",
            dcfValidationUtil.validateBigDecimalPrecisionAndScale(
                new BigDecimal("123456"), "Test field", 5, 6));

        // Test scale exceeds maximum
        assertEquals("Test field scale exceeds maximum allowed (2 decimal places)",
            dcfValidationUtil.validateBigDecimalPrecisionAndScale(
                new BigDecimal("12.345"), "Test field", 10, 2));

        // Test null value
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            null, "Test field", 10, 6));
    }

    @Test
    @DisplayName("DCF input precision and scale validation should work")
    void testDCFInputPrecisionAndScaleValidation() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.123456")); // Valid (10,6)
        dcfInput.setGrowthRate(new BigDecimal("15.654321")); // Valid (10,6)
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.123456")); // Valid (10,6)

        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfInput));

        // Test precision exceeds limit
        dcfInput.setDiscountRate(new BigDecimal("12345678901.123456")); // Exceeds precision (10,6)
        String error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfInput);
        assertNotNull(error);
        assertTrue(error.contains("Discount rate precision exceeds"));

        // Test scale exceeds limit
        dcfInput.setDiscountRate(new BigDecimal("10.1234567")); // Exceeds scale (10,6)
        error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfInput);
        assertNotNull(error);
        assertTrue(error.contains("Discount rate scale exceeds"));
    }

    @Test
    @DisplayName("DCF output precision and scale validation should work")
    void testDCFOutputPrecisionAndScaleValidation() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(new BigDecimal("150.123456")); // Valid (20,6)
        dcfOutput.setCurrentPrice(new BigDecimal("140.654321")); // Valid (20,6)
        dcfOutput.setEnterpriseValue(new BigDecimal("1000000000.12")); // Valid (25,2)
        dcfOutput.setEquityValue(new BigDecimal("950000000.34")); // Valid (25,2)
        dcfOutput.setValuation("Undervalued");

        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfOutput));

        // Test fair value precision exceeds limit
        dcfOutput.setFairValuePerShare(new BigDecimal("123456789012345678901.123456")); // Exceeds precision (20,6)
        String error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfOutput);
        assertNotNull(error);
        assertTrue(error.contains("Fair value per share precision exceeds"));

        // Test enterprise value scale exceeds limit
        dcfOutput.setFairValuePerShare(new BigDecimal("150.123456")); // Reset to valid
        dcfOutput.setEnterpriseValue(new BigDecimal("1000000000.123")); // Exceeds scale (25,2)
        error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(dcfOutput);
        assertNotNull(error);
        assertTrue(error.contains("Enterprise value scale exceeds"));
    }

    @Test
    @DisplayName("Reasonable bounds validation should work correctly")
    void testReasonableBoundsValidation() {
        BigDecimal minValue = new BigDecimal("0.0");
        BigDecimal maxValue = new BigDecimal("100.0");

        // Test value within bounds
        assertTrue(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("50.0"), minValue, maxValue));

        // Test value at lower bound
        assertTrue(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("0.0"), minValue, maxValue));

        // Test value at upper bound
        assertTrue(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("100.0"), minValue, maxValue));

        // Test value below lower bound
        assertFalse(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("-1.0"), minValue, maxValue));

        // Test value above upper bound
        assertFalse(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("101.0"), minValue, maxValue));

        // Test null values
        assertFalse(dcfValidationUtil.isWithinReasonableBounds(
            null, minValue, maxValue));
        assertFalse(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("50.0"), null, maxValue));
        assertFalse(dcfValidationUtil.isWithinReasonableBounds(
            new BigDecimal("50.0"), minValue, null));
    }

    @Test
    @DisplayName("BigDecimal validation should handle edge cases with very large and small values")
    void testBigDecimalEdgeCases() {
        // Test very large value
        BigDecimal veryLarge = new BigDecimal("999999999999999999999999999.999999");
        String error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            veryLarge, "Large value", 10, 6);
        assertNotNull(error);
        assertTrue(error.contains("precision exceeds"));

        // Test very small value with many decimal places
        BigDecimal verySmall = new BigDecimal("0.0000000001");
        error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            verySmall, "Small value", 10, 6);
        assertNotNull(error);
        assertTrue(error.contains("scale exceeds"));

        // Test zero value
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            BigDecimal.ZERO, "Zero value", 10, 6));

        // Test negative value
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            new BigDecimal("-10.123456"), "Negative value", 10, 6));
    }

    @Test
    @DisplayName("BigDecimal validation should handle scientific notation")
    void testBigDecimalScientificNotation() {
        // Test scientific notation input
        BigDecimal scientificValue = new BigDecimal("1.23E+5"); // 123000
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            scientificValue, "Scientific value", 10, 6));

        // Test very small scientific notation
        BigDecimal smallScientific = new BigDecimal("1.23E-10"); // 0.000000000123
        String error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            smallScientific, "Small scientific", 10, 6);
        assertNotNull(error);
        assertTrue(error.contains("scale exceeds"));
    }

    @Test
    @DisplayName("BigDecimal range validation should work with meaningful error messages")
    void testBigDecimalRangeValidation() {
        BigDecimal minValue = new BigDecimal("0.0");
        BigDecimal maxValue = new BigDecimal("100.0");

        // Test valid range
        assertNull(dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("50.0"), "Test Rate", minValue, maxValue, "%"));

        // Test below minimum
        String error = dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("-5.0"), "Test Rate", minValue, maxValue, "%");
        assertEquals("Test Rate must be at least 0.0% (provided: -5.0%)", error);

        // Test above maximum
        error = dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("150.0"), "Test Rate", minValue, maxValue, "%");
        assertEquals("Test Rate must not exceed 100.0% (provided: 150.0%)", error);

        // Test null value
        error = dcfValidationUtil.validateBigDecimalRange(
            null, "Test Rate", minValue, maxValue, "%");
        assertEquals("Test Rate is required", error);

        // Test with null bounds
        assertNull(dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("150.0"), "Test Rate", null, null, "%"));
    }

    @Test
    @DisplayName("Percentage rate validation should work correctly")
    void testPercentageRateValidation() {
        // Test valid percentage rate
        assertNull(dcfValidationUtil.validatePercentageRate(
            new BigDecimal("10.123456"), "Discount Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0")));

        // Test rate below minimum
        String error = dcfValidationUtil.validatePercentageRate(
            new BigDecimal("-5.0"), "Discount Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"));
        assertTrue(error.contains("must be at least 0.0%"));

        // Test rate above maximum
        error = dcfValidationUtil.validatePercentageRate(
            new BigDecimal("150.0"), "Discount Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"));
        assertTrue(error.contains("must not exceed 100.0%"));

        // Test precision exceeds limit
        error = dcfValidationUtil.validatePercentageRate(
            new BigDecimal("12345678901.123456"), "Discount Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"));
        assertTrue(error.contains("precision exceeds"));
        assertTrue(error.contains("rates should have at most 6 decimal places"));

        // Test scale exceeds limit
        error = dcfValidationUtil.validatePercentageRate(
            new BigDecimal("10.1234567"), "Discount Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"));
        assertTrue(error.contains("scale exceeds"));
        assertTrue(error.contains("rates should have at most 6 decimal places"));
    }

    @Test
    @DisplayName("Monetary amount validation should work correctly")
    void testMonetaryAmountValidation() {
        // Test valid monetary amount
        assertNull(dcfValidationUtil.validateMonetaryAmount(
            new BigDecimal("150.123456"), "Share Price", 
            new BigDecimal("0.01"), new BigDecimal("10000.0")));

        // Test null value
        String error = dcfValidationUtil.validateMonetaryAmount(
            null, "Share Price", new BigDecimal("0.01"), new BigDecimal("10000.0"));
        assertEquals("Share Price is required", error);

        // Test negative value when not allowed
        error = dcfValidationUtil.validateMonetaryAmount(
            new BigDecimal("-50.0"), "Share Price", 
            new BigDecimal("0.01"), new BigDecimal("10000.0"));
        assertTrue(error.contains("cannot be negative"));

        // Test precision exceeds limit
        error = dcfValidationUtil.validateMonetaryAmount(
            new BigDecimal("123456789012345678901.123456"), "Share Price", 
            new BigDecimal("0.01"), null);
        assertTrue(error.contains("precision exceeds"));
        assertTrue(error.contains("monetary values should have at most 6 decimal places"));

        // Test scale exceeds limit
        error = dcfValidationUtil.validateMonetaryAmount(
            new BigDecimal("150.1234567"), "Share Price", 
            new BigDecimal("0.01"), null);
        assertTrue(error.contains("scale exceeds"));
        assertTrue(error.contains("monetary values should have at most 6 decimal places"));
    }

    @Test
    @DisplayName("Large monetary amount validation should work correctly")
    void testLargeMonetaryAmountValidation() {
        // Test valid large monetary amount
        assertNull(dcfValidationUtil.validateLargeMonetaryAmount(
            new BigDecimal("1000000000.12"), "Enterprise Value"));

        // Test null value (should be allowed for large amounts)
        assertNull(dcfValidationUtil.validateLargeMonetaryAmount(null, "Enterprise Value"));

        // Test negative value
        String error = dcfValidationUtil.validateLargeMonetaryAmount(
            new BigDecimal("-1000000000.12"), "Enterprise Value");
        assertTrue(error.contains("cannot be negative"));

        // Test precision exceeds limit
        error = dcfValidationUtil.validateLargeMonetaryAmount(
            new BigDecimal("12345678901234567890123456.12"), "Enterprise Value");
        assertTrue(error.contains("precision exceeds"));
        assertTrue(error.contains("large monetary values should have at most 2 decimal places"));

        // Test scale exceeds limit
        error = dcfValidationUtil.validateLargeMonetaryAmount(
            new BigDecimal("1000000000.123"), "Enterprise Value");
        assertTrue(error.contains("scale exceeds"));
        assertTrue(error.contains("large monetary values should have at most 2 decimal places"));

        // Test unreasonably large value
        error = dcfValidationUtil.validateLargeMonetaryAmount(
            new BigDecimal("2000000000000000"), "Enterprise Value"); // 2 quadrillion
        assertTrue(error.contains("exceeds reasonable bounds"));
    }

    @Test
    @DisplayName("Enhanced DCF input validation should work correctly")
    void testEnhancedDCFInputValidation() {
        // Test valid enhanced input
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.123456"));
        dcfInput.setGrowthRate(new BigDecimal("15.654321"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.123456"));
        dcfInput.setProjectionYears(10);

        assertNull(dcfValidationUtil.validateDCFInputEnhanced(dcfInput));

        // Test enhanced error messages for discount rate
        dcfInput.setDiscountRate(new BigDecimal("150.0"));
        String error = dcfValidationUtil.validateDCFInputEnhanced(dcfInput);
        assertTrue(error.contains("must not exceed 100.0%"));

        // Test enhanced error messages for growth rate
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("-150.0"));
        error = dcfValidationUtil.validateDCFInputEnhanced(dcfInput);
        assertTrue(error.contains("must be at least -100.0%"));

        // Test enhanced error messages for terminal growth rate
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("15.0"));
        error = dcfValidationUtil.validateDCFInputEnhanced(dcfInput);
        assertTrue(error.contains("cannot exceed discount rate"));

        // Test enhanced error messages for projection years
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));
        dcfInput.setProjectionYears(25);
        error = dcfValidationUtil.validateDCFInputEnhanced(dcfInput);
        assertTrue(error.contains("must be between 1 and 20"));
        assertTrue(error.contains("provided: 25"));
    }

    @Test
    @DisplayName("Enhanced DCF output validation should work correctly")
    void testEnhancedDCFOutputValidation() {
        // Test valid enhanced output
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(new BigDecimal("150.123456"));
        dcfOutput.setCurrentPrice(new BigDecimal("140.654321"));
        dcfOutput.setEnterpriseValue(new BigDecimal("1000000000.12"));
        dcfOutput.setEquityValue(new BigDecimal("950000000.34"));
        dcfOutput.setTerminalValue(new BigDecimal("800000000.56"));
        dcfOutput.setPresentValueOfCashFlows(new BigDecimal("700000000.78"));
        dcfOutput.setValuation("Undervalued");

        assertNull(dcfValidationUtil.validateDCFOutputEnhanced(dcfOutput));

        // Test enhanced error messages for fair value
        dcfOutput.setFairValuePerShare(new BigDecimal("0.0"));
        String error = dcfValidationUtil.validateDCFOutputEnhanced(dcfOutput);
        assertTrue(error.contains("must be at least 0.01"));

        // Test enhanced error messages for current price
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("-50.0"));
        error = dcfValidationUtil.validateDCFOutputEnhanced(dcfOutput);
        assertTrue(error.contains("cannot be negative"));

        // Test enhanced error messages for enterprise value
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
        dcfOutput.setEnterpriseValue(new BigDecimal("-1000000000.12"));
        error = dcfValidationUtil.validateDCFOutputEnhanced(dcfOutput);
        assertTrue(error.contains("cannot be negative"));
    }

    @Test
    @DisplayName("Arithmetic result validation should work correctly")
    void testArithmeticResultValidation() {
        // Test valid arithmetic result
        assertNull(dcfValidationUtil.validateArithmeticResult(
            new BigDecimal("150.123456"), "DCF calculation"));

        // Test null result
        String error = dcfValidationUtil.validateArithmeticResult(
            null, "DCF calculation");
        assertEquals("DCF calculation resulted in null value", error);

        // Test unreasonably large result
        BigDecimal largeResult = new BigDecimal("1E+60"); // Extremely large
        error = dcfValidationUtil.validateArithmeticResult(largeResult, "DCF calculation");
        assertTrue(error.contains("unreasonably large value"));
    }

    @Test
    @DisplayName("Division operation validation should work correctly")
    void testDivisionOperationValidation() {
        // Test valid division parameters
        assertNull(dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), new BigDecimal("10.0"), "Present value calculation"));

        // Test null dividend
        String error = dcfValidationUtil.validateDivisionOperation(
            null, new BigDecimal("10.0"), "Present value calculation");
        assertEquals("Present value calculation: dividend cannot be null", error);

        // Test null divisor
        error = dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), null, "Present value calculation");
        assertEquals("Present value calculation: divisor cannot be null", error);

        // Test division by zero
        error = dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), BigDecimal.ZERO, "Present value calculation");
        assertEquals("Present value calculation: division by zero is not allowed", error);

        // Test very small divisor
        error = dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), new BigDecimal("1E-15"), "Present value calculation");
        assertTrue(error.contains("divisor is too small"));
        assertTrue(error.contains("may cause precision issues"));
    }

    @Test
    @DisplayName("Comprehensive validation summary should work correctly")
    void testComprehensiveValidationSummary() {
        // Test valid input with no errors
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.123456"));
        dcfInput.setGrowthRate(new BigDecimal("15.654321"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.123456"));

        assertNull(dcfValidationUtil.getComprehensiveValidationSummary(dcfInput));

        // Test input with multiple validation errors
        dcfInput.setDiscountRate(new BigDecimal("150.0")); // Too high
        dcfInput.setGrowthRate(new BigDecimal("75.0")); // Unreasonable
        dcfInput.setTerminalGrowthRate(new BigDecimal("160.0")); // Exceeds discount rate

        String summary = dcfValidationUtil.getComprehensiveValidationSummary(dcfInput);
        assertNotNull(summary);
        assertTrue(summary.contains("Basic validation"));
        assertTrue(summary.contains("Reasonableness check"));

        // Test input with precision errors
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.1234567")); // Too many decimal places

        summary = dcfValidationUtil.getComprehensiveValidationSummary(dcfInput);
        assertNotNull(summary);
        assertTrue(summary.contains("Precision validation"));
    }
}