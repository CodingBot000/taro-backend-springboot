package com.example.springservice.dto;

import java.time.OffsetDateTime;

public record AccessTokenResponse(
    String accessToken,
    String tokenType,
    OffsetDateTime expiresAt
) {
}
