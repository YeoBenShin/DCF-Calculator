package com.dcf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "fair_value_per_share", nullable = false, precision = 20, scale = 6)
    @NotNull(message = "Fair value per share is required")
    @DecimalMin(value = "0.0", message = "Fair value per share must be positive")
    private BigDecimal fairValuePerShare;

    @Column(name = "current_price", precision = 20, scale = 6)
    @DecimalMin(value = "0.0", message = "Current price must be positive")
    private BigDecimal currentPrice;

    @Column(nullable = false)
    @NotBlank(message = "Valuation status is required")
    private String valuation;

    @Column(name = "upside_downside_percentage", precision = 10, scale = 6)
    private BigDecimal upsideDownsidePercentage;

    @Column(name = "dcf_input_id")
    private String dcfInputId;

    @Column(name = "userId")
    private String userId;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    // Additional DCF calculation details
    @Column(name = "terminal_value", precision = 25, scale = 2)
    private BigDecimal terminalValue;

    @Column(name = "present_value_of_cash_flows", precision = 25, scale = 2)
    private BigDecimal presentValueOfCashFlows;

    @Column(name = "enterprise_value", precision = 25, scale = 2)
    private BigDecimal enterpriseValue;

    @Column(name = "equity_value", precision = 25, scale = 2)
    private BigDecimal equityValue;

    @Column(name = "shares_outstanding", precision = 20, scale = 0)
    private BigDecimal sharesOutstanding;

    public DCFOutput() {
        this.calculatedAt = LocalDateTime.now();
    }

    public DCFOutput(String ticker, BigDecimal fairValuePerShare, BigDecimal currentPrice, String valuation) {
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

    public BigDecimal getFairValuePerShare() {
        return fairValuePerShare;
    }

    public void setFairValuePerShare(BigDecimal fairValuePerShare) {
        this.fairValuePerShare = fairValuePerShare;
        this.calculateUpsideDownside();
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.calculateUpsideDownside();
    }

    public String getValuation() {
        return valuation;
    }

    public void setValuation(String valuation) {
        this.valuation = valuation;
    }

    public BigDecimal getUpsideDownsidePercentage() {
        return upsideDownsidePercentage;
    }

    public void setUpsideDownsidePercentage(BigDecimal upsideDownsidePercentage) {
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

    public BigDecimal getTerminalValue() {
        return terminalValue;
    }

    public void setTerminalValue(BigDecimal terminalValue) {
        this.terminalValue = terminalValue;
    }

    public BigDecimal getPresentValueOfCashFlows() {
        return presentValueOfCashFlows;
    }

    public void setPresentValueOfCashFlows(BigDecimal presentValueOfCashFlows) {
        this.presentValueOfCashFlows = presentValueOfCashFlows;
    }

    public BigDecimal getEnterpriseValue() {
        return enterpriseValue;
    }

    public void setEnterpriseValue(BigDecimal enterpriseValue) {
        this.enterpriseValue = enterpriseValue;
    }

    public BigDecimal getEquityValue() {
        return equityValue;
    }

    public void setEquityValue(BigDecimal equityValue) {
        this.equityValue = equityValue;
    }

    public BigDecimal getSharesOutstanding() {
        return sharesOutstanding;
    }

    public void setSharesOutstanding(BigDecimal sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    // Utility methods
    private void calculateUpsideDownside() {
        if (fairValuePerShare != null && currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal difference = fairValuePerShare.subtract(currentPrice);
            this.upsideDownsidePercentage = difference.divide(currentPrice, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    public boolean isUndervalued() {
        return fairValuePerShare != null && currentPrice != null && fairValuePerShare.compareTo(currentPrice) > 0;
    }

    public boolean isOvervalued() {
        return fairValuePerShare != null && currentPrice != null && fairValuePerShare.compareTo(currentPrice) < 0;
    }

    public boolean isFairlyValued(BigDecimal tolerancePercentage) {
        if (fairValuePerShare == null || currentPrice == null) {
            return false;
        }
        BigDecimal difference = fairValuePerShare.subtract(currentPrice).abs();
        BigDecimal tolerance = currentPrice.multiply(tolerancePercentage.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return difference.compareTo(tolerance) <= 0;
    }

    public String getValuationStatus() {
        if (fairValuePerShare == null || currentPrice == null) {
            return "Unknown";
        }
        
        if (isFairlyValued(new BigDecimal("5.0"))) { // 5% tolerance
            return "Fair Value";
        } else if (isUndervalued()) {
            return "Undervalued";
        } else {
            return "Overvalued";
        }
    }

    public BigDecimal getAbsoluteUpside() {
        return fairValuePerShare != null && currentPrice != null ? 
               fairValuePerShare.subtract(currentPrice) : null;
    }

    public boolean hasSignificantUpside(BigDecimal thresholdPercentage) {
        return upsideDownsidePercentage != null && upsideDownsidePercentage.compareTo(thresholdPercentage) > 0;
    }
}