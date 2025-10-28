package com.tim.appTim.controller;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.service.CommentService;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
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
            return comment.getUserId();
        }
        return currentUserId;
    }

    private Long getEffectiveUserIdForReply(Long replyCommentId, Long currentUserId, Authentication authentication) {
        if (hasAdminAuthority(authentication)) {
            ReplyComment reply = commentService.getReplyCommentById(replyCommentId);
            return reply.getUserId();
        }
        return currentUserId;
    }

    @PostMapping("/posts/{postId}")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) Long fileId) {

        User currentUser = getUserFromAuthentication(authentication);
        Comment.Emotion emotionEnum = parseEmotion(emotion, Comment.Emotion.class);

        CommentDTO comment = commentService.createComment(postId, currentUser.getId(), content, emotionEnum, fileId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasAuthority('comment:update_all') or @commentService.isOwner(authentication, #commentId)")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(required = false) String emotion) {

        User currentUser = getUserFromAuthentication(authentication);
        Comment.Emotion emotionEnum = parseEmotion(emotion, Comment.Emotion.class);
        
        Long effectiveUserId = getEffectiveUserId(commentId, currentUser.getId(), authentication);

        return ResponseEntity.ok(commentService.updateComment(commentId, effectiveUserId, content, emotionEnum));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAuthority('comment:delete_all') or @commentService.isOwner(authentication, #commentId)")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        Long effectiveUserId = getEffectiveUserId(commentId, currentUser.getId(), authentication);
        
        commentService.deleteComment(commentId, effectiveUserId);
        return ResponseEntity.ok("Comment deleted successfully");
    }

    @PostMapping("/{commentId}/replies")
    @PreAuthorize("hasAuthority('comment:create')")
    public ResponseEntity<ReplyCommentDTO> createReplyComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) Long fileId) {

        User currentUser = getUserFromAuthentication(authentication);
        ReplyComment.Emotion emotionEnum = parseEmotion(emotion, ReplyComment.Emotion.class);

        return ResponseEntity.ok(
                commentService.createReplyComment(commentId, currentUser.getId(), content, emotionEnum, fileId)
        );
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<ReplyCommentDTO>> getReplyCommentsByCommentId(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplyCommentsByCommentId(commentId));
    }

    @PutMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('comment:update_all') or @commentService.isReplyOwner(authentication, #replyCommentId)")
    public ResponseEntity<ReplyCommentDTO> updateReplyComment(
            @PathVariable Long replyCommentId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        ReplyComment.Emotion emotionEnum = parseEmotion(emotion, ReplyComment.Emotion.class);
        
        Long effectiveUserId = getEffectiveUserIdForReply(replyCommentId, currentUser.getId(), authentication);

        return ResponseEntity.ok(
                commentService.updateReplyComment(effectiveUserId, replyCommentId, content, emotionEnum)
        );
    }

    @DeleteMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('comment:delete_all') or @commentService.isReplyOwner(authentication, #replyCommentId)")
    public ResponseEntity<String> deleteReplyComment(@PathVariable Long replyCommentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        Long effectiveUserId = getEffectiveUserIdForReply(replyCommentId, currentUser.getId(), authentication);
        
        commentService.deleteReplyComment(effectiveUserId, replyCommentId);
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
