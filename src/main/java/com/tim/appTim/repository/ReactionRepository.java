package com.tim.appTim.repository;

import com.tim.appTim.entity.*; 
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(Post post, User user);
    List<Reaction> findByPostAndCommentIsNullAndReplyCommentIsNull(Post post);
    long countByPostAndEmotionType(Post post, Reaction.EmotionType emotionType);

    Optional<Reaction> findByCommentAndUserAndReplyCommentIsNull(Comment comment, User user);
    List<Reaction> findByCommentAndReplyCommentIsNull(Comment comment);
    long countByCommentAndEmotionType(Comment comment, Reaction.EmotionType emotionType);

    Optional<Reaction> findByReplyCommentAndUser(ReplyComment replyComment, User user);
    List<Reaction> findByReplyComment(ReplyComment replyComment);
    long countByReplyCommentAndEmotionType(ReplyComment replyComment, Reaction.EmotionType emotionType);

}