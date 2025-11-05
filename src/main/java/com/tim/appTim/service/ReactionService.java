package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tim.appTim.entity.*; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired private ReactionRepository reactionRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ReplyCommentRepository replyCommentRepository;
    @Autowired private NotificationService notificationService;

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

        // (Logic thông báo của bạn đã đúng)
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

        // SỬA: Dùng đối tượng để tìm
        Optional<Reaction> existing = reactionRepository.findByReplyCommentAndUser(replyComment, user);

        Reaction reaction = existing.orElseGet(Reaction::new);
        reaction.setReplyComment(replyComment);
        reaction.setComment(replyComment.getComment());
        reaction.setPost(replyComment.getComment().getPost());
        reaction.setUser(user);
        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());

        Reaction savedReaction = reactionRepository.save(reaction);

        // (Logic thông báo của bạn đã đúng)
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

    // --- CÁC HÀM GET (ĐÃ SỬA) ---
    public List<ReactionDTO> getReactionsByPostId(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));
        List<Reaction> reactions = reactionRepository.findByPostAndCommentIsNullAndReplyCommentIsNull(post);
        return reactions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ReactionDTO> getReactionsByCommentId(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));
        List<Reaction> reactions = reactionRepository.findByCommentAndReplyCommentIsNull(comment);
        return reactions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<ReactionDTO> getReactionsByReplyCommentId(Long replyCommentId) {
        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply not found: " + replyCommentId));
        return reactionRepository.findByReplyComment(reply)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
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