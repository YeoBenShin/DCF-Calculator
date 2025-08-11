package com.dcf.dto;

import com.dcf.service.WatchlistService.PopularTicker;

public class PopularTickerDto {
    private String ticker;
    private int count;

    public PopularTickerDto() {}

    public PopularTickerDto(PopularTicker popularTicker) {
        this.ticker = popularTicker.getTicker();
        this.count = popularTicker.getCount();
    }

    public PopularTickerDto(String ticker, int count) {
        this.ticker = ticker;
        this.count = count;
    }

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}