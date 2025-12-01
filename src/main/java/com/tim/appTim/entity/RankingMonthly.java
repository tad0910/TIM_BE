package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ranking_monthly")
public class RankingMonthly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear; 

    @Column(name = "total_diligence_score")
    private Integer totalDiligenceScore = 0;

    @Column(name = "total_competence_score")
    private Integer totalCompetenceScore = 0;

    @Column(name = "total_experience_score")
    private Integer totalExperienceScore = 0;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Class classEntity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public Integer getTotalDiligenceScore() { return totalDiligenceScore; }
    public void setTotalDiligenceScore(Integer totalDiligenceScore) { this.totalDiligenceScore = totalDiligenceScore; }

    public Integer getTotalCompetenceScore() { return totalCompetenceScore; }
    public void setTotalCompetenceScore(Integer totalCompetenceScore) { this.totalCompetenceScore = totalCompetenceScore; }

    public Integer getTotalExperienceScore() { return totalExperienceScore; }
    public void setTotalExperienceScore(Integer totalExperienceScore) { this.totalExperienceScore = totalExperienceScore; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Class getClassEntity() { return classEntity; }
    public void setClassEntity(Class classEntity) { this.classEntity = classEntity; }
}

