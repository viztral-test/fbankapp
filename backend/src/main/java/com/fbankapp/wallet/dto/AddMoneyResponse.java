package com.fbankapp.wallet.dto;

public class AddMoneyResponse {

    private final TransactionResponse transaction;
    private final BalanceResponse wallet;

    public AddMoneyResponse(TransactionResponse transaction, BalanceResponse wallet) {
        this.transaction = transaction;
        this.wallet = wallet;
    }

    public TransactionResponse getTransaction() {
        return transaction;
    }

    public BalanceResponse getWallet() {
        return wallet;
    }
}
