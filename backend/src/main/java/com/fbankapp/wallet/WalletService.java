package com.fbankapp.wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fbankapp.error.ApiException;
import com.fbankapp.wallet.dto.AddMoneyResponse;
import com.fbankapp.wallet.dto.BalanceResponse;
import com.fbankapp.wallet.dto.TransactionPageResponse;
import com.fbankapp.wallet.dto.TransactionResponse;

/**
 * Every method takes the id of the authenticated user and only ever reads or writes that user's wallet.
 */
@Service
public class WalletService {

    static final int MAX_PAGE_SIZE = 100;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        return BalanceResponse.from(walletRepository.findByUserId(userId).orElseThrow(WalletService::walletNotFound));
    }

    /** Simulated top-up: credits the wallet and records the transaction. No payment provider is involved. */
    @Transactional
    public AddMoneyResponse addMoney(Long userId, BigDecimal amount, String note) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount must be positive");
        }
        BigDecimal credit;
        try {
            credit = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount must have at most 2 decimal places");
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId).orElseThrow(WalletService::walletNotFound);
        Instant now = Instant.now();
        wallet.credit(credit, now);

        String description = StringUtils.hasText(note) ? note.trim() : "Wallet top-up (simulated)";
        WalletTransaction transaction = transactionRepository.save(new WalletTransaction(
                wallet, TransactionType.TOP_UP, credit, wallet.getBalance(), description, now));

        return new AddMoneyResponse(TransactionResponse.from(transaction), BalanceResponse.from(wallet));
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(Long userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<WalletTransaction> result = transactionRepository.findByWalletUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.of(safePage, safeSize));
        List<TransactionResponse> items = result.getContent().stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
        return new TransactionPageResponse(items, safePage, safeSize, result.getTotalElements(),
                result.getTotalPages());
    }

    private static ApiException walletNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "Wallet not found");
    }
}
