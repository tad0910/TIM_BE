package com.tim.appTim.repository;

import com.tim.appTim.entity.RankingMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RankingMonthlyRepository extends JpaRepository<RankingMonthly, Long> {
    Optional<RankingMonthly> findByUserIdAndMonthYearAndClassId(Long userId, String monthYear, Long classId);
    
    List<RankingMonthly> findByMonthYearAndClassIdOrderByRankPositionAsc(String monthYear, Long classId);
    
    List<RankingMonthly> findByMonthYearOrderByRankPositionAsc(String monthYear);

    @Query("SELECT rm FROM RankingMonthly rm " +
           "WHERE rm.monthYear = :monthYear " +
           "AND (:classId IS NULL OR rm.classId = :classId) " +
           "ORDER BY rm.totalExperienceScore DESC, rm.totalCompetenceScore DESC, rm.totalDiligenceScore DESC, rm.userId ASC")
    List<RankingMonthly> findByMonthYearOrderByExperience(@Param("monthYear") String monthYear, @Param("classId") Long classId);

    @Query("SELECT rm FROM RankingMonthly rm " +
           "WHERE rm.monthYear = :monthYear " +
           "AND (:classId IS NULL OR rm.classId = :classId) " +
           "ORDER BY rm.totalCompetenceScore DESC, rm.totalExperienceScore DESC, rm.totalDiligenceScore DESC, rm.userId ASC")
    List<RankingMonthly> findByMonthYearOrderByCompetence(@Param("monthYear") String monthYear, @Param("classId") Long classId);

    @Query("SELECT rm FROM RankingMonthly rm " +
           "WHERE rm.monthYear = :monthYear " +
           "AND (:classId IS NULL OR rm.classId = :classId) " +
           "ORDER BY rm.totalDiligenceScore DESC, rm.totalExperienceScore DESC, rm.totalCompetenceScore DESC, rm.userId ASC")
    List<RankingMonthly> findByMonthYearOrderByDiligence(@Param("monthYear") String monthYear, @Param("classId") Long classId);

    @Query("SELECT rm FROM RankingMonthly rm " +
           "WHERE rm.monthYear = :monthYear " +
           "AND (:classId IS NULL OR rm.classId = :classId) " +
           "ORDER BY (rm.totalDiligenceScore + rm.totalCompetenceScore + rm.totalExperienceScore) DESC, rm.userId ASC")
    List<RankingMonthly> findByMonthYearOrderByTotal(@Param("monthYear") String monthYear, @Param("classId") Long classId);
    
    Optional<RankingMonthly> findByUserIdAndMonthYear(Long userId, String monthYear);
}

