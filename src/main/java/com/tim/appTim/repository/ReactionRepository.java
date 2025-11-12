package com.tim.appTim.repository;

import com.tim.appTim.entity.*; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(Post post, User user);
    Page<Reaction> findByPostAndCommentIsNullAndReplyCommentIsNull(Post post, Pageable pageable);
    long countByPostAndEmotionType(Post post, Reaction.EmotionType emotionType);

    Optional<Reaction> findByCommentAndUserAndReplyCommentIsNull(Comment comment, User user);
    Page<Reaction> findByCommentAndReplyCommentIsNull(Comment comment, Pageable pageable);
    long countByCommentAndEmotionType(Comment comment, Reaction.EmotionType emotionType);

    Optional<Reaction> findByReplyCommentAndUser(ReplyComment replyComment, User user);
    Page<Reaction> findByReplyComment(ReplyComment replyComment, Pageable pageable);
    long countByReplyCommentAndEmotionType(ReplyComment replyComment, Reaction.EmotionType emotionType);

}