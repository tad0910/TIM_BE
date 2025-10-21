package com.tim.appTim.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import com.tim.appTim.dto.ProfileResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import com.tim.appTim.service.UserImageService;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserImageService userImageService;

    @Value("${upload.folder}")
    private String uploadFolder;

    public UserController(UserService userService, UserImageService userImageService) {
        this.userService = userService;
        this.userImageService = userImageService;
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('user:read_all')")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }


    @GetMapping("/profile/{email}")
    public ResponseEntity<ProfileResponse> getUserProfileByEmail(@PathVariable String email) {
        ProfileResponse profile = userService.getUserProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<User> create(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity.created(URI.create("/users/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<User> update(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findById(id);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User không tồn tại");
                return ResponseEntity.notFound().build();
            }

            File uploadDir = new File(uploadFolder);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg" ;
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            Path filePath = Paths.get(uploadFolder + File.separator + uniqueFilename);
            Files.write(filePath, file.getBytes());

            String imageUrl = "/uploads/" + uniqueFilename;

            com.tim.appTim.entity.UserImage userImage = new com.tim.appTim.entity.UserImage();
            userImage.setUserId(id);
            userImage.setImageUrl(imageUrl);
            userImage.setCreatedAt(java.time.LocalDateTime.now());
            userImageService.save(userImage);

            User updatedUser = userService.updateProfileImage(id, imageUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ảnh đại diện đã được cập nhật thành công");
            response.put("imageUrl", imageUrl);
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{id}/profile-image")
    public ResponseEntity<?> getProfileImage(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", id);
            response.put("profileImage", user.getProfileImage());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> updateProfileImage(@PathVariable Long id, @RequestBody Map<String, String> request) {
        // ... (code của bạn giữ nguyên)
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "imageUrl không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            User updatedUser = userService.updateProfileImage(id, imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ảnh đại diện đã được cập nhật thành công");
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/cover-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> uploadCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        // ... (code của bạn giữ nguyên)
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findById(id);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User không tồn tại");
                return ResponseEntity.notFound().build();
            }

            File uploadDir = new File(uploadFolder);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg" ;
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            Path filePath = Paths.get(uploadFolder + File.separator + uniqueFilename);
            Files.write(filePath, file.getBytes());

            String imageUrl = "/uploads/" + uniqueFilename;

            com.tim.appTim.entity.UserImage userImage = new com.tim.appTim.entity.UserImage();
            userImage.setUserId(id);
            userImage.setImageUrl(imageUrl);
            userImage.setCreatedAt(java.time.LocalDateTime.now());
            userImageService.save(userImage);

            User updatedUser = userService.updateCoverImage(id, imageUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ảnh bìa đã được cập nhật thành công");
            response.put("imageUrl", imageUrl);
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi khi upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{id}/cover-image")
    public ResponseEntity<?> getCoverImage(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", id);
            response.put("coverImage", user.getCoverImage());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/cover-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> updateCoverImage(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "imageUrl không được để trống");
                return ResponseEntity.badRequest().body(error);
            }

            User updatedUser = userService.updateCoverImage(id, imageUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ảnh bìa đã được cập nhật thành công");
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
