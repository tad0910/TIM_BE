package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


public class UserRankPositionDTO {
    private Long userId;
    private String username;
    private Integer totalDiligenceScore;
    private Integer totalCompetenceScore;
    private Integer totalExperienceScore;
    private Integer rankPosition;
    private Long totalUsers;
    private String monthYear; 
    private Long classId;
    private String className;

    public UserRankPositionDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getTotalDiligenceScore() { return totalDiligenceScore; }
    public void setTotalDiligenceScore(Integer totalDiligenceScore) { this.totalDiligenceScore = totalDiligenceScore; }

    public Integer getTotalCompetenceScore() { return totalCompetenceScore; }
    public void setTotalCompetenceScore(Integer totalCompetenceScore) { this.totalCompetenceScore = totalCompetenceScore; }

    public Integer getTotalExperienceScore() { return totalExperienceScore; }
    public void setTotalExperienceScore(Integer totalExperienceScore) { this.totalExperienceScore = totalExperienceScore; }

    public Integer getRankPosition() { return rankPosition; }
    public void setRankPosition(Integer rankPosition) { this.rankPosition = rankPosition; }

    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }

    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}






