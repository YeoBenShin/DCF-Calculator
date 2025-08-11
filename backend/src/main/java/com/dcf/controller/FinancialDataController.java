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

@RestController
@RequestMapping("/financials")
@CrossOrigin(origins = "*")
@Validated
public class FinancialDataController {

    private static final Logger logger = LoggerFactory.getLogger(FinancialDataController.class);

    @Autowired
    private FinancialDataScrapingService financialDataScrapingService;

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
            
            // Convert to DTO
            FinancialDataDto dto = convertToDto(financialData);
            
            logger.info("Successfully retrieved financial data for ticker: {}", ticker);
            return ResponseEntity.ok(new FinancialDataResponse("Financial data retrieved successfully", dto));
            
        } catch (FinancialDataException e) {
            logger.warn("Financial data error for ticker {}: {}", ticker, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FinancialDataResponse(e.getMessage(), null));
                
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
     * Convert FinancialData entity to DTO
     * @param financialData the entity to convert
     * @return FinancialDataDto
     */
    private FinancialDataDto convertToDto(FinancialData financialData) {
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
        dto.setDateFetched(financialData.getDateFetched());
        return dto;
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