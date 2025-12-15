package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tim.appTim.entity.File;

public interface FileRepository extends JpaRepository<File, Integer> {
    List<File> findByPostId(Long postId);
    List<File> findByCommentId(Long commentId);
    List<File> findByReplyCommentId(Long replyCommentId);

    @Query("SELECT f FROM File f WHERE f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%' " +
           "ORDER BY f.id DESC")
    List<File> findGamificationGuideFiles();
    
    @Query("SELECT f FROM File f WHERE f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%' " +
           "ORDER BY f.id DESC")
    Page<File> findGamificationGuideFiles(Pageable pageable);

    @Query("SELECT f FROM File f WHERE f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%' " +
           "ORDER BY f.id DESC")
    List<File> findActiveGamificationGuides();

    default Optional<File> findActiveGamificationGuide() {
        List<File> files = findActiveGamificationGuides();
        return files.isEmpty() ? Optional.empty() : Optional.of(files.get(0));
    }
    
    @Query("SELECT f FROM File f WHERE f.id = :id AND f.post IS NULL AND f.comment IS NULL AND f.replyComment IS NULL " +
           "AND f.fileName LIKE 'GAMIFICATION_GUIDE_%'")
    Optional<File> findGamificationGuideById(@Param("id") Integer id);
}
