package com.dcf.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DCF input with blank ticker should fail validation")
    void testBlankTicker() {
        dcfInput.setTicker("");
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

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
        dcfInput.setDiscountRate(-5.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

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
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(1500.0); // > 1000%
        dcfInput.setTerminalGrowthRate(3.0);

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
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(15.0); // > 10%

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
        DCFInput input = new DCFInput("googl", 12.0, 20.0, 2.5);
        
        assertEquals("GOOGL", input.getTicker());
        assertEquals(12.0, input.getDiscountRate());
        assertEquals(20.0, input.getGrowthRate());
        assertEquals(2.5, input.getTerminalGrowthRate());
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
        dcfInput.setGrowthRate(25.0);
        assertTrue(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setGrowthRate(150.0);
        assertFalse(dcfInput.isReasonableGrowthRate());
        
        dcfInput.setGrowthRate(null);
        assertFalse(dcfInput.isReasonableGrowthRate());
    }

    @Test
    @DisplayName("isConservativeTerminalGrowthRate should work correctly")
    void testIsConservativeTerminalGrowthRate() {
        dcfInput.setTerminalGrowthRate(2.5);
        assertTrue(dcfInput.isConservativeTerminalGrowthRate());
        
        dcfInput.setTerminalGrowthRate(5.0);
        assertFalse(dcfInput.isConservativeTerminalGrowthRate());
        
        dcfInput.setTerminalGrowthRate(null);
        assertFalse(dcfInput.isConservativeTerminalGrowthRate());
    }

    @Test
    @DisplayName("Rate conversion to decimal should work correctly")
    void testRateConversionToDecimal() {
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);

        assertEquals(0.10, dcfInput.getDiscountRateAsDecimal(), 0.001);
        assertEquals(0.15, dcfInput.getGrowthRateAsDecimal(), 0.001);
        assertEquals(0.03, dcfInput.getTerminalGrowthRateAsDecimal(), 0.001);
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
        dcfInput.setDiscountRate(10.0);
        dcfInput.setGrowthRate(15.0);
        dcfInput.setTerminalGrowthRate(3.0);
        dcfInput.setProjectionYears(25); // > 20

        Set<ConstraintViolation<DCFInput>> violations = validator.validate(dcfInput);
        assertFalse(violations.isEmpty());
        
        boolean projectionYearsViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Projection years must be at most 20"));
        assertTrue(projectionYearsViolationFound);
    }
}