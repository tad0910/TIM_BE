package com.tim.appTim.config;

import com.tim.appTim.repository.InvalidatedTokenRepository;
import com.tim.appTim.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
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
        String token = (String) authentication.getCredentials();

        try {
            io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
            String issuer = claims.getIssuer();
            if (issuer != null && issuer.toLowerCase().contains("keycloak")) {
                throw new BadCredentialsException("Skipping Keycloak token.");
            }

            String jti = jwtUtil.getClaims(token).getId();
            if (invalidatedTokenRepository.existsByJti(jti)) {
                throw new BadCredentialsException("Token has been invalidated (logged out).");
            }

            String usernameOrEmail = jwtUtil.extractUsernameOrEmail(token);
            if (usernameOrEmail != null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(usernameOrEmail);

                if (jwtUtil.isTokenValid(token)) {
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                }
            }
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid local JWT token", e);
        }

        throw new BadCredentialsException("Cannot authenticate local JWT token.");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}