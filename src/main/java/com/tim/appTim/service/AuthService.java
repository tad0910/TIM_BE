package com.tim.appTim.service;

import com.tim.appTim.entity.InvalidatedToken;
import com.tim.appTim.repository.InvalidatedTokenRepository;
import com.tim.appTim.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Date;

@Service
public class AuthService {

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final JwtUtil jwtUtil;

    public AuthService(InvalidatedTokenRepository invalidatedTokenRepository, JwtUtil jwtUtil) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    public void logout(String token) {
        if (!jwtUtil.isTokenValid(token)) {
            throw new IllegalArgumentException("Token không hợp lệ.");
        }

        Claims claims = jwtUtil.getClaims(token);
        String jti = claims.getId();
        Date expiryDate = claims.getExpiration();

        InvalidatedToken invalidatedToken = new InvalidatedToken(jti, expiryDate.toInstant());
            invalidatedTokenRepository.saveAndFlush(invalidatedToken);
    }
}