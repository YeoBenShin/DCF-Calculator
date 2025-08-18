package com.dcf.dto;

import java.math.BigDecimal;

import com.dcf.service.WatchlistService.WatchlistStats;

public class WatchlistStatsDto {
    private long totalStocks;
    private long undervaluedCount;
    private long overvaluedCount;
    private long fairValueCount;
    private BigDecimal averageUpside;
    private BigDecimal undervaluedPercentage;
    private BigDecimal overvaluedPercentage;
    private BigDecimal fairValuePercentage;

    public WatchlistStatsDto() {
    }

    public WatchlistStatsDto(WatchlistStats stats) {
        this.totalStocks = stats.getTotalStocks();
        this.undervaluedCount = stats.getUndervaluedCount();
        this.overvaluedCount = stats.getOvervaluedCount();
        this.fairValueCount = stats.getFairValueCount();
        this.averageUpside = stats.getAverageUpside();
        this.undervaluedPercentage = stats.getUndervaluedPercentage();
        this.overvaluedPercentage = stats.getOvervaluedPercentage();
        this.fairValuePercentage = stats.getFairValuePercentage();
    }

    // Getters and setters
    public long getTotalStocks() {
        return totalStocks;
    }

    public void setTotalStocks(long totalStocks) {
        this.totalStocks = totalStocks;
    }

    public long getUndervaluedCount() {
        return undervaluedCount;
    }

    public void setUndervaluedCount(long undervaluedCount) {
        this.undervaluedCount = undervaluedCount;
    }

    public long getOvervaluedCount() {
        return overvaluedCount;
    }

    public void setOvervaluedCount(long overvaluedCount) {
        this.overvaluedCount = overvaluedCount;
    }

    public long getFairValueCount() {
        return fairValueCount;
    }

    public void setFairValueCount(long fairValueCount) {
        this.fairValueCount = fairValueCount;
    }

    public BigDecimal getAverageUpside() {
        return averageUpside;
    }

    public void setAverageUpside(BigDecimal averageUpside) {
        this.averageUpside = averageUpside;
    }

    public BigDecimal getUndervaluedPercentage() {
        return undervaluedPercentage;
    }

    public void setUndervaluedPercentage(BigDecimal undervaluedPercentage) {
        this.undervaluedPercentage = undervaluedPercentage;
    }

    public BigDecimal getOvervaluedPercentage() {
        return overvaluedPercentage;
    }

    public void setOvervaluedPercentage(BigDecimal overvaluedPercentage) {
        this.overvaluedPercentage = overvaluedPercentage;
    }

    public BigDecimal getFairValuePercentage() {
        return fairValuePercentage;
    }

    public void setFairValuePercentage(BigDecimal fairValuePercentage) {
        this.fairValuePercentage = fairValuePercentage;
    }
}