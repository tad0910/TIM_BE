package com.tim.appTim.dto;

public class RankingDTO {
    private Long userId;
    private String username;
    private String displayName;
    private String profileImage;
    private Integer totalDiligenceScore;
    private Integer totalCompetenceScore;
    private Integer totalExperienceScore;
    private Integer rankPosition;
    private Long classId;
    private String className;

    public RankingDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

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

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}

