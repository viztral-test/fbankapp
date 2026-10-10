package com.fbankapp.config;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fbankapp.user.AppUser;
import com.fbankapp.user.AppUserRepository;
import com.fbankapp.wallet.TransactionType;
import com.fbankapp.wallet.Wallet;
import com.fbankapp.wallet.WalletRepository;
import com.fbankapp.wallet.WalletTransaction;
import com.fbankapp.wallet.WalletTransactionRepository;

/**
 * Seeds one demo account for LOCAL TESTING ONLY. The credentials are public (they are in the README), so
 * set fbankapp.seed-demo-data=false anywhere that is not a developer machine.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "Demo@1234";
    public static final BigDecimal DEMO_OPENING_BALANCE = new BigDecimal("1000.00");

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final FbankappProperties properties;
    private final AppUserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(FbankappProperties properties, AppUserRepository userRepository,
            WalletRepository walletRepository, WalletTransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isSeedDemoData() || userRepository.existsByUsername(DEMO_USERNAME)) {
            return;
        }
        Instant now = Instant.now();
        AppUser user = userRepository.save(
                new AppUser(DEMO_USERNAME, passwordEncoder.encode(DEMO_PASSWORD), "Demo User"));
        Wallet wallet = new Wallet(user, properties.getCurrency(), now);
        wallet.credit(DEMO_OPENING_BALANCE, now);
        walletRepository.save(wallet);
        transactionRepository.save(new WalletTransaction(wallet, TransactionType.OPENING_BALANCE,
                DEMO_OPENING_BALANCE, wallet.getBalance(), "Opening balance (demo data)", now));
        log.warn("Seeded demo account '{}' for local testing only", DEMO_USERNAME);
    }
}
