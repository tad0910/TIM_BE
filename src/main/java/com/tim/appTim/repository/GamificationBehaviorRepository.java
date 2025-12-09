package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.GamificationBehavior;

@Repository
public interface GamificationBehaviorRepository extends JpaRepository<GamificationBehavior, Integer> {
    Optional<GamificationBehavior> findByName(String name);
    List<GamificationBehavior> findByGroupId(Integer groupId);
}

