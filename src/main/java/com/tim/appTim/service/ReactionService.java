package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.tim.appTim.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReactionRepository;
import com.tim.appTim.repository.UserRepository;

@Service
@Transactional
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final NotificationService notificationService;
    private final GamificationService gamificationService;
    private final BehaviorLookupService behaviorLookupService;

    @Autowired
    public ReactionService(
            ReactionRepository reactionRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            CommentRepository commentRepository,
            ReplyCommentRepository replyCommentRepository,
            @Lazy NotificationService notificationService,
            @Lazy GamificationService gamificationService,
            BehaviorLookupService behaviorLookupService
    ) {
        this.reactionRepository = reactionRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.notificationService = notificationService;
        this.gamificationService = gamificationService;
        this.behaviorLookupService = behaviorLookupService;
    }

    @Transactional
    public ReactionDTO createOrUpdateReaction(Long postId, Long userId, Reaction.EmotionType emotionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Optional<Reaction> existingReaction = reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user);

        Reaction reaction;
        if (existingReaction.isPresent()) {
            reaction = existingReaction.get();
        } else {
            reaction = new Reaction();
            reaction.setPost(post);
            reaction.setUser(user);
        }

        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());
        Reaction savedReaction = reactionRepository.save(reaction);

        long totalReactions = reactionRepository.countByPostAndCommentIsNullAndReplyCommentIsNull(post);
        post.setTotalReactions((int) totalReactions);
        postRepository.save(post);

        if (totalReactions >= 10) {
            try {
                Integer behaviorId = behaviorLookupService.getIdByName("Bài viết được yêu thích (>10 likes)");
                gamificationService.awardPoints(post.getUser().getId(), behaviorId);
            } catch (Exception e) {
                System.err.println("Failed to award points for post likes: " + e.getMessage());
            }
        }

        try {
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createReactionNotification(
                        post.getUser().getId(), null, null, userId, user.getUsername(),
                        Notification.NotificationType.POST_REACTION, "POST", postId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertToDTO(savedReaction);
    }

    @Transactional
    public ReactionDTO createOrUpdateCommentReaction(Long commentId, Long userId, Reaction.EmotionType emotionType) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));


        Optional<Reaction> existing = reactionRepository.findByCommentAndUserAndReplyCommentIsNull(comment, user);

        Reaction reaction = existing.orElseGet(Reaction::new);
        reaction.setComment(comment);
        reaction.setPost(comment.getPost());
        reaction.setUser(user);
        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());

        Reaction savedReaction = reactionRepository.save(reaction);

        try {
            if (!comment.getUser().getId().equals(userId)) {
                notificationService.createReactionNotification(
                        null, comment.getUser().getId(), null, userId, user.getUsername(),
                        Notification.NotificationType.COMMENT_REACTION, "COMMENT", commentId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertToDTO(savedReaction);
    }

    @Transactional
    public ReactionDTO createOrUpdateReplyCommentReaction(Long replyCommentId, Long userId, Reaction.EmotionType emotionType) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> existing = reactionRepository.findByReplyCommentAndUser(replyComment, user);

        Reaction reaction = existing.orElseGet(Reaction::new);
        reaction.setReplyComment(replyComment);
        reaction.setComment(replyComment.getComment());
        reaction.setPost(replyComment.getComment().getPost());
        reaction.setUser(user);
        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());

        Reaction savedReaction = reactionRepository.save(reaction);

        try {
            if (!replyComment.getUser().getId().equals(userId)) {
                notificationService.createReactionNotification(
                        null, null, replyComment.getUser().getId(), userId, user.getUsername(),
                        Notification.NotificationType.REPLY_REACTION, "REPLY", replyCommentId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertToDTO(savedReaction);
    }

    public Page<ReactionDTO> getReactionsByPostId(Long postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        Page<Reaction> reactions = reactionRepository.findByPostAndCommentIsNullAndReplyCommentIsNull(post, pageable);
        return reactions.map(this::convertToDTO);
    }

    public Page<ReactionDTO> getReactionsByCommentId(Long commentId, Pageable pageable) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        Page<Reaction> reactions = reactionRepository.findByCommentAndReplyCommentIsNull(comment, pageable);
        return reactions.map(this::convertToDTO);
    }

    public Page<ReactionDTO> getReactionsByReplyCommentId(Long replyCommentId, Pageable pageable) {
        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found: " + replyCommentId));
        Page<Reaction> reactions = reactionRepository.findByReplyComment(reply, pageable);
        return reactions.map(this::convertToDTO);
    }

    @Transactional
    public void deleteReaction(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> reaction = reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user);
        reaction.ifPresent(reactionRepository::delete);
    }

    @Transactional
    public void deleteCommentReaction(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> reaction = reactionRepository.findByCommentAndUserAndReplyCommentIsNull(comment, user);
        reaction.ifPresent(reactionRepository::delete);
    }

    @Transactional
    public void deleteReplyCommentReaction(Long replyCommentId, Long userId) {
        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> reaction = reactionRepository.findByReplyCommentAndUser(reply, user);
        reaction.ifPresent(reactionRepository::delete);
    }

    public long countReactionsByType(Long postId, Reaction.EmotionType emotionType) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        return reactionRepository.countByPostAndEmotionType(post, emotionType);
    }

    public long countCommentReactionsByType(Long commentId, Reaction.EmotionType emotionType) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        return reactionRepository.countByCommentAndEmotionType(comment, emotionType);
    }

    public long countReplyCommentReactionsByType(Long replyCommentId, Reaction.EmotionType emotionType) {
        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found: " + replyCommentId));
        return reactionRepository.countByReplyCommentAndEmotionType(reply, emotionType);
    }

    private ReactionDTO convertToDTO(Reaction reaction) {
        String username = reaction.getUser() != null ? reaction.getUser().getUsername() : "Unknown";

        return new ReactionDTO(
                reaction.getId(),
                reaction.getUser().getId(), 
                username,
                reaction.getUser() != null? reaction.getUser().getProfileImage() : null,
                reaction.getEmotionType() != null ? reaction.getEmotionType().name() : null,
                reaction.getCreatedAt()
        );
    }
}