package com.tim.appTim.repository;

import com.tim.appTim.entity.ReplyComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReplyCommentRepository extends JpaRepository<ReplyComment, Long> {
    List<ReplyComment> findByCommentId(Long commentId);

    @Query("SELECT r FROM ReplyComment r WHERE r.comment.id = :commentId ORDER BY r.createdAt ASC")
    Page<ReplyComment> findByCommentId(@Param("commentId") Long commentId, Pageable pageable);
}