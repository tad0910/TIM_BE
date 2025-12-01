package com.tim.appTim.repository;

import com.tim.appTim.entity.GamificationAchievementLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamificationAchievementLevelRepository extends JpaRepository<GamificationAchievementLevel, Integer> {
    List<GamificationAchievementLevel> findByAchievementId(Integer achievementId);
    
    @Query("SELECT al FROM GamificationAchievementLevel al WHERE al.achievement.id = :achievementId ORDER BY al.minPointsRequired ASC")
    List<GamificationAchievementLevel> findByAchievementIdOrderByMinPointsRequiredAsc(@Param("achievementId") Integer achievementId);
}

