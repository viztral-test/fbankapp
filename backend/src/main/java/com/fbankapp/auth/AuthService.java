package com.fbankapp.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fbankapp.auth.dto.LoginResponse;
import com.fbankapp.config.FbankappProperties;
import com.fbankapp.error.ApiException;
import com.fbankapp.user.AppUser;
import com.fbankapp.user.AppUserRepository;

/**
 * Opaque bearer tokens: login issues a random 256-bit token, the server keeps only its SHA-256 hash with an
 * expiry, and logout deletes it.
 */
@Service
public class AuthService {

    private static final int TOKEN_BYTES = 32;

    private final AppUserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final FbankappProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyPasswordHash;

    public AuthService(AppUserRepository userRepository, AuthTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, FbankappProperties properties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        Optional<AppUser> candidate = userRepository.findByUsername(normalizeUsername(username));
        // Always run one BCrypt check so an unknown username takes as long as a wrong password
        String hash = candidate.map(AppUser::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);
        if (!candidate.isPresent() || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        AppUser user = candidate.get();

        Instant now = Instant.now();
        tokenRepository.deleteByExpiresAtBefore(now);

        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant expiresAt = now.plus(properties.getTokenTtl());
        tokenRepository.save(new AuthToken(sha256Hex(token), user, now, expiresAt));

        return new LoginResponse(token, expiresAt, user.getUsername(), user.getDisplayName());
    }

    /** Returns the id of the user the token belongs to, or empty when it is unknown or expired. */
    @Transactional(readOnly = true)
    public Optional<Long> authenticate(String token) {
        return tokenRepository.findByTokenHash(sha256Hex(token))
                .filter(authToken -> authToken.getExpiresAt().isAfter(Instant.now()))
                .map(authToken -> authToken.getUser().getId());
    }

    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByTokenHash(sha256Hex(token));
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
