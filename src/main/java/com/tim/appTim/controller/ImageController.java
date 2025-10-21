package com.tim.appTim.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.service.UserImageService;
import com.tim.appTim.service.UserService;

@RestController
@RequestMapping("/api/users")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserImageService userImageService;

    @Value("${upload.folder}")
    private String uploadFolder;

    @PostMapping("/{userId}/image")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<String> uploadImage(@PathVariable Long userId, @RequestParam("file") MultipartFile file, Authentication authentication) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            File uploadDir = new File(uploadFolder);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            Path filePath = Paths.get(uploadFolder + File.separator + uniqueFilename);
            Files.write(filePath, file.getBytes());

            User user = userService.findById(userId);
            if (user == null) {
                Files.deleteIfExists(filePath);
                return ResponseEntity.notFound().build();
            }

            UserImage userImage = new UserImage();
            userImage.setUserId(userId);
            userImage.setImageUrl("/uploads/" + uniqueFilename);
            userImage.setCreatedAt(LocalDateTime.now());
            userImageService.save(userImage);

            logger.info("Image uploaded for userId {}: {}", userId, uniqueFilename);

            return ResponseEntity.ok("Image uploaded successfully: " + uniqueFilename);
        } catch (IOException e) {
            logger.error("Failed to upload image for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/image")
    public ResponseEntity<List<UserImage>> getAllImages(@PathVariable Long userId) {
        List<UserImage> userImages = userImageService.findAllByUserId(userId);
        if (userImages == null || userImages.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userImages);
    }


    @DeleteMapping("/{userId}/image/{imageId}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<String> deleteImage(@PathVariable Long userId, @PathVariable Long imageId, Authentication authentication) {
        UserImage userImage = userImageService.findById(imageId);
        if (userImage == null || !userImage.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }

        userImageService.delete(imageId);
        logger.info("Image {} deleted for userId: {}", imageId, userId);
        return ResponseEntity.ok("Image deleted successfully");
    }
}