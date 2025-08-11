package com.dcf.util;

import com.dcf.entity.FinancialData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
public class FinancialDataUtil {

    /**
     * Validate ticker symbol format
     * @param ticker the ticker symbol to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            return false;
        }
        
        String cleanTicker = ticker.trim().toUpperCase();
        
        // Ticker should be 1-10 characters, alphanumeric only
        return cleanTicker.matches("^[A-Z0-9]{1,10}$");
    }

    /**
     * Validate numerical financial data
     * @param value the financial value to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidFinancialValue(Double value) {
        if (value == null) {
            return false;
        }
        
        // Check for reasonable bounds (not infinite, not NaN)
        return Double.isFinite(value) && Math.abs(value) < 1e15;
    }

    /**
     * Validate a list of financial values
     * @param values the list of values to validate
     * @return true if all values are valid, false otherwise
     */
    public boolean isValidFinancialValueList(List<Double> values) {
        if (values == null) {
            return true; // Empty list is valid
        }
        
        return values.stream().allMatch(this::isValidFinancialValue);
    }

    /**
     * Clean and normalize ticker symbol
     * @param ticker the ticker to normalize
     * @return normalized ticker or null if invalid
     */
    public String normalizeTicker(String ticker) {
        if (!isValidTicker(ticker)) {
            return null;
        }
        return ticker.trim().toUpperCase();
    }

    /**
     * Validate that financial data has minimum required fields
     * @param financialData the financial data to validate
     * @return true if has minimum required data, false otherwise
     */
    public boolean hasMinimumRequiredData(FinancialData financialData) {
        if (financialData == null) {
            return false;
        }
        
        // Must have at least revenue, free cash flow, and EPS data
        return !financialData.getRevenue().isEmpty() &&
               !financialData.getFreeCashFlow().isEmpty() &&
               !financialData.getEps().isEmpty();
    }

    /**
     * Validate data consistency (all lists should have same size)
     * @param financialData the financial data to validate
     * @return true if data is consistent, false otherwise
     */
    public boolean isDataConsistent(FinancialData financialData) {
        if (financialData == null) {
            return false;
        }
        
        List<Integer> sizes = new ArrayList<>();
        
        if (!financialData.getRevenue().isEmpty()) {
            sizes.add(financialData.getRevenue().size());
        }
        if (!financialData.getOperatingExpense().isEmpty()) {
            sizes.add(financialData.getOperatingExpense().size());
        }
        if (!financialData.getOperatingIncome().isEmpty()) {
            sizes.add(financialData.getOperatingIncome().size());
        }
        if (!financialData.getOperatingCashFlow().isEmpty()) {
            sizes.add(financialData.getOperatingCashFlow().size());
        }
        if (!financialData.getNetProfit().isEmpty()) {
            sizes.add(financialData.getNetProfit().size());
        }
        if (!financialData.getCapitalExpenditure().isEmpty()) {
            sizes.add(financialData.getCapitalExpenditure().size());
        }
        if (!financialData.getFreeCashFlow().isEmpty()) {
            sizes.add(financialData.getFreeCashFlow().size());
        }
        if (!financialData.getEps().isEmpty()) {
            sizes.add(financialData.getEps().size());
        }
        if (!financialData.getTotalDebt().isEmpty()) {
            sizes.add(financialData.getTotalDebt().size());
        }
        if (!financialData.getOrdinarySharesNumber().isEmpty()) {
            sizes.add(financialData.getOrdinarySharesNumber().size());
        }
        
        // All non-empty lists should have the same size
        return sizes.stream().distinct().count() <= 1;
    }

    /**
     * Get validation error message for financial data
     * @param financialData the financial data to validate
     * @return error message or null if valid
     */
    public String getValidationError(FinancialData financialData) {
        if (financialData == null) {
            return "Financial data is required";
        }
        
        if (!isValidTicker(financialData.getTicker())) {
            return "Invalid ticker symbol";
        }
        
        if (!hasMinimumRequiredData(financialData)) {
            return "Financial data must include revenue, free cash flow, and EPS";
        }
        
        if (!isDataConsistent(financialData)) {
            return "Financial data arrays must have consistent lengths";
        }
        
        // Validate individual value lists
        if (!isValidFinancialValueList(financialData.getRevenue())) {
            return "Invalid revenue data";
        }
        if (!isValidFinancialValueList(financialData.getFreeCashFlow())) {
            return "Invalid free cash flow data";
        }
        if (!isValidFinancialValueList(financialData.getEps())) {
            return "Invalid EPS data";
        }
        
        return null; // Data is valid
    }

    /**
     * Calculate revenue growth rate from financial data
     * @param financialData the financial data
     * @return average revenue growth rate or null if insufficient data
     */
    public Double calculateRevenueGrowthRate(FinancialData financialData) {
        if (financialData == null || financialData.getRevenue().size() < 2) {
            return null;
        }
        
        List<Double> revenue = financialData.getRevenue();
        List<Double> growthRates = new ArrayList<>();
        
        for (int i = 1; i < revenue.size(); i++) {
            Double previousYear = revenue.get(i - 1);
            Double currentYear = revenue.get(i);
            
            if (previousYear != null && currentYear != null && previousYear != 0) {
                double growthRate = (currentYear - previousYear) / previousYear;
                growthRates.add(growthRate);
            }
        }
        
        if (growthRates.isEmpty()) {
            return null;
        }
        
        return growthRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}