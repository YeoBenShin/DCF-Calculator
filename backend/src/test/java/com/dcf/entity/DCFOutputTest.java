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
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
        dcfOutput.setValuation("Undervalued");

        Set<ConstraintViolation<DCFOutput>> violations = validator.validate(dcfOutput);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("DCF output with blank ticker should fail validation")
    void testBlankTicker() {
        dcfOutput.setTicker("");
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
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
        dcfOutput.setFairValuePerShare(new BigDecimal("-50.0"));
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
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("-140.0"));
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
        DCFOutput output = new DCFOutput("aapl", new BigDecimal("150.0"), new BigDecimal("140.0"), "Undervalued");
        
        assertEquals("AAPL", output.getTicker());
        assertEquals(0, new BigDecimal("150.0").compareTo(output.getFairValuePerShare()));
        assertEquals(0, new BigDecimal("140.0").compareTo(output.getCurrentPrice()));
        assertEquals("Undervalued", output.getValuation());
        assertNotNull(output.getCalculatedAt());
        
        // Should calculate upside percentage: (150-140)/140 * 100 = 7.142900%
        BigDecimal expectedUpside = new BigDecimal("7.142900");
        assertEquals(0, expectedUpside.compareTo(output.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Upside/downside calculation should work correctly")
    void testUpsideDownsideCalculation() {
        dcfOutput.setFairValuePerShare(new BigDecimal("200.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("150.0"));
        
        // Should calculate: (200-150)/150 * 100 = 33.333300%
        BigDecimal expectedUpside = new BigDecimal("33.333300");
        assertEquals(0, expectedUpside.compareTo(dcfOutput.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("isUndervalued should work correctly")
    void testIsUndervalued() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
        assertTrue(dcfOutput.isUndervalued());
        
        dcfOutput.setCurrentPrice(new BigDecimal("160.0"));
        assertFalse(dcfOutput.isUndervalued());
    }

    @Test
    @DisplayName("isOvervalued should work correctly")
    void testIsOvervalued() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("160.0"));
        assertTrue(dcfOutput.isOvervalued());
        
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
        assertFalse(dcfOutput.isOvervalued());
    }

    @Test
    @DisplayName("isFairlyValued should work correctly")
    void testIsFairlyValued() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("148.0")); // Within 5% tolerance
        assertTrue(dcfOutput.isFairlyValued(new BigDecimal("5.0")));
        
        dcfOutput.setCurrentPrice(new BigDecimal("140.0")); // Outside 5% tolerance
        assertFalse(dcfOutput.isFairlyValued(new BigDecimal("5.0")));
    }

    @Test
    @DisplayName("getValuationStatus should return correct status")
    void testGetValuationStatus() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        
        dcfOutput.setCurrentPrice(new BigDecimal("148.0")); // Within 5% tolerance
        assertEquals("Fair Value", dcfOutput.getValuationStatus());
        
        dcfOutput.setCurrentPrice(new BigDecimal("140.0")); // Undervalued
        assertEquals("Undervalued", dcfOutput.getValuationStatus());
        
        dcfOutput.setCurrentPrice(new BigDecimal("160.0")); // Overvalued
        assertEquals("Overvalued", dcfOutput.getValuationStatus());
    }

    @Test
    @DisplayName("getAbsoluteUpside should work correctly")
    void testGetAbsoluteUpside() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("140.0"));
        
        assertEquals(0, new BigDecimal("10.0").compareTo(dcfOutput.getAbsoluteUpside()));
    }

    @Test
    @DisplayName("hasSignificantUpside should work correctly")
    void testHasSignificantUpside() {
        dcfOutput.setFairValuePerShare(new BigDecimal("150.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("130.0")); // ~15% upside
        
        assertTrue(dcfOutput.hasSignificantUpside(new BigDecimal("10.0"))); // Above 10% threshold
        assertFalse(dcfOutput.hasSignificantUpside(new BigDecimal("20.0"))); // Below 20% threshold
    }

    @Test
    @DisplayName("Methods should handle null values gracefully")
    void testNullValueHandling() {
        assertFalse(dcfOutput.isUndervalued());
        assertFalse(dcfOutput.isOvervalued());
        assertFalse(dcfOutput.isFairlyValued(new BigDecimal("5.0")));
        assertEquals("Unknown", dcfOutput.getValuationStatus());
        assertNull(dcfOutput.getAbsoluteUpside());
        assertFalse(dcfOutput.hasSignificantUpside(new BigDecimal("10.0")));
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
        dcfOutput.setCurrentPrice(new BigDecimal("100.0"));
        dcfOutput.setFairValuePerShare(new BigDecimal("110.0"));
        assertEquals(0, new BigDecimal("10.000000").compareTo(dcfOutput.getUpsideDownsidePercentage()));
        
        dcfOutput.setFairValuePerShare(new BigDecimal("120.0"));
        assertEquals(0, new BigDecimal("20.000000").compareTo(dcfOutput.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Setting current price should recalculate upside/downside")
    void testCurrentPriceUpdateRecalculation() {
        dcfOutput.setFairValuePerShare(new BigDecimal("110.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("100.0"));
        assertEquals(0, new BigDecimal("10.000000").compareTo(dcfOutput.getUpsideDownsidePercentage()));
        
        dcfOutput.setCurrentPrice(new BigDecimal("90.0"));
        assertEquals(0, new BigDecimal("22.222200").compareTo(dcfOutput.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("BigDecimal precision should be maintained in calculations")
    void testBigDecimalPrecisionMaintained() {
        dcfOutput.setFairValuePerShare(new BigDecimal("123.456789"));
        dcfOutput.setCurrentPrice(new BigDecimal("100.123456"));
        
        // Verify precision is maintained in upside calculation
        BigDecimal expectedUpside = new BigDecimal("23.304600"); // (123.456789 - 100.123456) / 100.123456 * 100
        assertEquals(0, expectedUpside.compareTo(dcfOutput.getUpsideDownsidePercentage()));
        
        // Verify absolute upside maintains precision
        BigDecimal expectedAbsoluteUpside = new BigDecimal("23.333333");
        assertEquals(0, expectedAbsoluteUpside.compareTo(dcfOutput.getAbsoluteUpside()));
    }

    @Test
    @DisplayName("BigDecimal comparisons should work correctly for edge cases")
    void testBigDecimalComparisons() {
        // Test with very small differences
        dcfOutput.setFairValuePerShare(new BigDecimal("100.000001"));
        dcfOutput.setCurrentPrice(new BigDecimal("100.000000"));
        assertTrue(dcfOutput.isUndervalued());
        
        // Test with equal values
        dcfOutput.setFairValuePerShare(new BigDecimal("100.000000"));
        dcfOutput.setCurrentPrice(new BigDecimal("100.000000"));
        assertFalse(dcfOutput.isUndervalued());
        assertFalse(dcfOutput.isOvervalued());
    }

    @Test
    @DisplayName("BigDecimal tolerance calculations should work correctly")
    void testBigDecimalToleranceCalculations() {
        dcfOutput.setFairValuePerShare(new BigDecimal("100.00"));
        dcfOutput.setCurrentPrice(new BigDecimal("95.00"));
        
        // 5% tolerance: 95 * 0.05 = 4.75, difference is 5.00, so not fairly valued
        assertFalse(dcfOutput.isFairlyValued(new BigDecimal("5.0")));
        
        // 6% tolerance: 95 * 0.06 = 5.70, difference is 5.00, so fairly valued
        assertTrue(dcfOutput.isFairlyValued(new BigDecimal("6.0")));
    }

    @Test
    @DisplayName("BigDecimal rounding should be consistent")
    void testBigDecimalRounding() {
        // Test division that requires rounding
        dcfOutput.setFairValuePerShare(new BigDecimal("100.0"));
        dcfOutput.setCurrentPrice(new BigDecimal("33.333333"));
        
        // Should round to 6 decimal places using HALF_UP
        BigDecimal expectedUpside = new BigDecimal("200.000000"); // (100 - 33.333333) / 33.333333 * 100
        assertEquals(0, expectedUpside.compareTo(dcfOutput.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Large BigDecimal values should be handled correctly")
    void testLargeBigDecimalValues() {
        // Test with very large values
        dcfOutput.setFairValuePerShare(new BigDecimal("999999999999.999999"));
        dcfOutput.setCurrentPrice(new BigDecimal("500000000000.000000"));
        
        assertTrue(dcfOutput.isUndervalued());
        assertEquals(0, new BigDecimal("100.000000").compareTo(dcfOutput.getUpsideDownsidePercentage()));
    }

    @Test
    @DisplayName("Zero current price should not cause division by zero")
    void testZeroCurrentPriceHandling() {
        dcfOutput.setFairValuePerShare(new BigDecimal("100.0"));
        dcfOutput.setCurrentPrice(BigDecimal.ZERO);
        
        // Should not calculate upside/downside when current price is zero
        assertNull(dcfOutput.getUpsideDownsidePercentage());
        assertTrue(dcfOutput.isUndervalued()); // 100 > 0, so it is undervalued
        assertFalse(dcfOutput.isOvervalued());
    }
}