package com.dcf.controller;

import com.dcf.dto.FinancialDataDto;
import com.dcf.entity.FinancialData;
import com.dcf.service.FinancialDataScrapingService;
import com.dcf.service.FinancialDataScrapingService.FinancialDataException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/financials")
@CrossOrigin(origins = "*")
@Validated
public class FinancialDataController {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataController.class);

    @Autowired
    private FinancialDataScrapingService financialDataScrapingService;

    /**
     * Handle BigDecimal parsing exceptions
     * @param e the NumberFormatException
     * @return error response
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<FinancialDataResponse> handleNumberFormatException(NumberFormatException e) {
        logger.warn("Number format exception in financial data controller: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new FinancialDataResponse("Invalid number format in request parameters", null));
    }

    /**
     * Handle IllegalArgumentException for BigDecimal validation
     * @param e the IllegalArgumentException
     * @return error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FinancialDataResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Validation error in financial data controller: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(new FinancialDataResponse(e.getMessage(), null));
    }

    /**
     * Get financial data for a ticker symbol
     * @param ticker the ticker symbol (e.g., AAPL, GOOGL)
     * @return FinancialDataDto with financial metrics
     */
    @GetMapping
    public ResponseEntity<FinancialDataResponse> getFinancialData(
            @RequestParam("ticker") 
            @NotBlank(message = "Ticker symbol is required")
            @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
            @Pattern(regexp = "^[A-Za-z0-9.-]+$", message = "Ticker symbol can only contain letters, numbers, dots, and hyphens")
            String ticker) {
        
        logger.info("Fetching financial data for ticker: {}", ticker);
        
        try {
            // Validate ticker format
            if (ticker == null || ticker.trim().isEmpty()) {
                logger.warn("Empty ticker symbol provided");
                return ResponseEntity.badRequest()
                    .body(new FinancialDataResponse("Ticker symbol is required", null));
            }

            // Get financial data from service
            FinancialData financialData = financialDataScrapingService.getFinancialData(ticker.trim().toUpperCase());
            
            // Convert to DTO with BigDecimal handling
            FinancialDataDto dto = convertToDto(financialData);
            
            logger.info("Successfully retrieved financial data for ticker: {}", ticker);
            return ResponseEntity.ok(new FinancialDataResponse("Financial data retrieved successfully", dto));
            
        } catch (FinancialDataException e) {
            logger.warn("Financial data error for ticker {}: {}", ticker, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FinancialDataResponse(e.getMessage(), null));
                
        } catch (RuntimeException e) {
            // Handle BigDecimal conversion errors specifically
            if (e.getMessage() != null && e.getMessage().contains("convert financial data")) {
                logger.error("BigDecimal serialization error for ticker {}: {}", ticker, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FinancialDataResponse("Financial data contains invalid numerical values", null));
            }
            throw e; // Re-throw if not a conversion error
            
        } catch (Exception e) {
            logger.error("Unexpected error fetching financial data for ticker {}: {}", ticker, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FinancialDataResponse("Unable to retrieve financial data at this time", null));
        }
    }

    /**
     * Validate if a ticker symbol exists
     * @param ticker the ticker symbol to validate
     * @return validation result
     */
    @GetMapping("/validate")
    public ResponseEntity<TickerValidationResponse> validateTicker(
            @RequestParam("ticker")
            @NotBlank(message = "Ticker symbol is required")
            @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
            @Pattern(regexp = "^[A-Za-z0-9.-]+$", message = "Ticker symbol can only contain letters, numbers, dots, and hyphens")
            String ticker) {
        
        logger.info("Validating ticker: {}", ticker);
        
        try {
            boolean isValid = financialDataScrapingService.isValidTicker(ticker.trim().toUpperCase());
            
            if (isValid) {
                logger.info("Ticker {} is valid", ticker);
                return ResponseEntity.ok(new TickerValidationResponse(true, "Ticker is valid", ticker.toUpperCase()));
            } else {
                logger.info("Ticker {} is invalid", ticker);
                return ResponseEntity.ok(new TickerValidationResponse(false, "Ticker not found or invalid", ticker.toUpperCase()));
            }
            
        } catch (Exception e) {
            logger.error("Error validating ticker {}: {}", ticker, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new TickerValidationResponse(false, "Unable to validate ticker at this time", ticker));
        }
    }

    /**
     * Get popular ticker symbols for suggestions
     * @return list of popular tickers
     */
    @GetMapping("/popular")
    public ResponseEntity<PopularTickersResponse> getPopularTickers() {
        logger.info("Fetching popular tickers");
        
        try {
            PopularTickersResponse response = new PopularTickersResponse();
            response.setMessage("Popular tickers retrieved successfully");
            response.addTicker("AAPL", "Apple Inc.");
            response.addTicker("GOOGL", "Alphabet Inc.");
            response.addTicker("MSFT", "Microsoft Corporation");
            response.addTicker("AMZN", "Amazon.com Inc.");
            response.addTicker("TSLA", "Tesla Inc.");
            response.addTicker("META", "Meta Platforms Inc.");
            response.addTicker("NVDA", "NVIDIA Corporation");
            response.addTicker("NFLX", "Netflix Inc.");
            response.addTicker("AMD", "Advanced Micro Devices");
            response.addTicker("INTC", "Intel Corporation");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching popular tickers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new PopularTickersResponse("Unable to retrieve popular tickers at this time"));
        }
    }

    /**
     * Get filtered financial data based on BigDecimal criteria
     * This endpoint demonstrates BigDecimal parameter handling
     * @param ticker the ticker symbol
     * @param minRevenue minimum revenue filter (optional)
     * @param maxDebt maximum debt filter (optional)
     * @return filtered financial data
     */
    @GetMapping("/filter")
    public ResponseEntity<FinancialDataResponse> getFilteredFinancialData(
            @RequestParam("ticker") 
            @NotBlank(message = "Ticker symbol is required")
            @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
            @Pattern(regexp = "^[A-Za-z0-9.-]+$", message = "Ticker symbol can only contain letters, numbers, dots, and hyphens")
            String ticker,
            
            @RequestParam(value = "minRevenue", required = false)
            String minRevenueStr,
            
            @RequestParam(value = "maxDebt", required = false)
            String maxDebtStr) {
        
        logger.info("Fetching filtered financial data for ticker: {} with filters - minRevenue: {}, maxDebt: {}", 
                   ticker, minRevenueStr, maxDebtStr);
        
        try {
            // Parse and validate BigDecimal parameters
            BigDecimal minRevenue = null;
            BigDecimal maxDebt = null;
            
            if (minRevenueStr != null && !minRevenueStr.trim().isEmpty()) {
                minRevenue = parseBigDecimalParameter(minRevenueStr, "minRevenue");
                validateBigDecimalBounds(minRevenue, "minRevenue", BigDecimal.ZERO, null);
            }
            
            if (maxDebtStr != null && !maxDebtStr.trim().isEmpty()) {
                maxDebt = parseBigDecimalParameter(maxDebtStr, "maxDebt");
                validateBigDecimalBounds(maxDebt, "maxDebt", BigDecimal.ZERO, null);
            }
            
            // Get financial data from service
            FinancialData financialData = financialDataScrapingService.getFinancialData(ticker.trim().toUpperCase());
            
            // Apply filters if specified
            if (minRevenue != null || maxDebt != null) {
                boolean passesFilter = true;
                
                // Check revenue filter
                if (minRevenue != null && financialData.getRevenue() != null && !financialData.getRevenue().isEmpty()) {
                    BigDecimal latestRevenue = financialData.getRevenue().get(0); // Most recent revenue
                    if (latestRevenue.compareTo(minRevenue) < 0) {
                        passesFilter = false;
                    }
                }
                
                // Check debt filter
                if (maxDebt != null && financialData.getTotalDebt() != null && !financialData.getTotalDebt().isEmpty()) {
                    BigDecimal latestDebt = financialData.getTotalDebt().get(0); // Most recent debt
                    if (latestDebt.compareTo(maxDebt) > 0) {
                        passesFilter = false;
                    }
                }
                
                if (!passesFilter) {
                    return ResponseEntity.ok(new FinancialDataResponse(
                        "Ticker does not meet the specified financial criteria", null));
                }
            }
            
            // Convert to DTO
            FinancialDataDto dto = convertToDto(financialData);
            
            logger.info("Successfully retrieved filtered financial data for ticker: {}", ticker);
            return ResponseEntity.ok(new FinancialDataResponse("Filtered financial data retrieved successfully", dto));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid parameter for filtered financial data request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new FinancialDataResponse(e.getMessage(), null));
                
        } catch (FinancialDataException e) {
            logger.warn("Financial data error for ticker {}: {}", ticker, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FinancialDataResponse(e.getMessage(), null));
                
        } catch (Exception e) {
            logger.error("Unexpected error fetching filtered financial data for ticker {}: {}", ticker, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new FinancialDataResponse("Unable to retrieve filtered financial data at this time", null));
        }
    }

    /**
     * Convert FinancialData entity to DTO
     * @param financialData the entity to convert
     * @return FinancialDataDto
     */
    private FinancialDataDto convertToDto(FinancialData financialData) {
        try {
            FinancialDataDto dto = new FinancialDataDto();
            dto.setTicker(financialData.getTicker());
            dto.setRevenue(financialData.getRevenue());
            dto.setOperatingExpense(financialData.getOperatingExpense());
            dto.setOperatingIncome(financialData.getOperatingIncome());
            dto.setOperatingCashFlow(financialData.getOperatingCashFlow());
            dto.setNetProfit(financialData.getNetProfit());
            dto.setCapitalExpenditure(financialData.getCapitalExpenditure());
            dto.setFreeCashFlow(financialData.getFreeCashFlow());
            dto.setEps(financialData.getEps());
            dto.setTotalDebt(financialData.getTotalDebt());
            dto.setOrdinarySharesNumber(financialData.getOrdinarySharesNumber());
            dto.setDateFetched(financialData.getDateFetched()); // This will use the LocalDate overload
            return dto;
        } catch (Exception e) {
            logger.error("Error converting FinancialData to DTO for ticker {}: {}", 
                        financialData.getTicker(), e.getMessage(), e);
            throw new RuntimeException("Failed to convert financial data for response", e);
        }
    }

    /**
     * Parse BigDecimal from string parameter with proper error handling
     * @param value the string value to parse
     * @param parameterName the name of the parameter for error messages
     * @return parsed BigDecimal value
     * @throws IllegalArgumentException if parsing fails
     */
    private BigDecimal parseBigDecimalParameter(String value, String parameterName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or empty");
        }
        
        try {
            // Remove any whitespace and validate format
            String cleanValue = value.trim();
            
            // Basic validation for reasonable financial values
            if (cleanValue.length() > 50) {
                throw new IllegalArgumentException(parameterName + " value is too long");
            }
            
            BigDecimal result = new BigDecimal(cleanValue);
            
            // Validate that the BigDecimal is not infinite or NaN equivalent
            if (result.scale() > 10) {
                // Limit precision to reasonable financial precision
                result = result.setScale(10, RoundingMode.HALF_UP);
            }
            
            return result;
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid BigDecimal format for parameter {}: {}", parameterName, value);
            throw new IllegalArgumentException(
                String.format("Invalid number format for %s: '%s'. Please provide a valid decimal number.", 
                             parameterName, value), e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing BigDecimal parameter {}: {}", parameterName, e.getMessage());
            throw new IllegalArgumentException(
                String.format("Failed to parse %s: %s", parameterName, e.getMessage()), e);
        }
    }

    /**
     * Validate BigDecimal value is within reasonable financial bounds
     * @param value the BigDecimal to validate
     * @param parameterName the parameter name for error messages
     * @param minValue minimum allowed value (inclusive)
     * @param maxValue maximum allowed value (inclusive)
     * @throws IllegalArgumentException if value is out of bounds
     */
    private void validateBigDecimalBounds(BigDecimal value, String parameterName, 
                                        BigDecimal minValue, BigDecimal maxValue) {
        if (value == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
        
        if (minValue != null && value.compareTo(minValue) < 0) {
            throw new IllegalArgumentException(
                String.format("%s must be greater than or equal to %s", parameterName, minValue.toPlainString()));
        }
        
        if (maxValue != null && value.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException(
                String.format("%s must be less than or equal to %s", parameterName, maxValue.toPlainString()));
        }
    }

    /**
     * Response wrapper for financial data
     */
    public static class FinancialDataResponse {
        private String message;
        private FinancialDataDto data;

        public FinancialDataResponse() {}

        public FinancialDataResponse(String message, FinancialDataDto data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public FinancialDataDto getData() {
            return data;
        }

        public void setData(FinancialDataDto data) {
            this.data = data;
        }
    }

    /**
     * Response wrapper for ticker validation
     */
    public static class TickerValidationResponse {
        private boolean valid;
        private String message;
        private String ticker;

        public TickerValidationResponse() {}

        public TickerValidationResponse(boolean valid, String message, String ticker) {
            this.valid = valid;
            this.message = message;
            this.ticker = ticker;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTicker() {
            return ticker;
        }

        public void setTicker(String ticker) {
            this.ticker = ticker;
        }
    }

    /**
     * Response wrapper for popular tickers
     */
    public static class PopularTickersResponse {
        private String message;
        private java.util.List<PopularTicker> tickers;

        public PopularTickersResponse() {
            this.tickers = new java.util.ArrayList<>();
        }

        public PopularTickersResponse(String message) {
            this.message = message;
            this.tickers = new java.util.ArrayList<>();
        }

        public void addTicker(String symbol, String name) {
            this.tickers.add(new PopularTicker(symbol, name));
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public java.util.List<PopularTicker> getTickers() {
            return tickers;
        }

        public void setTickers(java.util.List<PopularTicker> tickers) {
            this.tickers = tickers;
        }

        public static class PopularTicker {
            private String symbol;
            private String name;

            public PopularTicker() {}

            public PopularTicker(String symbol, String name) {
                this.symbol = symbol;
                this.name = name;
            }

            public String getSymbol() {
                return symbol;
            }

            public void setSymbol(String symbol) {
                this.symbol = symbol;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
    }
}