package com.tim.appTim.config;

import com.tim.appTim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager; // SỬA ĐỔI: Import class này
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// BỎ BỎ BỎ: import org.springframework.security.oauth2.jwt.JwtDecoder;
// BỎ BỎ BỎ: import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Qualifier;
// BỎ BỎ BỎ: import org.springframework.beans.factory.annotation.Autowired;
// BỎ BỎ BỎ: import com.tim.appTim.repository.UserRepository;

import javax.ws.rs.HttpMethod;
import java.util.Collections; // SỬA ĐỔI: Import class này


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // BỎ BỎ BỎ: private final UserRepository userRepository;
    private final CustomJwtAuthenticationProvider customJwtAuthenticationProvider;
    // BỎ BỎ BỎ: private final JwtDecoder jwtDecoder;
    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // SỬA ĐỔI: Constructor đã được đơn giản hóa, loại bỏ JwtDecoder và UserRepository
    public SecurityConfig(CustomJwtAuthenticationProvider customJwtAuthenticationProvider,
                          UserService userService,
                          JwtAuthenticationFilter jwtAuthFilter) {
        this.customJwtAuthenticationProvider = customJwtAuthenticationProvider;
        this.userService = userService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    @Qualifier("loginManager") // Dùng qualifier để phân biệt
    public AuthenticationManager loginAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // BỎ BỎ BỎ: Bean 'keycloakJwtAuthenticationProvider' đã bị xóa
    // @Bean
    // public JwtAuthenticationProvider keycloakJwtAuthenticationProvider() { ... }

    @Bean
    @Qualifier("tokenManager") // Dùng qualifier để phân biệt
    public AuthenticationManager tokenAuthenticationManager() {
        // SỬA ĐỔI: Chỉ dùng 'customJwtAuthenticationProvider' của bạn
        // Bọc nó trong ProviderManager để nó trở thành một AuthenticationManager
        // Bean này rất có thể được 'jwtAuthFilter' của bạn sử dụng
        return new ProviderManager(Collections.singletonList(customJwtAuthenticationProvider));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { // SỬA ĐỔI: Bỏ AuthenticationManager khỏi tham số
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/v1/keycloak/**").permitAll() // Bạn nên xem lại endpoint này
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/link-preview/**").permitAll()
                        .anyRequest().authenticated()
                )
                // BỎ BỎ BỎ: Đã loại bỏ toàn bộ khối .oauth2ResourceServer() để tránh xung đột
                // .oauth2ResourceServer(oauth2 -> oauth2
                //         .jwt(jwt -> jwt.authenticationManager(tokenAuthenticationManager))
                //         .authenticationEntryPoint(new CustomAuthEntryPoint())
                // )

                // Filter 'jwtAuthFilter' của bạn vẫn được giữ nguyên
                // Đây sẽ là cơ chế chính xử lý token JWT
                .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                // THÊM VÀO: Thêm entry point cho lỗi 401
                // (Vì .oauth2ResourceServer() đã bị xóa, chúng ta cần tự thêm)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new CustomAuthEntryPoint())
                );
        return http.build();
    }
}

