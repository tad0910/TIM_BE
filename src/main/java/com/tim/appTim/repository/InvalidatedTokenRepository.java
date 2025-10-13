package com.tim.appTim.repository;

import com.tim.appTim.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    // Phương thức kiểm tra xem một JTI đã tồn tại trong DB hay chưa
    boolean existsByJti(String jti);
}