package com.tim.appTim.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import com.tim.appTim.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.password = :password WHERE u.email = :email")
    void updatePasswordByEmail(String email, String password);
}