package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gamification_achievement_levels")
public class GamificationAchievementLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private GamificationAchievement achievement;

    @Column(name = "level_name", nullable = false, length = 100)
    private String levelName;

    @Column(name = "required_point_type_id")
    private Integer requiredPointTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_point_type_enum", length = 20)
    private PointTypeEnum requiredPointTypeEnum;

    @Column(name = "min_points_required")
    private Integer minPointsRequired = 0;

    @Column(name = "image_url_levels", length = 255)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PointTypeEnum {
        DILIGENCE,
        COMPETENCE,
        EXPERIENCE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GamificationAchievement getAchievement() { return achievement; }
    public void setAchievement(GamificationAchievement achievement) { this.achievement = achievement; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public Integer getRequiredPointTypeId() { return requiredPointTypeId; }
    public void setRequiredPointTypeId(Integer requiredPointTypeId) { this.requiredPointTypeId = requiredPointTypeId; }

    public PointTypeEnum getRequiredPointTypeEnum() { return requiredPointTypeEnum; }
    public void setRequiredPointTypeEnum(PointTypeEnum requiredPointTypeEnum) { this.requiredPointTypeEnum = requiredPointTypeEnum; }

    public Integer getMinPointsRequired() { return minPointsRequired; }
    public void setMinPointsRequired(Integer minPointsRequired) { this.minPointsRequired = minPointsRequired; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

