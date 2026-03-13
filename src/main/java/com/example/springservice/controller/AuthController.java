package com.example.springservice.controller;

import com.example.springservice.dto.AccessTokenResponse;
import com.example.springservice.dto.CurrentUserResponse;
import com.example.springservice.entity.User;
import com.example.springservice.exception.ApiException;
import com.example.springservice.repository.UserRepository;
import com.example.springservice.security.AuthCookieService;
import com.example.springservice.security.AuthenticatedUser;
import com.example.springservice.security.AuthTokens;
import com.example.springservice.security.OAuth2RedirectService;
import com.example.springservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final OAuth2RedirectService oAuth2RedirectService;
    private final UserRepository userRepository;

    public AuthController(
        AuthService authService,
        AuthCookieService authCookieService,
        OAuth2RedirectService oAuth2RedirectService,
        UserRepository userRepository
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.oAuth2RedirectService = oAuth2RedirectService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login/google")
    public void loginWithGoogle(
        @RequestParam(name = "returnTo", required = false) String returnTo,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        oAuth2RedirectService.storeRequestedFrontendOrigin(request, returnTo);
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", "UNAUTHORIZED");
        }

        User user = userRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));
        return authService.toCurrentUserResponse(user);
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = authCookieService.extractRefreshToken(request);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh token 쿠키가 없습니다.", "REFRESH_TOKEN_MISSING");
        }

        AuthTokens authTokens = authService.refresh(rawRefreshToken, request);
        long cookieMaxAge = Math.max(
            0,
            authTokens.refreshTokenExpiresAt().toEpochSecond()
                - java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toEpochSecond()
        );
        authCookieService.addRefreshTokenCookie(response, authTokens.refreshToken(), cookieMaxAge);
        return new AccessTokenResponse(authTokens.accessToken(), "Bearer", authTokens.accessTokenExpiresAt());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authCookieService.extractRefreshToken(request));
        authCookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }
}
