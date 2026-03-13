package com.example.springservice.security;

import com.example.springservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String ROLE = "role";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecretBytes(jwtProperties.getSecret()));
    }

    public AuthTokens issueAccessToken(User user, String refreshToken, OffsetDateTime refreshTokenExpiresAt) {
        OffsetDateTime accessTokenExpiresAt = OffsetDateTime.now(ZoneOffset.UTC)
            .plusSeconds(jwtProperties.getAccessTokenExpirationSeconds());
        String accessToken = Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim(TOKEN_TYPE, ACCESS_TOKEN_TYPE)
            .claim(ROLE, user.getRole().name())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(accessTokenExpiresAt.toInstant()))
            .signWith(signingKey)
            .compact();
        return new AuthTokens(accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE, String.class))) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }

    private byte[] resolveSecretBytes(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException | DecodingException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
