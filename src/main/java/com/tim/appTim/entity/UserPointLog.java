package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_point_logs")
public class UserPointLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "behavior_id")
    private GamificationBehavior behavior;

    @Column(name = "points_diligence_earned")
    private Integer pointsDiligenceEarned = 0;

    @Column(name = "points_competence_earned")
    private Integer pointsCompetenceEarned = 0;

    @Column(name = "points_experience_earned")
    private Integer pointsExperienceEarned = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public GamificationBehavior getBehavior() { return behavior; }
    public void setBehavior(GamificationBehavior behavior) { this.behavior = behavior; }

    public Integer getPointsDiligenceEarned() { return pointsDiligenceEarned; }
    public void setPointsDiligenceEarned(Integer pointsDiligenceEarned) { this.pointsDiligenceEarned = pointsDiligenceEarned; }

    public Integer getPointsCompetenceEarned() { return pointsCompetenceEarned; }
    public void setPointsCompetenceEarned(Integer pointsCompetenceEarned) { this.pointsCompetenceEarned = pointsCompetenceEarned; }

    public Integer getPointsExperienceEarned() { return pointsExperienceEarned; }
    public void setPointsExperienceEarned(Integer pointsExperienceEarned) { this.pointsExperienceEarned = pointsExperienceEarned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

