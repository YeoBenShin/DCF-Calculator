package com.dcf.dto;

import com.dcf.service.WatchlistService.WatchlistItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WatchlistItemDto {
    private String ticker;
    private BigDecimal fairValuePerShare;
    private BigDecimal currentPrice;
    private String valuation;
    private BigDecimal upsideDownsidePercentage;
    private LocalDateTime lastCalculated;
    private String error;
    private boolean hasCalculation;
    private boolean hasError;

    public WatchlistItemDto() {}

    public WatchlistItemDto(WatchlistItem watchlistItem) {
        this.ticker = watchlistItem.getTicker();
        this.fairValuePerShare = watchlistItem.getFairValuePerShare();
        this.currentPrice = watchlistItem.getCurrentPrice();
        this.valuation = watchlistItem.getValuation();
        this.upsideDownsidePercentage = watchlistItem.getUpsideDownsidePercentage();
        this.lastCalculated = watchlistItem.getLastCalculated();
        this.error = watchlistItem.getError();
        this.hasCalculation = watchlistItem.hasCalculation();
        this.hasError = watchlistItem.hasError();
    }

    // Getters and setters
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

    public LocalDateTime getLastCalculated() {
        return lastCalculated;
    }

    public void setLastCalculated(LocalDateTime lastCalculated) {
        this.lastCalculated = lastCalculated;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isHasCalculation() {
        return hasCalculation;
    }

    public void setHasCalculation(boolean hasCalculation) {
        this.hasCalculation = hasCalculation;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
}