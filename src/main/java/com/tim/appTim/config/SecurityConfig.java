package com.tim.appTim.config;

import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.UserService; // Cần import UserService
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final CustomJwtAuthenticationProvider customJwtAuthenticationProvider;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;

     public SecurityConfig(UserRepository userRepository,
                           CustomJwtAuthenticationProvider customJwtAuthenticationProvider,
                           JwtDecoder jwtDecoder,
                           UserService userService) {
         this.userRepository = userRepository;
         this.customJwtAuthenticationProvider = customJwtAuthenticationProvider;
         this.jwtDecoder = jwtDecoder;
         this.userService = userService;
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
    @Qualifier("loginManager")
    public AuthenticationManager loginAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // Bean này được inject vào AuthController để xử lý login
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationProvider keycloakJwtAuthenticationProvider() {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
        provider.setJwtAuthenticationConverter(new CustomJwtAuthenticationConverter(userRepository));
        return provider;
    }

    @Bean
    public AuthenticationManager tokenAuthenticationManager(JwtAuthenticationProvider keycloakJwtAuthenticationProvider) {
        // Bean này là "bộ não" phân loại token
        return new DelegatingAuthenticationManager(keycloakJwtAuthenticationProvider, customJwtAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager tokenAuthenticationManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthenticationProvider()) // Đăng ký provider cho luồng login
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(
                                "/users/**",
                                "/profile/**",
                                "/classes/**",
                                "/posts/**",
                                "/comments/**",
                                "/reactions/**"
                        ).hasRole("SINH_VIEN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.authenticationManager(tokenAuthenticationManager))
                        .authenticationEntryPoint(new CustomAuthEntryPoint())
                );

        return http.build();
    }
}