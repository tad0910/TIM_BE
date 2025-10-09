package com.tim.appTim.controller;

import java.util.ArrayList;
import java.util.List;

// Thêm các import cho Logger
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.service.PostService;

@RestController
@RequestMapping("/posts")
public class PostController {

    // 1. Khai báo Logger
    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/create")
    public ResponseEntity<PostDTO> createPost(
            @RequestParam("userId") Long userId,
            @RequestParam("content") String content,
            @RequestParam("privacy") String privacy,
            @RequestParam(value = "files", required = false) List<MultipartFile> multipartFiles
    ) {
        try {
            logger.info("Received request to create post for userId: {}", userId);
            Post.Privacy privacyEnum = Post.Privacy.valueOf(privacy);
            List<File> files = new ArrayList<>();

            if (multipartFiles != null) {
                // ... (giữ nguyên logic xử lý file)
                for (MultipartFile mf : multipartFiles) {
                    File f = new File();
                    f.setFileUrl(mf.getOriginalFilename());
                    String lowerName = mf.getOriginalFilename().toLowerCase();
                    if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".avi")) {
                        f.setFileType(File.FileType.VIDEO);
                    } else if (lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx")
                            || lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")
                            || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")
                            || lowerName.endsWith(".txt") || lowerName.endsWith(".rtf")
                            || lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z")) {
                        f.setFileType(File.FileType.DOCUMENT);
                    } else {
                        f.setFileType(File.FileType.IMAGE);
                    }
                    files.add(f);
                }
            }


            PostDTO createdPost = postService.createPostWithFiles(userId, content, privacyEnum, files);
            return ResponseEntity.ok(createdPost);

        } catch (IllegalArgumentException e) {
            // 2. Thêm lệnh ghi log lỗi vào đây
            logger.error("Error creating post due to invalid privacy value: {}", privacy, e);
            return ResponseEntity.badRequest().body(null);

        } catch (Exception e) {
            logger.error("An unexpected error occurred while creating post", e);
            e.printStackTrace(); // Có thể giữ lại để debug chi tiết
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping
    public ResponseEntity<Page<PostDTO>> getAllPosts(Pageable pageable) {
        Page<PostDTO> posts = postService.getAllPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable Long userId) {
        List<PostDTO> posts = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId, // Dùng để xác thực chủ sở hữu
            @RequestParam("content") String content,
            @RequestParam("privacy") String privacy) {
        try {
            Post.Privacy privacyEnum = Post.Privacy.valueOf(privacy.toLowerCase());
            PostDTO updatedPost = postService.updatePost(userId, postId, content, privacyEnum);
            return ResponseEntity.ok(updatedPost);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid privacy value: {}", privacy);
            return ResponseEntity.badRequest().build();
        }
        // Các exception ResourceNotFoundException và UnauthorizedException sẽ tự được Spring xử lý
    }
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long postId,
            @RequestParam("userId") Long userId) { // Dùng để xác thực
        postService.deletePost(userId, postId);
        return ResponseEntity.ok("Post with id " + postId + " deleted successfully.");
    }
}