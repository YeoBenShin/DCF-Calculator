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

class DCFOutputTest {

    private Validator validator;
    private DCFOutput dcfOutput;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        dcfOutput = new DCFOutput();
    }

    @Test
    @DisplayName("Valid DCF output should pass validation")
    void testValidDCFOutput() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(140.0);
        dcfOutput.setValuation("Undervalued");

        Set<ConstraintViolation<DCFOutput>> violations = validator.validate(dcfOutput);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DCF output with blank ticker should fail validation")
    void testBlankTicker() {
        dcfOutput.setTicker("");
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setValuation("Undervalued");

        Set<ConstraintViolation<DCFOutput>> violations = validator.validate(dcfOutput);
        assertFalse(violations.isEmpty());
        
        boolean tickerViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Ticker symbol is required"));
        assertTrue(tickerViolationFound);
    }

    @Test
    @DisplayName("DCF output with negative fair value should fail validation")
    void testNegativeFairValue() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(-50.0);
        dcfOutput.setValuation("Undervalued");

        Set<ConstraintViolation<DCFOutput>> violations = validator.validate(dcfOutput);
        assertFalse(violations.isEmpty());
        
        boolean fairValueViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Fair value per share must be positive"));
        assertTrue(fairValueViolationFound);
    }

    @Test
    @DisplayName("DCF output with negative current price should fail validation")
    void testNegativeCurrentPrice() {
        dcfOutput.setTicker("AAPL");
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(-140.0);
        dcfOutput.setValuation("Undervalued");

        Set<ConstraintViolation<DCFOutput>> violations = validator.validate(dcfOutput);
        assertFalse(violations.isEmpty());
        
        boolean currentPriceViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Current price must be positive"));
        assertTrue(currentPriceViolationFound);
    }

    @Test
    @DisplayName("Constructor should set values and calculate upside/downside")
    void testConstructorWithParameters() {
        DCFOutput output = new DCFOutput("aapl", 150.0, 140.0, "Undervalued");
        
        assertEquals("AAPL", output.getTicker());
        assertEquals(150.0, output.getFairValuePerShare());
        assertEquals(140.0, output.getCurrentPrice());
        assertEquals("Undervalued", output.getValuation());
        assertNotNull(output.getCalculatedAt());
        
        // Should calculate upside percentage: (150-140)/140 * 100 = 7.14%
        assertEquals(7.14, output.getUpsideDownsidePercentage(), 0.01);
    }

    @Test
    @DisplayName("Upside/downside calculation should work correctly")
    void testUpsideDownsideCalculation() {
        dcfOutput.setFairValuePerShare(200.0);
        dcfOutput.setCurrentPrice(150.0);
        
        // Should calculate: (200-150)/150 * 100 = 33.33%
        assertEquals(33.33, dcfOutput.getUpsideDownsidePercentage(), 0.01);
    }

    @Test
    @DisplayName("isUndervalued should work correctly")
    void testIsUndervalued() {
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(140.0);
        assertTrue(dcfOutput.isUndervalued());
        
        dcfOutput.setCurrentPrice(160.0);
        assertFalse(dcfOutput.isUndervalued());
    }

    @Test
    @DisplayName("isOvervalued should work correctly")
    void testIsOvervalued() {
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(160.0);
        assertTrue(dcfOutput.isOvervalued());
        
        dcfOutput.setCurrentPrice(140.0);
        assertFalse(dcfOutput.isOvervalued());
    }

    @Test
    @DisplayName("isFairlyValued should work correctly")
    void testIsFairlyValued() {
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(148.0); // Within 5% tolerance
        assertTrue(dcfOutput.isFairlyValued(5.0));
        
        dcfOutput.setCurrentPrice(140.0); // Outside 5% tolerance
        assertFalse(dcfOutput.isFairlyValued(5.0));
    }

    @Test
    @DisplayName("getValuationStatus should return correct status")
    void testGetValuationStatus() {
        dcfOutput.setFairValuePerShare(150.0);
        
        dcfOutput.setCurrentPrice(148.0); // Within 5% tolerance
        assertEquals("Fair Value", dcfOutput.getValuationStatus());
        
        dcfOutput.setCurrentPrice(140.0); // Undervalued
        assertEquals("Undervalued", dcfOutput.getValuationStatus());
        
        dcfOutput.setCurrentPrice(160.0); // Overvalued
        assertEquals("Overvalued", dcfOutput.getValuationStatus());
    }

    @Test
    @DisplayName("getAbsoluteUpside should work correctly")
    void testGetAbsoluteUpside() {
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(140.0);
        
        assertEquals(10.0, dcfOutput.getAbsoluteUpside());
    }

    @Test
    @DisplayName("hasSignificantUpside should work correctly")
    void testHasSignificantUpside() {
        dcfOutput.setFairValuePerShare(150.0);
        dcfOutput.setCurrentPrice(130.0); // ~15% upside
        
        assertTrue(dcfOutput.hasSignificantUpside(10.0)); // Above 10% threshold
        assertFalse(dcfOutput.hasSignificantUpside(20.0)); // Below 20% threshold
    }

    @Test
    @DisplayName("Methods should handle null values gracefully")
    void testNullValueHandling() {
        assertFalse(dcfOutput.isUndervalued());
        assertFalse(dcfOutput.isOvervalued());
        assertFalse(dcfOutput.isFairlyValued(5.0));
        assertEquals("Unknown", dcfOutput.getValuationStatus());
        assertNull(dcfOutput.getAbsoluteUpside());
        assertFalse(dcfOutput.hasSignificantUpside(10.0));
    }

    @Test
    @DisplayName("Calculation timestamp should be set automatically")
    void testCalculationTimestamp() {
        DCFOutput output = new DCFOutput();
        assertNotNull(output.getCalculatedAt());
        
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        LocalDateTime after = LocalDateTime.now().plusMinutes(1);
        
        assertTrue(output.getCalculatedAt().isAfter(before));
        assertTrue(output.getCalculatedAt().isBefore(after));
    }

    @Test
    @DisplayName("Ticker should be automatically converted to uppercase")
    void testTickerUppercaseConversion() {
        dcfOutput.setTicker("aapl");
        assertEquals("AAPL", dcfOutput.getTicker());
    }

    @Test
    @DisplayName("Setting fair value should recalculate upside/downside")
    void testFairValueUpdateRecalculation() {
        dcfOutput.setCurrentPrice(100.0);
        dcfOutput.setFairValuePerShare(110.0);
        assertEquals(10.0, dcfOutput.getUpsideDownsidePercentage(), 0.01);
        
        dcfOutput.setFairValuePerShare(120.0);
        assertEquals(20.0, dcfOutput.getUpsideDownsidePercentage(), 0.01);
    }

    @Test
    @DisplayName("Setting current price should recalculate upside/downside")
    void testCurrentPriceUpdateRecalculation() {
        dcfOutput.setFairValuePerShare(110.0);
        dcfOutput.setCurrentPrice(100.0);
        assertEquals(10.0, dcfOutput.getUpsideDownsidePercentage(), 0.01);
        
        dcfOutput.setCurrentPrice(90.0);
        assertEquals(22.22, dcfOutput.getUpsideDownsidePercentage(), 0.01);
    }
}