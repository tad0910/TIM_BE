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
        try {
            return keycloakJwtAuthenticationProvider.authenticate(authentication);
        } catch (AuthenticationException keycloakException) {
            UsernamePasswordAuthenticationToken customToken = new UsernamePasswordAuthenticationToken(null, tokenString);
            try {
                return customJwtAuthenticationProvider.authenticate(customToken);
            } catch (AuthenticationException localJwtException) {
                throw localJwtException;
            }
        }
    }
}