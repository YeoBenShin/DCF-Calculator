package com.dcf.service;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import com.dcf.repository.DCFInputRepository;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.util.DCFValidationUtil;
import com.dcf.util.BigDecimalPerformanceProfiler;
import com.dcf.util.OptimizedBigDecimalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DCFCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(DCFCalculationService.class);

    @Autowired
    private DCFInputRepository dcfInputRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @Autowired
    private DCFValidationUtil dcfValidationUtil;

    @Autowired
    private FinancialDataScrapingService financialDataScrapingService;

    @Autowired
    private BigDecimalPerformanceProfiler performanceProfiler;

    @Autowired
    private OptimizedBigDecimalMath optimizedMath;

    @Autowired
    private BigDecimalCalculationCacheService calculationCacheService;

    @Autowired
    private BigDecimalPerformanceMonitoringService performanceMonitoringService;

    /**
     * Calculate DCF valuation for a given input
     * 
     * @param dcfInput the DCF input parameters
     * @return DCFOutput with calculated fair value
     * @throws DCFCalculationException if calculation fails
     */
    public DCFOutput calculateDCF(DCFInput dcfInput) throws DCFCalculationException {
        logger.info("Starting DCF calculation for ticker: {}", dcfInput.getTicker());

        // Validate input
        String validationError = dcfValidationUtil.validateDCFInput(dcfInput);
        if (validationError != null) {
            throw new DCFCalculationException("Invalid DCF input: " + validationError);
        }

        try {
            // Get financial data
            FinancialData financialData = financialDataScrapingService.getFinancialData(dcfInput.getTicker());

            // Perform DCF calculation
            DCFOutput result = performDCFCalculation(dcfInput, financialData);

            // Save input and output
            DCFInput savedInput = dcfInputRepository.save(dcfInput);
            result.setDcfInputId(savedInput.getId());
            result.setUserId(dcfInput.getUserId());

            DCFOutput savedOutput = dcfOutputRepository.save(result);

            logger.info("DCF calculation completed for ticker: {} with fair value: {}",
                    dcfInput.getTicker(), result.getFairValuePerShare());

            return savedOutput;

        } catch (FinancialDataScrapingService.FinancialDataException e) {
            throw new DCFCalculationException("Failed to retrieve financial data: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error during DCF calculation for ticker: {}", dcfInput.getTicker(), e);
            throw new DCFCalculationException("DCF calculation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Perform the actual DCF calculation
     * 
     * @param dcfInput      the DCF input parameters
     * @param financialData the financial data
     * @return DCFOutput with calculated values
     */
    private DCFOutput performDCFCalculation(DCFInput dcfInput, FinancialData financialData) {
        logger.debug("Performing DCF calculation for ticker: {}", dcfInput.getTicker());

        // Extract parameters as BigDecimal
        BigDecimal discountRate = dcfInput.getDiscountRateAsDecimal();
        BigDecimal growthRate = dcfInput.getGrowthRateAsDecimal();
        BigDecimal terminalGrowthRate = dcfInput.getTerminalGrowthRateAsDecimal();
        int projectionYears = dcfInput.getProjectionYears();

        // Get latest free cash flow
        BigDecimal latestFCF = financialData.getLatestFreeCashFlow();
        if (latestFCF == null || latestFCF.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid or missing free cash flow data");
        }

        // Project future free cash flows
        List<BigDecimal> projectedFCFs = projectFreeCashFlows(latestFCF, growthRate, projectionYears);

        // Calculate present value of projected cash flows
        BigDecimal presentValueOfCashFlows = calculatePresentValue(projectedFCFs, discountRate);

        // Calculate terminal value
        BigDecimal terminalFCF = projectedFCFs.get(projectedFCFs.size() - 1)
                .multiply(BigDecimal.ONE.add(terminalGrowthRate));
        BigDecimal terminalValue = terminalFCF.divide(discountRate.subtract(terminalGrowthRate), 10,
                RoundingMode.HALF_UP);
        BigDecimal presentValueOfTerminalValue = terminalValue.divide(
                optimizedMath.pow(BigDecimal.ONE.add(discountRate), projectionYears), 10, RoundingMode.HALF_UP);

        // Calculate enterprise value
        BigDecimal enterpriseValue = presentValueOfCashFlows.add(presentValueOfTerminalValue);

        // Calculate equity value (enterprise value - net debt)
        BigDecimal latestDebt = financialData.getLatestDebt();
        BigDecimal netDebt = latestDebt != null ? latestDebt : BigDecimal.ZERO;
        BigDecimal equityValue = enterpriseValue.subtract(netDebt);

        // Calculate fair value per share
        BigDecimal sharesOutstanding = financialData.getLatestSharesOutstanding();
        if (sharesOutstanding == null || sharesOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid or missing shares outstanding data");
        }

        BigDecimal fairValuePerShare = equityValue.divide(sharesOutstanding, 6, RoundingMode.HALF_UP);

        // Get current stock price (mock for now)
        BigDecimal currentPrice = getCurrentStockPrice(dcfInput.getTicker());

        // Determine valuation status
        String valuationStatus = determineValuationStatus(fairValuePerShare, currentPrice);

        // Create DCF output
        DCFOutput dcfOutput = new DCFOutput(dcfInput.getTicker(), fairValuePerShare, currentPrice, valuationStatus);
        dcfOutput.setTerminalValue(terminalValue);
        dcfOutput.setPresentValueOfCashFlows(presentValueOfCashFlows);
        dcfOutput.setEnterpriseValue(enterpriseValue);
        dcfOutput.setEquityValue(equityValue);
        dcfOutput.setSharesOutstanding(sharesOutstanding);

        return dcfOutput;
    }

    /**
     * Project future free cash flows
     * 
     * @param baseFCF    the base free cash flow
     * @param growthRate the annual growth rate
     * @param years      the number of years to project
     * @return list of projected free cash flows
     */
    private List<BigDecimal> projectFreeCashFlows(BigDecimal baseFCF, BigDecimal growthRate, int years) {
        List<BigDecimal> projectedFCFs = new ArrayList<>();

        for (int i = 1; i <= years; i++) {
            BigDecimal projectedFCF = baseFCF.multiply(
                    optimizedMath.pow(BigDecimal.ONE.add(growthRate), i));
            projectedFCFs.add(projectedFCF);
        }

        return projectedFCFs;
    }

    /**
     * Calculate present value of cash flows
     * 
     * @param cashFlows    the list of future cash flows
     * @param discountRate the discount rate
     * @return present value of cash flows
     */
    private BigDecimal calculatePresentValue(List<BigDecimal> cashFlows, BigDecimal discountRate) {
        BigDecimal presentValue = BigDecimal.ZERO;

        for (int i = 0; i < cashFlows.size(); i++) {
            BigDecimal cashFlow = cashFlows.get(i);
            int year = i + 1;
            BigDecimal pv = cashFlow.divide(
                    optimizedMath.pow(BigDecimal.ONE.add(discountRate), year), 10, RoundingMode.HALF_UP);
            presentValue = presentValue.add(pv);
        }

        return presentValue;
    }

    /**
     * Get current stock price (realistic mock implementation)
     * 
     * @param ticker the ticker symbol
     * @return current stock price
     */
    private BigDecimal getCurrentStockPrice(String ticker) {
        // Realistic mock prices based on recent market data (as of August 2025)
        switch (ticker.toUpperCase()) {
            case "AAPL":
                return new BigDecimal("227.52"); // Apple Inc.
            case "GOOGL":
                return new BigDecimal("164.74"); // Alphabet Inc.
            case "GOOG":
                return new BigDecimal("166.21"); // Alphabet Inc. Class A
            case "MSFT":
                return new BigDecimal("428.75"); // Microsoft Corporation
            case "AMZN":
                return new BigDecimal("178.25"); // Amazon.com Inc.
            case "TSLA":
                return new BigDecimal("244.12"); // Tesla Inc.
            case "META":
                return new BigDecimal("503.22"); // Meta Platforms Inc.
            case "NVDA":
                return new BigDecimal("125.61"); // NVIDIA Corporation
            case "NFLX":
                return new BigDecimal("641.34"); // Netflix Inc.
            case "AMD":
                return new BigDecimal("144.58"); // Advanced Micro Devices
            case "INTC":
                return new BigDecimal("21.84"); // Intel Corporation
            case "CRM":
                return new BigDecimal("254.73"); // Salesforce Inc.
            case "ORCL":
                return new BigDecimal("138.45"); // Oracle Corporation
            case "ADBE":
                return new BigDecimal("556.78"); // Adobe Inc.
            case "PYPL":
                return new BigDecimal("64.23"); // PayPal Holdings Inc.
            default:
                // For unknown tickers, generate a reasonable price based on company size
                return generateReasonablePrice(ticker);
        }
    }

    /**
     * Generate a reasonable stock price for unknown tickers
     * 
     * @param ticker the ticker symbol
     * @return estimated stock price
     */
    private BigDecimal generateReasonablePrice(String ticker) {
        // Generate price based on ticker characteristics
        int hashCode = Math.abs(ticker.hashCode());

        // Most stocks trade between $10 and $500
        double basePrice = 50.0 + (hashCode % 200); // $50-$250 range

        // Add some randomness but keep it reasonable
        double variation = (hashCode % 20) - 10; // -$10 to +$10

        double finalPrice = Math.max(10.0, basePrice + variation); // Minimum $10
        return new BigDecimal(String.valueOf(finalPrice)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determine valuation status based on fair value vs current price
     * 
     * @param fairValue    the calculated fair value
     * @param currentPrice the current market price
     * @return valuation status string
     */
    private String determineValuationStatus(BigDecimal fairValue, BigDecimal currentPrice) {
        BigDecimal tolerance = new BigDecimal("0.05"); // 5% tolerance for "Fair Value"
        BigDecimal difference = fairValue.subtract(currentPrice).abs().divide(currentPrice, 10, RoundingMode.HALF_UP);

        if (difference.compareTo(tolerance) <= 0) {
            return "Fair Value";
        } else if (fairValue.compareTo(currentPrice) > 0) {
            return "Undervalued";
        } else {
            return "Overvalued";
        }
    }

    /**
     * Calculate DCF with sensitivity analysis
     * 
     * @param dcfInput          the base DCF input
     * @param growthRateRange   array of growth rates to test
     * @param discountRateRange array of discount rates to test
     * @return DCFSensitivityAnalysis with results
     */
    public DCFSensitivityAnalysis calculateSensitivityAnalysis(DCFInput dcfInput,
            BigDecimal[] growthRateRange,
            BigDecimal[] discountRateRange) throws DCFCalculationException {
        logger.info("Starting sensitivity analysis for ticker: {}", dcfInput.getTicker());

        try {
            FinancialData financialData = financialDataScrapingService.getFinancialData(dcfInput.getTicker());

            DCFSensitivityAnalysis analysis = new DCFSensitivityAnalysis();
            analysis.setTicker(dcfInput.getTicker());

            // Base case calculation
            DCFOutput baseCase = performDCFCalculation(dcfInput, financialData);
            analysis.setBaseCase(baseCase);

            // Sensitivity calculations
            List<DCFSensitivityResult> results = new ArrayList<>();

            for (BigDecimal growthRate : growthRateRange) {
                for (BigDecimal discountRate : discountRateRange) {
                    DCFInput sensitivityInput = createSensitivityInput(dcfInput, growthRate, discountRate);
                    DCFOutput sensitivityOutput = performDCFCalculation(sensitivityInput, financialData);

                    DCFSensitivityResult result = new DCFSensitivityResult();
                    result.setGrowthRate(growthRate.multiply(new BigDecimal("100"))); // Convert to percentage
                    result.setDiscountRate(discountRate.multiply(new BigDecimal("100"))); // Convert to percentage
                    result.setFairValue(sensitivityOutput.getFairValuePerShare());

                    results.add(result);
                }
            }

            analysis.setResults(results);
            return analysis;

        } catch (FinancialDataScrapingService.FinancialDataException e) {
            throw new DCFCalculationException(
                    "Failed to retrieve financial data for sensitivity analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Create a DCF input for sensitivity analysis
     * 
     * @param baseInput    the base DCF input
     * @param growthRate   the growth rate to test
     * @param discountRate the discount rate to test
     * @return new DCFInput with modified rates
     */
    private DCFInput createSensitivityInput(DCFInput baseInput, BigDecimal growthRate, BigDecimal discountRate) {
        DCFInput sensitivityInput = new DCFInput();
        sensitivityInput.setTicker(baseInput.getTicker());
        sensitivityInput.setDiscountRate(discountRate.multiply(new BigDecimal("100"))); // Convert from decimal to
                                                                                        // percentage
        sensitivityInput.setGrowthRate(growthRate.multiply(new BigDecimal("100"))); // Convert from decimal to
                                                                                    // percentage
        sensitivityInput.setTerminalGrowthRate(baseInput.getTerminalGrowthRate());
        sensitivityInput.setProjectionYears(baseInput.getProjectionYears());
        sensitivityInput.setUserId(baseInput.getUserId());

        return sensitivityInput;
    }

    /**
     * Get historical DCF calculations for a ticker
     * 
     * @param ticker the ticker symbol
     * @param userId the user ID (optional)
     * @return list of historical DCF outputs
     */
    public List<DCFOutput> getHistoricalCalculations(String ticker, String userId) {
        if (userId != null) {
            return dcfOutputRepository.findByUserIdAndTickerOrderByCalculatedAtDesc(userId, ticker);
        } else {
            return dcfOutputRepository.findByTickerOrderByCalculatedAtDesc(ticker);
        }
    }

    /**
     * Get DCF calculation statistics for a user
     * 
     * @param userId the user ID
     * @return DCFCalculationStats with user statistics
     */
    public DCFCalculationStats getUserCalculationStats(String userId) {
        long totalCalculations = dcfOutputRepository.countByUserId(userId);
        List<DCFOutput> userCalculations = dcfOutputRepository.findByUserIdOrderByCalculatedAtDesc(userId);

        long undervaluedCount = userCalculations.stream()
                .mapToLong(output -> "Undervalued".equals(output.getValuation()) ? 1 : 0)
                .sum();

        long overvaluedCount = userCalculations.stream()
                .mapToLong(output -> "Overvalued".equals(output.getValuation()) ? 1 : 0)
                .sum();

        return new DCFCalculationStats(totalCalculations, undervaluedCount, overvaluedCount);
    }

    /**
     * Custom exception for DCF calculation errors
     */
    public static class DCFCalculationException extends Exception {
        public DCFCalculationException(String message) {
            super(message);
        }

        public DCFCalculationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * DCF sensitivity analysis result
     */
    public static class DCFSensitivityAnalysis {
        private String ticker;
        private DCFOutput baseCase;
        private List<DCFSensitivityResult> results;

        // Getters and setters
        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }

        public DCFOutput getBaseCase() {
            return baseCase;
        }

        public void setBaseCase(DCFOutput baseCase) {
            this.baseCase = baseCase;
        }

        public List<DCFSensitivityResult> getResults() {
            return results;
        }

        public void setResults(List<DCFSensitivityResult> results) {
            this.results = results;
        }
    }

    /**
     * Individual sensitivity analysis result
     */
    public static class DCFSensitivityResult {
        private BigDecimal growthRate;
        private BigDecimal discountRate;
        private BigDecimal fairValue;

        // Getters and setters
        public BigDecimal getGrowthRate() {
            return growthRate;
        }

        public void setGrowthRate(BigDecimal growthRate) {
            this.growthRate = growthRate;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public void setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
        }

        public BigDecimal getFairValue() {
            return fairValue;
        }

        public void setFairValue(BigDecimal fairValue) {
            this.fairValue = fairValue;
        }
    }

    /**
     * DCF calculation statistics for a user
     */
    public static class DCFCalculationStats {
        private final long totalCalculations;
        private final long undervaluedCount;
        private final long overvaluedCount;

        public DCFCalculationStats(long totalCalculations, long undervaluedCount, long overvaluedCount) {
            this.totalCalculations = totalCalculations;
            this.undervaluedCount = undervaluedCount;
            this.overvaluedCount = overvaluedCount;
        }

        public long getTotalCalculations() {
            return totalCalculations;
        }

        public long getUndervaluedCount() {
            return undervaluedCount;
        }

        public long getOvervaluedCount() {
            return overvaluedCount;
        }

        public BigDecimal getUndervaluedPercentage() {
            return totalCalculations > 0
                    ? new BigDecimal(undervaluedCount)
                            .divide(new BigDecimal(totalCalculations), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
        }

        public BigDecimal getOvervaluedPercentage() {
            return totalCalculations > 0
                    ? new BigDecimal(overvaluedCount).divide(new BigDecimal(totalCalculations), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
        }
    }
}