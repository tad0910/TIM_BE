package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class UserGamificationStatsDTO {
    private Long userId;
    private Integer totalDiligence;
    private Integer totalCompetence;
    private Integer totalExperience;
    private LocalDateTime updatedAt;

    public UserGamificationStatsDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getTotalDiligence() { return totalDiligence; }
    public void setTotalDiligence(Integer totalDiligence) { this.totalDiligence = totalDiligence; }

    public Integer getTotalCompetence() { return totalCompetence; }
    public void setTotalCompetence(Integer totalCompetence) { this.totalCompetence = totalCompetence; }

    public Integer getTotalExperience() { return totalExperience; }
    public void setTotalExperience(Integer totalExperience) { this.totalExperience = totalExperience; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

