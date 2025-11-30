package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gamification_behaviors")
public class GamificationBehavior {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GamificationBehaviorGroup group;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false)
    private FrequencyType frequencyType = FrequencyType.UNLIMITED;

    @Column(name = "max_times_per_frequency")
    private Integer maxTimesPerFrequency = 1;

    @Column(name = "point_diligence")
    private Integer pointDiligence = 0;

    @Column(name = "point_competence")
    private Integer pointCompetence = 0;

    @Column(name = "point_experience")
    private Integer pointExperience = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum FrequencyType {
        UNLIMITED,
        DAILY,
        WEEKLY,
        MONTHLY,
        ONCE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GamificationBehaviorGroup getGroup() { return group; }
    public void setGroup(GamificationBehaviorGroup group) { this.group = group; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public FrequencyType getFrequencyType() { return frequencyType; }
    public void setFrequencyType(FrequencyType frequencyType) { this.frequencyType = frequencyType; }

    public Integer getMaxTimesPerFrequency() { return maxTimesPerFrequency; }
    public void setMaxTimesPerFrequency(Integer maxTimesPerFrequency) { this.maxTimesPerFrequency = maxTimesPerFrequency; }

    public Integer getPointDiligence() { return pointDiligence; }
    public void setPointDiligence(Integer pointDiligence) { this.pointDiligence = pointDiligence; }

    public Integer getPointCompetence() { return pointCompetence; }
    public void setPointCompetence(Integer pointCompetence) { this.pointCompetence = pointCompetence; }

    public Integer getPointExperience() { return pointExperience; }
    public void setPointExperience(Integer pointExperience) { this.pointExperience = pointExperience; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

