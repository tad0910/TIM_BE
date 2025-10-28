package com.tim.appTim.controller;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.File;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.service.CommentService;
import com.tim.appTim.service.UserService;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;


    @Value("${upload.folder}")
    private String uploadFolder;
    
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @PostMapping("/posts/{postId}")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<CommentDTO> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> multipartFiles) throws IOException {

        User currentUser = getUserFromAuthentication(authentication);
        List<File> files = new ArrayList<>();

        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            for (MultipartFile mf : multipartFiles) {
                if (mf.isEmpty()) continue;

                String originalName = mf.getOriginalFilename();
                String fileExt = originalName != null && originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf("."))
                        : "";

                String uniqueName = UUID.randomUUID().toString() + fileExt;
                Path uploadPath = Paths.get(uploadFolder, uniqueName);
                Files.createDirectories(uploadPath.getParent());
                Files.write(uploadPath, mf.getBytes());

                String lowerName = originalName != null ? originalName.toLowerCase() : "";
                File.FileType fileType;
                if (lowerName.matches(".*\\.(mp4|mov|avi)$")) {
                    fileType = File.FileType.VIDEO;
                } else if (lowerName.matches(".*\\.(pdf|docx?|xlsx?|pptx?|txt|rtf|zip|rar|7z)$")) {
                    fileType = File.FileType.DOCUMENT;
                } else {
                    fileType = File.FileType.IMAGE;
                }

                File f = new File();
                f.setFileUrl("/uploads/" + uniqueName);
                f.setFileName(originalName);
                f.setFileSize(mf.getSize());
                f.setFileType(fileType);
                files.add(f);
            }
        }

        CommentDTO createdComment = commentService.createCommentWithFiles(
                currentUser.getId(),
                postId,
                content,
                files
        );

        return ResponseEntity.ok(createdComment);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(required = false) String emotion) {

        User currentUser = getUserFromAuthentication(authentication);
        Comment.Emotion emotionEnum = parseEmotion(emotion, Comment.Emotion.class);

        return ResponseEntity.ok(commentService.updateComment(commentId, currentUser, authentication, content, emotionEnum));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        commentService.deleteComment(commentId, currentUser, authentication);
        return ResponseEntity.ok("Comment deleted successfully");
    }

    @PostMapping("{commentId}/replies")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<ReplyCommentDTO> createReply(
            Authentication authentication,
            @PathVariable Long commentId,
            @RequestParam("content") String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> multipartFiles) throws IOException {

        User currentUser = getUserFromAuthentication(authentication);
        List<File> files = new ArrayList<>();

        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            for (MultipartFile mf : multipartFiles) {
                if (mf.isEmpty()) continue;

                String originalName = mf.getOriginalFilename();
                String fileExt = originalName != null && originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf("."))
                        : "";

                String uniqueName = UUID.randomUUID().toString() + fileExt;
                Path uploadPath = Paths.get(uploadFolder, uniqueName);
                Files.createDirectories(uploadPath.getParent());
                Files.write(uploadPath, mf.getBytes());

                String lowerName = originalName != null ? originalName.toLowerCase() : "";
                File.FileType fileType;
                if (lowerName.matches(".*\\.(mp4|mov|avi)$")) {
                    fileType = File.FileType.VIDEO;
                } else if (lowerName.matches(".*\\.(pdf|docx?|xlsx?|pptx?|txt|rtf|zip|rar|7z)$")) {
                    fileType = File.FileType.DOCUMENT;
                } else {
                    fileType = File.FileType.IMAGE;
                }

                File f = new File();
                f.setFileUrl("/uploads/" + uniqueName);
                f.setFileName(originalName);
                f.setFileSize(mf.getSize());
                f.setFileType(fileType);
                files.add(f);
            }
        }

        ReplyCommentDTO createdReply = commentService.createReplyWithFiles(
                currentUser.getId(),
                commentId,
                content,
                files
        );

        return ResponseEntity.ok(createdReply);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<ReplyCommentDTO>> getReplyCommentsByCommentId(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplyCommentsByCommentId(commentId));
    }

    @PutMapping("/replies/{replyCommentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReplyCommentDTO> updateReplyComment(
            @PathVariable Long replyCommentId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        ReplyComment.Emotion emotionEnum = parseEmotion(emotion, ReplyComment.Emotion.class);

        return ResponseEntity.ok(
                commentService.updateReplyComment(currentUser, authentication, replyCommentId, content, emotionEnum)
        );
    }

    @DeleteMapping("/replies/{replyCommentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> deleteReplyComment(@PathVariable Long replyCommentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        commentService.deleteReplyComment(currentUser, authentication, replyCommentId);
        return ResponseEntity.ok("Reply comment deleted successfully");
    }

    private <T extends Enum<T>> T parseEmotion(String value, Class<T> enumType) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Emotion không hợp lệ: " + value);
        }
    }
}
