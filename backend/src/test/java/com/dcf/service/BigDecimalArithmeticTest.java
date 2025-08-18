package com.dcf.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify BigDecimal arithmetic operations work correctly
 */
public class BigDecimalArithmeticTest {

    @Test
    public void testProjectFreeCashFlowsLogic() {
        // Test the logic from projectFreeCashFlows method
        BigDecimal baseFCF = new BigDecimal("1000000"); // $1M
        BigDecimal growthRate = new BigDecimal("0.05"); // 5%
        int years = 5;
        
        List<BigDecimal> projectedFCFs = new ArrayList<>();
        BigDecimal growthMultiplier = BigDecimal.ONE.add(growthRate);
        
        for (int i = 1; i <= years; i++) {
            BigDecimal projectedFCF = baseFCF.multiply(growthMultiplier.pow(i));
            projectedFCFs.add(projectedFCF);
        }
        
        // Verify results
        assertEquals(5, projectedFCFs.size());
        assertEquals(new BigDecimal("1050000.00"), projectedFCFs.get(0).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1102500.00"), projectedFCFs.get(1).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1157625.00"), projectedFCFs.get(2).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1215506.25"), projectedFCFs.get(3).setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("1276281.56"), projectedFCFs.get(4).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void testCalculatePresentValueLogic() {
        // Test the logic from calculatePresentValue method
        List<BigDecimal> cashFlows = List.of(
            new BigDecimal("1050000"),
            new BigDecimal("1102500"),
            new BigDecimal("1157625")
        );
        BigDecimal discountRate = new BigDecimal("0.10"); // 10%
        
        BigDecimal presentValue = BigDecimal.ZERO;
        BigDecimal discountMultiplier = BigDecimal.ONE.add(discountRate);
        
        for (int i = 0; i < cashFlows.size(); i++) {
            BigDecimal cashFlow = cashFlows.get(i);
            int year = i + 1;
            BigDecimal pv = cashFlow.divide(discountMultiplier.pow(year), 10, RoundingMode.HALF_UP);
            presentValue = presentValue.add(pv);
        }
        
        // Verify the present value calculation
        assertTrue(presentValue.compareTo(BigDecimal.ZERO) > 0);
        // Expected: ~2,735,443.28 (actual calculated value)
        assertEquals(new BigDecimal("2735443.28"), presentValue.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void testTerminalValueCalculation() {
        // Test terminal value calculation logic
        BigDecimal terminalFCF = new BigDecimal("1276281.56");
        BigDecimal terminalGrowthRate = new BigDecimal("0.03"); // 3%
        BigDecimal discountRate = new BigDecimal("0.10"); // 10%
        
        BigDecimal terminalGrowthMultiplier = BigDecimal.ONE.add(terminalGrowthRate);
        BigDecimal terminalFCFGrown = terminalFCF.multiply(terminalGrowthMultiplier);
        BigDecimal terminalValue = terminalFCFGrown.divide(discountRate.subtract(terminalGrowthRate), 10, RoundingMode.HALF_UP);
        
        // Verify terminal value calculation
        assertTrue(terminalValue.compareTo(BigDecimal.ZERO) > 0);
        // Expected: ~18,779,571.53 (actual calculated value)
        assertEquals(new BigDecimal("18779571.53"), terminalValue.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    public void testValuationStatusLogic() {
        // Test valuation status determination logic
        BigDecimal fairValue = new BigDecimal("150.00");
        BigDecimal currentPrice = new BigDecimal("145.00");
        
        BigDecimal tolerance = new BigDecimal("0.05"); // 5% tolerance
        BigDecimal difference = fairValue.subtract(currentPrice).abs();
        BigDecimal toleranceAmount = currentPrice.multiply(tolerance).setScale(10, RoundingMode.HALF_UP);
        
        String status;
        if (difference.compareTo(toleranceAmount) <= 0) {
            status = "Fair Value";
        } else if (fairValue.compareTo(currentPrice) > 0) {
            status = "Undervalued";
        } else {
            status = "Overvalued";
        }
        
        // The difference is 5.00, tolerance is 145 * 0.05 = 7.25, so it should be "Fair Value"
        assertEquals("Fair Value", status);
        
        // Test undervalued case with larger difference
        BigDecimal fairValue2 = new BigDecimal("160.00");
        BigDecimal difference2 = fairValue2.subtract(currentPrice).abs();
        
        String status2;
        if (difference2.compareTo(toleranceAmount) <= 0) {
            status2 = "Fair Value";
        } else if (fairValue2.compareTo(currentPrice) > 0) {
            status2 = "Undervalued";
        } else {
            status2 = "Overvalued";
        }
        
        assertEquals("Undervalued", status2);
    }

    @Test
    public void testBigDecimalRoundingModes() {
        // Test that rounding modes work correctly
        BigDecimal numerator = new BigDecimal("10");
        BigDecimal denominator = new BigDecimal("3");
        
        BigDecimal result = numerator.divide(denominator, 6, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.333333"), result);
        
        BigDecimal result2 = numerator.divide(denominator, 2, RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("3.33"), result2);
    }

    @Test
    public void testStatisticsCalculation() {
        // Test statistics calculation with BigDecimal
        long totalCalculations = 100;
        long undervaluedCount = 35;
        
        BigDecimal undervaluedPercentage = new BigDecimal(undervaluedCount)
            .divide(new BigDecimal(totalCalculations), 6, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        assertEquals(new BigDecimal("35.000000"), undervaluedPercentage);
    }
}