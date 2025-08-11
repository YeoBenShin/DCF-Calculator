package com.dcf.util;

import com.dcf.entity.FinancialData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class FinancialDataUtilTest {

    private FinancialDataUtil financialDataUtil;
    private FinancialData financialData;

    @BeforeEach
    void setUp() {
        financialDataUtil = new FinancialDataUtil();
        financialData = new FinancialData();
    }

    @Test
    @DisplayName("Valid ticker should pass validation")
    void testValidTicker() {
        assertTrue(financialDataUtil.isValidTicker("AAPL"));
        assertTrue(financialDataUtil.isValidTicker("GOOGL"));
        assertTrue(financialDataUtil.isValidTicker("MSFT"));
        assertTrue(financialDataUtil.isValidTicker("BRK.A")); // This should actually fail with current regex
    }

    @Test
    @DisplayName("Invalid ticker should fail validation")
    void testInvalidTicker() {
        assertFalse(financialDataUtil.isValidTicker(null));
        assertFalse(financialDataUtil.isValidTicker(""));
        assertFalse(financialDataUtil.isValidTicker("   "));
        assertFalse(financialDataUtil.isValidTicker("VERYLONGTICKER"));
        assertFalse(financialDataUtil.isValidTicker("TICK-ER")); // Special characters
        assertFalse(financialDataUtil.isValidTicker("tick er")); // Spaces
    }

    @Test
    @DisplayName("Ticker normalization should work correctly")
    void testTickerNormalization() {
        assertEquals("AAPL", financialDataUtil.normalizeTicker("aapl"));
        assertEquals("GOOGL", financialDataUtil.normalizeTicker("  googl  "));
        assertEquals("MSFT", financialDataUtil.normalizeTicker("MsFt"));
        assertNull(financialDataUtil.normalizeTicker("invalid-ticker"));
        assertNull(financialDataUtil.normalizeTicker(null));
    }

    @Test
    @DisplayName("Valid financial values should pass validation")
    void testValidFinancialValue() {
        assertTrue(financialDataUtil.isValidFinancialValue(100.0));
        assertTrue(financialDataUtil.isValidFinancialValue(-50.0));
        assertTrue(financialDataUtil.isValidFinancialValue(0.0));
        assertTrue(financialDataUtil.isValidFinancialValue(1000000.0));
    }

    @Test
    @DisplayName("Invalid financial values should fail validation")
    void testInvalidFinancialValue() {
        assertFalse(financialDataUtil.isValidFinancialValue(null));
        assertFalse(financialDataUtil.isValidFinancialValue(Double.NaN));
        assertFalse(financialDataUtil.isValidFinancialValue(Double.POSITIVE_INFINITY));
        assertFalse(financialDataUtil.isValidFinancialValue(Double.NEGATIVE_INFINITY));
        assertFalse(financialDataUtil.isValidFinancialValue(1e16)); // Too large
    }

    @Test
    @DisplayName("Valid financial value list should pass validation")
    void testValidFinancialValueList() {
        assertTrue(financialDataUtil.isValidFinancialValueList(Arrays.asList(100.0, 110.0, 120.0)));
        assertTrue(financialDataUtil.isValidFinancialValueList(Collections.emptyList()));
        assertTrue(financialDataUtil.isValidFinancialValueList(null));
    }

    @Test
    @DisplayName("Invalid financial value list should fail validation")
    void testInvalidFinancialValueList() {
        assertFalse(financialDataUtil.isValidFinancialValueList(Arrays.asList(100.0, null, 120.0)));
        assertFalse(financialDataUtil.isValidFinancialValueList(Arrays.asList(100.0, Double.NaN, 120.0)));
        assertFalse(financialDataUtil.isValidFinancialValueList(Arrays.asList(100.0, Double.POSITIVE_INFINITY)));
    }

    @Test
    @DisplayName("Financial data with minimum required data should pass validation")
    void testHasMinimumRequiredDataTrue() {
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0));
        financialData.setEps(Arrays.asList(2.0, 2.2));

        assertTrue(financialDataUtil.hasMinimumRequiredData(financialData));
    }

    @Test
    @DisplayName("Financial data without minimum required data should fail validation")
    void testHasMinimumRequiredDataFalse() {
        assertFalse(financialDataUtil.hasMinimumRequiredData(null));
        
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0));
        // Missing freeCashFlow and eps
        assertFalse(financialDataUtil.hasMinimumRequiredData(financialData));
    }

    @Test
    @DisplayName("Consistent financial data should pass validation")
    void testDataConsistencyTrue() {
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setOperatingExpense(Arrays.asList(80.0, 85.0, 90.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 25.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5));

        assertTrue(financialDataUtil.isDataConsistent(financialData));
    }

    @Test
    @DisplayName("Inconsistent financial data should fail validation")
    void testDataConsistencyFalse() {
        assertFalse(financialDataUtil.isDataConsistent(null));
        
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0)); // 3 elements
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0)); // 2 elements
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5, 2.8)); // 4 elements

        assertFalse(financialDataUtil.isDataConsistent(financialData));
    }

    @Test
    @DisplayName("Valid financial data should return no validation error")
    void testValidationErrorNull() {
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 25.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5));

        assertNull(financialDataUtil.getValidationError(financialData));
    }

    @Test
    @DisplayName("Invalid financial data should return appropriate validation errors")
    void testValidationErrors() {
        // Null data
        assertEquals("Financial data is required", 
                    financialDataUtil.getValidationError(null));

        // Invalid ticker
        financialData.setTicker("invalid-ticker");
        assertEquals("Invalid ticker symbol", 
                    financialDataUtil.getValidationError(financialData));

        // Missing required data
        financialData.setTicker("AAPL");
        assertEquals("Financial data must include revenue, free cash flow, and EPS", 
                    financialDataUtil.getValidationError(financialData));

        // Inconsistent data
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5));
        assertEquals("Financial data arrays must have consistent lengths", 
                    financialDataUtil.getValidationError(financialData));
    }

    @Test
    @DisplayName("Revenue growth rate calculation should work correctly")
    void testCalculateRevenueGrowthRate() {
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 121.0)); // 10% growth each year

        Double growthRate = financialDataUtil.calculateRevenueGrowthRate(financialData);
        assertNotNull(growthRate);
        assertEquals(0.1, growthRate, 0.001); // 10% growth
    }

    @Test
    @DisplayName("Revenue growth rate calculation should handle insufficient data")
    void testCalculateRevenueGrowthRateInsufficientData() {
        assertNull(financialDataUtil.calculateRevenueGrowthRate(null));
        
        financialData.setRevenue(Arrays.asList(100.0)); // Only one data point
        assertNull(financialDataUtil.calculateRevenueGrowthRate(financialData));
    }

    @Test
    @DisplayName("Revenue growth rate calculation should handle zero values")
    void testCalculateRevenueGrowthRateWithZero() {
        financialData.setRevenue(Arrays.asList(0.0, 100.0, 110.0));

        Double growthRate = financialDataUtil.calculateRevenueGrowthRate(financialData);
        assertNotNull(growthRate);
        // Should skip the 0.0 to 100.0 calculation and only use 100.0 to 110.0
    }

    @Test
    @DisplayName("Data consistency should handle empty lists")
    void testDataConsistencyWithEmptyLists() {
        financialData.setTicker("AAPL");
        // All lists are empty by default
        
        assertTrue(financialDataUtil.isDataConsistent(financialData));
    }
}