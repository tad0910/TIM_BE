package com.tim.appTim.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GamificationBehaviorGroupDTO {
    private Integer id;
    private String name;
    private LocalDateTime createdAt;
    private List<GamificationBehaviorDTO> behaviors;

    public GamificationBehaviorGroupDTO() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<GamificationBehaviorDTO> getBehaviors() { return behaviors; }
    public void setBehaviors(List<GamificationBehaviorDTO> behaviors) { this.behaviors = behaviors; }
}

