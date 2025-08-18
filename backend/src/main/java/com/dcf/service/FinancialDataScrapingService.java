package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.util.FinancialDataUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FinancialDataScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataScrapingService.class);

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
     * 
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

        // Normalize data arrays to have consistent lengths
        normalizeDataArrays(financialData);

        // Validate the scraped data
        String validationError = financialDataUtil.getValidationError(financialData);
        if (validationError != null) {
            logger.error("Validation failed for ticker {}: {}", normalizedTicker, validationError);
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
     * 
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
     * 
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
     * 
     * @param financialData the FinancialData object to populate
     * @param ticker        the ticker symbol
     * @return true if successful
     */
    private boolean scrapeIncomeStatement(FinancialData financialData, String ticker) {
        try (Playwright playwright = Playwright.create()) {
            String url = String.format("https://finance.yahoo.com/quote/%s/financials", ticker);

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            context.route("**/*.{png,jpg,jpeg,svg,woff,css}", route -> route.abort());
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED) // Wait for DOM content only, faster than full load
                    .setTimeout(30000));

            // Extract revenue data
            List<BigDecimal> revenue = extractFinancialMetric(page, "Total Revenue");
            if (!revenue.isEmpty()) {
                financialData.setRevenue(revenue);
            }

            // Extract operating income
            List<BigDecimal> operatingIncome = extractFinancialMetric(page, "Operating Income");
            if (!operatingIncome.isEmpty()) {
                financialData.setOperatingIncome(operatingIncome);
            }

            // Extract net income
            List<BigDecimal> netProfit = extractFinancialMetric(page, "Net Income");
            if (!netProfit.isEmpty()) {
                financialData.setNetProfit(netProfit);
            }

            // Extract EPS
            List<BigDecimal> eps = extractFinancialMetric(page, "Diluted EPS");
            if (!eps.isEmpty()) {
                financialData.setEps(eps);
            }

            return !revenue.isEmpty();

        } catch (Exception e) {
            logger.error("Error scraping income statement for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Scrape cash flow statement data
     * 
     * @param financialData the FinancialData object to populate
     * @param ticker        the ticker symbol
     * @return true if successful
     */
    private boolean scrapeCashFlowStatement(FinancialData financialData, String ticker) {
        try (Playwright playwright = Playwright.create()) {
            String url = String.format("https://finance.yahoo.com/quote/%s/cash-flow", ticker);

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            context.route("**/*.{png,jpg,jpeg,svg,woff,css}", route -> route.abort());
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED) // Wait for DOM content only, faster than full load
                    .setTimeout(30000));

            // Extract operating cash flow
            List<BigDecimal> operatingCashFlow = extractFinancialMetric(page, "Operating Cash Flow");
            if (!operatingCashFlow.isEmpty()) {
                financialData.setOperatingCashFlow(operatingCashFlow);
            }

            // Extract capital expenditure
            List<BigDecimal> capex = extractFinancialMetric(page, "Capital Expenditure");
            if (!capex.isEmpty()) {
                financialData.setCapitalExpenditure(capex);
            }

            // Extract free cash flow
            List<BigDecimal> freeCashFlow = extractFinancialMetric(page, "Free Cash Flow");
            if (!freeCashFlow.isEmpty()) {
                financialData.setFreeCashFlow(freeCashFlow);
            }

            return !operatingCashFlow.isEmpty();

        } catch (Exception e) {
            logger.error("Error scraping cash flow statement for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Scrape balance sheet data
     * 
     * @param financialData the FinancialData object to populate
     * @param ticker        the ticker symbol
     * @return true if successful
     */
    private boolean scrapeBalanceSheet(FinancialData financialData, String ticker) {
        try (Playwright playwright = Playwright.create()) {
            String url = String.format("https://finance.yahoo.com/quote/%s/balance-sheet", ticker);

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            context.route("**/*.{png,jpg,jpeg,svg,woff,css}", route -> route.abort());
            Page page = context.newPage();
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED) // Wait for DOM content only, faster than full load
                    .setTimeout(30000));

            // Extract total debt
            List<BigDecimal> totalDebt = extractFinancialMetric(page, "Total Debt");
            if (!totalDebt.isEmpty()) {
                financialData.setTotalDebt(totalDebt);
            }

            // Extract shares outstanding
            List<BigDecimal> sharesOutstanding = extractFinancialMetric(page, "Ordinary Shares Number");
            if (!sharesOutstanding.isEmpty()) {
                financialData.setOrdinarySharesNumber(sharesOutstanding);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error scraping balance sheet for ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Extract financial metric values from HTML document
     * 
     * @param doc        the HTML document
     * @param metricName the name of the metric to extract
     * @return list of values for the metric
     */
    private List<BigDecimal> extractFinancialMetric(Page page, String metricName) {
        List<BigDecimal> values = new ArrayList<>();

        try {
            String xmlSelector = null;
            switch (metricName) {
                case "Total Revenue":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[1]";
                    break;
                case "Operating Income":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[5]";
                    break;
                case "Net Income":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[11]";
                    break;
                case "Diluted EPS":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[13]";
                    break;
                case "Operating Cash Flow":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[1]";
                    break;
                case "Capital Expenditure":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[7]";
                    break;
                case "Free Cash Flow":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[12]"; // different
                                                                                                              // stock
                                                                                                              // got
                                                                                                              // different
                                                                                                              // number
                                                                                                              // of
                                                                                                              // items
                    break;
                case "Total Debt":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[11]";
                    break;
                case "Ordinary Shares Number":
                    xmlSelector = "//*[@id=\"main-content-wrapper\"]/article/section/div/div/div[2]/div[13]";
                    break;
                default:

            }
            if (xmlSelector != null) {
                Locator rows = page.locator(xmlSelector);
                List<String> valueList = rows.allTextContents();
                logger.info("valueList that is being passed back: {}", valueList);
                if (valueList.size() <= 0) {
                    return values;
                }

                String[] valueArray = valueList.get(0).split(" ");
                for (String valueText : valueArray) {
                    BigDecimal value = parseFinancialValue(valueText);
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting metric: {}", metricName, e);
        }

        logger.info("{} values that is being passed back: {}", metricName, values);
        return values;
    }

    /**
     * Parse financial value from text (handles K, M, B suffixes and scientific
     * notation)
     * 
     * @param valueText the text to parse
     * @return parsed value or null if invalid
     */
    private BigDecimal parseFinancialValue(String valueText) {
        if (valueText == null || valueText.isEmpty() || valueText.equals("-") || valueText.equals("--")) {
            return null;
        }

        try {
            // Remove commas and parentheses, preserve negative sign
            String cleanValue = valueText.replaceAll("[,()]", "").trim();

            // Handle negative values (check for minus sign or parentheses in original)
            // boolean isNegative = valueText.contains("-") || (valueText.contains("(") &&
            // valueText.contains(")"));

            // Remove any remaining non-numeric characters except decimal point, and +/-
            String numericPart = cleanValue.replaceAll("[^0-9.+-]", "");
            if (numericPart.isEmpty()) {
                return null;
            }
            BigDecimal value = new BigDecimal(numericPart);

            // logger.info("isNegative value: {} || value that is being pssed back {}",
            // isNegative, value);
            // // Handle scientific notation and regular decimal numbers
            // BigDecimal value;
            // if (numericPart.toUpperCase().contains("E")) {
            // // Scientific notation - BigDecimal can handle this directly
            // value = new BigDecimal(numericPart);
            // } else {
            // // Regular decimal number
            // value = new BigDecimal(numericPart);
            // }

            // // Handle suffix multipliers (K, M, B, T)
            // String upperValue = cleanValue.toUpperCase();
            // if (upperValue.contains("K")) {
            // value = value.multiply(new BigDecimal("1000"));
            // } else if (upperValue.contains("M")) {
            // value = value.multiply(new BigDecimal("1000000"));
            // } else if (upperValue.contains("B")) {
            // value = value.multiply(new BigDecimal("1000000000"));
            // } else if (upperValue.contains("T")) {
            // value = value.multiply(new BigDecimal("1000000000000"));
            // }

            // logger.info("negative value {}", value.negate());

            return value;

        } catch (NumberFormatException e) {
            logger.warn("Could not parse financial value: '{}' - {}", valueText, e.getMessage());
            return null;
        }
    }

    /**
     * Generate realistic financial data based on actual company data using
     * BigDecimal arithmetic
     * 
     * @param ticker the ticker symbol
     * @return FinancialData with realistic data
     */
    private FinancialData generateMockFinancialData(String ticker) {
        FinancialData financialData = new FinancialData(ticker);

        // Use more realistic data based on actual company financials
        CompanyFinancials companyData = getRealisticCompanyData(ticker);

        List<BigDecimal> revenue = new ArrayList<>();
        List<BigDecimal> operatingIncome = new ArrayList<>();
        List<BigDecimal> netProfit = new ArrayList<>();
        List<BigDecimal> operatingCashFlow = new ArrayList<>();
        List<BigDecimal> freeCashFlow = new ArrayList<>();
        List<BigDecimal> eps = new ArrayList<>();
        List<BigDecimal> totalDebt = new ArrayList<>();
        List<BigDecimal> sharesOutstanding = new ArrayList<>();

        // Generate 4 years of historical data (most recent first)
        for (int i = 3; i >= 0; i--) {
            // Calculate year factor using BigDecimal arithmetic for precision
            BigDecimal yearFactor = companyData.growthRate.pow(i);

            // Calculate financial metrics using BigDecimal operations
            BigDecimal yearRevenue = companyData.baseRevenue.multiply(yearFactor);
            revenue.add(yearRevenue);

            operatingIncome.add(yearRevenue.multiply(companyData.operatingMargin));
            netProfit.add(yearRevenue.multiply(companyData.netMargin));
            operatingCashFlow.add(yearRevenue.multiply(companyData.cashFlowMargin));
            freeCashFlow.add(yearRevenue.multiply(companyData.fcfMargin));

            // Calculate EPS: (revenue * netMargin) / sharesOutstanding
            BigDecimal netIncome = yearRevenue.multiply(companyData.netMargin);
            BigDecimal epsValue = netIncome.divide(companyData.sharesOutstanding, 6, RoundingMode.HALF_UP);
            eps.add(epsValue);

            // Debt and shares remain constant across years
            totalDebt.add(companyData.totalDebt);
            sharesOutstanding.add(companyData.sharesOutstanding);
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
     * Get realistic company financial data using BigDecimal for precision
     * 
     * @param ticker the ticker symbol
     * @return CompanyFinancials object with realistic data
     */
    private CompanyFinancials getRealisticCompanyData(String ticker) {
        switch (ticker.toUpperCase()) {
            case "AAPL":
                return new CompanyFinancials(
                        "394328000000", // Revenue (2023)
                        "0.30", // Operating margin
                        "0.25", // Net margin
                        "0.28", // Cash flow margin
                        "0.24", // FCF margin
                        "15728700000", // Shares outstanding
                        "111100000000", // Total debt
                        "1.08" // Growth rate
                );
            case "GOOGL":
            case "GOOG":
                return new CompanyFinancials(
                        "307394000000", // Revenue (2023)
                        "0.25", // Operating margin
                        "0.21", // Net margin
                        "0.30", // Cash flow margin
                        "0.25", // FCF margin
                        "12700000000", // Shares outstanding
                        "28300000000", // Total debt
                        "1.12" // Growth rate
                );
            case "MSFT":
                return new CompanyFinancials(
                        "211915000000", // Revenue (2023)
                        "0.42", // Operating margin
                        "0.36", // Net margin
                        "0.35", // Cash flow margin
                        "0.28", // FCF margin
                        "7430000000", // Shares outstanding
                        "47032000000", // Total debt
                        "1.10" // Growth rate
                );
            case "AMZN":
                return new CompanyFinancials(
                        "574785000000", // Revenue (2023)
                        "0.05", // Operating margin
                        "0.02", // Net margin
                        "0.15", // Cash flow margin
                        "0.08", // FCF margin
                        "10757000000", // Shares outstanding
                        "67150000000", // Total debt
                        "1.15" // Growth rate
                );
            case "TSLA":
                return new CompanyFinancials(
                        "96773000000", // Revenue (2023)
                        "0.08", // Operating margin
                        "0.15", // Net margin
                        "0.12", // Cash flow margin
                        "0.08", // FCF margin
                        "3178000000", // Shares outstanding
                        "9566000000", // Total debt
                        "1.20" // Growth rate
                );
            case "META":
                return new CompanyFinancials(
                        "134902000000", // Revenue (2023)
                        "0.29", // Operating margin
                        "0.26", // Net margin
                        "0.32", // Cash flow margin
                        "0.28", // FCF margin
                        "2587000000", // Shares outstanding
                        "18385000000", // Total debt
                        "1.11" // Growth rate
                );
            case "NVDA":
                return new CompanyFinancials(
                        "60922000000", // Revenue (2023)
                        "0.32", // Operating margin
                        "0.49", // Net margin
                        "0.35", // Cash flow margin
                        "0.30", // FCF margin
                        "24700000000", // Shares outstanding
                        "9706000000", // Total debt
                        "1.35" // Growth rate
                );
            default:
                return new CompanyFinancials(
                        "50000000000", // Default revenue
                        "0.15", // Operating margin
                        "0.10", // Net margin
                        "0.18", // Cash flow margin
                        "0.12", // FCF margin
                        "1000000000", // Shares outstanding
                        "20000000000", // Total debt
                        "1.08" // Growth rate
                );
        }
    }

    /**
     * Helper class to store company financial data using BigDecimal for precision
     */
    private static class CompanyFinancials {
        final BigDecimal baseRevenue;
        final BigDecimal operatingMargin;
        final BigDecimal netMargin;
        final BigDecimal cashFlowMargin;
        final BigDecimal fcfMargin;
        final BigDecimal sharesOutstanding;
        final BigDecimal totalDebt;
        final BigDecimal growthRate;

        CompanyFinancials(String baseRevenue, String operatingMargin, String netMargin,
                String cashFlowMargin, String fcfMargin, String sharesOutstanding,
                String totalDebt, String growthRate) {
            this.baseRevenue = new BigDecimal(baseRevenue);
            this.operatingMargin = new BigDecimal(operatingMargin);
            this.netMargin = new BigDecimal(netMargin);
            this.cashFlowMargin = new BigDecimal(cashFlowMargin);
            this.fcfMargin = new BigDecimal(fcfMargin);
            this.sharesOutstanding = new BigDecimal(sharesOutstanding);
            this.totalDebt = new BigDecimal(totalDebt);
            this.growthRate = new BigDecimal(growthRate);
        }
    }

    /**
     * Update existing financial data with new data
     * 
     * @param existing the existing FinancialData
     * @param newData  the new FinancialData
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
     * 
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
     * Log the sizes of scraped data arrays for debugging
     * 
     * @param financialData the financial data to log
     */
    private void logScrapedDataSizes(FinancialData financialData) {
        logger.info(
                "Scraped data sizes for ticker {}: Revenue={}, OperatingExpense={}, OperatingIncome={}, NetProfit={}, "
                        +
                        "OperatingCashFlow={}, CapitalExpenditure={}, FreeCashFlow={}, EPS={}, TotalDebt={}, SharesOutstanding={}",
                financialData.getTicker(),
                financialData.getRevenue().size(),
                financialData.getOperatingExpense().size(),
                financialData.getOperatingIncome().size(),
                financialData.getNetProfit().size(),
                financialData.getOperatingCashFlow().size(),
                financialData.getCapitalExpenditure().size(),
                financialData.getFreeCashFlow().size(),
                financialData.getEps().size(),
                financialData.getTotalDebt().size(),
                financialData.getOrdinarySharesNumber().size());
    }

    /**
     * Normalize data arrays to have consistent lengths by truncating to the
     * shortest non-empty array
     * 
     * @param financialData the financial data to normalize
     */
    private void normalizeDataArrays(FinancialData financialData) {
        // Find the minimum length among non-empty arrays
        int minLength = Integer.MAX_VALUE;

        if (!financialData.getRevenue().isEmpty()) {
            minLength = Math.min(minLength, financialData.getRevenue().size());
        }
        if (!financialData.getOperatingExpense().isEmpty()) {
            minLength = Math.min(minLength, financialData.getOperatingExpense().size());
        }
        if (!financialData.getOperatingIncome().isEmpty()) {
            minLength = Math.min(minLength, financialData.getOperatingIncome().size());
        }
        if (!financialData.getNetProfit().isEmpty()) {
            minLength = Math.min(minLength, financialData.getNetProfit().size());
        }
        if (!financialData.getOperatingCashFlow().isEmpty()) {
            minLength = Math.min(minLength, financialData.getOperatingCashFlow().size());
        }
        if (!financialData.getCapitalExpenditure().isEmpty()) {
            minLength = Math.min(minLength, financialData.getCapitalExpenditure().size());
        }
        if (!financialData.getFreeCashFlow().isEmpty()) {
            minLength = Math.min(minLength, financialData.getFreeCashFlow().size());
        }
        if (!financialData.getEps().isEmpty()) {
            minLength = Math.min(minLength, financialData.getEps().size());
        }
        if (!financialData.getTotalDebt().isEmpty()) {
            minLength = Math.min(minLength, financialData.getTotalDebt().size());
        }
        if (!financialData.getOrdinarySharesNumber().isEmpty()) {
            minLength = Math.min(minLength, financialData.getOrdinarySharesNumber().size());
        }

        // If no data found or minLength is still MAX_VALUE, return
        if (minLength == Integer.MAX_VALUE || minLength <= 0) {
            logger.warn("No valid data arrays found for normalization");
            return;
        }

        logger.info("Normalizing data arrays to length: {}", minLength);

        // Truncate all non-empty arrays to the minimum length (create new lists to
        // avoid view issues)
        if (!financialData.getRevenue().isEmpty()) {
            financialData.setRevenue(new ArrayList<>(financialData.getRevenue().subList(0, minLength)));
        }
        if (!financialData.getOperatingExpense().isEmpty()) {
            financialData
                    .setOperatingExpense(new ArrayList<>(financialData.getOperatingExpense().subList(0, minLength)));
        }
        if (!financialData.getOperatingIncome().isEmpty()) {
            financialData.setOperatingIncome(new ArrayList<>(financialData.getOperatingIncome().subList(0, minLength)));
        }
        if (!financialData.getNetProfit().isEmpty()) {
            financialData.setNetProfit(new ArrayList<>(financialData.getNetProfit().subList(0, minLength)));
        }
        if (!financialData.getOperatingCashFlow().isEmpty()) {
            financialData
                    .setOperatingCashFlow(new ArrayList<>(financialData.getOperatingCashFlow().subList(0, minLength)));
        }
        if (!financialData.getCapitalExpenditure().isEmpty()) {
            financialData.setCapitalExpenditure(
                    new ArrayList<>(financialData.getCapitalExpenditure().subList(0, minLength)));
        }
        if (!financialData.getFreeCashFlow().isEmpty()) {
            financialData.setFreeCashFlow(new ArrayList<>(financialData.getFreeCashFlow().subList(0, minLength)));
        }
        if (!financialData.getEps().isEmpty()) {
            financialData.setEps(new ArrayList<>(financialData.getEps().subList(0, minLength)));
        }
        if (!financialData.getTotalDebt().isEmpty()) {
            financialData.setTotalDebt(new ArrayList<>(financialData.getTotalDebt().subList(0, minLength)));
        }
        if (!financialData.getOrdinarySharesNumber().isEmpty()) {
            financialData.setOrdinarySharesNumber(
                    new ArrayList<>(financialData.getOrdinarySharesNumber().subList(0, minLength)));
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