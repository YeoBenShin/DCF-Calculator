package com.dcf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class DCFInputDto {
    @NotBlank(message = "Ticker is required")
    private String ticker;

    @NotNull(message = "Discount rate is required")
    @Min(value = 0, message = "Discount rate must be positive")
    @Max(value = 100, message = "Discount rate must be less than 100%")
    private Double discountRate;

    @NotNull(message = "Growth rate is required")
    @Min(value = -100, message = "Growth rate must be greater than -100%")
    @Max(value = 1000, message = "Growth rate must be less than 1000%")
    private Double growthRate;

    @NotNull(message = "Terminal growth rate is required")
    @Min(value = 0, message = "Terminal growth rate must be positive")
    @Max(value = 10, message = "Terminal growth rate must be less than 10%")
    private Double terminalGrowthRate;

    public DCFInputDto() {}

    public DCFInputDto(String ticker, Double discountRate, Double growthRate, Double terminalGrowthRate) {
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

    public Double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(Double discountRate) {
        this.discountRate = discountRate;
    }

    public Double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }

    public Double getTerminalGrowthRate() {
        return terminalGrowthRate;
    }

    public void setTerminalGrowthRate(Double terminalGrowthRate) {
        this.terminalGrowthRate = terminalGrowthRate;
    }
}