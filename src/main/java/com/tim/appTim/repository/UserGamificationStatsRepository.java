package com.tim.appTim.repository;

import com.tim.appTim.entity.UserGamificationStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGamificationStatsRepository extends JpaRepository<UserGamificationStats, Long> {
    Optional<UserGamificationStats> findByUserId(Long userId);
    
    @Query("SELECT ugs FROM UserGamificationStats ugs ORDER BY ugs.totalCompetence DESC, ugs.totalDiligence DESC, ugs.totalExperience DESC")
    List<UserGamificationStats> findAllOrderByTotalPointsDesc();
    
    @Query("SELECT ugs FROM UserGamificationStats ugs WHERE ugs.totalCompetence >= :minPoints " +
           "ORDER BY ugs.totalCompetence DESC")
    List<UserGamificationStats> findTopByCompetence(@Param("minPoints") Integer minPoints);
}

