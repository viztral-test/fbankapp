package com.fbankapp.wallet;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fbankapp.auth.AuthInterceptor;
import com.fbankapp.wallet.dto.AddMoneyRequest;
import com.fbankapp.wallet.dto.AddMoneyResponse;
import com.fbankapp.wallet.dto.BalanceResponse;
import com.fbankapp.wallet.dto.TransactionPageResponse;

/**
 * No route accepts a user or wallet id: the owner always comes from the bearer token, so one user cannot
 * address another user's records.
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public BalanceResponse balance(@RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) Long userId) {
        return walletService.getBalance(userId);
    }

    @PostMapping("/add-money")
    @ResponseStatus(HttpStatus.CREATED)
    public AddMoneyResponse addMoney(@RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) Long userId,
            @Valid @RequestBody AddMoneyRequest request) {
        return walletService.addMoney(userId, request.getAmount(), request.getNote());
    }

    @GetMapping("/transactions")
    public TransactionPageResponse transactions(@RequestAttribute(AuthInterceptor.USER_ID_ATTRIBUTE) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletService.getTransactions(userId, page, size);
    }
}
