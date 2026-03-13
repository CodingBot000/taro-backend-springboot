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
    private final OAuth2RedirectService oAuth2RedirectService;

    public OAuth2AuthenticationSuccessHandler(
        AuthService authService,
        AuthCookieService authCookieService,
        OAuth2RedirectService oAuth2RedirectService
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.oAuth2RedirectService = oAuth2RedirectService;
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
        String successRedirectUrl = oAuth2RedirectService.buildSuccessRedirectUrl(request, authTokens);
        invalidateSession(request);
        response.sendRedirect(successRedirectUrl);
    }

    private void invalidateSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
