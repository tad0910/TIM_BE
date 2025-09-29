package com.tim.appTim.util;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "12345678901234567890123456789012"; // Thay bằng secret của bạn
    private final long EXPIRATION_MS = 1000 * 60 * 60; // 1 giờ

    public String generateToken(String usernameOrEmail) {
        return Jwts.builder()
            .setSubject(usernameOrEmail)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUsernameOrEmail(String token) { // Đổi tên để rõ ràng
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            return !getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}