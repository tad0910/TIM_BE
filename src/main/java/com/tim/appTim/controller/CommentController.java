package com.tim.appTim.controller;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.File.FileType;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.repository.FileRepository;
import com.tim.appTim.service.CommentService;
import com.tim.appTim.service.UserService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tim.appTim.entity.File;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;
    private final FileRepository fileRepository;

    @Value("${upload.folder}")
    private String uploadFolder;

    @Autowired
    public CommentController(CommentService commentService, UserService userService, FileRepository fileRepository) {
        this.commentService = commentService;
        this.userService = userService;
        this.fileRepository = fileRepository;
    }

    private List<com.tim.appTim.entity.File> createNewFileEntities(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        return files.stream()
                .filter(file -> !file.isEmpty())
                .map(file -> {
                    com.tim.appTim.entity.File newFile = new com.tim.appTim.entity.File();

                    String contentType = file.getContentType();
                    newFile.setFileType(determineFileType(contentType));
                    newFile.setFileUrl("/test/url/" + file.getOriginalFilename());

                    newFile.setFileName(file.getOriginalFilename());
                    newFile.setFileSize(file.getSize());

                    return newFile;
                })
                .collect(Collectors.toList());
    }

    private FileType determineFileType(String contentType) {
        if (contentType == null) {
            return FileType.DOCUMENT;
        }

        if (contentType.startsWith("image/")) {
            return FileType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return FileType.VIDEO;
        } else {
            return FileType.DOCUMENT;
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:update_all") || a.getAuthority().equals("comment:delete_all"));
    }

    private Long getEffectiveUserId(Long commentId, Long currentUserId, Authentication authentication) {
        if (hasAdminAuthority(authentication)) {
            Comment comment = commentService.getCommentById(commentId);
            return comment.getUser().getId();
        }
        return currentUserId;
    }

    private Long getEffectiveUserIdForReply(Long replyCommentId, Long currentUserId, Authentication authentication) {
        if (hasAdminAuthority(authentication)) {
            ReplyComment reply = commentService.getReplyCommentById(replyCommentId);
            return reply.getUser().getId();
        }
        return currentUserId;
    }

    @PostMapping("/posts/{postId}")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String emotion,
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
        Comment.Emotion emotionEnum = parseEmotion(emotion, Comment.Emotion.class);
        CommentDTO createdComment = commentService.createComment(
                postId,
                currentUser.getId(),
                content,
                emotionEnum,
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
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String emotion,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        User currentUser = getUserFromAuthentication(authentication);
        Comment.Emotion emotionEnum = parseEmotion(emotion, Comment.Emotion.class);

        List<com.tim.appTim.entity.File> fileEntities = createNewFileEntities(files);

        return ResponseEntity.ok(commentService.updateComment(commentId, currentUser, authentication, content, emotionEnum, fileEntities));
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

    @PostMapping("/{commentId}/replies")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<ReplyCommentDTO> createReplyComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String emotion,
            @RequestParam(value = "files", required = false)  List<MultipartFile> multipartFiles) throws IOException {

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
        ReplyComment.Emotion emotionEnum = parseEmotion(emotion, ReplyComment.Emotion.class);
        ReplyCommentDTO createdReply = commentService.createReplyComment(
                commentId,
                currentUser.getId(),
                content,
                emotionEnum,
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
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String emotion,
            Authentication authentication,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        User currentUser = getUserFromAuthentication(authentication);
        ReplyComment.Emotion emotionEnum = parseEmotion(emotion, ReplyComment.Emotion.class);
        List<com.tim.appTim.entity.File> fileEntities = createNewFileEntities(files);

        return ResponseEntity.ok(
                commentService.updateReplyComment(currentUser, authentication, replyCommentId, content, emotionEnum, fileEntities)
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
            return Enum.valueOf(enumType, value.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Emotion không hợp lệ: " + value);
        }
    }
}