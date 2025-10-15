package com.tim.appTim.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;

@RequiredArgsConstructor
public class DelegatingAuthenticationManager implements AuthenticationManager {

    private final JwtAuthenticationProvider keycloakJwtAuthenticationProvider;
    private final CustomJwtAuthenticationProvider customJwtAuthenticationProvider;

     public DelegatingAuthenticationManager(JwtAuthenticationProvider keycloakJwtAuthenticationProvider, CustomJwtAuthenticationProvider customJwtAuthenticationProvider) {
         this.keycloakJwtAuthenticationProvider = keycloakJwtAuthenticationProvider;
         this.customJwtAuthenticationProvider = customJwtAuthenticationProvider;
     }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String tokenString = ((BearerTokenAuthenticationToken) authentication).getToken();

        // 1. Thử xác thực bằng Keycloak trước
        try {
            return keycloakJwtAuthenticationProvider.authenticate(authentication);
        } catch (AuthenticationException keycloakException) {
            // 2. Nếu Keycloak thất bại, thử xác thực bằng provider JWT cục bộ
            UsernamePasswordAuthenticationToken customToken = new UsernamePasswordAuthenticationToken(null, tokenString);
            try {
                return customJwtAuthenticationProvider.authenticate(customToken);
            } catch (AuthenticationException localJwtException) {
                // 3. Nếu cả hai đều thất bại, ném ra lỗi ban đầu của Keycloak (hoặc lỗi của local)
                throw localJwtException;
            }
        }
    }
}