package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates the JWTs used by the /api/** REST surface. The access
 * token is short-lived and returned in the response body only (the frontend
 * keeps it in memory, never localStorage). The refresh token is longer-lived
 * and only ever transported as an httpOnly cookie.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;

    @Value("${app.jwt.access-token-ttl-minutes:15}")
    private long accessTokenTtlMinutes;

    @Value("${app.jwt.refresh-token-ttl-days:7}")
    private long refreshTokenTtlDays;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, TOKEN_TYPE_ACCESS, accessTokenTtlMinutes * 60_000L);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, TOKEN_TYPE_REFRESH, refreshTokenTtlDays * 24 * 60 * 60_000L);
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlMinutes * 60L;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlDays * 24 * 60 * 60L;
    }

    private String buildToken(User user, String tokenType, long ttlMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Parses and validates an access token, returning its claims if valid. */
    public Optional<Claims> parseAccessToken(String token) {
        return parseToken(token, TOKEN_TYPE_ACCESS);
    }

    /** Parses and validates a refresh token, returning its claims if valid. */
    public Optional<Claims> parseRefreshToken(String token) {
        return parseToken(token, TOKEN_TYPE_REFRESH);
    }

    private Optional<Claims> parseToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String getEmail(Claims claims) {
        return claims.getSubject();
    }

    public Long getUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }
}
