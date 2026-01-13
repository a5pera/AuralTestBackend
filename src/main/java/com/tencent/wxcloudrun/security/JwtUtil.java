package com.tencent.wxcloudrun.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class JwtUtil {
    private final SecretKey key;

    public JwtUtil(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(long userId, String role, Instant expiresAt, String jti) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("uid", userId)
                .claim("role", role)
                .setId(jti) // 可选：JWT ID
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String issueAdminToken(long adminId, String passHashFinger, Instant expiresAt, String jti) {
        return Jwts.builder()
                .setSubject("ADMIN:" + adminId)
                .claim("role", "ADMIN")
                .claim("ph", passHashFinger)
                .setId(jti)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }
}
