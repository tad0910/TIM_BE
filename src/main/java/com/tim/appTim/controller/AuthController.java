package com.tim.appTim.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tim.appTim.dto.request.LoginRequest;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.PasswordResetService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil;
import com.tim.appTim.service.AuthService;
import com.tim.appTim.dto.response.LoginResponse;
import com.tim.appTim.dto.response.UserResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil,
            @Qualifier("loginManager") AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService, AuthService authService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        userService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = userService.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());

            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS));

            UserResponse userResponse = new UserResponse(user);
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userResponse);

            return ResponseEntity.ok(loginResponse);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }

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

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsernameOrEmail(token);
                User user = userService.findByUsernameOrEmail(username);
                if (user != null) {
                    authService.logout(token);
                    user.setRefreshToken(null);
                    user.setRefreshTokenExpiry(null);
                    userService.save(user);
                }
            }
            return ResponseEntity.ok("Logout successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đăng xuất: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String email = requestBody.get("email");

        if (email == null || email.trim().isEmpty()) {
            System.out.println("Email không được để trống");
            return ResponseEntity.status(401).body("Email không được để trống");
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            System.out.println("Email không tồn tại trong hệ thống");
            return ResponseEntity.status(401).body("Email không tồn tại trong hệ thống");
        }

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

    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> changeUserPassword(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu không được để trống"));
            }
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy người dùng"));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordChangedAt(Instant.now());
            userService.internalSave(user);
            return ResponseEntity.ok(Map.of("message", "Mật khẩu người dùng đã được cập nhật thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
