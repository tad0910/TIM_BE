package com.tim.appTim.config;

import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final UserRepository userRepository;

    public CustomJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        // Lấy username từ token của Keycloak
        String username = jwt.getClaimAsString("preferred_username");

        // Tìm user trong DB
        Optional<User> userOptional = userRepository.findByUsername(username);

        List<GrantedAuthority> authorities;
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Lấy role trong DB — chú ý thêm prefix ROLE_
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase()));
        } else {
            // Nếu không tìm thấy, gán role mặc định
            authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        }

        return new JwtAuthenticationToken(jwt, authorities, username);
    }
}
