package com.tim.appTim.controller;

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.Reaction;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.service.ReactionService;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/reactions")
public class ReactionController {

    private final ReactionService reactionService;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReactionDTO> createOrUpdateReaction(
            @PathVariable Long postId,
            Authentication authentication,
            @RequestParam String emotionType) {

        User currentUser = getUserFromAuthentication(authentication);
        Reaction.EmotionType emotionTypeEnum;

        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        ReactionDTO reaction = reactionService.createOrUpdateReaction(postId, currentUser.getId(), emotionTypeEnum);
        return ResponseEntity.ok(reaction);
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<Page<ReactionDTO>> getReactionsByPostId(@PathVariable Long postId,
                                                                   Pageable pageable) {
        return ResponseEntity.ok(reactionService.getReactionsByPostId(postId, pageable));
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReaction(
            @PathVariable Long postId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteReaction(postId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{postId}/count/{emotionType}")
    public ResponseEntity<Long> countReactionsByType(
            @PathVariable Long postId,
            @PathVariable String emotionType) {

        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        return ResponseEntity.ok(reactionService.countReactionsByType(postId, emotionTypeEnum));
    }

    @PostMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReactionDTO> createOrUpdateCommentReaction(
            @PathVariable Long commentId,
            Authentication authentication,
            @RequestParam String emotionType) {

        User currentUser = getUserFromAuthentication(authentication);
        Reaction.EmotionType emotionTypeEnum;
//test
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        ReactionDTO reaction = reactionService.createOrUpdateCommentReaction(commentId, currentUser.getId(), emotionTypeEnum);
        return ResponseEntity.ok(reaction);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<Page<ReactionDTO>> getReactionsByCommentId(@PathVariable Long commentId,
                                                                      Pageable pageable) {
        return ResponseEntity.ok(reactionService.getReactionsByCommentId(commentId, pageable));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAuthority('reaction:delete')")
    public ResponseEntity<Void> deleteCommentReaction(@PathVariable Long commentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteCommentReaction(commentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments/{commentId}/count/{emotionType}")
    public ResponseEntity<Long> countCommentReactionsByType(
            @PathVariable Long commentId,
            @PathVariable String emotionType) {

        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        return ResponseEntity.ok(reactionService.countCommentReactionsByType(commentId, emotionTypeEnum));
    }

    @PostMapping("/replies/{replyCommentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReactionDTO> createOrUpdateReplyCommentReaction(
            @PathVariable Long replyCommentId,
            Authentication authentication,
            @RequestParam String emotionType) {

        User currentUser = getUserFromAuthentication(authentication);
        Reaction.EmotionType emotionTypeEnum;

        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        ReactionDTO reaction = reactionService.createOrUpdateReplyCommentReaction(replyCommentId, currentUser.getId(), emotionTypeEnum);
        return ResponseEntity.ok(reaction);
    }

    @GetMapping("/replies/{replyCommentId}")
    public ResponseEntity<Page<ReactionDTO>> getReactionsByReplyCommentId( @PathVariable Long replyCommentId,
                                                                           Pageable pageable) {
        return ResponseEntity.ok(reactionService.getReactionsByReplyCommentId(replyCommentId, pageable));
    }

    @DeleteMapping("/replies/{replyCommentId}")
    @PreAuthorize("hasAuthority('reaction:delete')")
    public ResponseEntity<Void> deleteReplyCommentReaction(@PathVariable Long replyCommentId, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        reactionService.deleteReplyCommentReaction(replyCommentId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/replies/{replyCommentId}/count/{emotionType}")
    public ResponseEntity<Long> countReplyCommentReactionsByType(
            @PathVariable Long replyCommentId,
            @PathVariable String emotionType) {

        Reaction.EmotionType emotionTypeEnum;
        try {
            emotionTypeEnum = Reaction.EmotionType.valueOf(emotionType.toLowerCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emotion type: " + emotionType);
        }

        return ResponseEntity.ok(reactionService.countReplyCommentReactionsByType(replyCommentId, emotionTypeEnum));
    }
}
