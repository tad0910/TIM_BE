package com.tim.appTim.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.InternalServerErrorException;
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
    public ResponseEntity<String> uploadImage(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (file.isEmpty()) {
            throw new BadRequestException("File tải lên bị trống");
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
                throw new ResourceNotFoundException("Không tìm thấy người dùng có ID: " + userId);
            }

            UserImage userImage = new UserImage();
            userImage.setUserId(userId);
            userImage.setImageUrl("/uploads/" + uniqueFilename);
            userImage.setCreatedAt(LocalDateTime.now());
            userImageService.save(userImage);

            logger.info("Tải ảnh thành công cho userId {}: {}", userId, uniqueFilename);
            return ResponseEntity.ok("Tải ảnh thành công: " + uniqueFilename);

        } catch (IOException e) {
            logger.error("Lỗi khi tải ảnh cho userId {}: {}", userId, e.getMessage());
            throw new InternalServerErrorException("Không thể lưu ảnh: " + e.getMessage());
        }
    }

    @GetMapping("/{userId}/image")
    public ResponseEntity<Page<UserImage>> getAllImages(@PathVariable Long userId,
                                                         Pageable pageable) {
        Page<UserImage> userImagesPage = userImageService.findAllByUserId(userId, pageable);
        return ResponseEntity.ok(userImagesPage);
    }

    @DeleteMapping("/{userId}/image/{imageId}")
    @PreAuthorize("hasAuthority('user:update_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<String> deleteImage(
            @PathVariable Long userId,
            @PathVariable Long imageId,
            Authentication authentication) {

        UserImage userImage = userImageService.findById(imageId);
        if (userImage == null || !userImage.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy ảnh hoặc ảnh không thuộc về userId: " + userId);
        }

        userImageService.delete(imageId);
        logger.info("Đã xóa ảnh {} cho userId: {}", imageId, userId);
        return ResponseEntity.ok("Xóa ảnh thành công");
    }
}
