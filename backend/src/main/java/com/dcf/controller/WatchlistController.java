package com.dcf.controller;

import com.dcf.dto.PopularTickerDto;
import com.dcf.dto.WatchlistItemDto;
import com.dcf.dto.WatchlistRequest;
import com.dcf.dto.WatchlistStatsDto;
import com.dcf.service.WatchlistService;
import com.dcf.service.WatchlistService.WatchlistException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/watchlist")
@CrossOrigin(origins = "*")
public class WatchlistController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistController.class);

    @Autowired
    private WatchlistService watchlistService;

    /**
     * Get user's watchlist with latest fair value status
     * GET /watchlist
     */
    @GetMapping
    public ResponseEntity<?> getWatchlist() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            logger.info("Getting watchlist for user: {}", userId);
            
            List<WatchlistService.WatchlistItem> watchlistItems = 
                watchlistService.getWatchlistWithFairValues(userId);
            
            List<WatchlistItemDto> watchlistDtos = watchlistItems.stream()
                .map(WatchlistItemDto::new)
                .collect(Collectors.toList());

            logger.info("Retrieved {} watchlist items for user: {}", watchlistDtos.size(), userId);
            return ResponseEntity.ok(watchlistDtos);

        } catch (WatchlistException e) {
            logger.error("Error getting watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Add a ticker to user's watchlist
     * POST /watchlist/add
     */
    @PostMapping("/add")
    public ResponseEntity<?> addToWatchlist(@Valid @RequestBody WatchlistRequest request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            String ticker = request.getTicker();
            logger.info("Adding ticker {} to watchlist for user: {}", ticker, userId);

            boolean added = watchlistService.addToWatchlist(userId, ticker);
            
            if (added) {
                logger.info("Successfully added ticker {} to watchlist for user: {}", ticker, userId);
                return ResponseEntity.ok(createSuccessResponse("Ticker added to watchlist successfully"));
            } else {
                logger.info("Ticker {} already in watchlist for user: {}", ticker, userId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("Ticker is already in your watchlist"));
            }

        } catch (WatchlistException e) {
            logger.error("Error adding to watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error adding to watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Remove a ticker from user's watchlist
     * DELETE /watchlist/remove
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromWatchlist(@Valid @RequestBody WatchlistRequest request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            String ticker = request.getTicker();
            logger.info("Removing ticker {} from watchlist for user: {}", ticker, userId);

            boolean removed = watchlistService.removeFromWatchlist(userId, ticker);
            
            if (removed) {
                logger.info("Successfully removed ticker {} from watchlist for user: {}", ticker, userId);
                return ResponseEntity.ok(createSuccessResponse("Ticker removed from watchlist successfully"));
            } else {
                logger.info("Ticker {} not in watchlist for user: {}", ticker, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Ticker is not in your watchlist"));
            }

        } catch (WatchlistException e) {
            logger.error("Error removing from watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error removing from watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get user's watchlist tickers only (without fair value data)
     * GET /watchlist/tickers
     */
    @GetMapping("/tickers")
    public ResponseEntity<?> getWatchlistTickers() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            logger.info("Getting watchlist tickers for user: {}", userId);
            
            List<String> tickers = watchlistService.getWatchlistTickers(userId);

            logger.info("Retrieved {} watchlist tickers for user: {}", tickers.size(), userId);
            return ResponseEntity.ok(tickers);

        } catch (WatchlistException e) {
            logger.error("Error getting watchlist tickers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting watchlist tickers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get watchlist statistics for a user
     * GET /watchlist/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getWatchlistStats() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            logger.info("Getting watchlist stats for user: {}", userId);
            
            WatchlistService.WatchlistStats stats = watchlistService.getWatchlistStats(userId);
            WatchlistStatsDto statsDto = new WatchlistStatsDto(stats);

            logger.info("Retrieved watchlist stats for user: {}", userId);
            return ResponseEntity.ok(statsDto);

        } catch (WatchlistException e) {
            logger.error("Error getting watchlist stats: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error getting watchlist stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Clear user's entire watchlist
     * DELETE /watchlist/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearWatchlist() {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            logger.info("Clearing watchlist for user: {}", userId);
            
            int removedCount = watchlistService.clearWatchlist(userId);

            logger.info("Cleared {} tickers from watchlist for user: {}", removedCount, userId);
            return ResponseEntity.ok(createSuccessResponse(
                String.format("Cleared %d tickers from watchlist", removedCount)));

        } catch (WatchlistException e) {
            logger.error("Error clearing watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error clearing watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Check if ticker is in user's watchlist
     * GET /watchlist/contains/{ticker}
     */
    @GetMapping("/contains/{ticker}")
    public ResponseEntity<?> isInWatchlist(@PathVariable String ticker) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("User not authenticated"));
            }

            logger.info("Checking if ticker {} is in watchlist for user: {}", ticker, userId);
            
            boolean inWatchlist = watchlistService.isInWatchlist(userId, ticker);

            Map<String, Object> response = new HashMap<>();
            response.put("ticker", ticker);
            response.put("inWatchlist", inWatchlist);

            return ResponseEntity.ok(response);

        } catch (WatchlistException e) {
            logger.error("Error checking watchlist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error checking watchlist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get popular tickers across all users (public endpoint)
     * GET /watchlist/popular
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularTickers(@RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Getting top {} popular tickers", limit);
            
            // Validate limit parameter
            if (limit < 1 || limit > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Limit must be between 1 and 100"));
            }
            
            List<WatchlistService.PopularTicker> popularTickers = 
                watchlistService.getPopularTickers(limit);
            
            List<PopularTickerDto> popularTickerDtos = popularTickers.stream()
                .map(PopularTickerDto::new)
                .collect(Collectors.toList());

            logger.info("Retrieved {} popular tickers", popularTickerDtos.size());
            return ResponseEntity.ok(popularTickerDtos);

        } catch (Exception e) {
            logger.error("Unexpected error getting popular tickers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
        }
    }

    /**
     * Get the current authenticated user's ID from the security context
     * @return user ID or null if not authenticated
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Create a standardized error response
     * @param message the error message
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Create a standardized success response
     * @param message the success message
     * @return success response map
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}