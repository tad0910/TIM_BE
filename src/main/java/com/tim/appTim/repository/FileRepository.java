package com.tim.appTim.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tim.appTim.entity.File;

public interface FileRepository extends JpaRepository<File, Integer> {
    List<File> findByPostId(Long postId);
    List<File> findByCommentId(Long commentId);
    List<File> findByReplyCommentId(Long replyCommentId);
}
