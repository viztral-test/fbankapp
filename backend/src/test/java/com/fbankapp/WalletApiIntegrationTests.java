package com.fbankapp;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbankapp.auth.AuthService;
import com.fbankapp.auth.AuthToken;
import com.fbankapp.auth.AuthTokenRepository;
import com.fbankapp.config.DemoDataSeeder;
import com.fbankapp.user.AppUser;
import com.fbankapp.user.AppUserRepository;
import com.fbankapp.wallet.Wallet;
import com.fbankapp.wallet.WalletRepository;

@SpringBootTest
@AutoConfigureMockMvc
class WalletApiIntegrationTests {

    private static final String PASSWORD = "Test@5678";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private AuthTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void demoAccountIsSeededWithHashedPasswordAndOpeningBalance() throws Exception {
        AppUser demo = userRepository.findByUsername(DemoDataSeeder.DEMO_USERNAME).get();
        assertNotEquals(DemoDataSeeder.DEMO_PASSWORD, demo.getPasswordHash());

        String token = login(DemoDataSeeder.DEMO_USERNAME, DemoDataSeeder.DEMO_PASSWORD);
        mockMvc.perform(get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00))
                .andExpect(jsonPath("$.currency").value("USD"));
        mockMvc.perform(get("/api/wallet/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("OPENING_BALANCE"));
    }

    @Test
    void loginReturnsBearerTokenAndStoresOnlyItsHash() throws Exception {
        String username = newUser();
        String token = login(username, PASSWORD);

        assertEquals(1, tokenRepository.findAll().stream()
                .filter(t -> t.getTokenHash().equals(AuthService.sha256Hex(token))).count());
        assertEquals(0, tokenRepository.findAll().stream()
                .filter(t -> t.getTokenHash().equals(token)).count());
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownUserWithTheSameMessage() throws Exception {
        String username = newUser();
        mockMvc.perform(loginRequest(username, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        mockMvc.perform(loginRequest("no-such-user", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginValidatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void protectedRoutesRejectMissingInvalidAndExpiredTokens() throws Exception {
        mockMvc.perform(get("/api/wallet/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        mockMvc.perform(get("/api/wallet/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/wallet/add-money").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 10.00}"))
                .andExpect(status().isUnauthorized());

        AppUser user = userRepository.findByUsername(newUser()).get();
        String expired = "expired-" + UUID.randomUUID();
        Instant now = Instant.now();
        tokenRepository.save(new AuthToken(AuthService.sha256Hex(expired), user, now.minusSeconds(7200),
                now.minusSeconds(3600)));
        mockMvc.perform(get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addMoneyCreditsWalletAndRecordsTransaction() throws Exception {
        String token = login(newUser(), PASSWORD);

        mockMvc.perform(addMoney(token, "{\"amount\": 250.50, \"note\": \"Lunch money\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transaction.type").value("TOP_UP"))
                .andExpect(jsonPath("$.transaction.amount").value(250.50))
                .andExpect(jsonPath("$.transaction.balanceAfter").value(250.50))
                .andExpect(jsonPath("$.transaction.description").value("Lunch money"))
                .andExpect(jsonPath("$.wallet.balance").value(250.50));
        // 0.1 + 0.2 must be exactly 0.30 on top of the previous balance
        mockMvc.perform(addMoney(token, "{\"amount\": 0.1}")).andExpect(status().isCreated());
        mockMvc.perform(addMoney(token, "{\"amount\": 0.2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wallet.balance").value(250.80));

        MvcResult balance = mockMvc.perform(
                get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        // The raw JSON keeps two decimals
        assertTrue(balance.getResponse().getContentAsString().contains("\"balance\":250.80,"));

        mockMvc.perform(get("/api/wallet/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].amount").value(0.20))
                .andExpect(jsonPath("$.items[2].amount").value(250.50));
    }

    @Test
    void addMoneyRejectsInvalidAmountsAndLeavesBalanceUnchanged() throws Exception {
        String token = login(newUser(), PASSWORD);
        String[] invalidBodies = {
                "{}",
                "{\"amount\": null}",
                "{\"amount\": 0}",
                "{\"amount\": -5.00}",
                "{\"amount\": 1.234}",
                "{\"amount\": 10000.01}",
                "{\"amount\": \"abc\"}",
                "not json"
        };
        for (String body : invalidBodies) {
            mockMvc.perform(addMoney(token, body)).andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.00));
        mockMvc.perform(get("/api/wallet/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void usersOnlySeeTheirOwnWalletAndTransactions() throws Exception {
        String aliceToken = login(newUser(), PASSWORD);
        String bobToken = login(newUser(), PASSWORD);

        mockMvc.perform(addMoney(aliceToken, "{\"amount\": 75.00}")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(jsonPath("$.balance").value(0.00));
        // Extra query parameters cannot redirect the lookup to another user
        mockMvc.perform(get("/api/wallet/transactions?userId=1&walletId=1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get("/api/wallet/transactions").header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void logoutInvalidatesTheToken() throws Exception {
        String token = login(newUser(), PASSWORD);
        mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/wallet/balance").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsPreflightFromAngularDevServerIsAllowedWithoutToken() throws Exception {
        mockMvc.perform(options("/api/wallet/balance")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        mockMvc.perform(options("/api/wallet/balance")
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private String newUser() {
        String username = "user-" + UUID.randomUUID().toString().substring(0, 8);
        AppUser user = userRepository.save(new AppUser(username, passwordEncoder.encode(PASSWORD), "Test User"));
        walletRepository.save(new Wallet(user, "USD", Instant.now()));
        return username;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(username, password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
            String username, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}");
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder addMoney(
            String token, String body) {
        return post("/api/wallet/add-money").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
