package com.dcf.service;

import com.dcf.entity.DCFOutput;
import com.dcf.entity.User;
import com.dcf.repository.DCFOutputRepository;
import com.dcf.repository.UserRepository;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import com.dcf.util.FinancialDataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchlistService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DCFOutputRepository dcfOutputRepository;

    @Autowired
    private FinancialDataScrapingService financialDataScrapingService;

    @Autowired
    private FinancialDataUtil financialDataUtil;

    /**
     * Add a ticker to user's watchlist
     * @param userId the user ID
     * @param ticker the ticker symbol to add
     * @return true if added successfully, false if already exists
     * @throws WatchlistException if operation fails
     */
    public boolean addToWatchlist(String userId, String ticker) throws WatchlistException {
        logger.info("Adding ticker {} to watchlist for user {}", ticker, userId);

        // Validate ticker format
        String normalizedTicker = financialDataUtil.normalizeTicker(ticker);
        if (normalizedTicker == null) {
            throw new WatchlistException("Invalid ticker symbol: " + ticker);
        }

        // Validate that ticker exists
        if (!financialDataScrapingService.isValidTicker(normalizedTicker)) {
            throw new WatchlistException("Ticker not found: " + normalizedTicker);
        }

        // Get user
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        User user = userOptional.get();

        // Check if ticker is already in watchlist
        if (user.isInWatchlist(normalizedTicker)) {
            logger.info("Ticker {} already in watchlist for user {}", normalizedTicker, userId);
            return false;
        }

        // Add to watchlist
        user.addToWatchlist(normalizedTicker);
        userRepository.save(user);

        logger.info("Successfully added ticker {} to watchlist for user {}", normalizedTicker, userId);
        return true;
    }

    /**
     * Remove a ticker from user's watchlist
     * @param userId the user ID
     * @param ticker the ticker symbol to remove
     * @return true if removed successfully, false if not in watchlist
     * @throws WatchlistException if operation fails
     */
    public boolean removeFromWatchlist(String userId, String ticker) throws WatchlistException {
        logger.info("Removing ticker {} from watchlist for user {}", ticker, userId);

        String normalizedTicker = financialDataUtil.normalizeTicker(ticker);
        if (normalizedTicker == null) {
            throw new WatchlistException("Invalid ticker symbol: " + ticker);
        }

        // Get user
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        User user = userOptional.get();

        // Check if ticker is in watchlist
        if (!user.isInWatchlist(normalizedTicker)) {
            logger.info("Ticker {} not in watchlist for user {}", normalizedTicker, userId);
            return false;
        }

        // Remove from watchlist
        user.removeFromWatchlist(normalizedTicker);
        userRepository.save(user);

        logger.info("Successfully removed ticker {} from watchlist for user {}", normalizedTicker, userId);
        return true;
    }

    /**
     * Get user's watchlist with latest fair value status
     * @param userId the user ID
     * @return list of watchlist items with fair value information
     * @throws WatchlistException if operation fails
     */
    public List<WatchlistItem> getWatchlistWithFairValues(String userId) throws WatchlistException {
        logger.info("Retrieving watchlist with fair values for user {}", userId);

        // Get user
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        User user = userOptional.get();
        List<String> watchlistTickers = user.getWatchlist();

        List<WatchlistItem> watchlistItems = new ArrayList<>();

        for (String ticker : watchlistTickers) {
            try {
                WatchlistItem item = createWatchlistItem(userId, ticker);
                watchlistItems.add(item);
            } catch (Exception e) {
                logger.error("Error creating watchlist item for ticker: {}", ticker, e);
                // Create item with error status
                WatchlistItem errorItem = new WatchlistItem();
                errorItem.setTicker(ticker);
                errorItem.setError("Unable to retrieve data");
                watchlistItems.add(errorItem);
            }
        }

        logger.info("Retrieved {} watchlist items for user {}", watchlistItems.size(), userId);
        return watchlistItems;
    }

    /**
     * Get user's watchlist tickers only (without fair value data)
     * @param userId the user ID
     * @return list of ticker symbols
     * @throws WatchlistException if operation fails
     */
    public List<String> getWatchlistTickers(String userId) throws WatchlistException {
        logger.info("Retrieving watchlist tickers for user {}", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        return new ArrayList<>(userOptional.get().getWatchlist());
    }

    /**
     * Check if ticker is in user's watchlist
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return true if ticker is in watchlist
     * @throws WatchlistException if operation fails
     */
    public boolean isInWatchlist(String userId, String ticker) throws WatchlistException {
        String normalizedTicker = financialDataUtil.normalizeTicker(ticker);
        if (normalizedTicker == null) {
            return false;
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        return userOptional.get().isInWatchlist(normalizedTicker);
    }

    /**
     * Get watchlist statistics for a user
     * @param userId the user ID
     * @return WatchlistStats with user statistics
     * @throws WatchlistException if operation fails
     */
    public WatchlistStats getWatchlistStats(String userId) throws WatchlistException {
        List<WatchlistItem> watchlistItems = getWatchlistWithFairValues(userId);

        long totalStocks = watchlistItems.size();
        long undervaluedCount = watchlistItems.stream()
            .mapToLong(item -> "Undervalued".equals(item.getValuation()) ? 1 : 0)
            .sum();
        long overvaluedCount = watchlistItems.stream()
            .mapToLong(item -> "Overvalued".equals(item.getValuation()) ? 1 : 0)
            .sum();
        long fairValueCount = watchlistItems.stream()
            .mapToLong(item -> "Fair Value".equals(item.getValuation()) ? 1 : 0)
            .sum();

        double averageUpside = watchlistItems.stream()
            .filter(item -> item.getUpsideDownsidePercentage() != null)
            .mapToDouble(WatchlistItem::getUpsideDownsidePercentage)
            .average()
            .orElse(0.0);

        return new WatchlistStats(totalStocks, undervaluedCount, overvaluedCount, fairValueCount, averageUpside);
    }

    /**
     * Clear user's entire watchlist
     * @param userId the user ID
     * @return number of tickers removed
     * @throws WatchlistException if operation fails
     */
    public int clearWatchlist(String userId) throws WatchlistException {
        logger.info("Clearing watchlist for user {}", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new WatchlistException("User not found: " + userId);
        }

        User user = userOptional.get();
        int removedCount = user.getWatchlist().size();
        user.getWatchlist().clear();
        userRepository.save(user);

        logger.info("Cleared {} tickers from watchlist for user {}", removedCount, userId);
        return removedCount;
    }

    /**
     * Get popular tickers across all users
     * @param limit the maximum number of tickers to return
     * @return list of popular tickers with count
     */
    public List<PopularTicker> getPopularTickers(int limit) {
        logger.info("Retrieving top {} popular tickers", limit);

        List<String> allTickers = userRepository.findAll().stream()
            .flatMap(user -> user.getWatchlist().stream())
            .collect(Collectors.toList());

        // Count occurrences
        return allTickers.stream()
            .collect(Collectors.groupingBy(ticker -> ticker, Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new PopularTicker(entry.getKey(), entry.getValue().intValue()))
            .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Create a watchlist item with fair value information
     * @param userId the user ID
     * @param ticker the ticker symbol
     * @return WatchlistItem with fair value data
     */
    private WatchlistItem createWatchlistItem(String userId, String ticker) {
        WatchlistItem item = new WatchlistItem();
        item.setTicker(ticker);

        // Get most recent DCF calculation for this user and ticker
        Optional<DCFOutput> latestDCF = dcfOutputRepository.findMostRecentByUserAndTicker(userId, ticker);

        if (latestDCF.isPresent()) {
            DCFOutput dcfOutput = latestDCF.get();
            item.setFairValuePerShare(dcfOutput.getFairValuePerShare());
            item.setCurrentPrice(dcfOutput.getCurrentPrice());
            item.setValuation(dcfOutput.getValuation());
            item.setUpsideDownsidePercentage(dcfOutput.getUpsideDownsidePercentage());
            item.setLastCalculated(dcfOutput.getCalculatedAt());
        } else {
            // No DCF calculation found - set default values
            item.setValuation("Not Calculated");
            item.setLastCalculated(null);
        }

        return item;
    }

    /**
     * Watchlist item with fair value information
     */
    public static class WatchlistItem {
        private String ticker;
        private Double fairValuePerShare;
        private Double currentPrice;
        private String valuation;
        private Double upsideDownsidePercentage;
        private LocalDateTime lastCalculated;
        private String error;

        // Getters and setters
        public String getTicker() { return ticker; }
        public void setTicker(String ticker) { this.ticker = ticker; }

        public Double getFairValuePerShare() { return fairValuePerShare; }
        public void setFairValuePerShare(Double fairValuePerShare) { this.fairValuePerShare = fairValuePerShare; }

        public Double getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

        public String getValuation() { return valuation; }
        public void setValuation(String valuation) { this.valuation = valuation; }

        public Double getUpsideDownsidePercentage() { return upsideDownsidePercentage; }
        public void setUpsideDownsidePercentage(Double upsideDownsidePercentage) { this.upsideDownsidePercentage = upsideDownsidePercentage; }

        public LocalDateTime getLastCalculated() { return lastCalculated; }
        public void setLastCalculated(LocalDateTime lastCalculated) { this.lastCalculated = lastCalculated; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public boolean hasError() { return error != null; }
        public boolean hasCalculation() { return fairValuePerShare != null; }
    }

    /**
     * Watchlist statistics
     */
    public static class WatchlistStats {
        private final long totalStocks;
        private final long undervaluedCount;
        private final long overvaluedCount;
        private final long fairValueCount;
        private final double averageUpside;

        public WatchlistStats(long totalStocks, long undervaluedCount, long overvaluedCount, 
                             long fairValueCount, double averageUpside) {
            this.totalStocks = totalStocks;
            this.undervaluedCount = undervaluedCount;
            this.overvaluedCount = overvaluedCount;
            this.fairValueCount = fairValueCount;
            this.averageUpside = averageUpside;
        }

        public long getTotalStocks() { return totalStocks; }
        public long getUndervaluedCount() { return undervaluedCount; }
        public long getOvervaluedCount() { return overvaluedCount; }
        public long getFairValueCount() { return fairValueCount; }
        public double getAverageUpside() { return averageUpside; }

        public double getUndervaluedPercentage() {
            return totalStocks > 0 ? (double) undervaluedCount / totalStocks * 100 : 0;
        }

        public double getOvervaluedPercentage() {
            return totalStocks > 0 ? (double) overvaluedCount / totalStocks * 100 : 0;
        }

        public double getFairValuePercentage() {
            return totalStocks > 0 ? (double) fairValueCount / totalStocks * 100 : 0;
        }
    }

    /**
     * Popular ticker information
     */
    public static class PopularTicker {
        private final String ticker;
        private final int count;

        public PopularTicker(String ticker, int count) {
            this.ticker = ticker;
            this.count = count;
        }

        public String getTicker() { return ticker; }
        public int getCount() { return count; }
    }

    /**
     * Custom exception for watchlist operations
     */
    public static class WatchlistException extends Exception {
        public WatchlistException(String message) {
            super(message);
        }

        public WatchlistException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}