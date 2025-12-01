package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tim.appTim.entity.File;

public interface FileRepository extends JpaRepository<File, Integer> {
    List<File> findByPostId(Long postId);
    List<File> findByCommentId(Long commentId);
    List<File> findByReplyCommentId(Long replyCommentId);
    
    /**
     * Tìm file gamification guide đang active
     * File gamification guide là file không thuộc post, comment, reply_comment
     * và có file_name bắt đầu với "GAMIFICATION_GUIDE_"
     */
    @Query("SELECT f FROM File f WHERE f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%' " +
           "ORDER BY f.id DESC")
    List<File> findGamificationGuideFiles();
    
    /**
     * Tìm file gamification guide đang active (mới nhất)
     */
    @Query("SELECT f FROM File f WHERE f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%' " +
           "ORDER BY f.id DESC")
    List<File> findActiveGamificationGuides();
    
    /**
     * Tìm file gamification guide đang active (mới nhất) - dùng Top1
     */
    default Optional<File> findActiveGamificationGuide() {
        List<File> files = findActiveGamificationGuides();
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }
    
    /**
     * Tìm file gamification guide theo ID
     */
    @Query("SELECT f FROM File f WHERE f.id = :id AND f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%'")
    Optional<File> findGamificationGuideById(@Param("id") Integer id);
}
