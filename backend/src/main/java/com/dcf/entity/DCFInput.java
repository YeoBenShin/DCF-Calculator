package com.dcf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dcf_inputs")
public class DCFInput {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Ticker symbol is required")
    @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
    private String ticker;

    @Column(name = "discount_rate", nullable = false)
    @NotNull(message = "Discount rate is required")
    @DecimalMin(value = "0.0", message = "Discount rate must be positive")
    @DecimalMax(value = "100.0", message = "Discount rate must be less than 100%")
    private Double discountRate;

    @Column(name = "growth_rate", nullable = false)
    @NotNull(message = "Growth rate is required")
    @DecimalMin(value = "-100.0", message = "Growth rate must be greater than -100%")
    @DecimalMax(value = "1000.0", message = "Growth rate must be less than 1000%")
    private Double growthRate;

    @Column(name = "terminal_growth_rate", nullable = false)
    @NotNull(message = "Terminal growth rate is required")
    @DecimalMin(value = "0.0", message = "Terminal growth rate must be positive")
    @DecimalMax(value = "10.0", message = "Terminal growth rate must be less than 10%")
    private Double terminalGrowthRate;

    @Column(name = "projection_years")
    @Min(value = 1, message = "Projection years must be at least 1")
    @Max(value = 20, message = "Projection years must be at most 20")
    private Integer projectionYears = 5; // Default to 5 years

    @Column(name = "user_id")
    private String userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public DCFInput() {
        this.createdAt = LocalDateTime.now();
    }

    public DCFInput(String ticker, Double discountRate, Double growthRate, Double terminalGrowthRate) {
        this();
        this.ticker = ticker != null ? ticker.toUpperCase() : null;
        this.discountRate = discountRate;
        this.growthRate = growthRate;
        this.terminalGrowthRate = terminalGrowthRate;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker != null ? ticker.toUpperCase() : null;
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

    public Integer getProjectionYears() {
        return projectionYears;
    }

    public void setProjectionYears(Integer projectionYears) {
        this.projectionYears = projectionYears;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    public boolean isReasonableGrowthRate() {
        return growthRate != null && growthRate <= 100.0; // 100% growth is already quite high
    }

    public boolean isConservativeTerminalGrowthRate() {
        return terminalGrowthRate != null && terminalGrowthRate <= 3.0; // 3% is conservative
    }

    public Double getDiscountRateAsDecimal() {
        return discountRate != null ? discountRate / 100.0 : null;
    }

    public Double getGrowthRateAsDecimal() {
        return growthRate != null ? growthRate / 100.0 : null;
    }

    public Double getTerminalGrowthRateAsDecimal() {
        return terminalGrowthRate != null ? terminalGrowthRate / 100.0 : null;
    }
}