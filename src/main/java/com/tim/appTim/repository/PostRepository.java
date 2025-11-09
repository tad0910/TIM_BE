package com.tim.appTim.repository;

import com.tim.appTim.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByUserId(Long userId, Pageable pageable);
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
}
