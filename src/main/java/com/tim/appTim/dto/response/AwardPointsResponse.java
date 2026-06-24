package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.util.List;

public class AwardPointsResponse {
    private Long userId;
    private Integer pointsDiligenceEarned;
    private Integer pointsCompetenceEarned;
    private Integer pointsExperienceEarned;
    private Integer totalDiligence;
    private Integer totalCompetence;
    private Integer totalExperience;
    private List<UserAchievementDTO> newlyUnlockedAchievements;
    private String message;

    public AwardPointsResponse() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getPointsDiligenceEarned() { return pointsDiligenceEarned; }
    public void setPointsDiligenceEarned(Integer pointsDiligenceEarned) { this.pointsDiligenceEarned = pointsDiligenceEarned; }

    public Integer getPointsCompetenceEarned() { return pointsCompetenceEarned; }
    public void setPointsCompetenceEarned(Integer pointsCompetenceEarned) { this.pointsCompetenceEarned = pointsCompetenceEarned; }

    public Integer getPointsExperienceEarned() { return pointsExperienceEarned; }
    public void setPointsExperienceEarned(Integer pointsExperienceEarned) { this.pointsExperienceEarned = pointsExperienceEarned; }

    public Integer getTotalDiligence() { return totalDiligence; }
    public void setTotalDiligence(Integer totalDiligence) { this.totalDiligence = totalDiligence; }

    public Integer getTotalCompetence() { return totalCompetence; }
    public void setTotalCompetence(Integer totalCompetence) { this.totalCompetence = totalCompetence; }

    public Integer getTotalExperience() { return totalExperience; }
    public void setTotalExperience(Integer totalExperience) { this.totalExperience = totalExperience; }

    public List<UserAchievementDTO> getNewlyUnlockedAchievements() { return newlyUnlockedAchievements; }
    public void setNewlyUnlockedAchievements(List<UserAchievementDTO> newlyUnlockedAchievements) { this.newlyUnlockedAchievements = newlyUnlockedAchievements; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}






