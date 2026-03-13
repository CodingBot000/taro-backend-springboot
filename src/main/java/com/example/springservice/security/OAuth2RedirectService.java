package com.example.springservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OAuth2RedirectService {

    private static final String FRONTEND_ORIGIN_SESSION_KEY = "oauth2.frontendOrigin";

    private final AuthProperties authProperties;

    public OAuth2RedirectService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public void storeRequestedFrontendOrigin(HttpServletRequest request, String requestedOrigin) {
        String resolvedOrigin = resolveAllowedOrigin(requestedOrigin);
        HttpSession session = request.getSession(true);
        session.setAttribute(FRONTEND_ORIGIN_SESSION_KEY, resolvedOrigin);
    }

    public String buildSuccessRedirectUrl(HttpServletRequest request, AuthTokens authTokens) {
        return resolveFrontendCallbackBaseUrl(request)
            + "#accessToken=" + encode(authTokens.accessToken())
            + "&tokenType=Bearer"
            + "&expiresAt=" + encode(authTokens.accessTokenExpiresAt().toString());
    }

    public String buildFailureRedirectUrl(HttpServletRequest request, String errorCode) {
        return resolveFrontendCallbackBaseUrl(request)
            + "#error=" + encode(errorCode);
    }

    private String resolveFrontendCallbackBaseUrl(HttpServletRequest request) {
        String origin = consumeStoredOrigin(request);
        return origin + authProperties.getOauth2CallbackPath();
    }

    private String consumeStoredOrigin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return normalizeOrigin(authProperties.getFrontendBaseUrl());
        }

        Object storedOrigin = session.getAttribute(FRONTEND_ORIGIN_SESSION_KEY);
        session.removeAttribute(FRONTEND_ORIGIN_SESSION_KEY);
        if (storedOrigin instanceof String storedValue && !storedValue.isBlank()) {
            return storedValue;
        }

        return normalizeOrigin(authProperties.getFrontendBaseUrl());
    }

    private String resolveAllowedOrigin(String requestedOrigin) {
        String normalizedRequestedOrigin = normalizeOrigin(requestedOrigin);
        if (normalizedRequestedOrigin == null) {
            return normalizeOrigin(authProperties.getFrontendBaseUrl());
        }

        List<String> allowedOrigins = authProperties.getAllowedFrontendOrigins();
        for (String allowedOrigin : allowedOrigins) {
            String normalizedAllowedOrigin = normalizeOrigin(allowedOrigin);
            if (normalizedRequestedOrigin.equals(normalizedAllowedOrigin)) {
                return normalizedRequestedOrigin;
            }
        }

        return normalizeOrigin(authProperties.getFrontendBaseUrl());
    }

    private String normalizeOrigin(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }

            int port = uri.getPort();
            return port >= 0
                ? String.format("%s://%s:%d", scheme, host, port)
                : String.format("%s://%s", scheme, host);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
