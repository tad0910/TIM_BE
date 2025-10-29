package com.tim.appTim.repository;

import com.tim.appTim.entity.*; // Import tất cả entity
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // --- CÁC HÀM TÌM KIẾM (ĐÃ SỬA SANG DÙNG ĐỐI TƯỢNG) ---

    // Dùng cho Post
    Optional<Reaction> findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(Post post, User user);
    List<Reaction> findByPostAndCommentIsNullAndReplyCommentIsNull(Post post);
    long countByPostAndEmotionType(Post post, Reaction.EmotionType emotionType);

    // Dùng cho Comment
    Optional<Reaction> findByCommentAndUserAndReplyCommentIsNull(Comment comment, User user);
    List<Reaction> findByCommentAndReplyCommentIsNull(Comment comment);
    long countByCommentAndEmotionType(Comment comment, Reaction.EmotionType emotionType);

    // Dùng cho ReplyComment
    Optional<Reaction> findByReplyCommentAndUser(ReplyComment replyComment, User user);
    List<Reaction> findByReplyComment(ReplyComment replyComment);
    long countByReplyCommentAndEmotionType(ReplyComment replyComment, Reaction.EmotionType emotionType);

    // --- CÁC HÀM CŨ (KHÔNG CẦN NỮA VÌ CASCADE SẼ LO) ---
    // Không cần các hàm @Query @Modifying delete... nữa
}