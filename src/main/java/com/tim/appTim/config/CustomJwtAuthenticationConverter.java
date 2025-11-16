package com.tim.appTim.config;


import java.util.Set;
import java.util.stream.Collectors;

import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.Optional;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final UserRepository userRepository;

    public CustomJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        Optional<User> userOptional = userRepository.findByUsername(username);

        Set<GrantedAuthority> authorities;

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            authorities = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                    .collect(Collectors.toSet());

            user.getRoles().forEach(role ->
                    authorities.add(new SimpleGrantedAuthority(role.getName()))
            );

        } else {
            authorities = Set.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        }

        return new JwtAuthenticationToken(jwt, authorities, username);
    }
}