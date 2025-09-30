package com.tim.appTim.repository;

import com.tim.appTim.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    List<UserImage> findByUserId(Long userId);
}