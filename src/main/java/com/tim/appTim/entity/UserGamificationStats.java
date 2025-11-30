package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_gamification_stats")
public class UserGamificationStats {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_diligence")
    private Integer totalDiligence = 0;

    @Column(name = "total_competence")
    private Integer totalCompetence = 0;

    @Column(name = "total_experience")
    private Integer totalExperience = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

