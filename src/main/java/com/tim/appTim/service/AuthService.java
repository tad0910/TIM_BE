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
        // 1. Verify token (optional but good practice)
        if (!jwtUtil.isTokenValid(token)) { // isTokenValid nên chỉ check chữ ký và thời gian hết hạn
            throw new IllegalArgumentException("Token không hợp lệ.");
        }

        // 2. Lấy thông tin từ token
        Claims claims = jwtUtil.getClaims(token);
        String jti = claims.getId();
        Date expiryDate = claims.getExpiration();

        // 3. Tạo đối tượng và lưu vào DB
        InvalidatedToken invalidatedToken = new InvalidatedToken(jti, expiryDate.toInstant());
            invalidatedTokenRepository.saveAndFlush(invalidatedToken);
    }
}