package com.fbankapp.config;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fbankapp")
public class FbankappProperties {

    /** How long a login token stays valid. The lifetime is fixed; it is not extended by activity. */
    private Duration tokenTtl = Duration.ofMinutes(60);

    /** Creates the local-testing demo account on startup when it does not exist yet. */
    private boolean seedDemoData = true;

    /** ISO 4217 code given to newly created wallets. */
    private String currency = "USD";

    /** Browser origins allowed to call /api/** (the Angular dev server by default). */
    private List<String> corsAllowedOrigins = Collections.singletonList("http://localhost:4200");

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }
}
