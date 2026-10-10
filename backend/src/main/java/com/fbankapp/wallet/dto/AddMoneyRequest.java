package com.fbankapp.wallet.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class AddMoneyRequest {

    public static final String MIN_AMOUNT = "0.01";
    public static final String MAX_AMOUNT = "10000.00";

    @NotNull(message = "amount is required")
    @DecimalMin(value = MIN_AMOUNT, message = "amount must be at least " + MIN_AMOUNT)
    @DecimalMax(value = MAX_AMOUNT, message = "amount must be at most " + MAX_AMOUNT)
    @Digits(integer = 5, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    @Size(max = 80, message = "note must be at most 80 characters")
    private String note;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
