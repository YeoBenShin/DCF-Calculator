package com.dcf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WatchlistRequest {
    @NotBlank(message = "Ticker symbol is required")
    @Size(min = 1, max = 10, message = "Ticker symbol must be between 1 and 10 characters")
    private String ticker;

    public WatchlistRequest() {}

    public WatchlistRequest(String ticker) {
        this.ticker = ticker;
    }

    // Getters and setters
    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
}