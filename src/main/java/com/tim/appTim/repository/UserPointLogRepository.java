package com.tim.appTim.repository;

import com.tim.appTim.entity.UserPointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserPointLogRepository extends JpaRepository<UserPointLog, Long> {
    List<UserPointLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Page<UserPointLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(upl) FROM UserPointLog upl WHERE upl.userId = :userId AND upl.behavior.id = :behaviorId " +
           "AND upl.createdAt >= :startDate AND upl.createdAt < :endDate")
    Long countByUserIdAndBehaviorIdAndDateRange(
        @Param("userId") Long userId,
        @Param("behaviorId") Integer behaviorId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT upl FROM UserPointLog upl WHERE upl.userId = :userId " +
           "AND upl.createdAt >= :startDate AND upl.createdAt < :endDate " +
           "ORDER BY upl.createdAt DESC")
    List<UserPointLog> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}

