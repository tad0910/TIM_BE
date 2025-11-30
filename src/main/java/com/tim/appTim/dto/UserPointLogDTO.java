package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class UserPointLogDTO {
    private Long id;
    private Long userId;
    private Integer behaviorId;
    private String behaviorCode;
    private String behaviorName;
    private Integer pointsDiligenceEarned;
    private Integer pointsCompetenceEarned;
    private Integer pointsExperienceEarned;
    private LocalDateTime createdAt;

    public UserPointLogDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getBehaviorId() { return behaviorId; }
    public void setBehaviorId(Integer behaviorId) { this.behaviorId = behaviorId; }

    public String getBehaviorCode() { return behaviorCode; }
    public void setBehaviorCode(String behaviorCode) { this.behaviorCode = behaviorCode; }

    public String getBehaviorName() { return behaviorName; }
    public void setBehaviorName(String behaviorName) { this.behaviorName = behaviorName; }

    public Integer getPointsDiligenceEarned() { return pointsDiligenceEarned; }
    public void setPointsDiligenceEarned(Integer pointsDiligenceEarned) { this.pointsDiligenceEarned = pointsDiligenceEarned; }

    public Integer getPointsCompetenceEarned() { return pointsCompetenceEarned; }
    public void setPointsCompetenceEarned(Integer pointsCompetenceEarned) { this.pointsCompetenceEarned = pointsCompetenceEarned; }

    public Integer getPointsExperienceEarned() { return pointsExperienceEarned; }
    public void setPointsExperienceEarned(Integer pointsExperienceEarned) { this.pointsExperienceEarned = pointsExperienceEarned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

