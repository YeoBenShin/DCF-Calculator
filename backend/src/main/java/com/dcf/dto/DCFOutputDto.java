package com.dcf.dto;

public class DCFOutputDto {
    private String ticker;
    private Double fairValuePerShare;
    private Double currentPrice;
    private String valuation;

    public DCFOutputDto() {}

    public DCFOutputDto(String ticker, Double fairValuePerShare, Double currentPrice, String valuation) {
        this.ticker = ticker;
        this.fairValuePerShare = fairValuePerShare;
        this.currentPrice = currentPrice;
        this.valuation = valuation;
    }

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getFairValuePerShare() {
        return fairValuePerShare;
    }

    public void setFairValuePerShare(Double fairValuePerShare) {
        this.fairValuePerShare = fairValuePerShare;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getValuation() {
        return valuation;
    }

    public void setValuation(String valuation) {
        this.valuation = valuation;
    }
}