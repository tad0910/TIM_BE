package com.tim.appTim.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.Reaction;
import com.tim.appTim.service.ReactionService;

@RestController
@RequestMapping("/reactions")
public class ReactionController {

    @Autowired
    private ReactionService reactionService;

    @PostMapping("/posts/{postId}")
    public ResponseEntity<?> createOrUpdateReaction(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestParam String emotionType) {

        try {
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateReaction(postId, userId, emotionTypeEnum);

            return ResponseEntity.ok(reaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid emotion type: " + emotionType);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error while creating/updating reaction: " + e.getMessage());
        }
    }

    
    // Lấy tất cả reactions của một post
    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<ReactionDTO>> getReactionsByPostId(@PathVariable Long postId) {
        List<ReactionDTO> reactions = reactionService.getReactionsByPostId(postId);
        return ResponseEntity.ok(reactions);
    }

    // Xóa reaction
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<String> deleteReaction(
            @PathVariable Long postId,
            @RequestParam Long userId) {
        reactionService.deleteReaction(postId, userId);
        return ResponseEntity.ok("Reaction deleted successfully");
    }

    // // Lấy reaction của user cho một post
    // @GetMapping("/posts/{postId}/users/{userId}")
    // public ResponseEntity<ReactionDTO> getUserReaction(
    //         @PathVariable Long postId,
    //         @PathVariable Long userId) {
    //     ReactionDTO reaction = reactionService.getUserReaction(postId, userId);
    //     if (reaction != null) {
    //         return ResponseEntity.ok(reaction);
    //     } else {
    //         return ResponseEntity.notFound().build();
    //     }
    // }

    // Đếm số lượng reaction theo loại
    @GetMapping("/posts/{postId}/count/{emotionType}")
    public ResponseEntity<Long> countReactionsByType(
            @PathVariable Long postId,
            @PathVariable String emotionType) {
        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        long count = reactionService.countReactionsByType(postId, emotionTypeEnum);
        return ResponseEntity.ok(count);
    }
    // Comment reactions
    @PostMapping("/comments/{commentId}")
    public ResponseEntity<?> createOrUpdateCommentReaction(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam String emotionType) {
        try {
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateCommentReaction(commentId, userId, emotionTypeEnum);
            return ResponseEntity.ok(reaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid emotion type: " + emotionType);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error while creating/updating reaction: " + e.getMessage());
        }
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<List<ReactionDTO>> getReactionsByCommentId(@PathVariable Long commentId) {
        return ResponseEntity.ok(reactionService.getReactionsByCommentId(commentId));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteCommentReaction(@PathVariable Long commentId, @RequestParam Long userId) {
        reactionService.deleteCommentReaction(commentId, userId);
        return ResponseEntity.ok("Reaction deleted successfully");
    }

    @GetMapping("/comments/{commentId}/count/{emotionType}")
    public ResponseEntity<Long> countCommentReactionsByType(@PathVariable Long commentId, @PathVariable String emotionType) {
        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reactionService.countCommentReactionsByType(commentId, emotionTypeEnum));
    }

    // Reply comment reactions
    @PostMapping("/replies/{replyCommentId}")
    public ResponseEntity<?> createOrUpdateReplyCommentReaction(
            @PathVariable Long replyCommentId,
            @RequestParam Long userId,
            @RequestParam String emotionType) {
        try {
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateReplyCommentReaction(replyCommentId, userId, emotionTypeEnum);
            return ResponseEntity.ok(reaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid emotion type: " + emotionType);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error while creating/updating reaction: " + e.getMessage());
        }
    }

    @GetMapping("/replies/{replyCommentId}")
    public ResponseEntity<List<ReactionDTO>> getReactionsByReplyCommentId(@PathVariable Long replyCommentId) {
        return ResponseEntity.ok(reactionService.getReactionsByReplyCommentId(replyCommentId));
    }

    @DeleteMapping("/replies/{replyCommentId}")
    public ResponseEntity<String> deleteReplyCommentReaction(@PathVariable Long replyCommentId, @RequestParam Long userId) {
        reactionService.deleteReplyCommentReaction(replyCommentId, userId);
        return ResponseEntity.ok("Reaction deleted successfully");
    }

    @GetMapping("/replies/{replyCommentId}/count/{emotionType}")
    public ResponseEntity<Long> countReplyCommentReactionsByType(@PathVariable Long replyCommentId, @PathVariable String emotionType) {
        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reactionService.countReplyCommentReactionsByType(replyCommentId, emotionTypeEnum));
    }
}

    