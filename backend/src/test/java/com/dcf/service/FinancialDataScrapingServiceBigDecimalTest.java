package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.util.FinancialDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FinancialDataScrapingService BigDecimal functionality
 * These tests focus on BigDecimal parsing and mock data generation without using Mockito
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FinancialDataScrapingServiceBigDecimalTest {

    private FinancialDataScrapingService financialDataScrapingService;

    @BeforeEach
    void setUp() {
        // Create a real instance for testing BigDecimal functionality
        financialDataScrapingService = new FinancialDataScrapingService();
    }

    @Test
    @DisplayName("Should parse BigDecimal values correctly from various text formats")
    void testBigDecimalParsing() throws Exception {
        // Use reflection to access the private parseFinancialValue method
        Method parseMethod = FinancialDataScrapingService.class.getDeclaredMethod("parseFinancialValue", String.class);
        parseMethod.setAccessible(true);

        // Test regular decimal numbers
        BigDecimal result1 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "123.45");
        assertEquals(new BigDecimal("123.45"), result1);

        // Test numbers with commas
        BigDecimal result2 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "1,234.56");
        assertEquals(new BigDecimal("1234.56"), result2);

        // Test negative numbers
        BigDecimal result3 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "-789.12");
        assertEquals(new BigDecimal("-789.12"), result3);

        // Test numbers in parentheses (negative)
        BigDecimal result4 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "(456.78)");
        assertEquals(new BigDecimal("-456.78"), result4);

        // Test scientific notation
        BigDecimal result5 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "1.23E+6");
        assertEquals(new BigDecimal("1230000"), result5);

        // Test numbers with K suffix
        BigDecimal result6 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "123K");
        assertEquals(new BigDecimal("123000"), result6);

        // Test numbers with M suffix
        BigDecimal result7 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "456M");
        assertEquals(new BigDecimal("456000000"), result7);

        // Test numbers with B suffix
        BigDecimal result8 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "789B");
        assertEquals(new BigDecimal("789000000000"), result8);

        // Test numbers with T suffix
        BigDecimal result9 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "1.5T");
        assertEquals(new BigDecimal("1500000000000"), result9);

        // Test invalid inputs
        BigDecimal result10 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "");
        assertNull(result10);

        BigDecimal result11 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "-");
        assertNull(result11);

        BigDecimal result12 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, "--");
        assertNull(result12);

        BigDecimal result13 = (BigDecimal) parseMethod.invoke(financialDataScrapingService, null);
        assertNull(result13);
    }

    @Test
    @DisplayName("Should generate mock data with BigDecimal precision")
    void testMockDataBigDecimalGeneration() throws Exception {
        // Use reflection to access the private generateMockFinancialData method
        Method generateMockMethod = FinancialDataScrapingService.class.getDeclaredMethod("generateMockFinancialData", String.class);
        generateMockMethod.setAccessible(true);

        // Test AAPL mock data generation
        FinancialData aaplData = (FinancialData) generateMockMethod.invoke(financialDataScrapingService, "AAPL");
        
        assertNotNull(aaplData);
        assertEquals("AAPL", aaplData.getTicker());
        
        // Verify all financial data lists are populated with BigDecimal values
        assertFalse(aaplData.getRevenue().isEmpty());
        assertFalse(aaplData.getOperatingIncome().isEmpty());
        assertFalse(aaplData.getNetProfit().isEmpty());
        assertFalse(aaplData.getOperatingCashFlow().isEmpty());
        assertFalse(aaplData.getFreeCashFlow().isEmpty());
        assertFalse(aaplData.getEps().isEmpty());
        assertFalse(aaplData.getTotalDebt().isEmpty());
        assertFalse(aaplData.getOrdinarySharesNumber().isEmpty());

        // Verify BigDecimal precision is maintained
        BigDecimal revenue = aaplData.getRevenue().get(0);
        assertTrue(revenue instanceof BigDecimal);
        assertTrue(revenue.compareTo(BigDecimal.ZERO) > 0);

        // Verify AAPL has realistic revenue (> $300B)
        assertTrue(revenue.compareTo(new BigDecimal("300000000000")) > 0);

        // Verify EPS calculation precision (should have reasonable scale)
        BigDecimal eps = aaplData.getEps().get(0);
        assertTrue(eps instanceof BigDecimal);
        assertTrue(eps.scale() <= 6); // Should not exceed 6 decimal places
        assertTrue(eps.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should generate consistent BigDecimal arithmetic across years")
    void testBigDecimalArithmeticConsistency() throws Exception {
        // Use reflection to access the private generateMockFinancialData method
        Method generateMockMethod = FinancialDataScrapingService.class.getDeclaredMethod("generateMockFinancialData", String.class);
        generateMockMethod.setAccessible(true);

        // Test TSLA mock data generation
        FinancialData tslaData = (FinancialData) generateMockMethod.invoke(financialDataScrapingService, "TSLA");
        
        assertNotNull(tslaData);
        assertEquals("TSLA", tslaData.getTicker());
        
        // Verify growth calculations are consistent across years (4 years of data)
        assertTrue(tslaData.getRevenue().size() >= 4);
        
        // Revenue should grow year over year (most recent first, so should decrease in list order)
        BigDecimal currentYearRevenue = tslaData.getRevenue().get(0);
        BigDecimal previousYearRevenue = tslaData.getRevenue().get(1);
        assertTrue(currentYearRevenue.compareTo(previousYearRevenue) > 0);

        // Verify operating income is calculated as revenue * operating margin
        BigDecimal revenue = tslaData.getRevenue().get(0);
        BigDecimal operatingIncome = tslaData.getOperatingIncome().get(0);
        
        // Operating income should be less than revenue (positive margin)
        assertTrue(operatingIncome.compareTo(revenue) < 0);
        assertTrue(operatingIncome.compareTo(BigDecimal.ZERO) > 0);

        // Verify EPS calculation: (revenue * netMargin) / sharesOutstanding
        BigDecimal netProfit = tslaData.getNetProfit().get(0);
        BigDecimal sharesOutstanding = tslaData.getOrdinarySharesNumber().get(0);
        BigDecimal eps = tslaData.getEps().get(0);
        
        // Calculate expected EPS
        BigDecimal expectedEps = netProfit.divide(sharesOutstanding, 6, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expectedEps.compareTo(eps)); // Use compareTo for BigDecimal equality
    }

    @Test
    @DisplayName("Should handle different company data with BigDecimal consistency")
    void testBigDecimalConsistencyAcrossCompanies() throws Exception {
        // Use reflection to access the private generateMockFinancialData method
        Method generateMockMethod = FinancialDataScrapingService.class.getDeclaredMethod("generateMockFinancialData", String.class);
        generateMockMethod.setAccessible(true);

        String[] tickers = {"AAPL", "GOOGL", "MSFT", "AMZN", "META", "NVDA", "UNKNOWN"};
        
        for (String ticker : tickers) {
            FinancialData data = (FinancialData) generateMockMethod.invoke(financialDataScrapingService, ticker);
            
            assertNotNull(data, "Financial data should not be null for " + ticker);
            assertEquals(ticker, data.getTicker());
            
            // Verify all BigDecimal fields are properly set
            assertFalse(data.getRevenue().isEmpty(), "Revenue should not be empty for " + ticker);
            assertFalse(data.getEps().isEmpty(), "EPS should not be empty for " + ticker);
            
            // Verify BigDecimal values are reasonable
            BigDecimal revenue = data.getRevenue().get(0);
            assertTrue(revenue.compareTo(new BigDecimal("1000000000")) > 0, 
                "Revenue should be > $1B for " + ticker); // All test companies have > $1B revenue
            
            // Verify all values are positive BigDecimal instances
            assertTrue(revenue instanceof BigDecimal);
            assertTrue(data.getEps().get(0) instanceof BigDecimal);
            assertTrue(data.getFreeCashFlow().get(0) instanceof BigDecimal);
            
            // Verify precision is maintained
            assertTrue(revenue.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(data.getEps().get(0).compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("Should use BigDecimal arithmetic in CompanyFinancials helper class")
    void testCompanyFinancialsUseBigDecimal() throws Exception {
        // Use reflection to access the private getRealisticCompanyData method
        Method getCompanyDataMethod = FinancialDataScrapingService.class.getDeclaredMethod("getRealisticCompanyData", String.class);
        getCompanyDataMethod.setAccessible(true);

        // Get the CompanyFinancials object for AAPL
        Object companyFinancials = getCompanyDataMethod.invoke(financialDataScrapingService, "AAPL");
        
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