package com.tim.appTim.dto;

public class LoginResponse {
    private String token;  // Placeholder, có thể thay bằng session ID hoặc JWT

    public LoginResponse(String token) {
        this.token = token;
    }

    // Getters/Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}