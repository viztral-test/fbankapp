package com.fbankapp.auth.dto;

import java.time.Instant;

public class LoginResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final Instant expiresAt;
    private final String username;
    private final String displayName;

    public LoginResponse(String token, Instant expiresAt, String username, String displayName) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.username = username;
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }
}
