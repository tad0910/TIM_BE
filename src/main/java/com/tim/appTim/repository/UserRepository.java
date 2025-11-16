package com.tim.appTim.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT id FROM users WHERE username = ?1 LIMIT 1", nativeQuery = true)
    Long findIdByUsernameIncludingDeleted(String username);

    @Query(value = "SELECT id FROM users WHERE email = ?1 LIMIT 1", nativeQuery = true)
    Long findIdByEmailIncludingDeleted(String email);

    @Query(value = "SELECT id FROM users WHERE so_dien_thoai = ?1 LIMIT 1", nativeQuery = true)
    Long findIdByPhoneNumberIncludingDeleted(String phoneNumber);

    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET deleted = 0 WHERE id = ?1", nativeQuery = true)
    void restoreById(Long id);

    @Query(value = "SELECT * FROM users", countQuery = "SELECT count(*) FROM users", nativeQuery = true)
    Page<User> findAllIncludingDeleted(Pageable pageable);

    @Query(value = "SELECT * FROM users WHERE id = ?1 AND deleted = 1", nativeQuery = true)
    Optional<User> findDeletedById(Long id);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findAnyByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.keycloakId = :keycloakId")
    Optional<User> findAnyByKeycloakId(String keycloakId);

    @Query(value = "SELECT * FROM users WHERE deleted = 1",
            countQuery = "SELECT count(*) FROM users WHERE deleted = 1",
            nativeQuery = true)
    Page<User> findAllDeleted(Pageable pageable);
}