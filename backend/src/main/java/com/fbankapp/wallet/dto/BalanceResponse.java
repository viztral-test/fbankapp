package com.fbankapp.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fbankapp.wallet.Wallet;

public class BalanceResponse {

    private final BigDecimal balance;
    private final String currency;
    private final Instant updatedAt;

    public BalanceResponse(BigDecimal balance, String currency, Instant updatedAt) {
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public static BalanceResponse from(Wallet wallet) {
        return new BalanceResponse(wallet.getBalance(), wallet.getCurrency(), wallet.getUpdatedAt());
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
