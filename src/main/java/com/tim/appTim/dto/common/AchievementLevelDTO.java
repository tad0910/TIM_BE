package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.time.LocalDateTime;

public class AchievementLevelDTO {
    private Integer id;
    private Integer achievementId;
    private String achievementName;
    private String levelName;
    private Integer requiredPointTypeId;
    private String requiredPointTypeEnum;
    private Integer minPointsRequired;
    private String imageUrl;
    private Long notificationTemplateId;
    private LocalDateTime createdAt;

    public AchievementLevelDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getAchievementId() { return achievementId; }
    public void setAchievementId(Integer achievementId) { this.achievementId = achievementId; }

    public String getAchievementName() { return achievementName; }
    public void setAchievementName(String achievementName) { this.achievementName = achievementName; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public Integer getRequiredPointTypeId() { return requiredPointTypeId; }
    public void setRequiredPointTypeId(Integer requiredPointTypeId) { this.requiredPointTypeId = requiredPointTypeId; }

    public String getRequiredPointTypeEnum() { return requiredPointTypeEnum; }
    public void setRequiredPointTypeEnum(String requiredPointTypeEnum) { this.requiredPointTypeEnum = requiredPointTypeEnum; }

    public Integer getMinPointsRequired() { return minPointsRequired; }
    public void setMinPointsRequired(Integer minPointsRequired) { this.minPointsRequired = minPointsRequired; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Long getNotificationTemplateId() { return notificationTemplateId; }
    public void setNotificationTemplateId(Long notificationTemplateId) { this.notificationTemplateId = notificationTemplateId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}






