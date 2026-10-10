package com.fbankapp.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fbankapp.wallet.TransactionType;
import com.fbankapp.wallet.WalletTransaction;

public class TransactionResponse {

    private final String reference;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final Instant createdAt;

    public TransactionResponse(String reference, TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
            String description, Instant createdAt) {
        this.reference = reference;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static TransactionResponse from(WalletTransaction transaction) {
        return new TransactionResponse(transaction.getReference(), transaction.getType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getDescription(), transaction.getCreatedAt());
    }

    public String getReference() {
        return reference;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
