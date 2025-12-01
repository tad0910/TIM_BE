package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class GamificationPointTypeDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer maxPoints;
    private String imageUrl;
    private Boolean isActive;
    private Boolean showOnDashboard;
    private Integer createdBy;
    private LocalDateTime createdAt;

    public GamificationPointTypeDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getMaxPoints() { return maxPoints; }
    public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getShowOnDashboard() { return showOnDashboard; }
    public void setShowOnDashboard(Boolean showOnDashboard) { this.showOnDashboard = showOnDashboard; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

