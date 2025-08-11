package com.dcf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "scraping")
public class ScrapingConfig {

    private int timeoutSeconds = 30;
    private int cacheDays = 7;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private boolean enableRealScraping = false; // Default to mock data for safety
    private int maxRetries = 3;
    private long retryDelayMs = 1000;

    // Yahoo Finance URLs
    private String yahooFinanceBaseUrl = "https://finance.yahoo.com/quote";
    private String yahooFinancialsPath = "/financials";
    private String yahooCashFlowPath = "/cash-flow";
    private String yahooBalanceSheetPath = "/balance-sheet";

    // Rate limiting
    private long requestDelayMs = 1000; // 1 second between requests
    private int maxRequestsPerMinute = 30;

    // Getters and setters
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getCacheDays() {
        return cacheDays;
    }

    public void setCacheDays(int cacheDays) {
        this.cacheDays = cacheDays;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isEnableRealScraping() {
        return enableRealScraping;
    }

    public void setEnableRealScraping(boolean enableRealScraping) {
        this.enableRealScraping = enableRealScraping;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public String getYahooFinanceBaseUrl() {
        return yahooFinanceBaseUrl;
    }

    public void setYahooFinanceBaseUrl(String yahooFinanceBaseUrl) {
        this.yahooFinanceBaseUrl = yahooFinanceBaseUrl;
    }

    public String getYahooFinancialsPath() {
        return yahooFinancialsPath;
    }

    public void setYahooFinancialsPath(String yahooFinancialsPath) {
        this.yahooFinancialsPath = yahooFinancialsPath;
    }

    public String getYahooCashFlowPath() {
        return yahooCashFlowPath;
    }

    public void setYahooCashFlowPath(String yahooCashFlowPath) {
        this.yahooCashFlowPath = yahooCashFlowPath;
    }

    public String getYahooBalanceSheetPath() {
        return yahooBalanceSheetPath;
    }

    public void setYahooBalanceSheetPath(String yahooBalanceSheetPath) {
        this.yahooBalanceSheetPath = yahooBalanceSheetPath;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }
}