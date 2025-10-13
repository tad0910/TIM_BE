package com.tim.appTim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.tim.appTim.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // JwtAuthenticationFilter của bạn vẫn có thể được giữ lại nếu bạn muốn
    // hỗ trợ cả luồng đăng nhập cũ, nhưng nó không cần thiết cho việc xác thực Keycloak.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // 1. Endpoint để đăng nhập/đăng ký cục bộ (nếu có) và các trang public
                        .requestMatchers("/auth/**").permitAll()

                        // 2. Bảo vệ tất cả các API nghiệp vụ chính.
                        // Chỉ những ai có token hợp lệ mới được truy cập.
                        .requestMatchers(
                                "/users/**",
                                "/profile/**",
                                "/classes/**",
                                "/posts/**",
                                "/comments/**",
                                "/reactions/**"
                        ).authenticated()

                        // 3. Bảo vệ các API quản trị của Keycloak
                        .requestMatchers("/api/v1/keycloak/**").authenticated()

                        // Mọi request khác cũng cần xác thực
                        .anyRequest().authenticated()
                )
                // 4. Kích hoạt cấu hình Resource Server để xác thực JWT từ Keycloak
                .oauth2ResourceServer(oauth2 -> oauth2.jwt())

                // 5. Đặt chế độ session là STATELESS (quan trọng cho API)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Dòng addFilterBefore không còn cần thiết cho việc xác thực token của Keycloak
        // vì .oauth2ResourceServer() đã xử lý việc đó một cách tự động và chuẩn hóa.

        return http.build();
    }

    // Các bean này dành cho luồng xác thực username/password cục bộ.
    // Bạn có thể giữ lại nếu muốn hỗ trợ cả hai luồng.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}