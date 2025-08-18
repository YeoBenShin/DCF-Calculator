package com.dcf.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DCFInputTest {

    private Validator validator;
    private DCFInput dcfInput;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        dcfInput = new DCFInput();
    }

    @Test
    @DisplayName("Valid DCF input should pass validation")
    void testValidDCFInput() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DCF input with blank ticker should fail validation")
    void testBlankTicker() {
        dcfInput.setTicker("");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean tickerViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Ticker symbol is required"));
        assertTrue(tickerViolationFound);
    }

    @Test
    @DisplayName("DCF input with negative discount rate should fail validation")
    void testNegativeDiscountRate() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("-5.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean discountRateViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Discount rate must be positive"));
        assertTrue(discountRateViolationFound);
    }

    @Test
    @DisplayName("DCF input with excessive growth rate should fail validation")
    void testExcessiveGrowthRate() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("1500.0")); // > 1000%
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean growthRateViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Growth rate must be less than 1000%"));
        assertTrue(growthRateViolationFound);
    }

    @Test
    @DisplayName("DCF input with excessive terminal growth rate should fail validation")
    void testExcessiveTerminalGrowthRate() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("15.0")); // > 10%

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean terminalGrowthViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Terminal growth rate must be less than 10%"));
        assertTrue(terminalGrowthViolationFound);
    }

    @Test
    @DisplayName("Ticker should be automatically converted to uppercase")
    void testTickerUppercaseConversion() {
        dcfInput.setTicker("aapl");
        assertEquals("AAPL", dcfInput.getTicker());
    }

    @Test
    @DisplayName("Constructor with parameters should set values correctly")
    void testConstructorWithParameters() {
        DCFInput input = new DCFInput("googl", new BigDecimal("12.0"), new BigDecimal("20.0"), new BigDecimal("2.5"));
        
        assertEquals("GOOGL", input.getTicker());
        assertEquals(0, new BigDecimal("12.0").compareTo(input.getDiscountRate()));
        assertEquals(0, new BigDecimal("20.0").compareTo(input.getGrowthRate()));
        assertEquals(0, new BigDecimal("2.5").compareTo(input.getTerminalGrowthRate()));
        assertNotNull(input.getCreatedAt());
    }

    @Test
    @DisplayName("Default projection years should be 5")
    void testDefaultProjectionYears() {
        assertEquals(5, dcfInput.getProjectionYears());
    }

    @Test
    @DisplayName("isReasonableGrowthRate should work correctly")
    void testIsReasonableGrowthRate() {
        dcfInput.setGrowthRate(new BigDecimal("25.0"));
        assertTrue(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setGrowthRate(new BigDecimal("150.0"));
        assertFalse(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setGrowthRate(null);
        assertFalse(dcfInput.isReasonableGrowthRate());
    }

    @Test
    @DisplayName("isConservativeTerminalGrowthRate should work correctly")
    void testIsConservativeTerminalGrowthRate() {
        dcfInput.setTerminalGrowthRate(new BigDecimal("2.5"));
        assertTrue(dcfInput.isConservativeTerminalGrowthRate());
        
        dcfInput.setTerminalGrowthRate(new BigDecimal("5.0"));
        assertFalse(dcfInput.isConservativeTerminalGrowthRate());
        
        dcfInput.setTerminalGrowthRate(null);
        assertFalse(dcfInput.isConservativeTerminalGrowthRate());
    }

    @Test
    @DisplayName("Rate conversion to decimal should work correctly")
    void testRateConversionToDecimal() {
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));

        assertEquals(0, new BigDecimal("0.10").compareTo(dcfInput.getDiscountRateAsDecimal()));
        assertEquals(0, new BigDecimal("0.15").compareTo(dcfInput.getGrowthRateAsDecimal()));
        assertEquals(0, new BigDecimal("0.03").compareTo(dcfInput.getTerminalGrowthRateAsDecimal()));
    }

    @Test
    @DisplayName("Rate conversion should handle null values")
    void testRateConversionWithNull() {
        assertNull(dcfInput.getDiscountRateAsDecimal());
        assertNull(dcfInput.getGrowthRateAsDecimal());
        assertNull(dcfInput.getTerminalGrowthRateAsDecimal());
    }

    @Test
    @DisplayName("Creation timestamp should be set automatically")
    void testCreationTimestamp() {
        DCFInput input = new DCFInput();
        assertNotNull(input.getCreatedAt());
        
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        LocalDateTime after = LocalDateTime.now().plusMinutes(1);
        
        assertTrue(input.getCreatedAt().isAfter(before));
        assertTrue(input.getCreatedAt().isBefore(after));
    }

    @Test
    @DisplayName("Projection years validation should work correctly")
    void testProjectionYearsValidation() {
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("10.0"));
        dcfInput.setGrowthRate(new BigDecimal("15.0"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));
        dcfInput.setProjectionYears(25); // > 20

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean projectionYearsViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Projection years must be at most 20"));
        assertTrue(projectionYearsViolationFound);
    }

    @Test
    @DisplayName("BigDecimal precision should be maintained")
    void testBigDecimalPrecision() {
        BigDecimal preciseRate = new BigDecimal("10.123456");
        dcfInput.setDiscountRate(preciseRate);
        
        assertEquals(0, preciseRate.compareTo(dcfInput.getDiscountRate()));
    }

    @Test
    @DisplayName("BigDecimal arithmetic in utility methods should maintain precision")
    void testBigDecimalArithmeticPrecision() {
        dcfInput.setDiscountRate(new BigDecimal("10.123456"));
        dcfInput.setGrowthRate(new BigDecimal("15.654321"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.987654"));

        BigDecimal expectedDiscountDecimal = new BigDecimal("10.123456").divide(new BigDecimal("100.0"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedGrowthDecimal = new BigDecimal("15.654321").divide(new BigDecimal("100.0"), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedTerminalDecimal = new BigDecimal("3.987654").divide(new BigDecimal("100.0"), 10, java.math.RoundingMode.HALF_UP);

        assertEquals(0, expectedDiscountDecimal.compareTo(dcfInput.getDiscountRateAsDecimal()));
        assertEquals(0, expectedGrowthDecimal.compareTo(dcfInput.getGrowthRateAsDecimal()));
        assertEquals(0, expectedTerminalDecimal.compareTo(dcfInput.getTerminalGrowthRateAsDecimal()));
    }

    @Test
    @DisplayName("BigDecimal validation should handle edge cases")
    void testBigDecimalValidationEdgeCases() {
        // Test with very precise values
        dcfInput.setTicker("AAPL");
        dcfInput.setDiscountRate(new BigDecimal("0.000001"));
        dcfInput.setGrowthRate(new BigDecimal("999.999999"));
        dcfInput.setTerminalGrowthRate(new BigDecimal("9.999999"));

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("BigDecimal comparison methods should work correctly")
    void testBigDecimalComparisons() {
        dcfInput.setGrowthRate(new BigDecimal("100.0"));
        assertTrue(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setGrowthRate(new BigDecimal("100.000001"));
        assertFalse(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.0"));
        assertTrue(dcfInput.isConservativeTerminalGrowthRate());
        
        dcfInput.setTerminalGrowthRate(new BigDecimal("3.000001"));
        assertFalse(dcfInput.isConservativeTerminalGrowthRate());
    }
}