package com.example.springservice.service;

import com.example.springservice.dto.CurrentUserResponse;
import com.example.springservice.entity.OAuthProvider;
import com.example.springservice.entity.User;
import com.example.springservice.entity.UserOAuthAccount;
import com.example.springservice.entity.UserRole;
import com.example.springservice.entity.UserStatus;
import com.example.springservice.exception.ApiException;
import com.example.springservice.repository.UserOAuthAccountRepository;
import com.example.springservice.repository.UserRepository;
import com.example.springservice.security.AuthTokens;
import com.example.springservice.security.GoogleUserInfo;
import com.example.springservice.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthService(
        UserRepository userRepository,
        UserOAuthAccountRepository userOAuthAccountRepository,
        RefreshTokenService refreshTokenService,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.userOAuthAccountRepository = userOAuthAccountRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokens loginWithGoogle(GoogleUserInfo googleUserInfo, HttpServletRequest request) {
        if (googleUserInfo.subject() == null || googleUserInfo.subject().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google 사용자 식별자가 없습니다.", "OAUTH_SUBJECT_MISSING");
        }

        User user = resolveOrCreateGoogleUser(googleUserInfo);
        ensureActiveUser(user);
        user.setName(defaultName(googleUserInfo.name()));
        user.setProfileImageUrl(googleUserInfo.picture());
        user.setEmail(googleUserInfo.email());
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);

        String rawRefreshToken = refreshTokenService.generateRawToken();
        OffsetDateTime refreshExpiresAt = refreshTokenService.calculateExpiry();
        refreshTokenService.save(
            user.getId(),
            rawRefreshToken,
            refreshExpiresAt,
            request.getHeader("User-Agent"),
            resolveClientIp(request)
        );
        return jwtService.issueAccessToken(user, rawRefreshToken, refreshExpiresAt);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken, HttpServletRequest request) {
        var storedToken = refreshTokenService.findActiveToken(rawRefreshToken)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "유효한 refresh token이 없습니다.", "INVALID_REFRESH_TOKEN"));
        refreshTokenService.revoke(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));
        ensureActiveUser(user);

        String nextRefreshToken = refreshTokenService.generateRawToken();
        OffsetDateTime refreshExpiresAt = refreshTokenService.calculateExpiry();
        refreshTokenService.save(
            user.getId(),
            nextRefreshToken,
            refreshExpiresAt,
            request.getHeader("User-Agent"),
            resolveClientIp(request)
        );
        return jwtService.issueAccessToken(user, nextRefreshToken, refreshExpiresAt);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenService.findActiveToken(rawRefreshToken).ifPresent(refreshTokenService::revoke);
    }

    public CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getProfileImageUrl(),
            user.getRole().name(),
            user.getStatus().name(),
            user.getLastLoginAt()
        );
    }

    private User resolveOrCreateGoogleUser(GoogleUserInfo googleUserInfo) {
        Optional<UserOAuthAccount> existingAccount = userOAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.GOOGLE,
            googleUserInfo.subject()
        );
        if (existingAccount.isPresent()) {
            return resolveUserForAccount(existingAccount.get(), googleUserInfo);
        }

        User user = createOrReuseUser(googleUserInfo);
        UserOAuthAccount account = new UserOAuthAccount();
        account.setUserId(user.getId());
        account.setProvider(OAuthProvider.GOOGLE);
        account.setProviderUserId(googleUserInfo.subject());
        account.setProviderEmail(googleUserInfo.email());
        userOAuthAccountRepository.save(account);
        return user;
    }

    private User resolveUserForAccount(UserOAuthAccount account, GoogleUserInfo googleUserInfo) {
        Optional<User> existingUser = userRepository.findById(account.getUserId());
        if (existingUser.isPresent()) {
            account.setProviderEmail(googleUserInfo.email());
            userOAuthAccountRepository.save(account);
            return existingUser.get();
        }

        User replacementUser = createOrReuseUser(googleUserInfo);
        account.setUserId(replacementUser.getId());
        account.setProviderEmail(googleUserInfo.email());
        userOAuthAccountRepository.save(account);
        return replacementUser;
    }

    private User createOrReuseUser(GoogleUserInfo googleUserInfo) {
        if (googleUserInfo.email() != null && !googleUserInfo.email().isBlank()) {
            Optional<User> existingUser = userRepository.findByEmail(googleUserInfo.email());
            if (existingUser.isPresent()) {
                return existingUser.get();
            }
        }

        User user = new User();
        user.setEmail(googleUserInfo.email());
        user.setName(defaultName(googleUserInfo.name()));
        user.setProfileImageUrl(googleUserInfo.picture());
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            if (googleUserInfo.email() != null && !googleUserInfo.email().isBlank()) {
                return userRepository.findByEmail(googleUserInfo.email())
                    .orElseThrow(() -> exception);
            }
            throw exception;
        }
    }

    private void ensureActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "활성 상태의 사용자만 로그인할 수 있습니다.", "USER_NOT_ACTIVE");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String defaultName(String candidate) {
        return (candidate == null || candidate.isBlank()) ? "Google User" : candidate;
    }
}
