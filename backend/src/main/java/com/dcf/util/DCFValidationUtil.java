package com.dcf.util;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

        if (dcfInput.getDiscountRate().compareTo(BigDecimal.ZERO) < 0 || 
            dcfInput.getDiscountRate().compareTo(new BigDecimal("100")) > 0) {
            return "Discount rate must be between 0% and 100%";
        }

        if (dcfInput.getGrowthRate() == null) {
            return "Growth rate is required";
        }

        if (dcfInput.getGrowthRate().compareTo(new BigDecimal("1000")) > 0) {
            return "Growth rate too high. Please input a realistic value.";
        }

        if (dcfInput.getGrowthRate().compareTo(new BigDecimal("-100")) < 0) {
            return "Growth rate cannot be less than -100%";
        }

        if (dcfInput.getTerminalGrowthRate() == null) {
            return "Terminal growth rate is required";
        }

        if (dcfInput.getTerminalGrowthRate().compareTo(BigDecimal.ZERO) < 0 || 
            dcfInput.getTerminalGrowthRate().compareTo(new BigDecimal("10")) > 0) {
            return "Terminal growth rate must be between 0% and 10%";
        }

        if (dcfInput.getTerminalGrowthRate().compareTo(dcfInput.getDiscountRate()) > 0) {
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

        if (dcfOutput.getFairValuePerShare().compareTo(BigDecimal.ZERO) <= 0) {
            return "Fair value per share must be positive";
        }

        if (dcfOutput.getCurrentPrice() != null && dcfOutput.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
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
    public boolean isReasonableGrowthRate(BigDecimal growthRate) {
        if (growthRate == null) {
            return false;
        }
        // Most companies don't sustain >50% growth for long periods
        return growthRate.compareTo(new BigDecimal("-50.0")) >= 0 && 
               growthRate.compareTo(new BigDecimal("50.0")) <= 0;
    }

    /**
     * Check if discount rate is reasonable
     * @param discountRate the discount rate to check
     * @return true if reasonable, false otherwise
     */
    public boolean isReasonableDiscountRate(BigDecimal discountRate) {
        if (discountRate == null) {
            return false;
        }
        // Typical discount rates are between 5% and 20%
        return discountRate.compareTo(new BigDecimal("5.0")) >= 0 && 
               discountRate.compareTo(new BigDecimal("20.0")) <= 0;
    }

    /**
     * Check if terminal growth rate is conservative
     * @param terminalGrowthRate the terminal growth rate to check
     * @return true if conservative, false otherwise
     */
    public boolean isConservativeTerminalGrowthRate(BigDecimal terminalGrowthRate) {
        if (terminalGrowthRate == null) {
            return false;
        }
        // Conservative terminal growth rates are typically 2-4%
        return terminalGrowthRate.compareTo(new BigDecimal("2.0")) >= 0 && 
               terminalGrowthRate.compareTo(new BigDecimal("4.0")) <= 0;
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
     * Validate BigDecimal precision and scale limits for financial rates
     * @param value the BigDecimal value to validate
     * @param fieldName the name of the field for error messages
     * @param maxPrecision maximum allowed precision (total digits)
     * @param maxScale maximum allowed scale (decimal places)
     * @return validation error message or null if valid
     */
    public String validateBigDecimalPrecisionAndScale(BigDecimal value, String fieldName, int maxPrecision, int maxScale) {
        if (value == null) {
            return null;
        }

        // Get precision (total number of digits)
        int precision = value.precision();
        if (precision > maxPrecision) {
            return fieldName + " precision exceeds maximum allowed (" + maxPrecision + " digits)";
        }

        // Get scale (number of decimal places)
        int scale = value.scale();
        if (scale > maxScale) {
            return fieldName + " scale exceeds maximum allowed (" + maxScale + " decimal places)";
        }

        return null; // Valid precision and scale
    }

    /**
     * Validate BigDecimal input parameters for precision and scale limits
     * @param dcfInput the DCF input to validate
     * @return validation error message or null if valid
     */
    public String validateBigDecimalPrecisionAndScale(DCFInput dcfInput) {
        if (dcfInput == null) {
            return null;
        }

        // Validate discount rate precision and scale (10,6)
        String error = validateBigDecimalPrecisionAndScale(dcfInput.getDiscountRate(), "Discount rate", 10, 6);
        if (error != null) {
            return error;
        }

        // Validate growth rate precision and scale (10,6)
        error = validateBigDecimalPrecisionAndScale(dcfInput.getGrowthRate(), "Growth rate", 10, 6);
        if (error != null) {
            return error;
        }

        // Validate terminal growth rate precision and scale (10,6)
        error = validateBigDecimalPrecisionAndScale(dcfInput.getTerminalGrowthRate(), "Terminal growth rate", 10, 6);
        if (error != null) {
            return error;
        }

        return null; // All precision and scale validations passed
    }

    /**
     * Validate BigDecimal output parameters for precision and scale limits
     * @param dcfOutput the DCF output to validate
     * @return validation error message or null if valid
     */
    public String validateBigDecimalPrecisionAndScale(DCFOutput dcfOutput) {
        if (dcfOutput == null) {
            return null;
        }

        // Validate fair value per share precision and scale (20,6)
        String error = validateBigDecimalPrecisionAndScale(dcfOutput.getFairValuePerShare(), "Fair value per share", 20, 6);
        if (error != null) {
            return error;
        }

        // Validate current price precision and scale (20,6)
        if (dcfOutput.getCurrentPrice() != null) {
            error = validateBigDecimalPrecisionAndScale(dcfOutput.getCurrentPrice(), "Current price", 20, 6);
            if (error != null) {
                return error;
            }
        }

        // Validate enterprise value precision and scale (25,2)
        if (dcfOutput.getEnterpriseValue() != null) {
            error = validateBigDecimalPrecisionAndScale(dcfOutput.getEnterpriseValue(), "Enterprise value", 25, 2);
            if (error != null) {
                return error;
            }
        }

        // Validate equity value precision and scale (25,2)
        if (dcfOutput.getEquityValue() != null) {
            error = validateBigDecimalPrecisionAndScale(dcfOutput.getEquityValue(), "Equity value", 25, 2);
            if (error != null) {
                return error;
            }
        }

        return null; // All precision and scale validations passed
    }

    /**
     * Check if BigDecimal value is within reasonable bounds for financial calculations
     * @param value the BigDecimal value to check
     * @param minValue minimum reasonable value
     * @param maxValue maximum reasonable value
     * @return true if within bounds, false otherwise
     */
    public boolean isWithinReasonableBounds(BigDecimal value, BigDecimal minValue, BigDecimal maxValue) {
        if (value == null || minValue == null || maxValue == null) {
            return false;
        }
        return value.compareTo(minValue) >= 0 && value.compareTo(maxValue) <= 0;
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

        if (dcfOutput.getFairValuePerShare() != null && 
            dcfOutput.getFairValuePerShare().compareTo(new BigDecimal("10000")) > 0) {
            return "Warning: Fair value per share seems unusually high";
        }

        if (dcfOutput.getCurrentPrice() != null && dcfOutput.getFairValuePerShare() != null) {
            BigDecimal ratio = dcfOutput.getFairValuePerShare().divide(dcfOutput.getCurrentPrice(), 10, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("10")) > 0) {
                return "Warning: Fair value is more than 10x current price - please verify assumptions";
            }
            if (ratio.compareTo(new BigDecimal("0.1")) < 0) {
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

    /**
     * Validate BigDecimal range with custom bounds and meaningful error messages
     * @param value the BigDecimal value to validate
     * @param fieldName the name of the field for error messages
     * @param minValue minimum allowed value (inclusive)
     * @param maxValue maximum allowed value (inclusive)
     * @param unit the unit for the field (e.g., "%", "$", "years")
     * @return validation error message or null if valid
     */
    public String validateBigDecimalRange(BigDecimal value, String fieldName, 
                                        BigDecimal minValue, BigDecimal maxValue, String unit) {
        if (value == null) {
            return fieldName + " is required";
        }

        if (minValue != null && value.compareTo(minValue) < 0) {
            return fieldName + " must be at least " + minValue + unit + 
                   " (provided: " + value + unit + ")";
        }

        if (maxValue != null && value.compareTo(maxValue) > 0) {
            return fieldName + " must not exceed " + maxValue + unit + 
                   " (provided: " + value + unit + ")";
        }

        return null; // Valid range
    }

    /**
     * Validate BigDecimal for financial percentage rates with enhanced error messages
     * @param rate the percentage rate to validate
     * @param fieldName the name of the field for error messages
     * @param minRate minimum allowed rate (as percentage, e.g., 0.0 for 0%)
     * @param maxRate maximum allowed rate (as percentage, e.g., 100.0 for 100%)
     * @return validation error message or null if valid
     */
    public String validatePercentageRate(BigDecimal rate, String fieldName, 
                                       BigDecimal minRate, BigDecimal maxRate) {
        String rangeError = validateBigDecimalRange(rate, fieldName, minRate, maxRate, "%");
        if (rangeError != null) {
            return rangeError;
        }

        // Additional validation for precision and scale
        String precisionError = validateBigDecimalPrecisionAndScale(rate, fieldName, 10, 6);
        if (precisionError != null) {
            return precisionError + " (rates should have at most 6 decimal places)";
        }

        return null; // Valid percentage rate
    }

    /**
     * Validate BigDecimal for monetary values with enhanced error messages
     * @param amount the monetary amount to validate
     * @param fieldName the name of the field for error messages
     * @param minAmount minimum allowed amount (can be null for no minimum)
     * @param maxAmount maximum allowed amount (can be null for no maximum)
     * @return validation error message or null if valid
     */
    public String validateMonetaryAmount(BigDecimal amount, String fieldName, 
                                       BigDecimal minAmount, BigDecimal maxAmount) {
        if (amount == null) {
            return fieldName + " is required";
        }

        String rangeError = validateBigDecimalRange(amount, fieldName, minAmount, maxAmount, "");
        if (rangeError != null) {
            return rangeError;
        }

        // Check for reasonable monetary precision (up to 6 decimal places for share prices)
        String precisionError = validateBigDecimalPrecisionAndScale(amount, fieldName, 20, 6);
        if (precisionError != null) {
            return precisionError + " (monetary values should have at most 6 decimal places)";
        }

        // Check for negative monetary values where not allowed
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) >= 0 && 
            amount.compareTo(BigDecimal.ZERO) < 0) {
            return fieldName + " cannot be negative (provided: " + amount + ")";
        }

        return null; // Valid monetary amount
    }

    /**
     * Validate BigDecimal for large monetary values (enterprise value, equity value) with enhanced error messages
     * @param amount the large monetary amount to validate
     * @param fieldName the name of the field for error messages
     * @return validation error message or null if valid
     */
    public String validateLargeMonetaryAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            return null; // Large monetary amounts are often optional
        }

        // Check for reasonable precision for large amounts (up to 2 decimal places)
        String precisionError = validateBigDecimalPrecisionAndScale(amount, fieldName, 25, 2);
        if (precisionError != null) {
            return precisionError + " (large monetary values should have at most 2 decimal places)";
        }

        // Check for negative values
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return fieldName + " cannot be negative (provided: " + amount + ")";
        }

        // Check for unreasonably large values (over 1 quadrillion)
        BigDecimal maxReasonable = new BigDecimal("1000000000000000"); // 1 quadrillion
        if (amount.compareTo(maxReasonable) > 0) {
            return fieldName + " exceeds reasonable bounds (provided: " + amount + 
                   ", maximum reasonable: " + maxReasonable + ")";
        }

        return null; // Valid large monetary amount
    }

    /**
     * Enhanced DCF input validation with improved BigDecimal error messages
     * @param dcfInput the DCF input to validate
     * @return validation error message or null if valid
     */
    public String validateDCFInputEnhanced(DCFInput dcfInput) {
        if (dcfInput == null) {
            return "DCF input is required";
        }

        if (dcfInput.getTicker() == null || dcfInput.getTicker().trim().isEmpty()) {
            return "Ticker symbol is required";
        }

        // Enhanced discount rate validation
        String error = validatePercentageRate(dcfInput.getDiscountRate(), "Discount rate", 
                                            new BigDecimal("0.0"), new BigDecimal("100.0"));
        if (error != null) {
            return error;
        }

        // Enhanced growth rate validation
        error = validatePercentageRate(dcfInput.getGrowthRate(), "Growth rate", 
                                     new BigDecimal("-100.0"), new BigDecimal("1000.0"));
        if (error != null) {
            return error;
        }

        // Enhanced terminal growth rate validation
        error = validatePercentageRate(dcfInput.getTerminalGrowthRate(), "Terminal growth rate", 
                                     new BigDecimal("0.0"), new BigDecimal("10.0"));
        if (error != null) {
            return error;
        }

        // Business logic validation
        if (dcfInput.getTerminalGrowthRate().compareTo(dcfInput.getDiscountRate()) > 0) {
            return "Terminal growth rate (" + dcfInput.getTerminalGrowthRate() + 
                   "%) cannot exceed discount rate (" + dcfInput.getDiscountRate() + "%)";
        }

        // Projection years validation
        if (dcfInput.getProjectionYears() != null && 
            (dcfInput.getProjectionYears() < 1 || dcfInput.getProjectionYears() > 20)) {
            return "Projection years must be between 1 and 20 (provided: " + 
                   dcfInput.getProjectionYears() + ")";
        }

        return null; // Valid
    }

    /**
     * Enhanced DCF output validation with improved BigDecimal error messages
     * @param dcfOutput the DCF output to validate
     * @return validation error message or null if valid
     */
    public String validateDCFOutputEnhanced(DCFOutput dcfOutput) {
        if (dcfOutput == null) {
            return "DCF output is required";
        }

        if (dcfOutput.getTicker() == null || dcfOutput.getTicker().trim().isEmpty()) {
            return "Ticker symbol is required";
        }

        // Enhanced fair value validation
        String error = validateMonetaryAmount(dcfOutput.getFairValuePerShare(), "Fair value per share", 
                                            new BigDecimal("0.01"), null);
        if (error != null) {
            return error;
        }

        // Enhanced current price validation
        if (dcfOutput.getCurrentPrice() != null) {
            error = validateMonetaryAmount(dcfOutput.getCurrentPrice(), "Current price", 
                                         new BigDecimal("0.01"), null);
            if (error != null) {
                return error;
            }
        }

        // Enhanced large monetary amount validation
        error = validateLargeMonetaryAmount(dcfOutput.getEnterpriseValue(), "Enterprise value");
        if (error != null) {
            return error;
        }

        error = validateLargeMonetaryAmount(dcfOutput.getEquityValue(), "Equity value");
        if (error != null) {
            return error;
        }

        error = validateLargeMonetaryAmount(dcfOutput.getTerminalValue(), "Terminal value");
        if (error != null) {
            return error;
        }

        error = validateLargeMonetaryAmount(dcfOutput.getPresentValueOfCashFlows(), "Present value of cash flows");
        if (error != null) {
            return error;
        }

        // Valuation status validation
        if (dcfOutput.getValuation() == null || dcfOutput.getValuation().trim().isEmpty()) {
            return "Valuation status is required";
        }

        return null; // Valid
    }

    /**
     * Validate BigDecimal arithmetic operation results for potential overflow or underflow
     * @param result the result of a BigDecimal arithmetic operation
     * @param operationDescription description of the operation for error messages
     * @return validation error message or null if valid
     */
    public String validateArithmeticResult(BigDecimal result, String operationDescription) {
        if (result == null) {
            return operationDescription + " resulted in null value";
        }

        // Check for infinite or NaN equivalent conditions
        try {
            // Test if the result can be converted to string without issues
            String resultStr = result.toPlainString();
            if (resultStr.length() > 1000) { // Extremely long result string
                return operationDescription + " resulted in an extremely large value that may cause display issues";
            }
        } catch (Exception e) {
            return operationDescription + " resulted in an invalid BigDecimal value: " + e.getMessage();
        }

        // Check for unreasonably large results
        BigDecimal maxReasonable = new BigDecimal("1E+50"); // 10^50
        if (result.abs().compareTo(maxReasonable) > 0) {
            return operationDescription + " resulted in an unreasonably large value: " + result.toPlainString();
        }

        return null; // Valid arithmetic result
    }

    /**
     * Validate BigDecimal division operation parameters to prevent division by zero
     * @param dividend the dividend
     * @param divisor the divisor
     * @param operationDescription description of the operation for error messages
     * @return validation error message or null if valid
     */
    public String validateDivisionOperation(BigDecimal dividend, BigDecimal divisor, String operationDescription) {
        if (dividend == null) {
            return operationDescription + ": dividend cannot be null";
        }

        if (divisor == null) {
            return operationDescription + ": divisor cannot be null";
        }

        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return operationDescription + ": division by zero is not allowed";
        }

        // Check for very small divisors that might cause precision issues
        BigDecimal minDivisor = new BigDecimal("1E-10");
        if (divisor.abs().compareTo(minDivisor) < 0) {
            return operationDescription + ": divisor is too small and may cause precision issues (divisor: " + 
                   divisor.toPlainString() + ")";
        }

        return null; // Valid division parameters
    }

    /**
     * Get comprehensive validation summary for DCF input with all BigDecimal validations
     * @param dcfInput the DCF input to validate
     * @return comprehensive validation summary or null if all validations pass
     */
    public String getComprehensiveValidationSummary(DCFInput dcfInput) {
        StringBuilder errors = new StringBuilder();

        // Basic validation
        String basicError = validateDCFInputEnhanced(dcfInput);
        if (basicError != null) {
            errors.append("Basic validation: ").append(basicError).append("; ");
        }

        // Precision and scale validation
        String precisionError = validateBigDecimalPrecisionAndScale(dcfInput);
        if (precisionError != null) {
            errors.append("Precision validation: ").append(precisionError).append("; ");
        }

        // Parameter reasonableness warnings
        String warningError = getParameterWarning(dcfInput);
        if (warningError != null) {
            errors.append("Reasonableness check: ").append(warningError).append("; ");
        }

        return errors.length() > 0 ? errors.toString().trim() : null;
    }
}