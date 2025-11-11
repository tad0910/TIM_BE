package com.tim.appTim.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tim.appTim.entity.UserImage;

import java.util.List;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    Page<UserImage> findByUserId(Long userId, Pageable pageable);
    List<UserImage> findByUserId(Long userId);
    UserImage findTopByUserIdOrderByCreatedAtDesc(Long userId);
}