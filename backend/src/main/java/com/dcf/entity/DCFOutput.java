package com.dcf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "dcf_outputs")
public class DCFOutput {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @NotBlank(message = "Ticker symbol is required")
    private String ticker;

    @Column(name = "fair_value_per_share", nullable = false)
    @NotNull(message = "Fair value per share is required")
    @Positive(message = "Fair value per share must be positive")
    private Double fairValuePerShare;

    @Column(name = "current_price")
    @Positive(message = "Current price must be positive")
    private Double currentPrice;

    @Column(nullable = false)
    @NotBlank(message = "Valuation status is required")
    private String valuation;

    @Column(name = "upside_downside_percentage")
    private Double upsideDownsidePercentage;

    @Column(name = "dcf_input_id")
    private String dcfInputId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    // Additional DCF calculation details
    @Column(name = "terminal_value")
    private Double terminalValue;

    @Column(name = "present_value_of_cash_flows")
    private Double presentValueOfCashFlows;

    @Column(name = "enterprise_value")
    private Double enterpriseValue;

    @Column(name = "equity_value")
    private Double equityValue;

    @Column(name = "shares_outstanding")
    private Double sharesOutstanding;

    public DCFOutput() {
        this.calculatedAt = LocalDateTime.now();
    }

    public DCFOutput(String ticker, Double fairValuePerShare, Double currentPrice, String valuation) {
        this();
        this.ticker = ticker != null ? ticker.toUpperCase() : null;
        this.fairValuePerShare = fairValuePerShare;
        this.currentPrice = currentPrice;
        this.valuation = valuation;
        this.calculateUpsideDownside();
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

    public Double getFairValuePerShare() {
        return fairValuePerShare;
    }

    public void setFairValuePerShare(Double fairValuePerShare) {
        this.fairValuePerShare = fairValuePerShare;
        this.calculateUpsideDownside();
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
        this.calculateUpsideDownside();
    }

    public String getValuation() {
        return valuation;
    }

    public void setValuation(String valuation) {
        this.valuation = valuation;
    }

    public Double getUpsideDownsidePercentage() {
        return upsideDownsidePercentage;
    }

    public void setUpsideDownsidePercentage(Double upsideDownsidePercentage) {
        this.upsideDownsidePercentage = upsideDownsidePercentage;
    }

    public String getDcfInputId() {
        return dcfInputId;
    }

    public void setDcfInputId(String dcfInputId) {
        this.dcfInputId = dcfInputId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Double getTerminalValue() {
        return terminalValue;
    }

    public void setTerminalValue(Double terminalValue) {
        this.terminalValue = terminalValue;
    }

    public Double getPresentValueOfCashFlows() {
        return presentValueOfCashFlows;
    }

    public void setPresentValueOfCashFlows(Double presentValueOfCashFlows) {
        this.presentValueOfCashFlows = presentValueOfCashFlows;
    }

    public Double getEnterpriseValue() {
        return enterpriseValue;
    }

    public void setEnterpriseValue(Double enterpriseValue) {
        this.enterpriseValue = enterpriseValue;
    }

    public Double getEquityValue() {
        return equityValue;
    }

    public void setEquityValue(Double equityValue) {
        this.equityValue = equityValue;
    }

    public Double getSharesOutstanding() {
        return sharesOutstanding;
    }

    public void setSharesOutstanding(Double sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    // Utility methods
    private void calculateUpsideDownside() {
        if (fairValuePerShare != null && currentPrice != null && currentPrice > 0) {
            this.upsideDownsidePercentage = ((fairValuePerShare - currentPrice) / currentPrice) * 100;
        }
    }

    public boolean isUndervalued() {
        return fairValuePerShare != null && currentPrice != null && fairValuePerShare > currentPrice;
    }

    public boolean isOvervalued() {
        return fairValuePerShare != null && currentPrice != null && fairValuePerShare < currentPrice;
    }

    public boolean isFairlyValued(double tolerancePercentage) {
        if (fairValuePerShare == null || currentPrice == null) {
            return false;
        }
        double difference = Math.abs(fairValuePerShare - currentPrice);
        double tolerance = currentPrice * (tolerancePercentage / 100.0);
        return difference <= tolerance;
    }

    public String getValuationStatus() {
        if (fairValuePerShare == null || currentPrice == null) {
            return "Unknown";
        }
        
        if (isFairlyValued(5.0)) { // 5% tolerance
            return "Fair Value";
        } else if (isUndervalued()) {
            return "Undervalued";
        } else {
            return "Overvalued";
        }
    }

    public Double getAbsoluteUpside() {
        return fairValuePerShare != null && currentPrice != null ? 
               fairValuePerShare - currentPrice : null;
    }

    public boolean hasSignificantUpside(double thresholdPercentage) {
        return upsideDownsidePercentage != null && upsideDownsidePercentage > thresholdPercentage;
    }
}