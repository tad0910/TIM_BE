package com.tim.appTim.service;

import com.tim.appTim.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set; // Import thêm
import java.util.stream.Collectors; // Import thêm

public class UserDetailsImpl implements UserDetails {
    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // *** LOGIC MỚI BẮT ĐẦU TỪ ĐÂY ***

        // 1. Lấy tất cả permissions từ tất cả roles của user
        Set<GrantedAuthority> authorities = user.getRoles().stream() // Lấy Set<Role> mới
                .flatMap(role -> role.getPermissions().stream()) // Biến thành một Stream<Permission>
                .map(permission -> new SimpleGrantedAuthority(permission.getName())) // Chuyển thành "user:read", "post:create"
                .collect(Collectors.toSet());

        // 2. (Tùy chọn) Thêm cả tên Role vào danh sách
        user.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority(role.getName()))
        );

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }

    public User getUser() {
        return user;
    }
}