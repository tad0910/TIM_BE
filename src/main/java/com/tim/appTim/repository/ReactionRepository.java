package com.tim.appTim.repository;

import com.tim.appTim.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByPostId(Long postId);
    Optional<Reaction> findByPostIdAndUserIdAndCommentIdIsNullAndReplyCommentIdIsNull(Long postId, Long userId);
    long countByPostIdAndEmotionType(Long postId, Reaction.EmotionType emotionType);

    // Comment targets
    List<Reaction> findByCommentId(Long commentId);
    Optional<Reaction> findByCommentIdAndUserIdAndReplyCommentIdIsNull(Long commentId, Long userId);
    long countByCommentIdAndEmotionType(Long commentId, Reaction.EmotionType emotionType);

    // Reply comment targets
    List<Reaction> findByReplyCommentId(Long replyCommentId);
    Optional<Reaction> findByReplyCommentIdAndUserId(Long replyCommentId, Long userId);
    long countByReplyCommentIdAndEmotionType(Long replyCommentId, Reaction.EmotionType emotionType);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.replyCommentId = :replyCommentId")
    void deleteByReplyCommentId(Long replyCommentId);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.commentId = :commentId")
    void deleteByCommentId(Long commentId);

    List<Reaction> findByPostIdAndCommentIdIsNullAndReplyCommentIdIsNull(Long postId);
    List<Reaction> findByCommentIdAndReplyCommentIdIsNull(Long commentId);
}