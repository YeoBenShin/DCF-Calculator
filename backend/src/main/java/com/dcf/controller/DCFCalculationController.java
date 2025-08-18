package com.dcf.controller;

import com.dcf.dto.DCFInputDto;
import com.dcf.dto.DCFOutputDto;
import com.dcf.entity.DCFInput;
import com.dcf.entity.DCFOutput;
import com.dcf.service.DCFCalculationService;
import com.dcf.service.DCFCalculationService.DCFCalculationException;
import com.dcf.service.DCFCalculationService.DCFSensitivityAnalysis;
import com.dcf.service.DCFCalculationService.DCFCalculationStats;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dcf")
@CrossOrigin(origins = "*")
public class DCFCalculationController {

    private static final Logger logger = LoggerFactory.getLogger(DCFCalculationController.class);

    @Autowired
    private DCFCalculationService dcfCalculationService;

    /**
     * Calculate DCF valuation for a stock
     * @param dcfInputDto the DCF input parameters
     * @return DCFOutput with calculated fair value and valuation status
     */
    @PostMapping("/calculate")
    public ResponseEntity<DCFCalculationResponse> calculateDCF(@Valid @RequestBody DCFInputDto dcfInputDto) {
        logger.info("DCF calculation request for ticker: {}", dcfInputDto.getTicker());
        
        try {
            // Get authenticated user ID (optional)
            String userId = getCurrentUserId();
            
            // Convert DTO to entity
            DCFInput dcfInput = convertToEntity(dcfInputDto, userId);
            
            // Perform DCF calculation
            DCFOutput dcfOutput = dcfCalculationService.calculateDCF(dcfInput);
            
            // Convert to DTO
            DCFOutputDto outputDto = convertToDto(dcfOutput);
            
            logger.info("DCF calculation completed for ticker: {} with fair value: {}", 
                       dcfInputDto.getTicker(), dcfOutput.getFairValuePerShare());
            
            return ResponseEntity.ok(new DCFCalculationResponse("DCF calculation completed successfully", outputDto));
            
        } catch (DCFCalculationException e) {
            logger.warn("DCF calculation failed for ticker {}: {}", dcfInputDto.getTicker(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DCFCalculationResponse(e.getMessage(), null));
                
        } catch (Exception e) {
            logger.error("Unexpected error during DCF calculation for ticker {}: {}", dcfInputDto.getTicker(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DCFCalculationResponse("DCF calculation failed due to server error", null));
        }
    }

    /**
     * Get sensitivity analysis for DCF calculation
     * @param dcfInputDto the base DCF input parameters
     * @return sensitivity analysis results
     */
    @PostMapping("/sensitivity")
    public ResponseEntity<SensitivityAnalysisResponse> getSensitivityAnalysis(@Valid @RequestBody DCFInputDto dcfInputDto) {
        logger.info("Sensitivity analysis request for ticker: {}", dcfInputDto.getTicker());
        
        try {
            // Get authenticated user ID (optional)
            String userId = getCurrentUserId();
            
            // Convert DTO to entity
            DCFInput dcfInput = convertToEntity(dcfInputDto, userId);
            
            // Define sensitivity ranges (as percentages)
            BigDecimal[] growthRateRange = {
                new BigDecimal("-5.0"), new BigDecimal("0.0"), new BigDecimal("5.0"), 
                new BigDecimal("10.0"), new BigDecimal("15.0"), new BigDecimal("20.0")
            }; // -5% to 20%
            BigDecimal[] discountRateRange = {
                new BigDecimal("8.0"), new BigDecimal("10.0"), new BigDecimal("12.0"), 
                new BigDecimal("15.0"), new BigDecimal("18.0"), new BigDecimal("20.0")
            }; // 8% to 20%
            
            // Perform sensitivity analysis
            DCFSensitivityAnalysis analysis = dcfCalculationService.calculateSensitivityAnalysis(
                dcfInput, growthRateRange, discountRateRange);
            
            logger.info("Sensitivity analysis completed for ticker: {}", dcfInputDto.getTicker());
            
            return ResponseEntity.ok(new SensitivityAnalysisResponse("Sensitivity analysis completed successfully", analysis));
            
        } catch (DCFCalculationException e) {
            logger.warn("Sensitivity analysis failed for ticker {}: {}", dcfInputDto.getTicker(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new SensitivityAnalysisResponse(e.getMessage(), null));
                
        } catch (Exception e) {
            logger.error("Unexpected error during sensitivity analysis for ticker {}: {}", dcfInputDto.getTicker(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SensitivityAnalysisResponse("Sensitivity analysis failed due to server error", null));
        }
    }

    /**
     * Get historical DCF calculations for a ticker
     * @param ticker the ticker symbol
     * @return list of historical DCF calculations
     */
    @GetMapping("/history/{ticker}")
    public ResponseEntity<HistoricalCalculationsResponse> getHistoricalCalculations(@PathVariable String ticker) {
        logger.info("Historical calculations request for ticker: {}", ticker);
        
        try {
            // Get authenticated user ID (optional)
            String userId = getCurrentUserId();
            
            // Get historical calculations
            List<DCFOutput> historicalOutputs = dcfCalculationService.getHistoricalCalculations(ticker, userId);
            
            // Convert to DTOs
            List<DCFOutputDto> outputDtos = historicalOutputs.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            logger.info("Retrieved {} historical calculations for ticker: {}", outputDtos.size(), ticker);
            
            return ResponseEntity.ok(new HistoricalCalculationsResponse(
                "Historical calculations retrieved successfully", outputDtos));
            
        } catch (Exception e) {
            logger.error("Error retrieving historical calculations for ticker {}: {}", ticker, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new HistoricalCalculationsResponse("Failed to retrieve historical calculations", null));
        }
    }

    /**
     * Get DCF calculation statistics for the authenticated user
     * @return user's DCF calculation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats() {
        logger.info("User stats request");
        
        try {
            // Get authenticated user ID
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserStatsResponse("Authentication required", null));
            }
            
            // Get user statistics
            DCFCalculationStats stats = dcfCalculationService.getUserCalculationStats(userId);
            
            logger.info("Retrieved stats for user {}: {} total calculations", userId, stats.getTotalCalculations());
            
            return ResponseEntity.ok(new UserStatsResponse("User statistics retrieved successfully", stats));
            
        } catch (Exception e) {
            logger.error("Error retrieving user stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UserStatsResponse("Failed to retrieve user statistics", null));
        }
    }

    /**
     * Validate DCF input parameters
     * @param dcfInputDto the DCF input to validate
     * @return validation result
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateDCFInput(@Valid @RequestBody DCFInputDto dcfInputDto) {
        logger.info("DCF input validation request for ticker: {}", dcfInputDto.getTicker());
        
        try {
            // Convert DTO to entity for validation
            DCFInput dcfInput = convertToEntity(dcfInputDto, null);
            
            // The validation is already done by @Valid annotation and DCF validation util
            // If we reach here, the input is valid
            
            return ResponseEntity.ok(new ValidationResponse(true, "DCF input is valid", null));
            
        } catch (Exception e) {
            logger.warn("DCF input validation failed for ticker {}: {}", dcfInputDto.getTicker(), e.getMessage());
            return ResponseEntity.ok(new ValidationResponse(false, "DCF input validation failed", e.getMessage()));
        }
    }

    /**
     * Get current authenticated user ID
     * @return user ID or null if not authenticated
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Convert DCFInputDto to DCFInput entity
     * @param dto the DTO to convert
     * @param userId the user ID
     * @return DCFInput entity
     */
    private DCFInput convertToEntity(DCFInputDto dto, String userId) {
        DCFInput entity = new DCFInput();
        entity.setTicker(dto.getTicker().toUpperCase());
        entity.setDiscountRate(dto.getDiscountRate());
        entity.setGrowthRate(dto.getGrowthRate());
        entity.setTerminalGrowthRate(dto.getTerminalGrowthRate());
        entity.setProjectionYears(5); // Default to 5 years
        entity.setUserId(userId);
        return entity;
    }

    /**
     * Convert DCFOutput entity to DCFOutputDto
     * @param entity the entity to convert
     * @return DCFOutputDto
     */
    private DCFOutputDto convertToDto(DCFOutput entity) {
        DCFOutputDto dto = new DCFOutputDto();
        dto.setId(entity.getId());
        dto.setTicker(entity.getTicker());
        dto.setFairValuePerShare(entity.getFairValuePerShare());
        dto.setCurrentPrice(entity.getCurrentPrice());
        dto.setValuation(entity.getValuation());
        dto.setUpsideDownsidePercentage(entity.getUpsideDownsidePercentage());
        dto.setTerminalValue(entity.getTerminalValue());
        dto.setPresentValueOfCashFlows(entity.getPresentValueOfCashFlows());
        dto.setEnterpriseValue(entity.getEnterpriseValue());
        dto.setEquityValue(entity.getEquityValue());
        dto.setSharesOutstanding(entity.getSharesOutstanding());
        dto.setDcfInputId(entity.getDcfInputId());
        dto.setUserId(entity.getUserId());
        dto.setCalculatedAt(entity.getCalculatedAt());
        return dto;
    }

    /**
     * Response wrapper for DCF calculations
     */
    public static class DCFCalculationResponse {
        private String message;
        private DCFOutputDto data;

        public DCFCalculationResponse() {}

        public DCFCalculationResponse(String message, DCFOutputDto data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DCFOutputDto getData() {
            return data;
        }

        public void setData(DCFOutputDto data) {
            this.data = data;
        }
    }

    /**
     * Response wrapper for sensitivity analysis
     */
    public static class SensitivityAnalysisResponse {
        private String message;
        private DCFSensitivityAnalysis data;

        public SensitivityAnalysisResponse() {}

        public SensitivityAnalysisResponse(String message, DCFSensitivityAnalysis data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DCFSensitivityAnalysis getData() {
            return data;
        }

        public void setData(DCFSensitivityAnalysis data) {
            this.data = data;
        }
    }

    /**
     * Response wrapper for historical calculations
     */
    public static class HistoricalCalculationsResponse {
        private String message;
        private List<DCFOutputDto> data;

        public HistoricalCalculationsResponse() {}

        public HistoricalCalculationsResponse(String message, List<DCFOutputDto> data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<DCFOutputDto> getData() {
            return data;
        }

        public void setData(List<DCFOutputDto> data) {
            this.data = data;
        }
    }

    /**
     * Response wrapper for user statistics
     */
    public static class UserStatsResponse {
        private String message;
        private DCFCalculationStats data;

        public UserStatsResponse() {}

        public UserStatsResponse(String message, DCFCalculationStats data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public DCFCalculationStats getData() {
            return data;
        }

        public void setData(DCFCalculationStats data) {
            this.data = data;
        }
    }

    /**
     * Response wrapper for validation
     */
    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private String error;

        public ValidationResponse() {}

        public ValidationResponse(boolean valid, String message, String error) {
            this.valid = valid;
            this.message = message;
            this.error = error;
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

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}