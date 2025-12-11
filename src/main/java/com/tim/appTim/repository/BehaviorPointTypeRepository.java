package com.tim.appTim.repository;

import com.tim.appTim.entity.BehaviorPointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehaviorPointTypeRepository extends JpaRepository<BehaviorPointType, Integer> {
    List<BehaviorPointType> findByBehaviorId(Integer behaviorId);
    
    @Query("SELECT bpt FROM BehaviorPointType bpt " +
           "LEFT JOIN FETCH bpt.pointType " +
           "LEFT JOIN FETCH bpt.notificationTemplate " +
           "WHERE bpt.behavior.id = :behaviorId")
    List<BehaviorPointType> findByBehaviorIdWithAssociations(@Param("behaviorId") Integer behaviorId);
    
    void deleteByBehaviorId(Integer behaviorId);
}

