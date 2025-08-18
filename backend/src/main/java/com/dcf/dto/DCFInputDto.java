package com.dcf.dto;


import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class DCFInputDto {
    @NotBlank(message = "Ticker is required")
    private String ticker;

    @NotNull(message = "Discount rate is required")
    @DecimalMin(value = "0.0", message = "Discount rate must be positive")
    @DecimalMax(value = "100.0", message = "Discount rate must be less than 100%")
    @Digits(integer = 4, fraction = 6, message = "Invalid discount rate format")
    private BigDecimal discountRate;

    @NotNull(message = "Growth rate is required")
    @DecimalMin(value = "-100.0", message = "Growth rate must be greater than -100%")
    @DecimalMax(value = "1000.0", message = "Growth rate must be less than 1000%")
    @Digits(integer = 4, fraction = 6, message = "Invalid growth rate format")
    private BigDecimal growthRate;

    @NotNull(message = "Terminal growth rate is required")
    @DecimalMin(value = "0.0", message = "Terminal growth rate must be positive")
    @DecimalMax(value = "10.0", message = "Terminal growth rate must be less than 10%")
    @Digits(integer = 4, fraction = 6, message = "Invalid terminal growth rate format")
    private BigDecimal terminalGrowthRate;

    public DCFInputDto() {}

    public DCFInputDto(String ticker, BigDecimal discountRate, BigDecimal growthRate, BigDecimal terminalGrowthRate) {
        this.ticker = ticker;
        this.discountRate = discountRate;
        this.growthRate = growthRate;
        this.terminalGrowthRate = terminalGrowthRate;
    }

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(BigDecimal growthRate) {
        this.growthRate = growthRate;
    }

    public BigDecimal getTerminalGrowthRate() {
        return terminalGrowthRate;
    }

    public void setTerminalGrowthRate(BigDecimal terminalGrowthRate) {
        this.terminalGrowthRate = terminalGrowthRate;
    }
}