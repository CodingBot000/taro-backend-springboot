package com.example.springservice.service;

import com.example.springservice.entity.RefreshToken;
import com.example.springservice.repository.RefreshTokenRepository;
import com.example.springservice.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public String generateRawToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public OffsetDateTime calculateExpiry() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(jwtProperties.getRefreshTokenExpirationSeconds());
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is required", exception);
        }
    }

    @Transactional
    public RefreshToken save(Long userId, String rawToken, OffsetDateTime expiresAt, String userAgent, String ipAddress) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setUserAgent(truncate(userAgent, 500));
        refreshToken.setIpAddress(truncate(ipAddress, 45));
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findActiveToken(String rawToken) {
        return refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken(rawToken))
            .filter(token -> token.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        if (refreshToken.getRevokedAt() == null) {
            refreshToken.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
            refreshTokenRepository.save(refreshToken);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
