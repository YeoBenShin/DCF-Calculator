package com.dcf.service;

import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Simple test runner for BigDecimal parsing functionality
 */
public class BigDecimalTestRunner {
    
    public static void main(String[] args) {
        try {
            testBigDecimalParsing();
            testMockDataGeneration();
            testCompanyFinancials();
            System.out.println("All BigDecimal tests passed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testBigDecimalParsing() throws Exception {
        System.out.println("Testing BigDecimal parsing...");
        
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        Method parseMethod = FinancialDataScrapingService.class.getDeclaredMethod("parseFinancialValue", String.class);
        parseMethod.setAccessible(true);

        // Test regular decimal numbers
        BigDecimal result1 = (BigDecimal) parseMethod.invoke(service, "123.45");
        assert result1.equals(new BigDecimal("123.45")) : "Regular decimal parsing failed";

        // Test numbers with commas
        BigDecimal result2 = (BigDecimal) parseMethod.invoke(service, "1,234.56");
        assert result2.equals(new BigDecimal("1234.56")) : "Comma parsing failed";

        // Test negative numbers
        BigDecimal result3 = (BigDecimal) parseMethod.invoke(service, "-789.12");
        assert result3.equals(new BigDecimal("-789.12")) : "Negative number parsing failed";

        // Test numbers in parentheses (negative)
        BigDecimal result4 = (BigDecimal) parseMethod.invoke(service, "(456.78)");
        assert result4.equals(new BigDecimal("-456.78")) : "Parentheses negative parsing failed";

        // Test scientific notation
        BigDecimal result5 = (BigDecimal) parseMethod.invoke(service, "1.23E+6");
        assert result5.equals(new BigDecimal("1230000")) : "Scientific notation parsing failed";

        // Test numbers with K suffix
        BigDecimal result6 = (BigDecimal) parseMethod.invoke(service, "123K");
        assert result6.equals(new BigDecimal("123000")) : "K suffix parsing failed";

        // Test numbers with M suffix
        BigDecimal result7 = (BigDecimal) parseMethod.invoke(service, "456M");
        assert result7.equals(new BigDecimal("456000000")) : "M suffix parsing failed";

        // Test numbers with B suffix
        BigDecimal result8 = (BigDecimal) parseMethod.invoke(service, "789B");
        assert result8.equals(new BigDecimal("789000000000")) : "B suffix parsing failed";

        // Test invalid inputs
        BigDecimal result9 = (BigDecimal) parseMethod.invoke(service, "");
        assert result9 == null : "Empty string should return null";

        BigDecimal result10 = (BigDecimal) parseMethod.invoke(service, "-");
        assert result10 == null : "Dash should return null";

        System.out.println("✓ BigDecimal parsing tests passed");
    }
    
    private static void testMockDataGeneration() throws Exception {
        System.out.println("Testing mock data generation...");
        
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        Method generateMockMethod = FinancialDataScrapingService.class.getDeclaredMethod("generateMockFinancialData", String.class);
        generateMockMethod.setAccessible(true);

        // Test AAPL mock data generation
        Object aaplData = generateMockMethod.invoke(service, "AAPL");
        assert aaplData != null : "AAPL data should not be null";
        
        Method getTickerMethod = aaplData.getClass().getMethod("getTicker");
        String ticker = (String) getTickerMethod.invoke(aaplData);
        assert "AAPL".equals(ticker) : "Ticker should be AAPL";
        
        Method getRevenueMethod = aaplData.getClass().getMethod("getRevenue");
        Object revenue = getRevenueMethod.invoke(aaplData);
        assert revenue != null : "Revenue should not be null";
        
        java.util.List<?> revenueList = (java.util.List<?>) revenue;
        assert !revenueList.isEmpty() : "Revenue list should not be empty";
        
        Object firstRevenue = revenueList.get(0);
        assert firstRevenue instanceof BigDecimal : "Revenue should be BigDecimal";
        
        BigDecimal revenueValue = (BigDecimal) firstRevenue;
        assert revenueValue.compareTo(BigDecimal.ZERO) > 0 : "Revenue should be positive";
        assert revenueValue.compareTo(new BigDecimal("300000000000")) > 0 : "AAPL revenue should be > $300B";
        
        System.out.println("✓ Mock data generation tests passed");
    }
    
    private static void testCompanyFinancials() throws Exception {
        System.out.println("Testing CompanyFinancials BigDecimal usage...");
        
        FinancialDataScrapingService service = new FinancialDataScrapingService();
        Method getCompanyDataMethod = FinancialDataScrapingService.class.getDeclaredMethod("getRealisticCompanyData", String.class);
        getCompanyDataMethod.setAccessible(true);

        Object companyFinancials = getCompanyDataMethod.invoke(service, "AAPL");
        assert companyFinancials != null : "CompanyFinancials should not be null";
        
        Class<?> companyFinancialsClass = companyFinancials.getClass();
        
        // Verify baseRevenue is BigDecimal
        Object baseRevenue = companyFinancialsClass.getDeclaredField("baseRevenue").get(companyFinancials);
        assert baseRevenue instanceof BigDecimal : "baseRevenue should be BigDecimal";
        assert ((BigDecimal) baseRevenue).compareTo(BigDecimal.ZERO) > 0 : "baseRevenue should be positive";
        
        // Verify operatingMargin is BigDecimal
        Object operatingMargin = companyFinancialsClass.getDeclaredField("operatingMargin").get(companyFinancials);
        assert operatingMargin instanceof BigDecimal : "operatingMargin should be BigDecimal";
        assert ((BigDecimal) operatingMargin).compareTo(BigDecimal.ZERO) > 0 : "operatingMargin should be positive";
        assert ((BigDecimal) operatingMargin).compareTo(BigDecimal.ONE) < 0 : "operatingMargin should be < 1";
        
        // Verify growthRate is BigDecimal
        Object growthRate = companyFinancialsClass.getDeclaredField("growthRate").get(companyFinancials);
        assert growthRate instanceof BigDecimal : "growthRate should be BigDecimal";
        assert ((BigDecimal) growthRate).compareTo(BigDecimal.ONE) > 0 : "growthRate should be > 1";
        
        System.out.println("✓ CompanyFinancials BigDecimal tests passed");
    }
}