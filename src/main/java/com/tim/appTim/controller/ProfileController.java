package com.tim.appTim.controller;

import java.util.List;
import java.util.Map;

import com.tim.appTim.dto.UserUpdateDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import com.tim.appTim.exception.BadRequestException;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(userService.getUserProfile(userId, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody UserUpdateDTO userDTO) {
        return ResponseEntity.ok(userService.update(id, userDTO));
    }

    @GetMapping("/{userId}/images")
    public ResponseEntity<List<UserImageDTO>> getUserImages(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserImages(userId));
    }

    @PostMapping("/{userId}/images")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<UserImageDTO> createUserImage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody) {

        String imageUrl = requestBody.get("imageUrl");
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("imageUrl không được để trống");
        }

        String description = requestBody.get("description");
        UserImageDTO created = userService.createUserImage(userId, imageUrl, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{userId}/images/{imageId}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<UserImageDTO> updateUserImage(
            @PathVariable Long userId,
            @PathVariable Long imageId,
            @RequestBody Map<String, String> requestBody) {

        String imageUrl = requestBody.get("imageUrl");
        String description = requestBody.get("description");

        return ResponseEntity.ok(userService.updateUserImage(userId, imageId, imageUrl, description));
    }

    @DeleteMapping("/{userId}/images/{imageId}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<Void> deleteUserImage(@PathVariable Long userId, @PathVariable Long imageId) {
        userService.deleteUserImage(userId, imageId);
        return ResponseEntity.noContent().build();
    }
}
