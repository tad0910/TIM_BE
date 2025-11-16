package com.tim.appTim.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class StudentGradeDTO {

    private Long gradeId;
    private String componentName;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal weightPercent;
    private Instant lastUpdated;

    // --- Constructors ---
    public StudentGradeDTO(Long gradeId, String componentName, BigDecimal score, BigDecimal maxScore, BigDecimal weightPercent, Instant lastUpdated) {
        this.gradeId = gradeId;
        this.componentName = componentName;
        this.score = score;
        this.maxScore = maxScore;
        this.weightPercent = weightPercent;
        this.lastUpdated = lastUpdated;
    }

    public StudentGradeDTO(String componentName, BigDecimal score, BigDecimal maxScore, BigDecimal weightPercent, Instant lastUpdated) {
        this.componentName = componentName;
        this.score = score;
        this.maxScore = maxScore;
        this.weightPercent = weightPercent;
        this.lastUpdated = lastUpdated;
    }

    // --- Getters & Setters ---

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getWeightPercent() {
        return weightPercent;
    }

    public void setWeightPercent(BigDecimal weightPercent) {
        this.weightPercent = weightPercent;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
