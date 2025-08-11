package com.dcf.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DCFMathUtil {

    /**
     * Calculate compound annual growth rate (CAGR)
     * @param beginningValue the starting value
     * @param endingValue the ending value
     * @param numberOfYears the number of years
     * @return CAGR as a decimal (e.g., 0.10 for 10%)
     */
    public double calculateCAGR(double beginningValue, double endingValue, int numberOfYears) {
        if (beginningValue <= 0 || endingValue <= 0 || numberOfYears <= 0) {
            throw new IllegalArgumentException("Values must be positive and years must be greater than 0");
        }
        
        return Math.pow(endingValue / beginningValue, 1.0 / numberOfYears) - 1.0;
    }

    /**
     * Calculate present value of a single cash flow
     * @param futureValue the future cash flow
     * @param discountRate the discount rate as decimal
     * @param periods the number of periods
     * @return present value
     */
    public double calculatePresentValue(double futureValue, double discountRate, int periods) {
        if (periods < 0) {
            throw new IllegalArgumentException("Periods cannot be negative");
        }
        
        if (periods == 0) {
            return futureValue;
        }
        
        return futureValue / Math.pow(1 + discountRate, periods);
    }

    /**
     * Calculate present value of a series of cash flows
     * @param cashFlows list of future cash flows
     * @param discountRate the discount rate as decimal
     * @return total present value
     */
    public double calculatePresentValueOfCashFlows(List<Double> cashFlows, double discountRate) {
        double totalPV = 0.0;
        
        for (int i = 0; i < cashFlows.size(); i++) {
            double cashFlow = cashFlows.get(i);
            int period = i + 1; // Periods start from 1
            totalPV += calculatePresentValue(cashFlow, discountRate, period);
        }
        
        return totalPV;
    }

    /**
     * Calculate terminal value using perpetual growth model
     * @param finalCashFlow the cash flow in the final projection year
     * @param terminalGrowthRate the perpetual growth rate as decimal
     * @param discountRate the discount rate as decimal
     * @return terminal value
     */
    public double calculateTerminalValue(double finalCashFlow, double terminalGrowthRate, double discountRate) {
        if (terminalGrowthRate >= discountRate) {
            throw new IllegalArgumentException("Terminal growth rate must be less than discount rate");
        }
        
        double nextYearCashFlow = finalCashFlow * (1 + terminalGrowthRate);
        return nextYearCashFlow / (discountRate - terminalGrowthRate);
    }

    /**
     * Project future cash flows with constant growth rate
     * @param baseCashFlow the base cash flow
     * @param growthRate the annual growth rate as decimal
     * @param years the number of years to project
     * @return list of projected cash flows
     */
    public List<Double> projectCashFlows(double baseCashFlow, double growthRate, int years) {
        if (years <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }
        
        List<Double> projectedCashFlows = new ArrayList<>();
        
        for (int i = 1; i <= years; i++) {
            double projectedCashFlow = baseCashFlow * Math.pow(1 + growthRate, i);
            projectedCashFlows.add(projectedCashFlow);
        }
        
        return projectedCashFlows;
    }

    /**
     * Project future cash flows with declining growth rate
     * @param baseCashFlow the base cash flow
     * @param initialGrowthRate the initial growth rate as decimal
     * @param finalGrowthRate the final growth rate as decimal
     * @param years the number of years to project
     * @return list of projected cash flows
     */
    public List<Double> projectCashFlowsWithDecliningGrowth(double baseCashFlow, 
                                                           double initialGrowthRate, 
                                                           double finalGrowthRate, 
                                                           int years) {
        if (years <= 0) {
            throw new IllegalArgumentException("Years must be positive");
        }
        
        List<Double> projectedCashFlows = new ArrayList<>();
        double currentCashFlow = baseCashFlow;
        
        for (int i = 1; i <= years; i++) {
            // Linear decline in growth rate
            double currentGrowthRate = initialGrowthRate - 
                ((initialGrowthRate - finalGrowthRate) * (i - 1) / (years - 1));
            
            currentCashFlow = currentCashFlow * (1 + currentGrowthRate);
            projectedCashFlows.add(currentCashFlow);
        }
        
        return projectedCashFlows;
    }

    /**
     * Calculate weighted average cost of capital (WACC)
     * @param costOfEquity the cost of equity as decimal
     * @param costOfDebt the cost of debt as decimal
     * @param marketValueOfEquity the market value of equity
     * @param marketValueOfDebt the market value of debt
     * @param taxRate the corporate tax rate as decimal
     * @return WACC as decimal
     */
    public double calculateWACC(double costOfEquity, double costOfDebt, 
                               double marketValueOfEquity, double marketValueOfDebt, 
                               double taxRate) {
        double totalValue = marketValueOfEquity + marketValueOfDebt;
        
        if (totalValue <= 0) {
            throw new IllegalArgumentException("Total market value must be positive");
        }
        
        double equityWeight = marketValueOfEquity / totalValue;
        double debtWeight = marketValueOfDebt / totalValue;
        
        return (equityWeight * costOfEquity) + (debtWeight * costOfDebt * (1 - taxRate));
    }

    /**
     * Calculate enterprise value
     * @param presentValueOfCashFlows the PV of projected cash flows
     * @param terminalValue the terminal value
     * @param discountRate the discount rate as decimal
     * @param projectionYears the number of projection years
     * @return enterprise value
     */
    public double calculateEnterpriseValue(double presentValueOfCashFlows, double terminalValue, 
                                         double discountRate, int projectionYears) {
        double presentValueOfTerminalValue = calculatePresentValue(terminalValue, discountRate, projectionYears);
        return presentValueOfCashFlows + presentValueOfTerminalValue;
    }

    /**
     * Calculate equity value from enterprise value
     * @param enterpriseValue the enterprise value
     * @param totalDebt the total debt
     * @param cash the cash and cash equivalents
     * @return equity value
     */
    public double calculateEquityValue(double enterpriseValue, double totalDebt, double cash) {
        return enterpriseValue - totalDebt + cash;
    }

    /**
     * Calculate fair value per share
     * @param equityValue the total equity value
     * @param sharesOutstanding the number of shares outstanding
     * @return fair value per share
     */
    public double calculateFairValuePerShare(double equityValue, double sharesOutstanding) {
        if (sharesOutstanding <= 0) {
            throw new IllegalArgumentException("Shares outstanding must be positive");
        }
        
        return equityValue / sharesOutstanding;
    }

    /**
     * Calculate upside/downside percentage
     * @param fairValue the calculated fair value
     * @param currentPrice the current market price
     * @return upside/downside as percentage (positive = upside, negative = downside)
     */
    public double calculateUpsideDownside(double fairValue, double currentPrice) {
        if (currentPrice <= 0) {
            throw new IllegalArgumentException("Current price must be positive");
        }
        
        return ((fairValue - currentPrice) / currentPrice) * 100;
    }

    /**
     * Calculate margin of safety
     * @param fairValue the calculated fair value
     * @param currentPrice the current market price
     * @return margin of safety as percentage
     */
    public double calculateMarginOfSafety(double fairValue, double currentPrice) {
        if (fairValue <= 0) {
            throw new IllegalArgumentException("Fair value must be positive");
        }
        
        return ((fairValue - currentPrice) / fairValue) * 100;
    }

    /**
     * Calculate annualized return
     * @param beginningValue the starting value
     * @param endingValue the ending value
     * @param years the number of years
     * @return annualized return as decimal
     */
    public double calculateAnnualizedReturn(double beginningValue, double endingValue, double years) {
        if (beginningValue <= 0 || endingValue <= 0 || years <= 0) {
            throw new IllegalArgumentException("All values must be positive");
        }
        
        return Math.pow(endingValue / beginningValue, 1.0 / years) - 1.0;
    }

    /**
     * Validate DCF calculation inputs
     * @param discountRate the discount rate
     * @param growthRate the growth rate
     * @param terminalGrowthRate the terminal growth rate
     * @throws IllegalArgumentException if inputs are invalid
     */
    public void validateDCFInputs(double discountRate, double growthRate, double terminalGrowthRate) {
        if (discountRate <= 0) {
            throw new IllegalArgumentException("Discount rate must be positive");
        }
        
        if (terminalGrowthRate >= discountRate) {
            throw new IllegalArgumentException("Terminal growth rate must be less than discount rate");
        }
        
        if (Math.abs(growthRate) > 10.0) { // 1000% seems too high for validation
            throw new IllegalArgumentException("Growth rate seems unrealistic");
        }
    }
}