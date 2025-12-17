package com.tim.appTim.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AchievementWithStatusDTO {
    private Integer achievementId;
    private String achievementName;
    private String achievementImageUrl;
    private String description; // Có thể lấy từ level description hoặc achievement description
    
    // Thông tin về level cao nhất user đã unlock (nếu có)
    private Integer highestUnlockedLevelId;
    private String highestUnlockedLevelName;
    private LocalDateTime unlockedAt;
    private String highestUnlockedLevelImageUrl;
    
    // Thông tin về level tiếp theo user có thể unlock (nếu có)
    private Integer nextLevelId;
    private String nextLevelName;
    private Integer nextLevelMinPoints;
    private String nextLevelImageUrl;
    
    // Progress information
    private Integer currentPoints; // Điểm hiện tại của user cho loại điểm này
    private Integer minPointsRequired; // Điểm tối thiểu của level cao nhất đã unlock (hoặc next level nếu chưa unlock)
    private Integer maxPointsRequired; // Điểm tối thiểu của level cao nhất trong achievement này
    private Double progressPercentage; // Phần trăm progress (0-100)
    
    // Status
    private Boolean isUnlocked; // true nếu user đã unlock ít nhất 1 level
    private Boolean isFullyUnlocked; // true nếu user đã unlock tất cả levels
    
    // Danh sách tất cả levels của achievement này
    private List<AchievementLevelInfoDTO> levels;
    
    // Rarity (phần trăm người dùng có achievement này)
    private Double rarityPercentage;

    public AchievementWithStatusDTO() {}

    // Getters and Setters
    public Integer getAchievementId() { return achievementId; }
    public void setAchievementId(Integer achievementId) { this.achievementId = achievementId; }

    public String getAchievementName() { return achievementName; }
    public void setAchievementName(String achievementName) { this.achievementName = achievementName; }

    public String getAchievementImageUrl() { return achievementImageUrl; }
    public void setAchievementImageUrl(String achievementImageUrl) { this.achievementImageUrl = achievementImageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getHighestUnlockedLevelId() { return highestUnlockedLevelId; }
    public void setHighestUnlockedLevelId(Integer highestUnlockedLevelId) { this.highestUnlockedLevelId = highestUnlockedLevelId; }

    public String getHighestUnlockedLevelName() { return highestUnlockedLevelName; }
    public void setHighestUnlockedLevelName(String highestUnlockedLevelName) { this.highestUnlockedLevelName = highestUnlockedLevelName; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    public String getHighestUnlockedLevelImageUrl() { return highestUnlockedLevelImageUrl; }
    public void setHighestUnlockedLevelImageUrl(String highestUnlockedLevelImageUrl) { this.highestUnlockedLevelImageUrl = highestUnlockedLevelImageUrl; }

    public Integer getNextLevelId() { return nextLevelId; }
    public void setNextLevelId(Integer nextLevelId) { this.nextLevelId = nextLevelId; }

    public String getNextLevelName() { return nextLevelName; }
    public void setNextLevelName(String nextLevelName) { this.nextLevelName = nextLevelName; }

    public Integer getNextLevelMinPoints() { return nextLevelMinPoints; }
    public void setNextLevelMinPoints(Integer nextLevelMinPoints) { this.nextLevelMinPoints = nextLevelMinPoints; }

    public String getNextLevelImageUrl() { return nextLevelImageUrl; }
    public void setNextLevelImageUrl(String nextLevelImageUrl) { this.nextLevelImageUrl = nextLevelImageUrl; }

    public Integer getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(Integer currentPoints) { this.currentPoints = currentPoints; }

    public Integer getMinPointsRequired() { return minPointsRequired; }
    public void setMinPointsRequired(Integer minPointsRequired) { this.minPointsRequired = minPointsRequired; }

    public Integer getMaxPointsRequired() { return maxPointsRequired; }
    public void setMaxPointsRequired(Integer maxPointsRequired) { this.maxPointsRequired = maxPointsRequired; }

    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }

    public Boolean getIsUnlocked() { return isUnlocked; }
    public void setIsUnlocked(Boolean isUnlocked) { this.isUnlocked = isUnlocked; }

    public Boolean getIsFullyUnlocked() { return isFullyUnlocked; }
    public void setIsFullyUnlocked(Boolean isFullyUnlocked) { this.isFullyUnlocked = isFullyUnlocked; }

    public List<AchievementLevelInfoDTO> getLevels() { return levels; }
    public void setLevels(List<AchievementLevelInfoDTO> levels) { this.levels = levels; }

    public Double getRarityPercentage() { return rarityPercentage; }
    public void setRarityPercentage(Double rarityPercentage) { this.rarityPercentage = rarityPercentage; }

    // Inner DTO for level info
    public static class AchievementLevelInfoDTO {
        private Integer levelId;
        private String levelName;
        private Integer minPointsRequired;
        private String imageUrl;
        private Boolean isUnlocked;
        private LocalDateTime unlockedAt;

        public AchievementLevelInfoDTO() {}

        public Integer getLevelId() { return levelId; }
        public void setLevelId(Integer levelId) { this.levelId = levelId; }

        public String getLevelName() { return levelName; }
        public void setLevelName(String levelName) { this.levelName = levelName; }

        public Integer getMinPointsRequired() { return minPointsRequired; }
        public void setMinPointsRequired(Integer minPointsRequired) { this.minPointsRequired = minPointsRequired; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public Boolean getIsUnlocked() { return isUnlocked; }
        public void setIsUnlocked(Boolean isUnlocked) { this.isUnlocked = isUnlocked; }

        public LocalDateTime getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    }
}


