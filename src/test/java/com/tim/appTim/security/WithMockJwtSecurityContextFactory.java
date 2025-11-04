package com.tim.appTim.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WithMockJwtSecurityContextFactory implements WithSecurityContextFactory<WithMockJwt> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwt annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Jwt jwt = new Jwt(
                "mock-token-value",
                null,
                null,
                Map.of("alg", "none"),
                Map.of(
                        "sub", annotation.value(),
                        "preferred_username", annotation.value()
                )
        );

        List<SimpleGrantedAuthority> authorities =
                List.of(annotation.authorities()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // ✅ Chỉ định principal là subject (điểm quan trọng)
        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());

        context.setAuthentication(authentication);
        return context;
    }
}
