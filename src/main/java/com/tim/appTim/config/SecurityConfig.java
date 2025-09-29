package com.tim.appTim.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.tim.appTim.service.UserService;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Vô hiệu hóa CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Thêm cấu hình CORS
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/register", "/auth/login", "/auth/logout").permitAll() // Cho phép truy cập
                .anyRequest().authenticated() // Các request khác yêu cầu xác thực
            )
            .authenticationProvider(authenticationProvider) // Đăng ký authentication provider
            .formLogin().disable() // Vô hiệu hóa form login
            .logout().disable() // Vô hiệu hóa logout
            .httpBasic().disable(); // Vô hiệu hóa HTTP Basic
        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:5173"); // Cho phép origin của frontend
        configuration.addAllowedMethod("*"); // Cho phép tất cả các phương thức (GET, POST, etc.)
        configuration.addAllowedHeader("*"); // Cho phép tất cả header
        configuration.setAllowCredentials(true); // Cho phép gửi cookie/auth nếu cần
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Áp dụng cho tất cả endpoint
        return source;
    }
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