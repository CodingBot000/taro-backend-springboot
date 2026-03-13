package com.example.springservice.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final OAuth2RedirectService oAuth2RedirectService;

    public OAuth2AuthenticationFailureHandler(OAuth2RedirectService oAuth2RedirectService) {
        this.oAuth2RedirectService = oAuth2RedirectService;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        String failureRedirectUrl = oAuth2RedirectService.buildFailureRedirectUrl(request, "oauth_login_failed");
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(failureRedirectUrl);
    }
}
