package com.dcf.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FinancialDataTest {

    private Validator validator;
    private FinancialData financialData;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        financialData = new FinancialData();
    }

    @Test
    @DisplayName("Valid financial data should pass validation")
    void testValidFinancialData() {
        financialData.setTicker("AAPL");
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 25.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5));

        Set<ConstraintViolation<FinancialData>> violations = validator.validate(financialData);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Financial data with blank ticker should fail validation")
    void testBlankTicker() {
        financialData.setTicker("");

        Set<ConstraintViolation<FinancialData>> violations = validator.validate(financialData);
        assertFalse(violations.isEmpty());
        
        boolean tickerViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Ticker symbol is required"));
        assertTrue(tickerViolationFound);
    }

    @Test
    @DisplayName("Financial data with long ticker should fail validation")
    void testLongTicker() {
        financialData.setTicker("VERYLONGTICKER");

        Set<ConstraintViolation<FinancialData>> violations = validator.validate(financialData);
        assertFalse(violations.isEmpty());
        
        boolean tickerViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Ticker symbol must be between 1 and 10 characters"));
        assertTrue(tickerViolationFound);
    }

    @Test
    @DisplayName("Ticker should be automatically converted to uppercase")
    void testTickerUppercaseConversion() {
        financialData.setTicker("aapl");
        assertEquals("AAPL", financialData.getTicker());
    }

    @Test
    @DisplayName("Constructor with ticker should set ticker correctly")
    void testConstructorWithTicker() {
        FinancialData data = new FinancialData("googl");
        assertEquals("GOOGL", data.getTicker());
        assertNotNull(data.getCreatedAt());
        assertNotNull(data.getUpdatedAt());
        assertNotNull(data.getDateFetched());
    }

    @Test
    @DisplayName("hasValidData should return true when required data is present")
    void testHasValidDataTrue() {
        financialData.setRevenue(Arrays.asList(100.0, 110.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0));
        financialData.setEps(Arrays.asList(2.0, 2.2));

        assertTrue(financialData.hasValidData());
    }

    @Test
    @DisplayName("hasValidData should return false when required data is missing")
    void testHasValidDataFalse() {
        financialData.setRevenue(Arrays.asList(100.0, 110.0));
        // Missing freeCashFlow and eps

        assertFalse(financialData.hasValidData());
    }

    @Test
    @DisplayName("isDataStale should work correctly")
    void testIsDataStale() {
        // Set date to 10 days ago
        financialData.setDateFetched(LocalDate.now().minusDays(10));
        
        assertTrue(financialData.isDataStale(5)); // 5 day threshold
        assertFalse(financialData.isDataStale(15)); // 15 day threshold
    }

    @Test
    @DisplayName("getLatest methods should return correct values")
    void testGetLatestMethods() {
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0, 25.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5));

        assertEquals(120.0, financialData.getLatestRevenue());
        assertEquals(25.0, financialData.getLatestFreeCashFlow());
        assertEquals(2.5, financialData.getLatestEps());
    }

    @Test
    @DisplayName("getLatest methods should return null for empty lists")
    void testGetLatestMethodsEmpty() {
        assertNull(financialData.getLatestRevenue());
        assertNull(financialData.getLatestFreeCashFlow());
        assertNull(financialData.getLatestEps());
    }

    @Test
    @DisplayName("getDataYears should return correct count")
    void testGetDataYears() {
        financialData.setRevenue(Arrays.asList(100.0, 110.0, 120.0));
        financialData.setFreeCashFlow(Arrays.asList(20.0, 22.0));
        financialData.setEps(Arrays.asList(2.0, 2.2, 2.5, 2.8));

        assertEquals(4, financialData.getDataYears()); // Max of 3, 2, 4
    }

    @Test
    @DisplayName("Setters should handle null values gracefully")
    void testSettersWithNull() {
        financialData.setRevenue(null);
        financialData.setOperatingExpense(null);
        financialData.setFreeCashFlow(null);

        assertNotNull(financialData.getRevenue());
        assertNotNull(financialData.getOperatingExpense());
        assertNotNull(financialData.getFreeCashFlow());
        
        assertTrue(financialData.getRevenue().isEmpty());
        assertTrue(financialData.getOperatingExpense().isEmpty());
        assertTrue(financialData.getFreeCashFlow().isEmpty());
    }

    @Test
    @DisplayName("All financial data lists should be initialized as empty lists")
    void testInitializedLists() {
        FinancialData data = new FinancialData();
        
        assertNotNull(data.getRevenue());
        assertNotNull(data.getOperatingExpense());
        assertNotNull(data.getOperatingIncome());
        assertNotNull(data.getOperatingCashFlow());
        assertNotNull(data.getNetProfit());
        assertNotNull(data.getCapitalExpenditure());
        assertNotNull(data.getFreeCashFlow());
        assertNotNull(data.getEps());
        assertNotNull(data.getTotalDebt());
        assertNotNull(data.getOrdinarySharesNumber());
        
        assertTrue(data.getRevenue().isEmpty());
        assertTrue(data.getOperatingExpense().isEmpty());
        assertTrue(data.getOperatingIncome().isEmpty());
    }

    @Test
    @DisplayName("Date fields should be set correctly on creation")
    void testDateFieldsOnCreation() {
        FinancialData data = new FinancialData();
        LocalDate today = LocalDate.now();
        
        assertEquals(today, data.getCreatedAt());
        assertEquals(today, data.getUpdatedAt());
        assertEquals(today, data.getDateFetched());
    }
}