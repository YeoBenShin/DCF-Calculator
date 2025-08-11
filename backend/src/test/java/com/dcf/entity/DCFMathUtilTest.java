package com.dcf.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DCFMathUtilTest {

    private DCFMathUtil dcfMathUtil;

    @BeforeEach
    void setUp() {
        dcfMathUtil = new DCFMathUtil();
    }

    @Test
    @DisplayName("Should calculate CAGR correctly")
    void testCalculateCAGR() {
        // Test case: $100 to $121 over 2 years = 10% CAGR
        double cagr = dcfMathUtil.calculateCAGR(100.0, 121.0, 2);
        assertEquals(0.10, cagr, 0.001);
    }

    @Test
    @DisplayName("Should throw exception for invalid CAGR inputs")
    void testCalculateCAGRInvalidInputs() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateCAGR(-100.0, 121.0, 2));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateCAGR(100.0, -121.0, 2));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateCAGR(100.0, 121.0, 0));
    }

    @Test
    @DisplayName("Should calculate present value correctly")
    void testCalculatePresentValue() {
        // Test case: $110 in 1 year at 10% discount rate = $100 PV
        double pv = dcfMathUtil.calculatePresentValue(110.0, 0.10, 1);
        assertEquals(100.0, pv, 0.01);
        
        // Test case: $121 in 2 years at 10% discount rate = $100 PV
        pv = dcfMathUtil.calculatePresentValue(121.0, 0.10, 2);
        assertEquals(100.0, pv, 0.01);
    }

    @Test
    @DisplayName("Should handle zero periods in present value calculation")
    void testCalculatePresentValueZeroPeriods() {
        double pv = dcfMathUtil.calculatePresentValue(100.0, 0.10, 0);
        assertEquals(100.0, pv, 0.01);
    }

    @Test
    @DisplayName("Should throw exception for negative periods")
    void testCalculatePresentValueNegativePeriods() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculatePresentValue(100.0, 0.10, -1));
    }

    @Test
    @DisplayName("Should calculate present value of cash flows correctly")
    void testCalculatePresentValueOfCashFlows() {
        List<Double> cashFlows = Arrays.asList(100.0, 110.0, 121.0);
        double totalPV = dcfMathUtil.calculatePresentValueOfCashFlows(cashFlows, 0.10);
        
        // Expected: 100/1.1 + 110/1.21 + 121/1.331 = 90.91 + 90.91 + 90.91 = 272.73
        assertEquals(272.73, totalPV, 0.01);
    }

    @Test
    @DisplayName("Should calculate terminal value correctly")
    void testCalculateTerminalValue() {
        // Test case: $100 final FCF, 3% terminal growth, 10% discount rate
        double terminalValue = dcfMathUtil.calculateTerminalValue(100.0, 0.03, 0.10);
        
        // Expected: (100 * 1.03) / (0.10 - 0.03) = 103 / 0.07 = 1471.43
        assertEquals(1471.43, terminalValue, 0.01);
    }

    @Test
    @DisplayName("Should throw exception when terminal growth >= discount rate")
    void testCalculateTerminalValueInvalidRates() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateTerminalValue(100.0, 0.10, 0.10));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateTerminalValue(100.0, 0.15, 0.10));
    }

    @Test
    @DisplayName("Should project cash flows with constant growth correctly")
    void testProjectCashFlows() {
        List<Double> projectedCashFlows = dcfMathUtil.projectCashFlows(100.0, 0.10, 3);
        
        assertEquals(3, projectedCashFlows.size());
        assertEquals(110.0, projectedCashFlows.get(0), 0.01); // Year 1: 100 * 1.1
        assertEquals(121.0, projectedCashFlows.get(1), 0.01); // Year 2: 100 * 1.1^2
        assertEquals(133.1, projectedCashFlows.get(2), 0.01); // Year 3: 100 * 1.1^3
    }

    @Test
    @DisplayName("Should throw exception for invalid projection years")
    void testProjectCashFlowsInvalidYears() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.projectCashFlows(100.0, 0.10, 0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.projectCashFlows(100.0, 0.10, -1));
    }

    @Test
    @DisplayName("Should project cash flows with declining growth correctly")
    void testProjectCashFlowsWithDecliningGrowth() {
        List<Double> projectedCashFlows = dcfMathUtil.projectCashFlowsWithDecliningGrowth(
            100.0, 0.20, 0.10, 3);
        
        assertEquals(3, projectedCashFlows.size());
        
        // Year 1: 100 * 1.20 = 120 (20% growth)
        assertEquals(120.0, projectedCashFlows.get(0), 0.01);
        
        // Year 2: 120 * 1.15 = 138 (15% growth - midpoint)
        assertEquals(138.0, projectedCashFlows.get(1), 0.01);
        
        // Year 3: 138 * 1.10 = 151.8 (10% growth)
        assertEquals(151.8, projectedCashFlows.get(2), 0.01);
    }

    @Test
    @DisplayName("Should calculate WACC correctly")
    void testCalculateWACC() {
        double wacc = dcfMathUtil.calculateWACC(
            0.12, // 12% cost of equity
            0.06, // 6% cost of debt
            800.0, // $800M equity
            200.0, // $200M debt
            0.25  // 25% tax rate
        );
        
        // Expected: (800/1000 * 0.12) + (200/1000 * 0.06 * 0.75) = 0.096 + 0.009 = 0.105 = 10.5%
        assertEquals(0.105, wacc, 0.001);
    }

    @Test
    @DisplayName("Should throw exception for invalid WACC inputs")
    void testCalculateWACCInvalidInputs() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateWACC(0.12, 0.06, -800.0, 200.0, 0.25));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateWACC(0.12, 0.06, 0.0, 0.0, 0.25));
    }

    @Test
    @DisplayName("Should calculate enterprise value correctly")
    void testCalculateEnterpriseValue() {
        double enterpriseValue = dcfMathUtil.calculateEnterpriseValue(
            500.0, // PV of cash flows
            1000.0, // Terminal value
            0.10, // Discount rate
            5 // Projection years
        );
        
        // Expected: 500 + (1000 / 1.1^5) = 500 + 620.92 = 1120.92
        assertEquals(1120.92, enterpriseValue, 0.01);
    }

    @Test
    @DisplayName("Should calculate equity value correctly")
    void testCalculateEquityValue() {
        double equityValue = dcfMathUtil.calculateEquityValue(
            1000.0, // Enterprise value
            200.0,  // Total debt
            50.0    // Cash
        );
        
        // Expected: 1000 - 200 + 50 = 850
        assertEquals(850.0, equityValue, 0.01);
    }

    @Test
    @DisplayName("Should calculate fair value per share correctly")
    void testCalculateFairValuePerShare() {
        double fairValuePerShare = dcfMathUtil.calculateFairValuePerShare(850.0, 10.0);
        
        // Expected: 850 / 10 = 85
        assertEquals(85.0, fairValuePerShare, 0.01);
    }

    @Test
    @DisplayName("Should throw exception for invalid shares outstanding")
    void testCalculateFairValuePerShareInvalidShares() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateFairValuePerShare(850.0, 0.0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateFairValuePerShare(850.0, -10.0));
    }

    @Test
    @DisplayName("Should calculate upside/downside correctly")
    void testCalculateUpsideDownside() {
        // Upside case: fair value > current price
        double upside = dcfMathUtil.calculateUpsideDownside(110.0, 100.0);
        assertEquals(10.0, upside, 0.01); // 10% upside
        
        // Downside case: fair value < current price
        double downside = dcfMathUtil.calculateUpsideDownside(90.0, 100.0);
        assertEquals(-10.0, downside, 0.01); // 10% downside
    }

    @Test
    @DisplayName("Should throw exception for invalid current price")
    void testCalculateUpsideDownsideInvalidPrice() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateUpsideDownside(110.0, 0.0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateUpsideDownside(110.0, -100.0));
    }

    @Test
    @DisplayName("Should calculate margin of safety correctly")
    void testCalculateMarginOfSafety() {
        double marginOfSafety = dcfMathUtil.calculateMarginOfSafety(120.0, 100.0);
        
        // Expected: (120 - 100) / 120 * 100 = 16.67%
        assertEquals(16.67, marginOfSafety, 0.01);
    }

    @Test
    @DisplayName("Should throw exception for invalid fair value in margin of safety")
    void testCalculateMarginOfSafetyInvalidFairValue() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateMarginOfSafety(0.0, 100.0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateMarginOfSafety(-120.0, 100.0));
    }

    @Test
    @DisplayName("Should calculate annualized return correctly")
    void testCalculateAnnualizedReturn() {
        double annualizedReturn = dcfMathUtil.calculateAnnualizedReturn(100.0, 121.0, 2.0);
        
        // Expected: (121/100)^(1/2) - 1 = 0.10 = 10%
        assertEquals(0.10, annualizedReturn, 0.001);
    }

    @Test
    @DisplayName("Should throw exception for invalid annualized return inputs")
    void testCalculateAnnualizedReturnInvalidInputs() {
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateAnnualizedReturn(-100.0, 121.0, 2.0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateAnnualizedReturn(100.0, -121.0, 2.0));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.calculateAnnualizedReturn(100.0, 121.0, 0.0));
    }

    @Test
    @DisplayName("Should validate DCF inputs correctly")
    void testValidateDCFInputs() {
        // Valid inputs should not throw exception
        assertDoesNotThrow(() -> dcfMathUtil.validateDCFInputs(0.10, 0.15, 0.03));
        
        // Invalid discount rate
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.validateDCFInputs(0.0, 0.15, 0.03));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.validateDCFInputs(-0.10, 0.15, 0.03));
        
        // Terminal growth rate >= discount rate
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.validateDCFInputs(0.10, 0.15, 0.10));
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.validateDCFInputs(0.10, 0.15, 0.15));
        
        // Unrealistic growth rate
        assertThrows(IllegalArgumentException.class, 
            () -> dcfMathUtil.validateDCFInputs(0.10, 15.0, 0.03)); // 1500% growth
    }

    @Test
    @DisplayName("Should handle edge cases in calculations")
    void testEdgeCases() {
        // Zero growth rate
        List<Double> zeroGrowthCashFlows = dcfMathUtil.projectCashFlows(100.0, 0.0, 3);
        assertEquals(100.0, zeroGrowthCashFlows.get(0), 0.01);
        assertEquals(100.0, zeroGrowthCashFlows.get(1), 0.01);
        assertEquals(100.0, zeroGrowthCashFlows.get(2), 0.01);
        
        // Negative growth rate
        List<Double> negativeGrowthCashFlows = dcfMathUtil.projectCashFlows(100.0, -0.10, 2);
        assertEquals(90.0, negativeGrowthCashFlows.get(0), 0.01);
        assertEquals(81.0, negativeGrowthCashFlows.get(1), 0.01);
    }
}