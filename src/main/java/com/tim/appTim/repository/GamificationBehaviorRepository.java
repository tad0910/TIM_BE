package com.tim.appTim.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tim.appTim.entity.GamificationBehavior;

@Repository
public interface GamificationBehaviorRepository extends JpaRepository<GamificationBehavior, Integer> {
    @EntityGraph(attributePaths = {"notificationTemplateDiligence", "notificationTemplateCompetence", "notificationTemplateExperience", "behaviorPointTypes", "behaviorPointTypes.pointType", "behaviorPointTypes.notificationTemplate"})
    Optional<GamificationBehavior> findByName(String name);
    
    @EntityGraph(attributePaths = {"notificationTemplateDiligence", "notificationTemplateCompetence", "notificationTemplateExperience", "behaviorPointTypes", "behaviorPointTypes.pointType", "behaviorPointTypes.notificationTemplate"})
    List<GamificationBehavior> findByGroupId(Integer groupId);
    
    @Override
    @EntityGraph(attributePaths = {"notificationTemplateDiligence", "notificationTemplateCompetence", "notificationTemplateExperience", "behaviorPointTypes", "behaviorPointTypes.pointType", "behaviorPointTypes.notificationTemplate"})
    List<GamificationBehavior> findAll();
    
    @Override
    @EntityGraph(attributePaths = {"notificationTemplateDiligence", "notificationTemplateCompetence", "notificationTemplateExperience", "behaviorPointTypes", "behaviorPointTypes.pointType", "behaviorPointTypes.notificationTemplate"})
    Optional<GamificationBehavior> findById(Integer id);
}

