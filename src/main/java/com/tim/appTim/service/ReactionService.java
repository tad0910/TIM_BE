package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.Reaction;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReactionRepository;
import com.tim.appTim.repository.UserRepository;

@Service
@Transactional
public class ReactionService {

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReplyCommentRepository replyCommentRepository;

    // Tạo hoặc cập nhật reaction
    public ReactionDTO createOrUpdateReaction(Long postId, Long userId, Reaction.EmotionType emotionType) {
        // Kiểm tra post tồn tại
        postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        
        // Kiểm tra user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra xem user đã reaction chưa
        Optional<Reaction> existingReaction = reactionRepository.findByPostIdAndUserId(postId, userId);
        
        Reaction reaction;
        if (existingReaction.isPresent()) {
            // Cập nhật reaction hiện tại
            reaction = existingReaction.get();
            reaction.setEmotionType(emotionType);
            reaction.setCreatedAt(LocalDateTime.now());
        } else {
            // Tạo reaction mới
            reaction = new Reaction();
            reaction.setPostId(postId);
            reaction.setUserId(userId);
            reaction.setEmotionType(emotionType);
            reaction.setCreatedAt(LocalDateTime.now());
        }

        Reaction savedReaction = reactionRepository.save(reaction);
        return convertToDTO(savedReaction);
    }

    // Comment: create or update reaction
    public ReactionDTO createOrUpdateCommentReaction(Long commentId, Long userId, Reaction.EmotionType emotionType) {
        // Ensure comment exists and get its postId to backfill reactions.bai_viet_id
        Long postIdOfComment = commentRepository.findById(commentId)
                .map(c -> c.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> existing = reactionRepository.findByCommentIdAndUserId(commentId, userId);

        Reaction reaction = existing.orElseGet(Reaction::new);
        reaction.setCommentId(commentId);
        // also set postId to enable tracing comment's post directly from reactions
        reaction.setPostId(postIdOfComment);
        reaction.setUserId(userId);
        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());

        return convertToDTO(reactionRepository.save(reaction));
    }

    public List<ReactionDTO> getReactionsByCommentId(Long commentId) {
        return reactionRepository.findByCommentId(commentId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteCommentReaction(Long commentId, Long userId) {
        Optional<Reaction> reaction = reactionRepository.findByCommentIdAndUserId(commentId, userId);
        if (reaction.isPresent()) {
            reactionRepository.delete(reaction.get());
        } else {
            throw new ResourceNotFoundException("Reaction not found for comment: " + commentId + " and user: " + userId);
        }
    }

    public long countCommentReactionsByType(Long commentId, Reaction.EmotionType emotionType) {
        return reactionRepository.countByCommentIdAndEmotionType(commentId, emotionType);
    }

    // Reply comment: create or update reaction
    public ReactionDTO createOrUpdateReplyCommentReaction(Long replyCommentId, Long userId, Reaction.EmotionType emotionType) {
        // Ensure reply exists and derive its parent commentId and postId
        Long parentCommentId = replyCommentRepository.findById(replyCommentId)
                .map(rc -> rc.getCommentId())
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));
        Long postIdOfParentComment = commentRepository.findById(parentCommentId)
                .map(c -> c.getPostId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with id: " + parentCommentId));
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<Reaction> existing = reactionRepository.findByReplyCommentIdAndUserId(replyCommentId, userId);

        Reaction reaction = existing.orElseGet(Reaction::new);
        reaction.setReplyCommentId(replyCommentId);
        // also set parent comment and post to enable tracing from reactions
        reaction.setCommentId(parentCommentId);
        reaction.setPostId(postIdOfParentComment);
        reaction.setUserId(userId);
        reaction.setEmotionType(emotionType);
        reaction.setCreatedAt(LocalDateTime.now());

        return convertToDTO(reactionRepository.save(reaction));
    }

    public List<ReactionDTO> getReactionsByReplyCommentId(Long replyCommentId) {
        return reactionRepository.findByReplyCommentId(replyCommentId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteReplyCommentReaction(Long replyCommentId, Long userId) {
        Optional<Reaction> reaction = reactionRepository.findByReplyCommentIdAndUserId(replyCommentId, userId);
        if (reaction.isPresent()) {
            reactionRepository.delete(reaction.get());
        } else {
            throw new ResourceNotFoundException("Reaction not found for reply comment: " + replyCommentId + " and user: " + userId);
        }
    }

    public long countReplyCommentReactionsByType(Long replyCommentId, Reaction.EmotionType emotionType) {
        return reactionRepository.countByReplyCommentIdAndEmotionType(replyCommentId, emotionType);
    }

    // Lấy tất cả reactions của một post
    public List<ReactionDTO> getReactionsByPostId(Long postId) {
        List<Reaction> reactions = reactionRepository.findByPostId(postId);
        return reactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Xóa reaction
    public void deleteReaction(Long postId, Long userId) {
        Optional<Reaction> reaction = reactionRepository.findByPostIdAndUserId(postId, userId);
        if (reaction.isPresent()) {
            reactionRepository.delete(reaction.get());
        } else {
            throw new ResourceNotFoundException("Reaction not found for post: " + postId + " and user: " + userId);
        }
    }

    // Kiểm tra user đã reaction chưa
    public boolean hasUserReacted(Long postId, Long userId) {
        return reactionRepository.findByPostIdAndUserId(postId, userId).isPresent();
    }

    // Lấy reaction của user cho một post
    public ReactionDTO getUserReaction(Long postId, Long userId) {
        Optional<Reaction> reaction = reactionRepository.findByPostIdAndUserId(postId, userId);
        if (reaction.isPresent()) {
            return convertToDTO(reaction.get());
        }
        return null;
    }

    // Đếm số lượng reaction theo loại
    public long countReactionsByType(Long postId, Reaction.EmotionType emotionType) {
        return reactionRepository.countByPostIdAndEmotionType(postId, emotionType);
    }

    // Convert Reaction entity to DTO
    private ReactionDTO convertToDTO(Reaction reaction) {
        // Lấy username từ user
        String username = reaction.getUser() != null ? reaction.getUser().getUsername() : "Unknown";
        
        return new ReactionDTO(
                reaction.getId(),
                reaction.getUserId(),
                username,
                reaction.getEmotionType() != null ? reaction.getEmotionType().name() : null,
                reaction.getCreatedAt()
        );
    }
}
