package com.tim.appTim.util;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.tim.appTim.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.tim.appTim.entity.User;

@Component
public class JwtUtil {

    private final String SECRET_KEY = "12345678901234567890123456789012"; // Thay bằng secret của bạn
    private final long EXPIRATION_MS = 1000 * 60 * 60; // 1 giờ

    @Autowired
    private UserService userService;

    public String generateAccessToken(String usernameOrEmail) {
        return Jwts.builder()
                .setSubject(usernameOrEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1h
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .setId(UUID.randomUUID().toString())
                .compact();
    }

    public String generateRefreshToken(String usernameOrEmail) {
        return Jwts.builder()
                .setSubject(usernameOrEmail)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7 ngày
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsernameOrEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);
            String usernameOrEmail = claims.getSubject();
            User user = userService.findByUsernameOrEmail(usernameOrEmail);  // Thêm method này ở UserService nếu chưa có
            Date issuedAt = claims.getIssuedAt();
            if (user.getPasswordChangedAt() != null && issuedAt.before(Date.from(user.getPasswordChangedAt()))) {
                return false;  // Invalidate nếu token issued trước password change
            }
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}