package com.dcf.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Optimized BigDecimal mathematical operations with caching and performance improvements
 */
@Component
public class OptimizedBigDecimalMath {

    @Autowired
    private BigDecimalPerformanceProfiler profiler;

    // Standard precision and rounding mode for financial calculations
    private static final int CALCULATION_PRECISION = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final MathContext MATH_CONTEXT = new MathContext(CALCULATION_PRECISION, ROUNDING_MODE);

    // Cache for commonly used constants and power calculations
    private static final ConcurrentMap<String, BigDecimal> constantCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, BigDecimal> powerCache = new ConcurrentHashMap<>();

    // Pre-computed constants
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal ONE = BigDecimal.ONE;
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    static {
        // Pre-populate cache with commonly used values
        constantCache.put("0", ZERO);
        constantCache.put("1", ONE);
        constantCache.put("100", HUNDRED);
        
        // Common percentages as decimals
        for (int i = 1; i <= 50; i++) {
            BigDecimal percentage = new BigDecimal(i).divide(HUNDRED, CALCULATION_PRECISION, ROUNDING_MODE);
            constantCache.put("percent_" + i, percentage);
        }
    }

    /**
     * Optimized power calculation with caching
     * @param base the base value
     * @param exponent the exponent (integer)
     * @return base^exponent
     */
    public BigDecimal pow(BigDecimal base, int exponent) {
        if (exponent == 0) return ONE;
        if (exponent == 1) return base;
        if (base.equals(ZERO)) return ZERO;
        if (base.equals(ONE)) return ONE;

        String cacheKey = base.toPlainString() + "^" + exponent;
        BigDecimal cached = powerCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        return profiler.profileOperation("pow_" + exponent, () -> {
            BigDecimal result;
            
            if (exponent < 0) {
                // Handle negative exponents: base^(-n) = 1 / (base^n)
                result = ONE.divide(pow(base, -exponent), CALCULATION_PRECISION, ROUNDING_MODE);
            } else if (exponent <= 10) {
                // Use built-in pow for small exponents
                result = base.pow(exponent, MATH_CONTEXT);
            } else {
                // Use binary exponentiation for larger exponents
                result = binaryExponentiation(base, exponent);
            }
            
            // Cache the result if it's reasonable to cache
            if (powerCache.size() < 1000) {
                powerCache.put(cacheKey, result);
            }
            
            return result;
        });
    }

    /**
     * Binary exponentiation for efficient large power calculations
     * @param base the base value
     * @param exponent the exponent
     * @return base^exponent
     */
    private BigDecimal binaryExponentiation(BigDecimal base, int exponent) {
        BigDecimal result = ONE;
        BigDecimal currentBase = base;
        int currentExponent = exponent;

        while (currentExponent > 0) {
            if (currentExponent % 2 == 1) {
                result = result.multiply(currentBase, MATH_CONTEXT);
            }
            currentBase = currentBase.multiply(currentBase, MATH_CONTEXT);
            currentExponent /= 2;
        }

        return result;
    }

    /**
     * Optimized division with consistent precision
     * @param dividend the dividend
     * @param divisor the divisor
     * @return dividend / divisor
     */
    public BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.equals(ZERO)) {
            throw new ArithmeticException("Division by zero");
        }
        
        return profiler.profileOperation("divide", () -> 
            dividend.divide(divisor, CALCULATION_PRECISION, ROUNDING_MODE)
        );
    }

    /**
     * Optimized multiplication
     * @param multiplicand the multiplicand
     * @param multiplier the multiplier
     * @return multiplicand * multiplier
     */
    public BigDecimal multiply(BigDecimal multiplicand, BigDecimal multiplier) {
        return profiler.profileOperation("multiply", () -> 
            multiplicand.multiply(multiplier, MATH_CONTEXT)
        );
    }

    /**
     * Optimized addition
     * @param augend the augend
     * @param addend the addend
     * @return augend + addend
     */
    public BigDecimal add(BigDecimal augend, BigDecimal addend) {
        return augend.add(addend, MATH_CONTEXT);
    }

    /**
     * Optimized subtraction
     * @param minuend the minuend
     * @param subtrahend the subtrahend
     * @return minuend - subtrahend
     */
    public BigDecimal subtract(BigDecimal minuend, BigDecimal subtrahend) {
        return minuend.subtract(subtrahend, MATH_CONTEXT);
    }

    /**
     * Calculate compound growth with optimized power operations
     * @param principal the principal amount
     * @param rate the growth rate (as decimal, e.g., 0.05 for 5%)
     * @param periods the number of periods
     * @return principal * (1 + rate)^periods
     */
    public BigDecimal compoundGrowth(BigDecimal principal, BigDecimal rate, int periods) {
        return profiler.profileOperation("compound_growth", () -> {
            BigDecimal growthFactor = ONE.add(rate);
            BigDecimal compoundFactor = pow(growthFactor, periods);
            return multiply(principal, compoundFactor);
        });
    }

    /**
     * Calculate present value with optimized operations
     * @param futureValue the future value
     * @param discountRate the discount rate (as decimal)
     * @param periods the number of periods
     * @return futureValue / (1 + discountRate)^periods
     */
    public BigDecimal presentValue(BigDecimal futureValue, BigDecimal discountRate, int periods) {
        return profiler.profileOperation("present_value", () -> {
            BigDecimal discountFactor = ONE.add(discountRate);
            BigDecimal discountMultiplier = pow(discountFactor, periods);
            return divide(futureValue, discountMultiplier);
        });
    }

    /**
     * Calculate sum of a list of BigDecimal values with optimized operations
     * @param values the list of values to sum
     * @return the sum of all values
     */
    public BigDecimal sum(List<BigDecimal> values) {
        return profiler.profileOperation("sum_list", () -> {
            BigDecimal sum = ZERO;
            for (BigDecimal value : values) {
                sum = sum.add(value, MATH_CONTEXT);
            }
            return sum;
        });
    }

    /**
     * Project cash flows with optimized calculations
     * @param baseCashFlow the base cash flow
     * @param growthRate the growth rate (as decimal)
     * @param years the number of years to project
     * @return list of projected cash flows
     */
    public List<BigDecimal> projectCashFlows(BigDecimal baseCashFlow, BigDecimal growthRate, int years) {
        return profiler.profileOperation("project_cash_flows", () -> {
            List<BigDecimal> projections = new ArrayList<>(years);
            BigDecimal growthFactor = ONE.add(growthRate);
            BigDecimal currentValue = baseCashFlow;
            
            for (int i = 1; i <= years; i++) {
                currentValue = multiply(currentValue, growthFactor);
                projections.add(currentValue);
            }
            
            return projections;
        });
    }

    /**
     * Calculate terminal value using Gordon Growth Model
     * @param terminalCashFlow the terminal year cash flow
     * @param discountRate the discount rate (as decimal)
     * @param terminalGrowthRate the terminal growth rate (as decimal)
     * @return terminal value
     */
    public BigDecimal terminalValue(BigDecimal terminalCashFlow, BigDecimal discountRate, BigDecimal terminalGrowthRate) {
        return profiler.profileOperation("terminal_value", () -> {
            BigDecimal growthAdjustedCashFlow = multiply(terminalCashFlow, ONE.add(terminalGrowthRate));
            BigDecimal denominator = subtract(discountRate, terminalGrowthRate);
            
            if (denominator.compareTo(ZERO) <= 0) {
                throw new ArithmeticException("Terminal growth rate must be less than discount rate");
            }
            
            return divide(growthAdjustedCashFlow, denominator);
        });
    }

    /**
     * Get a commonly used percentage as decimal from cache
     * @param percentage the percentage (1-50)
     * @return the percentage as decimal (e.g., 5% = 0.05)
     */
    public BigDecimal getPercentageAsDecimal(int percentage) {
        if (percentage < 1 || percentage > 50) {
            return new BigDecimal(percentage).divide(HUNDRED, CALCULATION_PRECISION, ROUNDING_MODE);
        }
        return constantCache.get("percent_" + percentage);
    }

    /**
     * Convert percentage to decimal with caching
     * @param percentage the percentage value
     * @return the decimal equivalent
     */
    public BigDecimal percentageToDecimal(BigDecimal percentage) {
        return divide(percentage, HUNDRED);
    }

    /**
     * Round to standard financial precision (6 decimal places)
     * @param value the value to round
     * @return rounded value
     */
    public BigDecimal roundFinancial(BigDecimal value) {
        return value.setScale(6, ROUNDING_MODE);
    }

    /**
     * Round to currency precision (2 decimal places)
     * @param value the value to round
     * @return rounded value
     */
    public BigDecimal roundCurrency(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE);
    }

    /**
     * Clear all caches
     */
    public void clearCaches() {
        powerCache.clear();
        // Don't clear constantCache as it contains pre-computed constants
    }

    /**
     * Get cache statistics
     * @return cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(constantCache.size(), powerCache.size());
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        private final int constantCacheSize;
        private final int powerCacheSize;

        public CacheStats(int constantCacheSize, int powerCacheSize) {
            this.constantCacheSize = constantCacheSize;
            this.powerCacheSize = powerCacheSize;
        }

        public int getConstantCacheSize() { return constantCacheSize; }
        public int getPowerCacheSize() { return powerCacheSize; }
    }
}