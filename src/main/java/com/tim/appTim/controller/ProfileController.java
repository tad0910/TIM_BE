package com.tim.appTim.controller;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;


@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable Long userId) {
        ProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @GetMapping("/{userId}/images")
    public ResponseEntity<List<UserImageDTO>> getUserImages(@PathVariable Long userId) {
        List<UserImageDTO> images = userService.getUserImages(userId);
        return ResponseEntity.ok(images);
    }

    @PostMapping("/{userId}/images")
    public ResponseEntity<UserImageDTO> createUserImage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> requestBody) {
        String imageUrl = requestBody.get("imageUrl");
        String description = requestBody.get("description");
        if (imageUrl == null || imageUrl.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        UserImageDTO created = userService.createUserImage(userId, imageUrl, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{userId}/images/{imageId}")
    public ResponseEntity<UserImageDTO> updateUserImage(
            @PathVariable Long userId,
            @PathVariable Long imageId,
            @RequestBody Map<String, String> requestBody) {
        String imageUrl = requestBody.get("imageUrl");
        String description = requestBody.get("description");
        UserImageDTO updated = userService.updateUserImage(userId, imageId, imageUrl, description);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}/images/{imageId}")
    public ResponseEntity<Void> deleteUserImage(
            @PathVariable Long userId,
            @PathVariable Long imageId) {
        userService.deleteUserImage(userId, imageId);
        return ResponseEntity.noContent().build();
    }
}