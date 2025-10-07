package com.tim.appTim.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tim.appTim.dto.LoginRequest;
import com.tim.appTim.dto.LoginResponse;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.PasswordResetService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    public AuthController(UserService userService, JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          PasswordResetService passwordResetService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    // 🟢 REGISTER
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        userService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // 🟢 LOGIN — trả về Access + Refresh token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = userService.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());

            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS));
           // userService.save(user); // thêm hàm này trong UserService để cập nhật user

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login failed: " + e.getMessage());
        }
    }

    // 🟠 REFRESH TOKEN — lấy access token mới khi hết hạn
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsernameOrEmail(refreshToken);
        User user = userService.findByUsernameOrEmail(username);

        if (user == null || user.getRefreshToken() == null ||
                !user.getRefreshToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token mismatch");
        }

        if (user.getRefreshTokenExpiry().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token expired");
        }

        String newAccessToken = jwtUtil.generateAccessToken(username);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    // 🔴 LOGOUT — xóa refresh token để vô hiệu hóa đăng nhập lại
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsernameOrEmail(token);
            User user = userService.findByUsernameOrEmail(username);
            if (user != null) {
                user.setRefreshToken(null);
                user.setRefreshTokenExpiry(null);
                userService.save(user);
            }
        }
        return ResponseEntity.ok("Logout successful");
    }

    // 🟢 PASSWORD RESET (vẫn giữ nguyên)
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> requestBody,
                                                 HttpServletRequest request) {
        String email = requestBody.get("email");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        passwordResetService.requestReset(email, ip, userAgent);
        return ResponseEntity.ok("Nếu có tài khoản, chúng tôi đã gửi hướng dẫn đến email.");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String otp = requestBody.get("otp");
        try {
            String resetToken = passwordResetService.verifyOtp(email, otp);
            return ResponseEntity.ok(Map.of("reset_token", resetToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String resetToken = requestBody.get("reset_token");
        String newPassword = requestBody.get("newPassword");
        try {
            passwordResetService.resetPassword(email, resetToken, newPassword);
            return ResponseEntity.ok("Mật khẩu đã được thay đổi.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🟢 CORS CONFIG
    @Configuration
    public class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:5173")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    }
}
