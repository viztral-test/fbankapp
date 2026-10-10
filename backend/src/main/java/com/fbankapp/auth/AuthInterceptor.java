package com.fbankapp.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fbankapp.error.ApiException;

/**
 * Requires "Authorization: Bearer &lt;token&gt;" and exposes the authenticated user's id to controllers as a
 * request attribute.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "fbankapp.auth.userId";
    public static final String TOKEN_ATTRIBUTE = "fbankapp.auth.token";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS preflight requests never carry the Authorization header
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        Long userId = authService.authenticate(token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        request.setAttribute(USER_ID_ATTRIBUTE, userId);
        request.setAttribute(TOKEN_ATTRIBUTE, token);
        return true;
    }
}
