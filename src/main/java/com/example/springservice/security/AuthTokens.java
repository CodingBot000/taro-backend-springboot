package com.example.springservice.security;

import java.time.OffsetDateTime;

public record AuthTokens(
    String accessToken,
    OffsetDateTime accessTokenExpiresAt,
    String refreshToken,
    OffsetDateTime refreshTokenExpiresAt
) {
}
