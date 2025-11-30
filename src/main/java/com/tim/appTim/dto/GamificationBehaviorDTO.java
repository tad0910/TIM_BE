package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class GamificationBehaviorDTO {
    private Integer id;
    private Integer groupId;
    private String groupName;
    private String code;
    private String name;
    private String frequencyType;
    private Integer maxTimesPerFrequency;
    private Integer pointDiligence;
    private Integer pointCompetence;
    private Integer pointExperience;
    private LocalDateTime createdAt;

    public GamificationBehaviorDTO() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getGroupId() { return groupId; }
    public void setGroupId(Integer groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFrequencyType() { return frequencyType; }
    public void setFrequencyType(String frequencyType) { this.frequencyType = frequencyType; }

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

