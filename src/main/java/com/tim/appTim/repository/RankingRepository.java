package com.tim.appTim.repository;

import com.tim.appTim.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    Optional<Ranking> findByUserId(Long userId);
    
    Optional<Ranking> findByUserIdAndClassId(Long userId, Long classId);
    
    List<Ranking> findByClassId(Long classId);
    
    // Query để lấy ranking sắp xếp theo experience (Option 1)
    @Query("SELECT r FROM Ranking r " +
           "WHERE (:classId IS NULL OR r.classId = :classId) " +
           "ORDER BY r.totalExperienceScore DESC, r.totalCompetenceScore DESC, r.totalDiligenceScore DESC, r.userId ASC")
    List<Ranking> findAllOrderByExperience(@Param("classId") Long classId);
    
    // Query để lấy ranking sắp xếp theo competence
    @Query("SELECT r FROM Ranking r " +
           "WHERE (:classId IS NULL OR r.classId = :classId) " +
           "ORDER BY r.totalCompetenceScore DESC, r.totalExperienceScore DESC, r.totalDiligenceScore DESC, r.userId ASC")
    List<Ranking> findAllOrderByCompetence(@Param("classId") Long classId);
    
    // Query để lấy ranking sắp xếp theo diligence
    @Query("SELECT r FROM Ranking r " +
           "WHERE (:classId IS NULL OR r.classId = :classId) " +
           "ORDER BY r.totalDiligenceScore DESC, r.totalExperienceScore DESC, r.totalCompetenceScore DESC, r.userId ASC")
    List<Ranking> findAllOrderByDiligence(@Param("classId") Long classId);
    
    // Query để lấy ranking sắp xếp theo tổng điểm
    @Query("SELECT r FROM Ranking r " +
           "WHERE (:classId IS NULL OR r.classId = :classId) " +
           "ORDER BY (r.totalDiligenceScore + r.totalCompetenceScore + r.totalExperienceScore) DESC, r.userId ASC")
    List<Ranking> findAllOrderByTotal(@Param("classId") Long classId);
}

