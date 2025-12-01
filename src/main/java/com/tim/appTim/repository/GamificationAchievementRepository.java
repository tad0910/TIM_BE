package com.tim.appTim.repository;

import com.tim.appTim.entity.GamificationAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GamificationAchievementRepository extends JpaRepository<GamificationAchievement, Integer> {
}

