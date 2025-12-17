package com.tim.appTim.repository;

import com.tim.appTim.entity.UserAchievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId);
    
    Page<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId AND ua.achievementLevel.id = :achievementLevelId")
    List<UserAchievement> findByUserIdAndAchievementLevelId(
        @Param("userId") Long userId,
        @Param("achievementLevelId") Integer achievementLevelId
    );

    boolean existsByUserIdAndAchievementLevelId(Long userId, Integer achievementLevelId);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId AND ua.isDisplayed = true " +
           "ORDER BY ua.unlockedAt DESC")
    List<UserAchievement> findDisplayedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT ua.userId) FROM UserAchievement ua " +
           "WHERE ua.achievementLevel.achievement.id = :achievementId AND ua.isDisplayed = true")
    long countDistinctUsersByAchievementId(@Param("achievementId") Integer achievementId);
}

