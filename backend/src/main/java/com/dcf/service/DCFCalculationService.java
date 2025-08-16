package com.dcf.service;

import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.entity.FinancialData;
import com.dcf.repository.DCFInputRepository;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.util.DCFValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /**
     * Calculate DCF valuation for a given input
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
     * @param dcfInput the DCF input parameters
     * @param financialData the financial data
     * @return DCFOutput with calculated values
     */
    private DCFOutput performDCFCalculation(DCFInput dcfInput, FinancialData financialData) {
        logger.debug("Performing DCF calculation for ticker: {}", dcfInput.getTicker());

        // Extract parameters
        double discountRate = dcfInput.getDiscountRateAsDecimal();
        double growthRate = dcfInput.getGrowthRateAsDecimal();
        double terminalGrowthRate = dcfInput.getTerminalGrowthRateAsDecimal();
        int projectionYears = dcfInput.getProjectionYears();

        // Get latest free cash flow
        Double latestFCF = financialData.getLatestFreeCashFlow();
        if (latestFCF == null || latestFCF <= 0) {
            throw new IllegalArgumentException("Invalid or missing free cash flow data");
        }

        // Project future free cash flows
        List<Double> projectedFCFs = projectFreeCashFlows(latestFCF, growthRate, projectionYears);
        
        // Calculate present value of projected cash flows
        double presentValueOfCashFlows = calculatePresentValue(projectedFCFs, discountRate);
        
        // Calculate terminal value
        double terminalFCF = projectedFCFs.get(projectedFCFs.size() - 1) * (1 + terminalGrowthRate);
        double terminalValue = terminalFCF / (discountRate - terminalGrowthRate);
        double presentValueOfTerminalValue = terminalValue / Math.pow(1 + discountRate, projectionYears);
        
        // Calculate enterprise value
        double enterpriseValue = presentValueOfCashFlows + presentValueOfTerminalValue;
        
        // Calculate equity value (enterprise value - net debt)
        Double latestDebt = financialData.getLatestDebt();
        double netDebt = latestDebt != null ? latestDebt : 0.0;
        double equityValue = enterpriseValue - netDebt;
        
        // Calculate fair value per share
        Double sharesOutstanding = financialData.getLatestSharesOutstanding();
        if (sharesOutstanding == null || sharesOutstanding <= 0) {
            throw new IllegalArgumentException("Invalid or missing shares outstanding data");
        }
        
        double fairValuePerShare = equityValue / sharesOutstanding;
        
        // Get current stock price (mock for now)
        double currentPrice = getCurrentStockPrice(dcfInput.getTicker());
        
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
     * @param baseFCF the base free cash flow
     * @param growthRate the annual growth rate
     * @param years the number of years to project
     * @return list of projected free cash flows
     */
    private List<Double> projectFreeCashFlows(double baseFCF, double growthRate, int years) {
        List<Double> projectedFCFs = new ArrayList<>();
        
        for (int i = 1; i <= years; i++) {
            double projectedFCF = baseFCF * Math.pow(1 + growthRate, i);
            projectedFCFs.add(projectedFCF);
        }
        
        return projectedFCFs;
    }

    /**
     * Calculate present value of cash flows
     * @param cashFlows the list of future cash flows
     * @param discountRate the discount rate
     * @return present value of cash flows
     */
    private double calculatePresentValue(List<Double> cashFlows, double discountRate) {
        double presentValue = 0.0;
        
        for (int i = 0; i < cashFlows.size(); i++) {
            double cashFlow = cashFlows.get(i);
            int year = i + 1;
            double pv = cashFlow / Math.pow(1 + discountRate, year);
            presentValue += pv;
        }
        
        return presentValue;
    }

    /**
     * Get current stock price (realistic mock implementation)
     * @param ticker the ticker symbol
     * @return current stock price
     */
    private double getCurrentStockPrice(String ticker) {
        // Realistic mock prices based on recent market data (as of August 2025)
        switch (ticker.toUpperCase()) {
            case "AAPL":
                return 227.52; // Apple Inc.
            case "GOOGL":
                return 164.74; // Alphabet Inc.
            case "GOOG":
                return 166.21; // Alphabet Inc. Class A
            case "MSFT":
                return 428.75; // Microsoft Corporation
            case "AMZN":
                return 178.25; // Amazon.com Inc.
            case "TSLA":
                return 244.12; // Tesla Inc.
            case "META":
                return 503.22; // Meta Platforms Inc.
            case "NVDA":
                return 125.61; // NVIDIA Corporation
            case "NFLX":
                return 641.34; // Netflix Inc.
            case "AMD":
                return 144.58; // Advanced Micro Devices
            case "INTC":
                return 21.84; // Intel Corporation
            case "CRM":
                return 254.73; // Salesforce Inc.
            case "ORCL":
                return 138.45; // Oracle Corporation
            case "ADBE":
                return 556.78; // Adobe Inc.
            case "PYPL":
                return 64.23; // PayPal Holdings Inc.
            default:
                // For unknown tickers, generate a reasonable price based on company size
                return generateReasonablePrice(ticker);
        }
    }
    
    /**
     * Generate a reasonable stock price for unknown tickers
     * @param ticker the ticker symbol
     * @return estimated stock price
     */
    private double generateReasonablePrice(String ticker) {
        // Generate price based on ticker characteristics
        int hashCode = Math.abs(ticker.hashCode());
        
        // Most stocks trade between $10 and $500
        double basePrice = 50.0 + (hashCode % 200); // $50-$250 range
        
        // Add some randomness but keep it reasonable
        double variation = (hashCode % 20) - 10; // -$10 to +$10
        
        return Math.max(10.0, basePrice + variation); // Minimum $10
    }

    /**
     * Determine valuation status based on fair value vs current price
     * @param fairValue the calculated fair value
     * @param currentPrice the current market price
     * @return valuation status string
     */
    private String determineValuationStatus(double fairValue, double currentPrice) {
        double tolerance = 0.05; // 5% tolerance for "Fair Value"
        double difference = Math.abs(fairValue - currentPrice) / currentPrice;
        
        if (difference <= tolerance) {
            return "Fair Value";
        } else if (fairValue > currentPrice) {
            return "Undervalued";
        } else {
            return "Overvalued";
        }
    }

    /**
     * Calculate DCF with sensitivity analysis
     * @param dcfInput the base DCF input
     * @param growthRateRange array of growth rates to test
     * @param discountRateRange array of discount rates to test
     * @return DCFSensitivityAnalysis with results
     */
    public DCFSensitivityAnalysis calculateSensitivityAnalysis(DCFInput dcfInput, 
                                                              double[] growthRateRange, 
                                                              double[] discountRateRange) throws DCFCalculationException {
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
            
            for (double growthRate : growthRateRange) {
                for (double discountRate : discountRateRange) {
                    DCFInput sensitivityInput = createSensitivityInput(dcfInput, growthRate, discountRate);
                    DCFOutput sensitivityOutput = performDCFCalculation(sensitivityInput, financialData);
                    
                    DCFSensitivityResult result = new DCFSensitivityResult();
                    result.setGrowthRate(growthRate * 100); // Convert to percentage
                    result.setDiscountRate(discountRate * 100); // Convert to percentage
                    result.setFairValue(sensitivityOutput.getFairValuePerShare());
                    
                    results.add(result);
                }
            }
            
            analysis.setResults(results);
            return analysis;
            
        } catch (FinancialDataScrapingService.FinancialDataException e) {
            throw new DCFCalculationException("Failed to retrieve financial data for sensitivity analysis: " + e.getMessage(), e);
        }
    }

    /**
     * Create a DCF input for sensitivity analysis
     * @param baseInput the base DCF input
     * @param growthRate the growth rate to test
     * @param discountRate the discount rate to test
     * @return new DCFInput with modified rates
     */
    private DCFInput createSensitivityInput(DCFInput baseInput, double growthRate, double discountRate) {
        DCFInput sensitivityInput = new DCFInput();
        sensitivityInput.setTicker(baseInput.getTicker());
        sensitivityInput.setDiscountRate(discountRate * 100); // Convert from decimal to percentage
        sensitivityInput.setGrowthRate(growthRate * 100); // Convert from decimal to percentage
        sensitivityInput.setTerminalGrowthRate(baseInput.getTerminalGrowthRate());
        sensitivityInput.setProjectionYears(baseInput.getProjectionYears());
        sensitivityInput.setUserId(baseInput.getUserId());
        
        return sensitivityInput;
    }

    /**
     * Get historical DCF calculations for a ticker
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
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }
        
        public DCFOutput getBaseCase() { return baseCase; }
        public void setBaseCase(DCFOutput baseCase) { this.baseCase = baseCase; }
        
        public List<DCFSensitivityResult> getResults() { return results; }
        public void setResults(List<DCFSensitivityResult> results) { this.results = results; }
    }

    /**
     * Individual sensitivity analysis result
     */
    public static class DCFSensitivityResult {
        private double growthRate;
        private double discountRate;
        private double fairValue;

        // Getters and setters
        public double getGrowthRate() { return growthRate; }
        public void setGrowthRate(double growthRate) { this.growthRate = growthRate; }
        
        public double getDiscountRate() { return discountRate; }
        public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }
        
        public double getFairValue() { return fairValue; }
        public void setFairValue(double fairValue) { this.fairValue = fairValue; }
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

        public long getTotalCalculations() { return totalCalculations; }
        public long getUndervaluedCount() { return undervaluedCount; }
        public long getOvervaluedCount() { return overvaluedCount; }
        
        public double getUndervaluedPercentage() {
            return totalCalculations > 0 ? (double) undervaluedCount / totalCalculations * 100 : 0;
        }
        
        public double getOvervaluedPercentage() {
            return totalCalculations > 0 ? (double) overvaluedCount / totalCalculations * 100 : 0;
        }
    }
}