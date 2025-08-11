package com.dcf.dto;

import com.dcf.service.WatchlistService.WatchlistStats;

public class WatchlistStatsDto {
    private long totalStocks;
    private long undervaluedCount;
    private long overvaluedCount;
    private long fairValueCount;
    private double averageUpside;
    private double undervaluedPercentage;
    private double overvaluedPercentage;
    private double fairValuePercentage;

    public WatchlistStatsDto() {}

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

    public double getAverageUpside() {
        return averageUpside;
    }

    public void setAverageUpside(double averageUpside) {
        this.averageUpside = averageUpside;
    }

    public double getUndervaluedPercentage() {
        return undervaluedPercentage;
    }

    public void setUndervaluedPercentage(double undervaluedPercentage) {
        this.undervaluedPercentage = undervaluedPercentage;
    }

    public double getOvervaluedPercentage() {
        return overvaluedPercentage;
    }

    public void setOvervaluedPercentage(double overvaluedPercentage) {
        this.overvaluedPercentage = overvaluedPercentage;
    }

    public double getFairValuePercentage() {
        return fairValuePercentage;
    }

    public void setFairValuePercentage(double fairValuePercentage) {
        this.fairValuePercentage = fairValuePercentage;
    }
}