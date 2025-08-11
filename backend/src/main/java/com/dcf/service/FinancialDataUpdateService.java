package com.dcf.service;

import com.dcf.entity.FinancialData;
import com.dcf.repository.FinancialDataRepository;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialDataUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataUpdateService.class);

    @Autowired
    private FinancialDataRepository financialDataRepository;

    @Autowired
    private FinancialDataScrapingService scrapingService;

    /**
     * Update stale financial data - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void updateStaleFinancialData() {
        logger.info("Starting scheduled update of stale financial data");
        
        try {
            // Find data older than 7 days
            LocalDate cutoffDate = LocalDate.now().minusDays(7);
            List<FinancialData> staleData = financialDataRepository.findStaleData(cutoffDate);
            
            logger.info("Found {} stale financial data records to update", staleData.size());
            
            int updated = 0;
            int failed = 0;
            
            for (FinancialData data : staleData) {
                try {
                    // This will fetch fresh data and update the existing record
                    scrapingService.getFinancialData(data.getTicker());
                    updated++;
                    
                    // Add delay to avoid overwhelming the source
                    Thread.sleep(2000); // 2 second delay between requests
                    
                } catch (FinancialDataException e) {
                    logger.error("Failed to update financial data for ticker: {}", data.getTicker(), e);
                    failed++;
                } catch (InterruptedException e) {
                    logger.error("Update process interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            logger.info("Completed scheduled update: {} updated, {} failed", updated, failed);
            
        } catch (Exception e) {
            logger.error("Error during scheduled financial data update", e);
        }
    }

    /**
     * Clean up very old financial data - runs weekly on Sunday at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupOldFinancialData() {
        logger.info("Starting cleanup of old financial data");
        
        try {
            // Delete data older than 1 year
            LocalDate cutoffDate = LocalDate.now().minusYears(1);
            int deletedCount = financialDataRepository.deleteStaleData(cutoffDate);
            
            logger.info("Cleaned up {} old financial data records", deletedCount);
            
        } catch (Exception e) {
            logger.error("Error during financial data cleanup", e);
        }
    }

    /**
     * Update financial data for popular tickers - runs quarterly
     * This ensures we have fresh data for commonly requested stocks
     */
    @Scheduled(cron = "0 0 1 1 */3 *") // 1st day of every 3rd month at 1 AM
    public void updatePopularTickers() {
        logger.info("Starting quarterly update of popular tickers");
        
        String[] popularTickers = {
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", 
            "META", "NVDA", "NFLX", "AMD", "INTC",
            "JPM", "BAC", "WMT", "JNJ", "PG"
        };
        
        int updated = 0;
        int failed = 0;
        
        for (String ticker : popularTickers) {
            try {
                scrapingService.getFinancialData(ticker);
                updated++;
                
                // Add delay to avoid overwhelming the source
                Thread.sleep(5000); // 5 second delay for quarterly updates
                
            } catch (FinancialDataException e) {
                logger.error("Failed to update popular ticker: {}", ticker, e);
                failed++;
            } catch (InterruptedException e) {
                logger.error("Popular ticker update process interrupted", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("Completed quarterly popular ticker update: {} updated, {} failed", updated, failed);
    }

    /**
     * Manually trigger update for a specific ticker
     * @param ticker the ticker to update
     * @return true if successful, false otherwise
     */
    public boolean updateTicker(String ticker) {
        try {
            scrapingService.getFinancialData(ticker);
            logger.info("Successfully updated financial data for ticker: {}", ticker);
            return true;
        } catch (FinancialDataException e) {
            logger.error("Failed to update ticker: {}", ticker, e);
            return false;
        }
    }

    /**
     * Get statistics about financial data freshness
     * @return FinancialDataStats object with statistics
     */
    public FinancialDataStats getDataFreshnessStats() {
        long totalRecords = financialDataRepository.countAllRecords();
        
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        List<FinancialData> staleData = financialDataRepository.findStaleData(oneWeekAgo);
        long staleRecords = staleData.size();
        
        LocalDate oneMonthAgo = LocalDate.now().minusDays(30);
        List<FinancialData> recentData = financialDataRepository.findRecentlyUpdated(oneMonthAgo);
        long recentRecords = recentData.size();
        
        return new FinancialDataStats(totalRecords, staleRecords, recentRecords);
    }

    /**
     * Statistics about financial data freshness
     */
    public static class FinancialDataStats {
        private final long totalRecords;
        private final long staleRecords;
        private final long recentRecords;

        public FinancialDataStats(long totalRecords, long staleRecords, long recentRecords) {
            this.totalRecords = totalRecords;
            this.staleRecords = staleRecords;
            this.recentRecords = recentRecords;
        }

        public long getTotalRecords() {
            return totalRecords;
        }

        public long getStaleRecords() {
            return staleRecords;
        }

        public long getRecentRecords() {
            return recentRecords;
        }

        public double getStalePercentage() {
            return totalRecords > 0 ? (double) staleRecords / totalRecords * 100 : 0;
        }

        public double getRecentPercentage() {
            return totalRecords > 0 ? (double) recentRecords / totalRecords * 100 : 0;
        }
    }
}