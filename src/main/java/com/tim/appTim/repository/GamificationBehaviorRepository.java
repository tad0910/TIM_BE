package com.tim.appTim.repository;

import com.tim.appTim.entity.GamificationBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GamificationBehaviorRepository extends JpaRepository<GamificationBehavior, Integer> {
    Optional<GamificationBehavior> findByCode(String code);
    List<GamificationBehavior> findByGroupId(Integer groupId);
}

