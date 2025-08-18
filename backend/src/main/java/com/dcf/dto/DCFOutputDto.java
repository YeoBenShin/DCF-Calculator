package com.dcf.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DCFOutputDto {
    private String id;
    private String ticker;
    
    private BigDecimal fairValuePerShare;
    private BigDecimal currentPrice;
    private String valuation;
    private BigDecimal upsideDownsidePercentage;
    private BigDecimal terminalValue;
    private BigDecimal presentValueOfCashFlows;
    private BigDecimal enterpriseValue;
    private BigDecimal equityValue;
    private BigDecimal sharesOutstanding;
    
    private String dcfInputId;
    private String userId;
    private LocalDateTime calculatedAt;

    public DCFOutputDto() {}

    public DCFOutputDto(String ticker, BigDecimal fairValuePerShare, BigDecimal currentPrice, String valuation) {
        this.ticker = ticker;
        this.fairValuePerShare = fairValuePerShare;
        this.currentPrice = currentPrice;
        this.valuation = valuation;
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
        this.ticker = ticker;
    }

    public BigDecimal getFairValuePerShare() {
        return fairValuePerShare;
    }

    public void setFairValuePerShare(BigDecimal fairValuePerShare) {
        this.fairValuePerShare = fairValuePerShare;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
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
}