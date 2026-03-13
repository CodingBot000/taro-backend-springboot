package com.example.springservice.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    private final AuthProperties authProperties;

    public AuthCookieService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                authProperties.getRefreshCookieName(),
                refreshToken
            )
            .httpOnly(true)
            .secure(authProperties.isRefreshCookieSecure())
            .sameSite(authProperties.getRefreshCookieSameSite())
            .path(authProperties.getRefreshCookiePath())
            .maxAge(maxAgeSeconds);
        if (authProperties.getRefreshCookieDomain() != null && !authProperties.getRefreshCookieDomain().isBlank()) {
            builder.domain(authProperties.getRefreshCookieDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                authProperties.getRefreshCookieName(),
                ""
            )
            .httpOnly(true)
            .secure(authProperties.isRefreshCookieSecure())
            .sameSite(authProperties.getRefreshCookieSameSite())
            .path(authProperties.getRefreshCookiePath())
            .maxAge(0);
        if (authProperties.getRefreshCookieDomain() != null && !authProperties.getRefreshCookieDomain().isBlank()) {
            builder.domain(authProperties.getRefreshCookieDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
