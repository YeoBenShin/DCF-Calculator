package com.dcf.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test for BigDecimal parsing functionality in FinancialDataScrapingService
 * This test doesn't use Spring Boot context to avoid dependency issues
 */
class BigDecimalParsingTest {

    @Test
    @DisplayName("Should parse BigDecimal values correctly from various text formats")
    void testBigDecimalParsing() throws Exception {
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        
        // Use reflection to access the private parseFinancialValue method
        Method parseMethod = FinancialDataScrapingService.class.getDeclaredMethod("parseFinancialValue", String.class);
        parseMethod.setAccessible(true);

        // Test regular decimal numbers
        BigDecimal result1 = (BigDecimal) parseMethod.invoke(service, "123.45");
        assertEquals(new BigDecimal("123.45"), result1);

        // Test numbers with commas
        BigDecimal result2 = (BigDecimal) parseMethod.invoke(service, "1,234.56");
        assertEquals(new BigDecimal("1234.56"), result2);

        // Test negative numbers
        BigDecimal result3 = (BigDecimal) parseMethod.invoke(service, "-789.12");
        assertEquals(new BigDecimal("-789.12"), result3);

        // Test numbers in parentheses (negative)
        BigDecimal result4 = (BigDecimal) parseMethod.invoke(service, "(456.78)");
        assertEquals(new BigDecimal("-456.78"), result4);

        // Test scientific notation
        BigDecimal result5 = (BigDecimal) parseMethod.invoke(service, "1.23E+6");
        assertEquals(new BigDecimal("1230000"), result5);

        // Test numbers with K suffix
        BigDecimal result6 = (BigDecimal) parseMethod.invoke(service, "123K");
        assertEquals(new BigDecimal("123000"), result6);

        // Test numbers with M suffix
        BigDecimal result7 = (BigDecimal) parseMethod.invoke(service, "456M");
        assertEquals(new BigDecimal("456000000"), result7);

        // Test numbers with B suffix
        BigDecimal result8 = (BigDecimal) parseMethod.invoke(service, "789B");
        assertEquals(new BigDecimal("789000000000"), result8);

        // Test numbers with T suffix
        BigDecimal result9 = (BigDecimal) parseMethod.invoke(service, "1.5T");
        assertEquals(new BigDecimal("1500000000000"), result9);

        // Test invalid inputs
        BigDecimal result10 = (BigDecimal) parseMethod.invoke(service, "");
        assertNull(result10);

        BigDecimal result11 = (BigDecimal) parseMethod.invoke(service, "-");
        assertNull(result11);

        BigDecimal result12 = (BigDecimal) parseMethod.invoke(service, "--");
        assertNull(result12);

        BigDecimal result13 = (BigDecimal) parseMethod.invoke(service, null);
        assertNull(result13);
    }

    @Test
    @DisplayName("Should generate mock data with BigDecimal precision")
    void testMockDataBigDecimalGeneration() throws Exception {
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        
        // Use reflection to access the private generateMockFinancialData method
        Method generateMockMethod = FinancialDataScrapingService.class.getDeclaredMethod("generateMockFinancialData", String.class);
        generateMockMethod.setAccessible(true);

        // Test AAPL mock data generation
        Object aaplData = generateMockMethod.invoke(service, "AAPL");
        
        assertNotNull(aaplData);
        
        // Use reflection to verify the data
        Method getTickerMethod = aaplData.getClass().getMethod("getTicker");
        String ticker = (String) getTickerMethod.invoke(aaplData);
        assertEquals("AAPL", ticker);
        
        Method getRevenueMethod = aaplData.getClass().getMethod("getRevenue");
        Object revenue = getRevenueMethod.invoke(aaplData);
        assertNotNull(revenue);
        
        // Verify it's a list and not empty
        assertTrue(revenue instanceof java.util.List);
        java.util.List<?> revenueList = (java.util.List<?>) revenue;
        assertFalse(revenueList.isEmpty());
        
        // Verify first element is BigDecimal
        Object firstRevenue = revenueList.get(0);
        assertTrue(firstRevenue instanceof BigDecimal);
        
        BigDecimal revenueValue = (BigDecimal) firstRevenue;
        assertTrue(revenueValue.compareTo(BigDecimal.ZERO) > 0);
        
        // Verify AAPL has realistic revenue (> $300B)
        assertTrue(revenueValue.compareTo(new BigDecimal("300000000000")) > 0);
    }

    @Test
    @DisplayName("Should use BigDecimal arithmetic in CompanyFinancials helper class")
    void testCompanyFinancialsUseBigDecimal() throws Exception {
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        
        // Use reflection to access the private getRealisticCompanyData method
        Method getCompanyDataMethod = FinancialDataScrapingService.class.getDeclaredMethod("getRealisticCompanyData", String.class);
        getCompanyDataMethod.setAccessible(true);

        // Get the CompanyFinancials object for AAPL
        Object companyFinancials = getCompanyDataMethod.invoke(service, "AAPL");
        
        assertNotNull(companyFinancials);
        
        // Use reflection to verify BigDecimal fields
        Class<?> companyFinancialsClass = companyFinancials.getClass();
        
        // Verify baseRevenue is BigDecimal
        Object baseRevenue = companyFinancialsClass.getField("baseRevenue").get(companyFinancials);
        assertTrue(baseRevenue instanceof BigDecimal);
        assertTrue(((BigDecimal) baseRevenue).compareTo(BigDecimal.ZERO) > 0);
        
        // Verify operatingMargin is BigDecimal
        Object operatingMargin = companyFinancialsClass.getField("operatingMargin").get(companyFinancials);
        assertTrue(operatingMargin instanceof BigDecimal);
        assertTrue(((BigDecimal) operatingMargin).compareTo(BigDecimal.ZERO) > 0);
        assertTrue(((BigDecimal) operatingMargin).compareTo(BigDecimal.ONE) < 0); // Should be < 1 (percentage)
        
        // Verify growthRate is BigDecimal
        Object growthRate = companyFinancialsClass.getField("growthRate").get(companyFinancials);
        assertTrue(growthRate instanceof BigDecimal);
        assertTrue(((BigDecimal) growthRate).compareTo(BigDecimal.ONE) > 0); // Should be > 1 (growth multiplier)
    }
}