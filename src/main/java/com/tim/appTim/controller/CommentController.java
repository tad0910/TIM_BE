package com.tim.appTim.controller;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.CommentService;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private CommentService commentService;
    private final UserService userService;

    @Autowired
    public CommentController(CommentService commentService, UserService userService) { // <-- THÊM
        this.commentService = commentService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
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
        Comment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = Comment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        CommentDTO comment = commentService.createComment(postId, currentUser.getId(), content, emotionEnum, fileId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasAuthority('comment:update_all') or @commentService.isOwner(authentication, #commentId)")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String content,
            @RequestParam(required = false) String emotion) {

        User currentUser = getUserFromAuthentication(authentication);
        Comment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = Comment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        CommentDTO comment = commentService.updateComment(commentId, currentUser.getId(), content, emotionEnum);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAuthority('comment:delete_all') or @commentService.isOwner(authentication, #commentId)")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        commentService.deleteComment(commentId, currentUser.getId());
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

        ReplyComment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = ReplyComment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        User currentUser = getUserFromAuthentication(authentication);
        ReplyCommentDTO replyComment = commentService.createReplyComment(commentId, currentUser.getId(), content, emotionEnum, fileId);
        return ResponseEntity.ok(replyComment);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<ReplyCommentDTO>> getReplyCommentsByCommentId(@PathVariable Long commentId) {
        List<ReplyCommentDTO> replyComments = commentService.getReplyCommentsByCommentId(commentId);
        return ResponseEntity.ok(replyComments);
    }

    @PutMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('comment:update_all') or @commentService.isReplyOwner(authentication, #replyCommentId)")
    public ResponseEntity<ReplyCommentDTO> updateReplyComment(
            @PathVariable Long replyCommentId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        ReplyComment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = ReplyComment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReplyCommentDTO replyComment = commentService.updateReplyComment(currentUser.getId(), replyCommentId, content, emotionEnum);
        return ResponseEntity.ok(replyComment);
    }

    @DeleteMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('comment:delete_all') or @commentService.isReplyOwner(authentication, #replyCommentId)")
    public ResponseEntity<String> deleteReplyComment(@PathVariable Long replyCommentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        commentService.deleteReplyComment(currentUser.getId(), replyCommentId);
        return ResponseEntity.ok("Reply comment deleted successfully");
    }
}
