package com.tim.appTim.config;

// Import các thư viện cần thiết
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.HashMap;
import java.util.Map;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Rất quan trọng để @PreAuthorize hoạt động
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth ->
                        // Yêu cầu xác thực, để @WithMockUser có tác dụng
                        auth.anyRequest().authenticated()
                )
                // --- PHẦN QUAN TRỌNG NHẤT ĐỂ FIX LỖI 500 ---
                .exceptionHandling(exceptions -> exceptions
                        // Xử lý khi user đã đăng nhập nhưng không có quyền (403 Forbidden)
                        .accessDeniedHandler(accessDeniedHandler())
                        // Xử lý khi user chưa đăng nhập (401 Unauthorized)
                        .authenticationEntryPoint(authenticationEntryPoint())
                );

        return http.build();
    }

    /**
     * Bean này sẽ "bắt" lỗi AuthorizationDeniedException
     * và trả về 403 JSON thay vì 500.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.FORBIDDEN.value());
            body.put("error", "Forbidden");
            body.put("message", "Bạn không có quyền truy cập tài nguyên này");
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    /**
     * Bean này xử lý khi truy cập mà không có @WithMockUser
     * (trả về 401 JSON).
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            Map<String, Object> body = new HashMap<>();
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "Unauthorized");
            body.put("message", "Yêu cầu xác thực");
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}