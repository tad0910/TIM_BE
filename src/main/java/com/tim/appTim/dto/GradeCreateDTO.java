package com.tim.appTim.dto;

import java.math.BigDecimal;

public class GradeCreateDTO {

    private Long studentId;
    private Long classModuleId;
    private String componentName;
    private BigDecimal score;
    private BigDecimal maxScore;
    private BigDecimal weightPercent;

    public Long getStudentId() {
        return studentId;
    }
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    public Long getClassModuleId() {
        return classModuleId;
    }
    public void setClassModuleId(Long classModuleId) {
        this.classModuleId = classModuleId;
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
}