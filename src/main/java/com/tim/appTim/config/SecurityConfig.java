package com.tim.appTim.config;

import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.HttpMethod;


@Configuration
@EnableWebSecurity

@EnableMethodSecurity

public class SecurityConfig {

    private final UserRepository userRepository;
    private final CustomJwtAuthenticationProvider customJwtAuthenticationProvider;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    @Autowired
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(UserRepository userRepository,
                          CustomJwtAuthenticationProvider customJwtAuthenticationProvider,
                          JwtDecoder jwtDecoder,
                          UserService userService, JwtAuthenticationFilter jwtAuthFilter) {
        this.userRepository = userRepository;
        this.customJwtAuthenticationProvider = customJwtAuthenticationProvider;
        this.jwtDecoder = jwtDecoder;
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
    @Qualifier("loginManager")
    public AuthenticationManager loginAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
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
        return new DelegatingAuthenticationManager(keycloakJwtAuthenticationProvider, customJwtAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager tokenAuthenticationManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/keycloak/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers("/link-preview/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/profile/**").permitAll()
                        .requestMatchers("/news/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.authenticationManager(tokenAuthenticationManager))
                        .authenticationEntryPoint(new CustomAuthEntryPoint())
                )

                .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}