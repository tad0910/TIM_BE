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
}