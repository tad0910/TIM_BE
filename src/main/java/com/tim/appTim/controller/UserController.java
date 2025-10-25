package com.tim.appTim.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.UserClassDTO;
import com.tim.appTim.entity.ClassMember;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import com.tim.appTim.service.UserImageService;
import com.tim.appTim.exception.*; // << Thêm dòng này

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserImageService userImageService;
    private final ClassService classService;

    @Value("${upload.folder}")
    private String uploadFolder;

    public UserController(UserService userService, UserImageService userImageService, ClassService classService) {
        this.userService = userService;
        this.userImageService = userImageService;
        this.classService = classService;
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
        return ResponseEntity.ok(userService.getUserProfileByEmail(email));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
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
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        User user = userService.findById(id);
        if (user == null) throw new ResourceNotFoundException("User không tồn tại");

        File uploadDir = new File(uploadFolder);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String uniqueFilename = UUID.randomUUID() + fileExtension;

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
    }

    @GetMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> getProfileImage(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User không tồn tại");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("profileImage", user.getProfileImage());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/profile-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> updateProfileImage(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BadRequestException("imageUrl không được để trống");
        }

        User updatedUser = userService.updateProfileImage(id, imageUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ảnh đại diện đã được cập nhật thành công");
        response.put("user", updatedUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cover-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> uploadCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }

        User user = userService.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User không tồn tại");
        }

        File uploadDir = new File(uploadFolder);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains(".")) ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID() + fileExtension;

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
    }

    @GetMapping("/{id}/cover-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> getCoverImage(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User không tồn tại");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("coverImage", user.getCoverImage());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cover-image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #id)")
    public ResponseEntity<?> updateCoverImage(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BadRequestException("imageUrl không được để trống");
        }

        User updatedUser = userService.updateCoverImage(id, imageUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ảnh bìa đã được cập nhật thành công");
        response.put("user", updatedUser);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}/classes")
    public ResponseEntity<?> getUserClasses(@PathVariable Long id) {
        List<ClassMember> userClasses = classService.getUserClasses(id);
        List<UserClassDTO> classDTOs = userClasses.stream()
                .map(c -> new UserClassDTO(
                        c.getClassId(),
                        c.getClassEntity() != null ? c.getClassEntity().getClassName() : "N/A",
                        c.getClassEntity() != null ? c.getClassEntity().getDescription() : "N/A",
                        c.getRole().name(),
                        c.getJoinDate()
                )).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("userId", id);
        response.put("classes", classDTOs);
        response.put("totalClasses", classDTOs.size());

        return ResponseEntity.ok(response);
    }
}
