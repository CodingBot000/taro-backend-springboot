package com.example.springservice.dto;

import java.time.OffsetDateTime;

public record CurrentUserResponse(
    Long id,
    String email,
    String name,
    String profileImageUrl,
    String role,
    String status,
    OffsetDateTime lastLoginAt
) {
}
