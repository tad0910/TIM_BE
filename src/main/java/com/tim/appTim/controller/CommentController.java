package com.tim.appTim.controller;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/posts/{postId}")
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion,
            @RequestParam(required = false) Long fileId) {

        Comment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = Comment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        CommentDTO comment = commentService.createComment(postId, userId, content, emotionEnum, fileId);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion) {

        Comment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = Comment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        CommentDTO comment = commentService.updateComment(commentId, userId, content, emotionEnum);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok("Comment deleted successfully");
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<ReplyCommentDTO> createReplyComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
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

        ReplyCommentDTO replyComment = commentService.createReplyComment(commentId, userId, content, emotionEnum, fileId);
        return ResponseEntity.ok(replyComment);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<ReplyCommentDTO>> getReplyCommentsByCommentId(@PathVariable Long commentId) {
        List<ReplyCommentDTO> replyComments = commentService.getReplyCommentsByCommentId(commentId);
        return ResponseEntity.ok(replyComments);
    }

    @PutMapping("/replies/{replyCommentId}")
    public ResponseEntity<ReplyCommentDTO> updateReplyComment(
            @PathVariable Long replyCommentId,
            @RequestParam String content,
            @RequestParam(required = false) String emotion) {

        ReplyComment.Emotion emotionEnum = null;
        if (emotion != null && !emotion.isEmpty()) {
            try {
                emotionEnum = ReplyComment.Emotion.valueOf(emotion.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        ReplyCommentDTO replyComment = commentService.updateReplyComment(replyCommentId, content, emotionEnum);
        return ResponseEntity.ok(replyComment);
    }

    @DeleteMapping("/replies/{replyCommentId}")
    public ResponseEntity<String> deleteReplyComment(@PathVariable Long replyCommentId) {
        commentService.deleteReplyComment(replyCommentId);
        return ResponseEntity.ok("Reply comment deleted successfully");
    }
}
