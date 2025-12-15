package com.tim.appTim.repository;

import com.tim.appTim.entity.GamificationPointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamificationPointTypeRepository extends JpaRepository<GamificationPointType, Integer> {
    List<GamificationPointType> findByIsActiveTrue();
    Page<GamificationPointType> findByIsActiveTrue(Pageable pageable);
    List<GamificationPointType> findByShowOnDashboardTrueAndIsActiveTrue();
    java.util.Optional<GamificationPointType> findByName(String name);
}

