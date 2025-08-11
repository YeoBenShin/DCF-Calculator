package com.dcf.dto;

import java.util.List;

public class UserDto {
    private String userId;
    private String email;
    private List<String> watchlist;

    public UserDto() {}

    public UserDto(String userId, String email, List<String> watchlist) {
        this.userId = userId;
        this.email = email;
        this.watchlist = watchlist;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(List<String> watchlist) {
        this.watchlist = watchlist;
    }
}