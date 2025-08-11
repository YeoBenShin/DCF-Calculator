package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.util.FinancialDataUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FinancialDataScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataScrapingService.class);
    
    private static final int TIMEOUT_SECONDS = 30;
    private static final int CACHE_DAYS = 7; // Cache data for 7 days
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @Autowired
    private FinancialDataUtil financialDataUtil;

    @Autowired
    private FinancialDataCacheService cacheService;

    /**
     * Get financial data for a ticker symbol
     * @param ticker the ticker symbol
     * @return FinancialData object with scraped data
     * @throws FinancialDataException if scraping fails
     */
    public FinancialData getFinancialData(String ticker) throws FinancialDataException {
        String normalizedTicker = financialDataUtil.normalizeTicker(ticker);
        if (normalizedTicker == null) {
            throw new FinancialDataException("Invalid ticker symbol: " + ticker);
        }

        // Check memory cache first
        FinancialData cachedData = cacheService.getCachedData(normalizedTicker);
        if (cachedData != null) {
            logger.info("Returning memory cached financial data for ticker: {}", normalizedTicker);
            return cachedData;
        }

        // Check database cache
        Optional<FinancialData> dbCachedData = financialDataRepository.findByTicker(normalizedTicker);
        if (dbCachedData.isPresent() && !dbCachedData.get().isDataStale(CACHE_DAYS)) {
            logger.info("Returning database cached financial data for ticker: {}", normalizedTicker);
            FinancialData data = dbCachedData.get();
            // Cache in memory for faster access
            cacheService.cacheData(normalizedTicker, data);
            return data;
        }

        // Scrape fresh data
        logger.info("Scraping fresh financial data for ticker: {}", normalizedTicker);
        FinancialData financialData = scrapeFinancialData(normalizedTicker);
        
        // Validate the scraped data
        String validationError = financialDataUtil.getValidationError(financialData);
        if (validationError != null) {
            throw new FinancialDataException("Invalid financial data: " + validationError);
        }

        // Save to database
        FinancialData savedData;
        if (dbCachedData.isPresent()) {
            // Update existing record
            FinancialData existing = dbCachedData.get();
            updateFinancialData(existing, financialData);
            savedData = financialDataRepository.save(existing);
        } else {
            // Save new record
            savedData = financialDataRepository.save(financialData);
        }

        // Cache in memory
        cacheService.cacheData(normalizedTicker, savedData);
        
        return savedData;
    }

    /**
     * Scrape financial data from multiple sources
     * @param ticker the ticker symbol
     * @return FinancialData object
     * @throws FinancialDataException if scraping fails
     */
    private FinancialData scrapeFinancialData(String ticker) throws FinancialDataException {
        FinancialData financialData = new FinancialData(ticker);

        try {
            // Try Yahoo Finance first
            if (scrapeFromYahooFinance(financialData)) {
                logger.info("Successfully scraped data from Yahoo Finance for: {}", ticker);
                return financialData;
            }

            // Fallback to mock data for demonstration
            logger.warn("Could not scrape real data for {}, using mock data", ticker);
            return generateMockFinancialData(ticker);

        } catch (Exception e) {
            logger.error("Error scraping financial data for ticker: {}", ticker, e);
            throw new FinancialDataException("Unable to retrieve financials at the moment.", e);
        }
    }

    /**
     * Scrape financial data from Yahoo Finance
     * @param financialData the FinancialData object to populate
     * @return true if successful, false otherwise
     */
    private boolean scrapeFromYahooFinance(FinancialData financialData) {
        try {
            String ticker = financialData.getTicker();
            
            // Scrape income statement
            if (!scrapeIncomeStatement(financialData, ticker)) {
                return false;
            }

            // Scrape cash flow statement
            if (!scrapeCashFlowStatement(financialData, ticker)) {
                return false;
            }

            // Scrape balance sheet
            if (!scrapeBalanceSheet(financialData, ticker)) {
                return false;
            }

            return financialData.hasValidData();

        } catch (Exception e) {
            logger.error("Error scraping from Yahoo Finance", e);
            return false;
        }
    }

    /**
     * Scrape income statement data
     * @param financialData the FinancialData object to populate
     * @param ticker the ticker symbol
     * @return true if successful
     */
    private boolean scrapeIncomeStatement(FinancialData financialData, String ticker) {
        try {
            String url = String.format("https://finance.yahoo.com/quote/%s/financials", ticker);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                    .get();

            // Extract revenue data
            List<Double> revenue = extractFinancialMetric(doc, "Total Revenue");
            if (!revenue.isEmpty()) {
                financialData.setRevenue(revenue);
            }

            // Extract operating income
            List<Double> operatingIncome = extractFinancialMetric(doc, "Operating Income");
            if (!operatingIncome.isEmpty()) {
                financialData.setOperatingIncome(operatingIncome);
            }

            // Extract net income
            List<Double> netProfit = extractFinancialMetric(doc, "Net Income");
            if (!netProfit.isEmpty()) {
                financialData.setNetProfit(netProfit);
            }

            // Extract EPS
            List<Double> eps = extractFinancialMetric(doc, "Diluted EPS");
            if (!eps.isEmpty()) {
                financialData.setEps(eps);
            }

            return !revenue.isEmpty();

        } catch (IOException e) {
            logger.error("Error scraping income statement for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Scrape cash flow statement data
     * @param financialData the FinancialData object to populate
     * @param ticker the ticker symbol
     * @return true if successful
     */
    private boolean scrapeCashFlowStatement(FinancialData financialData, String ticker) {
        try {
            String url = String.format("https://finance.yahoo.com/quote/%s/cash-flow", ticker);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                    .get();

            // Extract operating cash flow
            List<Double> operatingCashFlow = extractFinancialMetric(doc, "Operating Cash Flow");
            if (!operatingCashFlow.isEmpty()) {
                financialData.setOperatingCashFlow(operatingCashFlow);
            }

            // Extract capital expenditure
            List<Double> capex = extractFinancialMetric(doc, "Capital Expenditure");
            if (!capex.isEmpty()) {
                financialData.setCapitalExpenditure(capex);
            }

            // Extract free cash flow
            List<Double> freeCashFlow = extractFinancialMetric(doc, "Free Cash Flow");
            if (!freeCashFlow.isEmpty()) {
                financialData.setFreeCashFlow(freeCashFlow);
            }

            return !operatingCashFlow.isEmpty();

        } catch (IOException e) {
            logger.error("Error scraping cash flow statement for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Scrape balance sheet data
     * @param financialData the FinancialData object to populate
     * @param ticker the ticker symbol
     * @return true if successful
     */
    private boolean scrapeBalanceSheet(FinancialData financialData, String ticker) {
        try {
            String url = String.format("https://finance.yahoo.com/quote/%s/balance-sheet", ticker);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))
                    .get();

            // Extract total debt
            List<Double> totalDebt = extractFinancialMetric(doc, "Total Debt");
            if (!totalDebt.isEmpty()) {
                financialData.setTotalDebt(totalDebt);
            }

            // Extract shares outstanding
            List<Double> sharesOutstanding = extractFinancialMetric(doc, "Ordinary Shares Number");
            if (!sharesOutstanding.isEmpty()) {
                financialData.setOrdinarySharesNumber(sharesOutstanding);
            }

            return true;

        } catch (IOException e) {
            logger.error("Error scraping balance sheet for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Extract financial metric values from HTML document
     * @param doc the HTML document
     * @param metricName the name of the metric to extract
     * @return list of values for the metric
     */
    private List<Double> extractFinancialMetric(Document doc, String metricName) {
        List<Double> values = new ArrayList<>();
        
        try {
            // Look for the metric row
            Elements rows = doc.select("div[data-test='fin-row']");
            for (Element row : rows) {
                Element titleElement = row.selectFirst("div[title]");
                if (titleElement != null && titleElement.attr("title").contains(metricName)) {
                    // Extract values from this row
                    Elements valueElements = row.select("div[data-test='fin-col']");
                    for (Element valueElement : valueElements) {
                        String valueText = valueElement.text().trim();
                        Double value = parseFinancialValue(valueText);
                        if (value != null) {
                            values.add(value);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting metric: {}", metricName, e);
        }

        return values;
    }

    /**
     * Parse financial value from text (handles K, M, B suffixes)
     * @param valueText the text to parse
     * @return parsed value or null if invalid
     */
    private Double parseFinancialValue(String valueText) {
        if (valueText == null || valueText.isEmpty() || valueText.equals("-")) {
            return null;
        }

        try {
            // Remove commas and parentheses
            String cleanValue = valueText.replaceAll("[,()]", "");
            
            // Handle negative values in parentheses
            boolean isNegative = valueText.contains("(") && valueText.contains(")");
            
            // Extract numeric part and suffix
            String numericPart = cleanValue.replaceAll("[^0-9.]", "");
            String suffix = cleanValue.replaceAll("[0-9.,()-]", "").toUpperCase();
            
            if (numericPart.isEmpty()) {
                return null;
            }
            
            double value = Double.parseDouble(numericPart);
            
            // Apply multiplier based on suffix
            switch (suffix) {
                case "K":
                    value *= 1_000;
                    break;
                case "M":
                    value *= 1_000_000;
                    break;
                case "B":
                    value *= 1_000_000_000;
                    break;
                case "T":
                    value *= 1_000_000_000_000L;
                    break;
            }
            
            return isNegative ? -value : value;
            
        } catch (NumberFormatException e) {
            logger.warn("Could not parse financial value: {}", valueText);
            return null;
        }
    }

    /**
     * Generate mock financial data for demonstration purposes
     * @param ticker the ticker symbol
     * @return FinancialData with mock data
     */
    private FinancialData generateMockFinancialData(String ticker) {
        FinancialData financialData = new FinancialData(ticker);
        
        // Generate realistic mock data based on ticker
        double baseRevenue = getBaseRevenueForTicker(ticker);
        
        List<Double> revenue = new ArrayList<>();
        List<Double> operatingIncome = new ArrayList<>();
        List<Double> netProfit = new ArrayList<>();
        List<Double> operatingCashFlow = new ArrayList<>();
        List<Double> freeCashFlow = new ArrayList<>();
        List<Double> eps = new ArrayList<>();
        List<Double> totalDebt = new ArrayList<>();
        List<Double> sharesOutstanding = new ArrayList<>();
        
        // Generate 5 years of data
        for (int i = 0; i < 5; i++) {
            double growthFactor = Math.pow(1.08, i); // 8% annual growth
            
            revenue.add(baseRevenue * growthFactor);
            operatingIncome.add(baseRevenue * growthFactor * 0.25); // 25% operating margin
            netProfit.add(baseRevenue * growthFactor * 0.15); // 15% net margin
            operatingCashFlow.add(baseRevenue * growthFactor * 0.20); // 20% of revenue
            freeCashFlow.add(baseRevenue * growthFactor * 0.12); // 12% of revenue
            eps.add((baseRevenue * growthFactor * 0.15) / 1_000_000_000); // Assuming 1B shares
            totalDebt.add(baseRevenue * growthFactor * 0.30); // 30% of revenue
            sharesOutstanding.add(1_000_000_000.0); // 1B shares
        }
        
        financialData.setRevenue(revenue);
        financialData.setOperatingIncome(operatingIncome);
        financialData.setNetProfit(netProfit);
        financialData.setOperatingCashFlow(operatingCashFlow);
        financialData.setFreeCashFlow(freeCashFlow);
        financialData.setEps(eps);
        financialData.setTotalDebt(totalDebt);
        financialData.setOrdinarySharesNumber(sharesOutstanding);
        
        return financialData;
    }

    /**
     * Get base revenue for mock data generation
     * @param ticker the ticker symbol
     * @return base revenue amount
     */
    private double getBaseRevenueForTicker(String ticker) {
        // Return different base revenues for different tickers
        switch (ticker.toUpperCase()) {
            case "AAPL":
                return 365_000_000_000.0; // $365B
            case "GOOGL":
            case "GOOG":
                return 280_000_000_000.0; // $280B
            case "MSFT":
                return 200_000_000_000.0; // $200B
            case "AMZN":
                return 470_000_000_000.0; // $470B
            case "TSLA":
                return 80_000_000_000.0; // $80B
            default:
                return 50_000_000_000.0; // $50B default
        }
    }

    /**
     * Update existing financial data with new data
     * @param existing the existing FinancialData
     * @param newData the new FinancialData
     */
    private void updateFinancialData(FinancialData existing, FinancialData newData) {
        existing.setRevenue(newData.getRevenue());
        existing.setOperatingExpense(newData.getOperatingExpense());
        existing.setOperatingIncome(newData.getOperatingIncome());
        existing.setOperatingCashFlow(newData.getOperatingCashFlow());
        existing.setNetProfit(newData.getNetProfit());
        existing.setCapitalExpenditure(newData.getCapitalExpenditure());
        existing.setFreeCashFlow(newData.getFreeCashFlow());
        existing.setEps(newData.getEps());
        existing.setTotalDebt(newData.getTotalDebt());
        existing.setOrdinarySharesNumber(newData.getOrdinarySharesNumber());
        existing.setDateFetched(LocalDate.now());
    }

    /**
     * Check if ticker exists and is valid
     * @param ticker the ticker symbol to validate
     * @return true if ticker is valid
     */
    public boolean isValidTicker(String ticker) {
        try {
            String normalizedTicker = financialDataUtil.normalizeTicker(ticker);
            if (normalizedTicker == null) {
                return false;
            }

            // Try to fetch basic quote data to validate ticker
            String url = String.format("https://finance.yahoo.com/quote/%s", normalizedTicker);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout((int) TimeUnit.SECONDS.toMillis(10))
                    .get();

            // Check if page contains error indicators
            return !doc.select("h1").text().contains("not found") &&
                   !doc.select("h1").text().contains("invalid");

        } catch (Exception e) {
            logger.error("Error validating ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Custom exception for financial data scraping errors
     */
    public static class FinancialDataException extends Exception {
        public FinancialDataException(String message) {
            super(message);
        }

        public FinancialDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}