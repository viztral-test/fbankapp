package com.fbankapp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fbankapp.auth.AuthInterceptor;

@Configuration
@EnableConfigurationProperties(FbankappProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final FbankappProperties properties;

    public WebConfig(AuthInterceptor authInterceptor, FbankappProperties properties) {
        this.authInterceptor = authInterceptor;
        this.properties = properties;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Everything under /api needs a valid bearer token except the login call itself
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Tokens travel in the Authorization header, so cookies/credentials are not allowed cross-origin
        registry.addMapping("/api/**")
                .allowedOrigins(properties.getCorsAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .maxAge(3600);
    }
}
