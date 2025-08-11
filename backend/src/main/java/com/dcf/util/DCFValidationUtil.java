package com.dcf.util;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import org.springframework.stereotype.Component;

@Component
public class DCFValidationUtil {

    /**
     * Validate DCF input parameters
     * @param dcfInput the DCF input to validate
     * @return validation error message or null if valid
     */
    public String validateDCFInput(DCFInput dcfInput) {
        if (dcfInput == null) {
            return "DCF input is required";
        }

        if (dcfInput.getTicker() == null || dcfInput.getTicker().trim().isEmpty()) {
            return "Ticker symbol is required";
        }

        if (dcfInput.getDiscountRate() == null) {
            return "Discount rate is required";
        }

        if (dcfInput.getDiscountRate() < 0 || dcfInput.getDiscountRate() > 100) {
            return "Discount rate must be between 0% and 100%";
        }

        if (dcfInput.getGrowthRate() == null) {
            return "Growth rate is required";
        }

        if (dcfInput.getGrowthRate() > 1000) {
            return "Growth rate too high. Please input a realistic value.";
        }

        if (dcfInput.getGrowthRate() < -100) {
            return "Growth rate cannot be less than -100%";
        }

        if (dcfInput.getTerminalGrowthRate() == null) {
            return "Terminal growth rate is required";
        }

        if (dcfInput.getTerminalGrowthRate() < 0 || dcfInput.getTerminalGrowthRate() > 10) {
            return "Terminal growth rate must be between 0% and 10%";
        }

        if (dcfInput.getTerminalGrowthRate() > dcfInput.getDiscountRate()) {
            return "Terminal growth rate cannot exceed discount rate";
        }

        if (dcfInput.getProjectionYears() != null && 
            (dcfInput.getProjectionYears() < 1 || dcfInput.getProjectionYears() > 20)) {
            return "Projection years must be between 1 and 20";
        }

        return null; // Valid
    }

    /**
     * Validate DCF output
     * @param dcfOutput the DCF output to validate
     * @return validation error message or null if valid
     */
    public String validateDCFOutput(DCFOutput dcfOutput) {
        if (dcfOutput == null) {
            return "DCF output is required";
        }

        if (dcfOutput.getTicker() == null || dcfOutput.getTicker().trim().isEmpty()) {
            return "Ticker symbol is required";
        }

        if (dcfOutput.getFairValuePerShare() == null) {
            return "Fair value per share is required";
        }

        if (dcfOutput.getFairValuePerShare() <= 0) {
            return "Fair value per share must be positive";
        }

        if (dcfOutput.getCurrentPrice() != null && dcfOutput.getCurrentPrice() <= 0) {
            return "Current price must be positive";
        }

        if (dcfOutput.getValuation() == null || dcfOutput.getValuation().trim().isEmpty()) {
            return "Valuation status is required";
        }

        return null; // Valid
    }

    /**
     * Check if growth rate is reasonable
     * @param growthRate the growth rate to check
     * @return true if reasonable, false otherwise
     */
    public boolean isReasonableGrowthRate(Double growthRate) {
        if (growthRate == null) {
            return false;
        }
        // Most companies don't sustain >50% growth for long periods
        return growthRate >= -50.0 && growthRate <= 50.0;
    }

    /**
     * Check if discount rate is reasonable
     * @param discountRate the discount rate to check
     * @return true if reasonable, false otherwise
     */
    public boolean isReasonableDiscountRate(Double discountRate) {
        if (discountRate == null) {
            return false;
        }
        // Typical discount rates are between 5% and 20%
        return discountRate >= 5.0 && discountRate <= 20.0;
    }

    /**
     * Check if terminal growth rate is conservative
     * @param terminalGrowthRate the terminal growth rate to check
     * @return true if conservative, false otherwise
     */
    public boolean isConservativeTerminalGrowthRate(Double terminalGrowthRate) {
        if (terminalGrowthRate == null) {
            return false;
        }
        // Conservative terminal growth rates are typically 2-4%
        return terminalGrowthRate >= 2.0 && terminalGrowthRate <= 4.0;
    }

    /**
     * Get warning message for potentially unrealistic parameters
     * @param dcfInput the DCF input to check
     * @return warning message or null if no warnings
     */
    public String getParameterWarning(DCFInput dcfInput) {
        if (dcfInput == null) {
            return null;
        }

        if (!isReasonableGrowthRate(dcfInput.getGrowthRate())) {
            return "Warning: Growth rate of " + dcfInput.getGrowthRate() + 
                   "% may be unrealistic for sustained periods";
        }

        if (!isReasonableDiscountRate(dcfInput.getDiscountRate())) {
            return "Warning: Discount rate of " + dcfInput.getDiscountRate() + 
                   "% is outside typical range (5-20%)";
        }

        if (!isConservativeTerminalGrowthRate(dcfInput.getTerminalGrowthRate())) {
            return "Warning: Terminal growth rate of " + dcfInput.getTerminalGrowthRate() + 
                   "% may be too optimistic (consider 2-4%)";
        }

        return null; // No warnings
    }

    /**
     * Validate that DCF calculation makes sense
     * @param dcfOutput the DCF output to validate
     * @return validation error or null if reasonable
     */
    public String validateCalculationReasonableness(DCFOutput dcfOutput) {
        if (dcfOutput == null) {
            return null;
        }

        if (dcfOutput.getFairValuePerShare() != null && dcfOutput.getFairValuePerShare() > 10000) {
            return "Warning: Fair value per share seems unusually high";
        }

        if (dcfOutput.getCurrentPrice() != null && dcfOutput.getFairValuePerShare() != null) {
            double ratio = dcfOutput.getFairValuePerShare() / dcfOutput.getCurrentPrice();
            if (ratio > 10) {
                return "Warning: Fair value is more than 10x current price - please verify assumptions";
            }
            if (ratio < 0.1) {
                return "Warning: Fair value is less than 10% of current price - please verify assumptions";
            }
        }

        return null; // Calculation seems reasonable
    }

    /**
     * Check if all required fields are present for DCF calculation
     * @param dcfInput the DCF input to check
     * @return true if all required fields present, false otherwise
     */
    public boolean hasAllRequiredFields(DCFInput dcfInput) {
        return dcfInput != null &&
               dcfInput.getTicker() != null && !dcfInput.getTicker().trim().isEmpty() &&
               dcfInput.getDiscountRate() != null &&
               dcfInput.getGrowthRate() != null &&
               dcfInput.getTerminalGrowthRate() != null;
    }
}