package com.example.springservice.security;

import com.example.springservice.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final AuthProperties authProperties;

    public OAuth2AuthenticationSuccessHandler(
        AuthService authService,
        AuthCookieService authCookieService,
        AuthProperties authProperties
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.authProperties = authProperties;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        AuthTokens authTokens = authService.loginWithGoogle(
            GoogleUserInfo.fromAttributes(oAuth2User.getAttributes()),
            request
        );
        long cookieMaxAge = Math.max(
            0,
            authTokens.refreshTokenExpiresAt().toEpochSecond()
                - java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toEpochSecond()
        );
        authCookieService.addRefreshTokenCookie(response, authTokens.refreshToken(), cookieMaxAge);
        invalidateSession(request);
        response.sendRedirect(buildSuccessRedirectUrl(authTokens));
    }

    private String buildSuccessRedirectUrl(AuthTokens authTokens) {
        return authProperties.getFrontendBaseUrl()
            + authProperties.getOauth2CallbackPath()
            + "#accessToken=" + encode(authTokens.accessToken())
            + "&tokenType=Bearer"
            + "&expiresAt=" + encode(authTokens.accessTokenExpiresAt().toString());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void invalidateSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
