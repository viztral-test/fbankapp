package com.fbankapp.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must be at most 50 characters")
    private String username;

    // BCrypt only reads the first 72 bytes of a password
    @NotBlank(message = "password is required")
    @Size(max = 72, message = "password must be at most 72 characters")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
