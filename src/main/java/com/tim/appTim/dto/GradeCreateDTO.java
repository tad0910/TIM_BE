package com.tim.appTim.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class GradeCreateDTO {

    @NotNull(message = "Student ID không được để trống")
    private Long studentId;

    @NotNull(message = "ClassModule ID không được để trống")
    private Long classModuleId;

    @NotNull(message = "Tên thành phần điểm không được để trống")
    private String componentName;

    @NotNull(message = "Điểm không được để trống")
    private BigDecimal score;

    @NotNull(message = "Điểm tối đa không được để trống")
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