package com.tim.appTim.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tim.appTim.entity.UserImage;

public interface UserImageService {

    UserImage save(UserImage userImage);
    UserImage findById(Long id);
    UserImage findLatestByUserId(Long userId);
    void delete(Long id);
    void deleteAllByUserId(Long userId);

    Page<UserImage> findAllByUserId(Long userId, Pageable pageable);
}