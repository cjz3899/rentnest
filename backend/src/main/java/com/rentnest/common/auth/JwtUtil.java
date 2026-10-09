package com.rentnest.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final String CLAIM_TYP = "typ";
    private static final String TYP_ACCESS = "ACCESS";
    private static final String TYP_REFRESH = "REFRESH";

    private final SecretKey key;
    private final long accessExpireHours;
    private final Duration refreshTtl;

    public JwtUtil(@Value("${rentnest.jwt.secret}") String secret,
                   @Value("${rentnest.jwt.access-expire-hours:2}") long accessExpireHours,
                   @Value("${rentnest.jwt.refresh-expire-days:30}") long refreshExpireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpireHours = accessExpireHours;
        this.refreshTtl = Duration.ofDays(refreshExpireDays);
    }

    public String issueAccess(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim(CLAIM_TYP, TYP_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpireHours, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    public record RefreshGrant(String token, String jti) {
    }

    public RefreshGrant issueRefresh(Long userId, String role) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim(CLAIM_TYP, TYP_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtl)))
                .signWith(key)
                .compact();
        return new RefreshGrant(token, jti);
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    public CurrentUser parseAccess(String token) {
        Claims claims = parse(token);
        if (!TYP_ACCESS.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new JwtException("token类型错误");
        }
        return new CurrentUser(Long.valueOf(claims.getSubject()), claims.get("role", String.class));
    }

    public record RefreshClaims(Long userId, String jti, String role) {
    }

    public RefreshClaims parseRefresh(String token) {
        Claims claims = parse(token);
        if (!TYP_REFRESH.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new JwtException("token类型错误");
        }
        return new RefreshClaims(Long.valueOf(claims.getSubject()), claims.getId(),
                claims.get("role", String.class));
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
