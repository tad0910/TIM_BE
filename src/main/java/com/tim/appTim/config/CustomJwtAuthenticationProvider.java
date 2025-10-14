package com.tim.appTim.config;

import com.tim.appTim.repository.InvalidatedTokenRepository;
import com.tim.appTim.util.JwtUtil; // Sử dụng JwtUtil của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public CustomJwtAuthenticationProvider(JwtUtil jwtUtil, UserDetailsService userDetailsService, InvalidatedTokenRepository invalidatedTokenRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Lấy token string từ đối tượng Authentication
        String token = (String) authentication.getCredentials();

        // ❗️ Logic được chuyển từ JwtAuthenticationFilter vào đây ❗️
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
            String issuer = claims.getIssuer();
            if (issuer != null && issuer.toLowerCase().contains("keycloak")) {
                // Đây là token của Keycloak, provider này không xử lý nó.
                // Ném lỗi để DelegatingAuthenticationManager biết và dừng lại.
                throw new BadCredentialsException("Skipping Keycloak token.");
            }

            // Kiểm tra token có bị vô hiệu hóa (logout) không
            String jti = jwtUtil.getClaims(token).getId();
            if (invalidatedTokenRepository.existsByJti(jti)) {
                throw new BadCredentialsException("Token has been invalidated (logged out).");
            }

            // Giải mã và xác thực token
            String usernameOrEmail = jwtUtil.extractUsernameOrEmail(token);
            if (usernameOrEmail != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(usernameOrEmail);

                if (jwtUtil.isTokenValid(token)) {
                    // Nếu token hợp lệ, trả về một đối tượng Authentication đã được xác thực
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                }
            }
        } catch (Exception e) {
            // Nếu có bất kỳ lỗi nào trong quá trình xử lý token nội bộ, báo hiệu xác thực thất bại
            throw new BadCredentialsException("Invalid local JWT token", e);
        }

        // Nếu không xác thực được, ném exception
        throw new BadCredentialsException("Cannot authenticate local JWT token.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Provider này chỉ xử lý loại token mà chúng ta tạo ra trong DelegatingAuthenticationManager
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}