package com.tim.appTim.repository;

import com.tim.appTim.entity.GamificationBehaviorGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamificationBehaviorGroupRepository extends JpaRepository<GamificationBehaviorGroup, Integer> {
}

