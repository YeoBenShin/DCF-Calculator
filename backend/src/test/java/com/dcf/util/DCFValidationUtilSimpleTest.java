package com.dcf.util;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.util.DCFValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DCFValidationUtilSimpleTest {

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
    @DisplayName("Test basic BigDecimal range validation")
    void testBasicBigDecimalRangeValidation() {
        // Test valid range
        assertNull(dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("50.0"), "Test Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"), "%"));

        // Test below minimum
        String error = dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("-5.0"), "Test Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"), "%");
        assertNotNull(error);
        assertTrue(error.contains("must be at least 0.0%"));

        // Test above maximum
        error = dcfValidationUtil.validateBigDecimalRange(
            new BigDecimal("150.0"), "Test Rate", 
            new BigDecimal("0.0"), new BigDecimal("100.0"), "%");
        assertNotNull(error);
        assertTrue(error.contains("must not exceed 100.0%"));
    }

    @Test
    @DisplayName("Test BigDecimal precision and scale validation")
    void testBigDecimalPrecisionAndScaleValidation() {
        // Test valid precision and scale
        assertNull(dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            new BigDecimal("12.345678"), "Test field", 10, 6));

        // Test precision exceeds maximum
        String error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            new BigDecimal("123456"), "Test field", 5, 6);
        assertNotNull(error);
        assertTrue(error.contains("precision exceeds maximum allowed"));

        // Test scale exceeds maximum
        error = dcfValidationUtil.validateBigDecimalPrecisionAndScale(
            new BigDecimal("12.345"), "Test field", 10, 2);
        assertNotNull(error);
        assertTrue(error.contains("scale exceeds maximum allowed"));
    }

    @Test
    @DisplayName("Test enhanced DCF input validation works")
    void testEnhancedDCFInputValidationWorks() {
        // Test valid enhanced input
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.123456"));
        dcfInput.setGrowthRate(new BigDecimal("15.654321"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.123456"));
        dcfInput.setProjectionYears(10);

        String result = dcfValidationUtil.validateDCFInputEnhanced(dcfInput);
        assertNull(result, "Valid input should pass validation");
    }

    @Test
    @DisplayName("Test arithmetic result validation")
    void testArithmeticResultValidation() {
        // Test valid arithmetic result
        assertNull(dcfValidationUtil.validateArithmeticResult(
            new BigDecimal("150.123456"), "DCF calculation"));

        // Test null result
        String error = dcfValidationUtil.validateArithmeticResult(
            null, "DCF calculation");
        assertNotNull(error);
        assertTrue(error.contains("resulted in null value"));
    }

    @Test
    @DisplayName("Test division operation validation")
    void testDivisionOperationValidation() {
        // Test valid division parameters
        assertNull(dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), new BigDecimal("10.0"), "Present value calculation"));

        // Test division by zero
        String error = dcfValidationUtil.validateDivisionOperation(
            new BigDecimal("100.0"), BigDecimal.ZERO, "Present value calculation");
        assertNotNull(error);
        assertTrue(error.contains("division by zero is not allowed"));
    }
}