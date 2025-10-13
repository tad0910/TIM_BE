package com.tim.appTim.repository;

import com.tim.appTim.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByPostId(Long postId);
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostIdAndEmotionType(Long postId, Reaction.EmotionType emotionType);
}