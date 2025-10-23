package com.tim.appTim.controller;

import java.util.List;

import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.Reaction;
import com.tim.appTim.service.ReactionService;

@RestController
@RequestMapping("/reactions")
public class ReactionController {

    private ReactionService reactionService;
    private final UserService userService;

    @Autowired
    public ReactionController(ReactionService reactionService, UserService userService) {
        this.reactionService = reactionService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @PostMapping("/posts/{postId}")
    @PreAuthorize("hasAuthority('reaction:create')")
    public ResponseEntity<?> createOrUpdateReaction(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam String emotionType) {

        try {
            User currentUser = getUserFromAuthentication(authentication);
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateReaction(postId, currentUser.getId(), emotionTypeEnum);

            return ResponseEntity.ok(reaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid emotion type: " + emotionType);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error while creating/updating reaction: " + e.getMessage());
        }
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<List<ReactionDTO>> getReactionsByPostId(@PathVariable Long postId) {
        List<ReactionDTO> reactions = reactionService.getReactionsByPostId(postId);
        return ResponseEntity.ok(reactions);
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("hasAuthority('reaction:delete')")
    public ResponseEntity<String> deleteReaction(
            @PathVariable Long postId,
            Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteReaction(postId, currentUser.getId());
        return ResponseEntity.ok("Reaction deleted successfully");
    }

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

    @PostMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('reaction:create')")
    public ResponseEntity<?> createOrUpdateCommentReaction(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String emotionType) {
        try {
            User currentUser = getUserFromAuthentication(authentication);
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateCommentReaction(commentId, currentUser.getId(), emotionTypeEnum);
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
    @PreAuthorize("hasAuthority('reaction:delete')")
    public ResponseEntity<String> deleteCommentReaction(@PathVariable Long commentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteCommentReaction(commentId, currentUser.getId());
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

    @PostMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('reaction:create')")
    public ResponseEntity<?> createOrUpdateReplyCommentReaction(
            @PathVariable Long replyCommentId,
            Authentication authentication,
            @RequestParam String emotionType) {
        try {
            User currentUser = getUserFromAuthentication(authentication);
            Reaction.EmotionType emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
            ReactionDTO reaction = reactionService.createOrUpdateReplyCommentReaction(replyCommentId, currentUser.getId(), emotionTypeEnum);
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
    @PreAuthorize("hasAuthority('reaction:delete')")
    public ResponseEntity<String> deleteReplyCommentReaction(@PathVariable Long replyCommentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteReplyCommentReaction(replyCommentId, currentUser.getId());
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

    